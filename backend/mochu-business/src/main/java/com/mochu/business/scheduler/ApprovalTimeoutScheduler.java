package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochu.business.entity.BizApprovalInstance;
import com.mochu.business.entity.BizApprovalRecord;
import com.mochu.business.entity.SysFlowDef;
import com.mochu.business.mapper.BizApprovalInstanceMapper;
import com.mochu.business.mapper.BizApprovalRecordMapper;
import com.mochu.business.mapper.SysFlowDefMapper;
import com.mochu.business.service.ApprovalService;
import com.mochu.system.entity.SysDept;
import com.mochu.system.entity.SysUser;
import com.mochu.system.entity.SysUserRole;
import com.mochu.system.mapper.SysDeptMapper;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.mapper.SysUserRoleMapper;
import com.mochu.system.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批超时调度器
 * 24h → 催办提醒
 * 48h → 抄送上级
 * 72h → 自动转办同角色用户
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalTimeoutScheduler {

    private final BizApprovalInstanceMapper instanceMapper;
    private final BizApprovalRecordMapper recordMapper;
    private final SysFlowDefMapper flowDefMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final TodoService todoService;
    private final ObjectMapper objectMapper;

    /**
     * 每30分钟扫描一次超时审批
     */
    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void checkTimeouts() {
        List<BizApprovalInstance> pendingInstances = instanceMapper.selectList(
                new LambdaQueryWrapper<BizApprovalInstance>()
                        .eq(BizApprovalInstance::getStatus, "pending")
                        .isNotNull(BizApprovalInstance::getDeadlineAt));

        LocalDateTime now = LocalDateTime.now();
        for (BizApprovalInstance inst : pendingInstances) {
            try {
                long hoursElapsed = Duration.between(inst.getDeadlineAt(), now).toHours();
                int currentLevel = inst.getReminderLevel() != null ? inst.getReminderLevel() : 0;

                if (hoursElapsed >= 72 && currentLevel < 3) {
                    autoTransfer(inst);
                    inst.setReminderLevel(3);
                    instanceMapper.updateById(inst);
                } else if (hoursElapsed >= 48 && currentLevel < 2) {
                    ccToSuperior(inst);
                    inst.setReminderLevel(2);
                    instanceMapper.updateById(inst);
                } else if (hoursElapsed >= 24 && currentLevel < 1) {
                    sendReminder(inst);
                    inst.setReminderLevel(1);
                    instanceMapper.updateById(inst);
                }
            } catch (Exception e) {
                log.warn("审批超时处理异常, instanceId={}: {}", inst.getId(), e.getMessage());
            }
        }
    }

    /**
     * 24h催办 — 给当前节点审批人发送催办待办
     */
    private void sendReminder(BizApprovalInstance inst) {
        SysFlowDef flowDef = flowDefMapper.selectById(inst.getFlowDefId());
        if (flowDef == null) return;
        List<ApprovalService.FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int idx = inst.getCurrentNode() - 1;
        if (idx < 0 || idx >= nodes.size()) return;

        ApprovalService.FlowNode node = nodes.get(idx);
        String title = "[催办] " + node.getNodeName() + " - " + flowDef.getFlowName();
        String content = "该审批已超过24小时未处理，请尽快审批";
        String todoBizType = "approval_" + inst.getBizType();

        List<Integer> approverIds = resolveApproverIds(node, inst);
        for (Integer uid : approverIds) {
            todoService.createTodo(uid, todoBizType, inst.getId(), title, content);
        }
        log.info("审批催办: instanceId={}, node={}", inst.getId(), node.getNodeName());
    }

    /**
     * 48h抄送上级 — 给当前审批人的部门负责人发送通知
     */
    private void ccToSuperior(BizApprovalInstance inst) {
        SysFlowDef flowDef = flowDefMapper.selectById(inst.getFlowDefId());
        if (flowDef == null) return;
        List<ApprovalService.FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int idx = inst.getCurrentNode() - 1;
        if (idx < 0 || idx >= nodes.size()) return;

        ApprovalService.FlowNode node = nodes.get(idx);
        List<Integer> approverIds = resolveApproverIds(node, inst);

        String title = "[超时抄送] " + node.getNodeName() + " - " + flowDef.getFlowName();
        String content = "该审批已超过48小时未处理，已抄送至您";
        String todoBizType = "approval_" + inst.getBizType();

        for (Integer uid : approverIds) {
            SysUser user = sysUserMapper.selectById(uid);
            if (user != null && user.getDeptId() != null) {
                SysDept dept = sysDeptMapper.selectById(user.getDeptId());
                if (dept != null && dept.getLeaderId() != null && !dept.getLeaderId().equals(uid)) {
                    todoService.createTodo(dept.getLeaderId(), todoBizType, inst.getId(), title, content);
                }
            }
        }
        log.info("审批超时抄送上级: instanceId={}", inst.getId());
    }

    /**
     * 72h自动转办 — role节点转给同角色其他用户; user/dept_leader节点抄送上级并记录
     */
    private void autoTransfer(BizApprovalInstance inst) {
        SysFlowDef flowDef = flowDefMapper.selectById(inst.getFlowDefId());
        if (flowDef == null) return;
        List<ApprovalService.FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int idx = inst.getCurrentNode() - 1;
        if (idx < 0 || idx >= nodes.size()) return;

        ApprovalService.FlowNode node = nodes.get(idx);
        Integer targetUser = null;

        if ("role".equals(node.getApproverType())) {
            // 角色节点：转给同角色的其他用户
            List<Integer> allRoleUsers = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, node.getApproverId())
            ).stream().map(SysUserRole::getUserId).collect(Collectors.toList());

            List<Integer> currentApprovers = resolveApproverIds(node, inst);
            for (Integer uid : allRoleUsers) {
                if (!currentApprovers.contains(uid)) {
                    targetUser = uid;
                    break;
                }
            }
            if (targetUser == null && !allRoleUsers.isEmpty()) {
                targetUser = allRoleUsers.get(0);
            }
        } else if ("user".equals(node.getApproverType())) {
            // 指定用户节点：转给该用户的部门负责人
            SysUser user = sysUserMapper.selectById(node.getApproverId());
            if (user != null && user.getDeptId() != null) {
                SysDept dept = sysDeptMapper.selectById(user.getDeptId());
                if (dept != null && dept.getLeaderId() != null && !dept.getLeaderId().equals(node.getApproverId())) {
                    targetUser = dept.getLeaderId();
                }
            }
        } else if ("dept_leader".equals(node.getApproverType())) {
            // 部门负责人节点：转给上级部门的负责人
            SysUser initiator = sysUserMapper.selectById(inst.getInitiatorId());
            if (initiator != null && initiator.getDeptId() != null) {
                SysDept dept = sysDeptMapper.selectById(initiator.getDeptId());
                if (dept != null && dept.getParentId() != null && dept.getParentId() > 0) {
                    SysDept parentDept = sysDeptMapper.selectById(dept.getParentId());
                    if (parentDept != null && parentDept.getLeaderId() != null) {
                        targetUser = parentDept.getLeaderId();
                    }
                }
            }
        }

        if (targetUser == null) {
            log.warn("审批自动转办: 无法找到目标用户, instanceId={}, nodeType={}", inst.getId(), node.getApproverType());
            return;
        }

        // 写入自动转办记录
        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(inst.getId());
        record.setNodeOrder(inst.getCurrentNode());
        record.setNodeName(node.getNodeName());
        record.setApproverId(targetUser);
        record.setAction("auto_transfer");
        record.setOpinion("系统自动转办(超时72小时)");
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 重置超时计时和提醒级别（让新审批人也能收到催办）
        inst.setDeadlineAt(LocalDateTime.now());
        inst.setReminderLevel(0);
        inst.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(inst);

        // 标记旧待办、创建新待办
        String todoBizType = "approval_" + inst.getBizType();
        todoService.markDoneByBiz(todoBizType, inst.getId());
        String title = "[系统转办] " + node.getNodeName() + " - " + flowDef.getFlowName();
        todoService.createTodo(targetUser, todoBizType, inst.getId(), title, "原审批人超时72小时，系统自动转办");

        log.info("审批自动转办: instanceId={}, targetUser={}, nodeType={}", inst.getId(), targetUser, node.getApproverType());
    }

    /**
     * 解析节点的审批人ID列表
     */
    private List<Integer> resolveApproverIds(ApprovalService.FlowNode node, BizApprovalInstance inst) {
        if ("user".equals(node.getApproverType())) {
            return List.of(node.getApproverId());
        } else if ("role".equals(node.getApproverType())) {
            return sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, node.getApproverId())
            ).stream().map(SysUserRole::getUserId).collect(Collectors.toList());
        } else if ("dept_leader".equals(node.getApproverType())) {
            SysUser initiator = sysUserMapper.selectById(inst.getInitiatorId());
            if (initiator != null && initiator.getDeptId() != null) {
                SysDept dept = sysDeptMapper.selectById(initiator.getDeptId());
                if (dept != null && dept.getLeaderId() != null) {
                    return List.of(dept.getLeaderId());
                }
            }
        }
        return List.of();
    }

    private List<ApprovalService.FlowNode> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(nodesJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
