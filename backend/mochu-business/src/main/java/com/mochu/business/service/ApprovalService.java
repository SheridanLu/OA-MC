package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochu.business.dto.FlowDefDTO;
import com.mochu.business.entity.*;
import com.mochu.business.event.ApprovalCompletedEvent;
import com.mochu.business.event.ApprovalSubmittedEvent;
import com.mochu.business.mapper.*;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.system.entity.SysDept;
import com.mochu.system.entity.SysUser;
import com.mochu.system.entity.SysUserRole;
import com.mochu.system.mapper.SysDeptMapper;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.mapper.SysUserRoleMapper;
import com.mochu.system.service.TodoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批服务 — 对照 V3.2 审批流程引擎
 * 支持：审批/驳回/撤回/转办/加签/阅办/阅知/条件分支/部门主管/超时升级
 */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final SysFlowDefMapper flowDefMapper;
    private final BizApprovalInstanceMapper instanceMapper;
    private final BizApprovalRecordMapper recordMapper;
    private final BizApprovalCosignMapper cosignMapper;
    private final BizApprovalCcMapper ccMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final TodoService todoService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ===================== 流程定义 CRUD =====================

    public PageResult<SysFlowDef> listFlowDefs(String bizType, Integer status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<SysFlowDef> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<SysFlowDef> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(SysFlowDef::getBizType, bizType);
        }
        if (status != null) {
            wrapper.eq(SysFlowDef::getStatus, status);
        }
        wrapper.orderByDesc(SysFlowDef::getId);

        flowDefMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public SysFlowDef getFlowDefById(Integer id) {
        return flowDefMapper.selectById(id);
    }

    public void createFlowDef(FlowDefDTO dto) {
        SysFlowDef entity = new SysFlowDef();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getVersion() == null) entity.setVersion(1);
        flowDefMapper.insert(entity);
    }

    public void updateFlowDef(Integer id, FlowDefDTO dto) {
        SysFlowDef entity = flowDefMapper.selectById(id);
        if (entity == null) throw new BusinessException("流程定义不存在");
        entity.setFlowName(dto.getFlowName());
        entity.setNodesJson(dto.getNodesJson());
        entity.setConditionJson(dto.getConditionJson());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getVersion() != null) entity.setVersion(dto.getVersion());
        flowDefMapper.updateById(entity);
    }

    public void deleteFlowDef(Integer id) {
        flowDefMapper.deleteById(id);
    }

    // ===================== 审批操作 =====================

    /**
     * 检查指定业务类型是否已配置审批流程
     */
    public boolean hasFlowDef(String bizType) {
        Long count = flowDefMapper.selectCount(
                new LambdaQueryWrapper<SysFlowDef>()
                        .eq(SysFlowDef::getBizType, bizType)
                        .eq(SysFlowDef::getStatus, 1));
        return count > 0;
    }

    /**
     * 提交审批 — 创建审批实例（支持条件分支）
     */
    @Transactional
    public void submitForApproval(String bizType, Integer bizId, Integer initiatorId, Map<String, Object> bizContext) {
        // 查找该业务类型的启用流程定义
        SysFlowDef flowDef = resolveFlowDef(bizType, bizContext);
        if (flowDef == null) {
            throw new BusinessException("该业务类型未配置审批流程");
        }

        // 检查是否已有进行中的审批
        Long existCount = instanceMapper.selectCount(
                new LambdaQueryWrapper<BizApprovalInstance>()
                        .eq(BizApprovalInstance::getBizType, bizType)
                        .eq(BizApprovalInstance::getBizId, bizId)
                        .eq(BizApprovalInstance::getStatus, "pending"));
        if (existCount > 0) {
            throw new BusinessException("该单据已有进行中的审批");
        }

        BizApprovalInstance instance = new BizApprovalInstance();
        instance.setBizType(bizType);
        instance.setBizId(bizId);
        instance.setFlowDefId(flowDef.getId());
        instance.setCurrentNode(1);
        instance.setStatus("pending");
        instance.setInitiatorId(initiatorId);
        instance.setCreatedAt(LocalDateTime.now());
        // deadline_at/reminder_level 由超时调度器使用，初始可不设置以兼容未迁移的数据库
        try {
            instance.setDeadlineAt(LocalDateTime.now());
            instance.setReminderLevel(0);
        } catch (Exception ignored) {
            // 字段不存在时跳过
        }
        instanceMapper.insert(instance);

        // 为第一个节点的审批人创建待办
        List<FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        if (!nodes.isEmpty()) {
            createTodoForNode(nodes.get(0), instance);
        }

        // 回写业务单据状态为 pending
        eventPublisher.publishEvent(new ApprovalSubmittedEvent(this, bizType, bizId));
    }

    /** 兼容无条件调用 */
    @Transactional
    public void submitForApproval(String bizType, Integer bizId, Integer initiatorId) {
        submitForApproval(bizType, bizId, initiatorId, null);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(Integer instanceId, Integer approverId, String opinion) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");
        if (!"pending".equals(instance.getStatus())) throw new BusinessException("该审批已结束");

        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        List<FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int currentIdx = instance.getCurrentNode() - 1;
        if (currentIdx < 0 || currentIdx >= nodes.size()) {
            throw new BusinessException("审批节点配置异常");
        }
        FlowNode currentNode = nodes.get(currentIdx);
        checkApproverPermission(currentNode, approverId, instance);

        // 检查是否有未完成的会签
        Long pendingCosigns = cosignMapper.selectCount(
                new LambdaQueryWrapper<BizApprovalCosign>()
                        .eq(BizApprovalCosign::getInstanceId, instanceId)
                        .eq(BizApprovalCosign::getNodeOrder, instance.getCurrentNode())
                        .eq(BizApprovalCosign::getStatus, "pending"));
        if (pendingCosigns > 0) {
            throw new BusinessException("尚有未完成的会签，请等待会签人完成后再审批");
        }

        // 写入审批记录
        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName(currentNode.getNodeName());
        record.setApproverId(approverId);
        record.setAction("approve");
        record.setOpinion(opinion);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 标记当前节点相关待办为已处理
        todoService.markDoneByBiz("approval_" + instance.getBizType(), instanceId);

        // 判断是否为最后一个节点
        if (instance.getCurrentNode() >= nodes.size()) {
            instance.setStatus("approved");
            instance.setDeadlineAt(null);
            eventPublisher.publishEvent(new ApprovalCompletedEvent(this, instance.getBizType(), instance.getBizId(), "approved"));
        } else {
            instance.setCurrentNode(instance.getCurrentNode() + 1);
            instance.setDeadlineAt(LocalDateTime.now());
            instance.setReminderLevel(0);
            // 为下一个节点创建待办
            FlowNode nextNode = nodes.get(instance.getCurrentNode() - 1);
            createTodoForNode(nextNode, instance);
        }
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);
    }

    /**
     * 审批驳回
     */
    @Transactional
    public void reject(Integer instanceId, Integer approverId, String opinion) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");
        if (!"pending".equals(instance.getStatus())) throw new BusinessException("该审批已结束");

        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        List<FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int currentIdx = instance.getCurrentNode() - 1;
        FlowNode currentNode = nodes.get(currentIdx);
        checkApproverPermission(currentNode, approverId, instance);

        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName(currentNode.getNodeName());
        record.setApproverId(approverId);
        record.setAction("reject");
        record.setOpinion(opinion);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        instance.setStatus("rejected");
        instance.setDeadlineAt(null);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);

        // 标记所有待办为已处理
        todoService.markDoneByBiz("approval_" + instance.getBizType(), instanceId);

        eventPublisher.publishEvent(new ApprovalCompletedEvent(this, instance.getBizType(), instance.getBizId(), "rejected"));
    }

    /**
     * 撤回 — 发起人在当前节点尚未操作时可撤回
     */
    @Transactional
    public void withdraw(Integer instanceId, Integer userId) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");
        if (!"pending".equals(instance.getStatus())) throw new BusinessException("该审批已结束");
        if (!userId.equals(instance.getInitiatorId())) throw new BusinessException("只有发起人可以撤回");

        // 检查当前节点是否有人已操作
        Long actedCount = recordMapper.selectCount(
                new LambdaQueryWrapper<BizApprovalRecord>()
                        .eq(BizApprovalRecord::getInstanceId, instanceId)
                        .eq(BizApprovalRecord::getNodeOrder, instance.getCurrentNode()));
        if (actedCount > 0) {
            throw new BusinessException("当前节点已有审批操作，无法撤回");
        }

        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName("撤回");
        record.setApproverId(userId);
        record.setAction("cancel");
        record.setOpinion("发起人撤回");
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        instance.setStatus("cancelled");
        instance.setDeadlineAt(null);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);

        todoService.markDoneByBiz("approval_" + instance.getBizType(), instanceId);
    }

    /**
     * 转办 — 当前审批人将任务转给指定用户
     */
    @Transactional
    public void transfer(Integer instanceId, Integer currentUserId, Integer targetUserId, String opinion) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");
        if (!"pending".equals(instance.getStatus())) throw new BusinessException("该审批已结束");

        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        List<FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int currentIdx = instance.getCurrentNode() - 1;
        FlowNode currentNode = nodes.get(currentIdx);
        checkApproverPermission(currentNode, currentUserId, instance);

        // 写入转办记录
        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName(currentNode.getNodeName());
        record.setApproverId(targetUserId);
        record.setAction("delegate");
        record.setOpinion(opinion);
        record.setDelegateFromId(currentUserId);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 重置超时计时
        instance.setDeadlineAt(LocalDateTime.now());
        instance.setReminderLevel(0);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);

        // 标记旧待办、创建新待办
        todoService.markDoneByUserAndBiz(currentUserId, "approval_" + instance.getBizType(), instanceId);
        String title = "[转办] " + currentNode.getNodeName() + " - " + (flowDef.getFlowName());
        todoService.createTodo(targetUserId, "approval_" + instance.getBizType(), instanceId, title, "由" + resolveUserName(currentUserId) + "转办");
    }

    /**
     * 加签 — 当前审批人添加会签人
     */
    @Transactional
    public void addCosigner(Integer instanceId, Integer currentUserId, Integer cosignerId, String opinion) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");
        if (!"pending".equals(instance.getStatus())) throw new BusinessException("该审批已结束");

        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        List<FlowNode> nodes = parseNodes(flowDef.getNodesJson());
        int currentIdx = instance.getCurrentNode() - 1;
        FlowNode currentNode = nodes.get(currentIdx);
        checkApproverPermission(currentNode, currentUserId, instance);

        // 创建会签记录
        BizApprovalCosign cosign = new BizApprovalCosign();
        cosign.setInstanceId(instanceId);
        cosign.setNodeOrder(instance.getCurrentNode());
        cosign.setCosignerId(cosignerId);
        cosign.setStatus("pending");
        cosign.setCreatedAt(LocalDateTime.now());
        cosignMapper.insert(cosign);

        // 写入审批记录
        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName(currentNode.getNodeName());
        record.setApproverId(currentUserId);
        record.setAction("cosign_add");
        record.setOpinion(opinion != null ? opinion : "加签给" + resolveUserName(cosignerId));
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 创建待办
        String title = "[会签] " + currentNode.getNodeName() + " - " + flowDef.getFlowName();
        todoService.createTodo(cosignerId, "approval_" + instance.getBizType(), instanceId, title, "由" + resolveUserName(currentUserId) + "发起会签");
    }

    /**
     * 会签审批 — 会签人完成签署
     */
    @Transactional
    public void approveCosign(Integer cosignId, Integer userId, String opinion) {
        BizApprovalCosign cosign = cosignMapper.selectById(cosignId);
        if (cosign == null) throw new BusinessException("会签记录不存在");
        if (!"pending".equals(cosign.getStatus())) throw new BusinessException("该会签已完成");
        if (!userId.equals(cosign.getCosignerId())) throw new BusinessException("您不是该会签的审批人");

        cosign.setStatus("approved");
        cosign.setOpinion(opinion);
        cosign.setCompletedAt(LocalDateTime.now());
        cosignMapper.updateById(cosign);

        // 写入审批记录
        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(cosign.getInstanceId());
        record.setNodeOrder(cosign.getNodeOrder());
        record.setNodeName("会签");
        record.setApproverId(userId);
        record.setAction("cosign_approve");
        record.setOpinion(opinion);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 标记会签人待办
        BizApprovalInstance instance = instanceMapper.selectById(cosign.getInstanceId());
        if (instance != null) {
            todoService.markDoneByUserAndBiz(userId, "approval_" + instance.getBizType(), cosign.getInstanceId());
        }
    }

    /**
     * 阅办 — 发送给指定用户处理
     */
    @Transactional
    public void sendReadHandle(Integer instanceId, Integer currentUserId, Integer targetUserId) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");

        BizApprovalCc cc = new BizApprovalCc();
        cc.setInstanceId(instanceId);
        cc.setUserId(targetUserId);
        cc.setCcType("read_handle");
        cc.setIsRead(0);
        cc.setIsHandled(0);
        cc.setCreatedAt(LocalDateTime.now());
        ccMapper.insert(cc);

        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        String title = "[阅办] " + (flowDef != null ? flowDef.getFlowName() : instance.getBizType());
        todoService.createTodo(targetUserId, "approval_" + instance.getBizType(), instanceId, title, "由" + resolveUserName(currentUserId) + "发送阅办");

        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName("阅办");
        record.setApproverId(currentUserId);
        record.setAction("read");
        record.setOpinion("发送阅办给" + resolveUserName(targetUserId));
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    /**
     * 阅知/抄送 — 发送给多个用户知悉
     */
    @Transactional
    public void sendCc(Integer instanceId, Integer currentUserId, List<Integer> userIds) {
        BizApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) throw new BusinessException("审批实例不存在");
        if (userIds == null || userIds.isEmpty()) throw new BusinessException("请选择抄送人");

        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        for (Integer uid : userIds) {
            BizApprovalCc cc = new BizApprovalCc();
            cc.setInstanceId(instanceId);
            cc.setUserId(uid);
            cc.setCcType("read_ack");
            cc.setIsRead(0);
            cc.setIsHandled(0);
            cc.setCreatedAt(LocalDateTime.now());
            ccMapper.insert(cc);

            String title = "[阅知] " + (flowDef != null ? flowDef.getFlowName() : instance.getBizType());
            todoService.createTodo(uid, "approval_" + instance.getBizType(), instanceId, title, "由" + resolveUserName(currentUserId) + "发送阅知");
        }

        BizApprovalRecord record = new BizApprovalRecord();
        record.setInstanceId(instanceId);
        record.setNodeOrder(instance.getCurrentNode());
        record.setNodeName("阅知");
        record.setApproverId(currentUserId);
        record.setAction("cc");
        record.setOpinion("抄送" + userIds.size() + "人");
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    /**
     * 标记阅办已处理
     */
    @Transactional
    public void markHandled(Integer ccId, Integer userId) {
        BizApprovalCc cc = ccMapper.selectById(ccId);
        if (cc == null) throw new BusinessException("记录不存在");
        if (!userId.equals(cc.getUserId())) throw new BusinessException("只能处理自己的阅办");
        cc.setIsRead(1);
        cc.setIsHandled(1);
        cc.setHandledAt(LocalDateTime.now());
        ccMapper.updateById(cc);

        BizApprovalInstance instance = instanceMapper.selectById(cc.getInstanceId());
        if (instance != null) {
            todoService.markDoneByUserAndBiz(userId, "approval_" + instance.getBizType(), cc.getInstanceId());
        }
    }

    // ===================== 查询 =====================

    /**
     * 我的待审批列表
     */
    public PageResult<Map<String, Object>> getMyPending(Integer userId, Integer page, Integer size) {
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        // 查询用户角色
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        Set<Integer> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());

        // 查询用户部门(判断是否为部门负责人)
        SysUser currentUser = sysUserMapper.selectById(userId);

        // 查询所有 pending 的实例
        List<BizApprovalInstance> allPending = instanceMapper.selectList(
                new LambdaQueryWrapper<BizApprovalInstance>()
                        .eq(BizApprovalInstance::getStatus, "pending")
                        .orderByDesc(BizApprovalInstance::getCreatedAt));

        // 缓存 FlowDef 避免 N+1 查询（同一个流程定义只查一次数据库）
        Map<Integer, SysFlowDef> flowDefCache = new HashMap<>();

        List<Map<String, Object>> matched = new ArrayList<>();
        for (BizApprovalInstance inst : allPending) {
            SysFlowDef flowDef = flowDefCache.computeIfAbsent(inst.getFlowDefId(),
                    id -> flowDefMapper.selectById(id));
            if (flowDef == null) continue;
            List<FlowNode> nodes = parseNodes(flowDef.getNodesJson());
            int idx = inst.getCurrentNode() - 1;
            if (idx < 0 || idx >= nodes.size()) continue;
            FlowNode node = nodes.get(idx);

            boolean isApprover = isUserApproverForNode(node, userId, roleIds, inst, currentUser);
            if (!isApprover) continue;

            Map<String, Object> item = buildInstanceMap(inst, flowDef, node);
            matched.add(item);
        }

        // 手动分页
        int total = matched.size();
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);
        List<Map<String, Object>> result = matched.subList(from, to);
        return new PageResult<>(result, total, p, s);
    }

    /**
     * 我发起的审批列表
     */
    public PageResult<Map<String, Object>> getMyInitiated(Integer userId, Integer page, Integer size) {
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizApprovalInstance> pageParam = new Page<>(p, s);
        instanceMapper.selectPage(pageParam, new LambdaQueryWrapper<BizApprovalInstance>()
                .eq(BizApprovalInstance::getInitiatorId, userId)
                .orderByDesc(BizApprovalInstance::getCreatedAt));

        List<Map<String, Object>> result = new ArrayList<>();
        for (BizApprovalInstance inst : pageParam.getRecords()) {
            SysFlowDef flowDef = flowDefMapper.selectById(inst.getFlowDefId());
            List<FlowNode> nodes = parseNodes(flowDef != null ? flowDef.getNodesJson() : "[]");
            int idx = inst.getCurrentNode() - 1;
            FlowNode node = (idx >= 0 && idx < nodes.size()) ? nodes.get(idx) : null;
            Map<String, Object> item = buildInstanceMap(inst, flowDef, node);
            result.add(item);
        }
        return new PageResult<>(result, pageParam.getTotal(), p, s);
    }

    /**
     * 审批实例详情（含记录 + 会签 + 抄送）
     */
    public Map<String, Object> getInstanceDetail(Integer instanceId) {
        BizApprovalInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new BusinessException("审批实例不存在");

        SysFlowDef flowDef = flowDefMapper.selectById(inst.getFlowDefId());
        List<FlowNode> nodes = parseNodes(flowDef != null ? flowDef.getNodesJson() : "[]");
        int idx = inst.getCurrentNode() - 1;
        FlowNode node = (idx >= 0 && idx < nodes.size()) ? nodes.get(idx) : null;

        Map<String, Object> result = buildInstanceMap(inst, flowDef, node);

        // 审批记录
        List<BizApprovalRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<BizApprovalRecord>()
                        .eq(BizApprovalRecord::getInstanceId, instanceId)
                        .orderByAsc(BizApprovalRecord::getNodeOrder)
                        .orderByAsc(BizApprovalRecord::getId));
        List<Map<String, Object>> recordList = new ArrayList<>();
        for (BizApprovalRecord r : records) {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.getId());
            rm.put("node_order", r.getNodeOrder());
            rm.put("node_name", r.getNodeName());
            rm.put("approver_id", r.getApproverId());
            rm.put("approver_name", resolveUserName(r.getApproverId()));
            rm.put("action", r.getAction());
            rm.put("opinion", r.getOpinion());
            rm.put("delegate_from_id", r.getDelegateFromId());
            rm.put("delegate_from_name", r.getDelegateFromId() != null ? resolveUserName(r.getDelegateFromId()) : null);
            rm.put("created_at", r.getCreatedAt());
            recordList.add(rm);
        }
        result.put("records", recordList);

        // 流程节点列表
        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (FlowNode n : nodes) {
            Map<String, Object> nm = new LinkedHashMap<>();
            nm.put("node_order", n.getNodeOrder());
            nm.put("node_name", n.getNodeName());
            nm.put("approver_type", n.getApproverType());
            nm.put("approver_id", n.getApproverId());
            nodeList.add(nm);
        }
        result.put("nodes", nodeList);

        // 会签记录
        List<BizApprovalCosign> cosigns = cosignMapper.selectList(
                new LambdaQueryWrapper<BizApprovalCosign>()
                        .eq(BizApprovalCosign::getInstanceId, instanceId)
                        .orderByAsc(BizApprovalCosign::getNodeOrder));
        List<Map<String, Object>> cosignList = new ArrayList<>();
        for (BizApprovalCosign c : cosigns) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("node_order", c.getNodeOrder());
            cm.put("cosigner_id", c.getCosignerId());
            cm.put("cosigner_name", resolveUserName(c.getCosignerId()));
            cm.put("status", c.getStatus());
            cm.put("opinion", c.getOpinion());
            cm.put("created_at", c.getCreatedAt());
            cm.put("completed_at", c.getCompletedAt());
            cosignList.add(cm);
        }
        result.put("cosigns", cosignList);

        // 抄送/阅办记录
        List<BizApprovalCc> ccs = ccMapper.selectList(
                new LambdaQueryWrapper<BizApprovalCc>()
                        .eq(BizApprovalCc::getInstanceId, instanceId)
                        .orderByAsc(BizApprovalCc::getCreatedAt));
        List<Map<String, Object>> ccList = new ArrayList<>();
        for (BizApprovalCc cc : ccs) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", cc.getId());
            cm.put("user_id", cc.getUserId());
            cm.put("user_name", resolveUserName(cc.getUserId()));
            cm.put("cc_type", cc.getCcType());
            cm.put("is_read", cc.getIsRead());
            cm.put("is_handled", cc.getIsHandled());
            cm.put("created_at", cc.getCreatedAt());
            cm.put("handled_at", cc.getHandledAt());
            ccList.add(cm);
        }
        result.put("cc_list", ccList);

        return result;
    }

    // ===================== 内部方法 =====================

    private Map<String, Object> buildInstanceMap(BizApprovalInstance inst, SysFlowDef flowDef, FlowNode currentNode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", inst.getId());
        item.put("biz_type", inst.getBizType());
        item.put("biz_id", inst.getBizId());
        item.put("flow_name", flowDef != null ? flowDef.getFlowName() : "");
        item.put("current_node", inst.getCurrentNode());
        item.put("current_node_name", currentNode != null ? currentNode.getNodeName() : "");
        item.put("status", inst.getStatus());
        item.put("initiator_id", inst.getInitiatorId());
        item.put("initiator_name", resolveUserName(inst.getInitiatorId()));
        item.put("created_at", inst.getCreatedAt());
        item.put("updated_at", inst.getUpdatedAt());
        return item;
    }

    private String resolveUserName(Integer userId) {
        if (userId == null) return "";
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getRealName() : "";
    }

    /**
     * 选择流程定义 — 支持条件分支
     */
    private SysFlowDef resolveFlowDef(String bizType, Map<String, Object> bizContext) {
        List<SysFlowDef> defs = flowDefMapper.selectList(
                new LambdaQueryWrapper<SysFlowDef>()
                        .eq(SysFlowDef::getBizType, bizType)
                        .eq(SysFlowDef::getStatus, 1)
                        .orderByDesc(SysFlowDef::getVersion));
        if (defs.isEmpty()) return null;

        // 先尝试匹配有条件的流程
        if (bizContext != null && !bizContext.isEmpty()) {
            for (SysFlowDef def : defs) {
                if (def.getConditionJson() != null && !def.getConditionJson().isBlank()) {
                    if (evaluateCondition(def.getConditionJson(), bizContext)) {
                        return def;
                    }
                }
            }
        }

        // 返回无条件的默认流程
        for (SysFlowDef def : defs) {
            if (def.getConditionJson() == null || def.getConditionJson().isBlank()) {
                return def;
            }
        }
        return defs.get(defs.size() - 1);
    }

    /**
     * 条件表达式求值: {"field":"X","op":"eq","value":V,"and":{...},"or":{...}}
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateCondition(String conditionJson, Map<String, Object> bizContext) {
        try {
            Map<String, Object> cond = objectMapper.readValue(conditionJson, new TypeReference<>() {});
            return evalNode(cond, bizContext);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean evalNode(Map<String, Object> cond, Map<String, Object> ctx) {
        String field = (String) cond.get("field");
        String op = (String) cond.get("op");
        Object expected = cond.get("value");
        Object actual = ctx.get(field);

        boolean result = compareValues(actual, expected, op);

        // AND 组合
        if (cond.containsKey("and")) {
            Map<String, Object> andCond = (Map<String, Object>) cond.get("and");
            result = result && evalNode(andCond, ctx);
        }
        // OR 组合
        if (cond.containsKey("or")) {
            Map<String, Object> orCond = (Map<String, Object>) cond.get("or");
            result = result || evalNode(orCond, ctx);
        }
        return result;
    }

    @SuppressWarnings("all")
    private boolean compareValues(Object actual, Object expected, String op) {
        if (actual == null) return false;
        if ("eq".equals(op)) return actual.toString().equals(expected.toString());
        if ("ne".equals(op)) return !actual.toString().equals(expected.toString());
        try {
            double a = Double.parseDouble(actual.toString());
            double e = Double.parseDouble(expected.toString());
            return switch (op) {
                case "gt" -> a > e;
                case "gte" -> a >= e;
                case "lt" -> a < e;
                case "lte" -> a <= e;
                default -> false;
            };
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 检查审批人权限 — 支持 user / role / dept_leader
     */
    private void checkApproverPermission(FlowNode node, Integer approverId, BizApprovalInstance instance) {
        if ("user".equals(node.getApproverType())) {
            if (!approverId.equals(node.getApproverId())) {
                throw new BusinessException("您不是当前节点的审批人");
            }
        } else if ("role".equals(node.getApproverType())) {
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, approverId)
                            .eq(SysUserRole::getRoleId, node.getApproverId()));
            if (userRoles.isEmpty()) {
                throw new BusinessException("您不是当前节点的审批人");
            }
        } else if ("dept_leader".equals(node.getApproverType())) {
            SysUser initiator = sysUserMapper.selectById(instance.getInitiatorId());
            if (initiator == null || initiator.getDeptId() == null) {
                throw new BusinessException("无法确定发起人的部门");
            }
            SysDept dept = sysDeptMapper.selectById(initiator.getDeptId());
            if (dept == null || !approverId.equals(dept.getLeaderId())) {
                throw new BusinessException("您不是当前节点的审批人");
            }
        }
    }

    /**
     * 判断用户是否为某节点的审批人
     */
    private boolean isUserApproverForNode(FlowNode node, Integer userId, Set<Integer> roleIds,
                                          BizApprovalInstance instance, SysUser currentUser) {
        if ("user".equals(node.getApproverType())) {
            return userId.equals(node.getApproverId());
        } else if ("role".equals(node.getApproverType())) {
            return roleIds.contains(node.getApproverId());
        } else if ("dept_leader".equals(node.getApproverType())) {
            SysUser initiator = sysUserMapper.selectById(instance.getInitiatorId());
            if (initiator == null || initiator.getDeptId() == null) return false;
            SysDept dept = sysDeptMapper.selectById(initiator.getDeptId());
            return dept != null && userId.equals(dept.getLeaderId());
        }
        return false;
    }

    /**
     * 为审批节点创建待办
     */
    private void createTodoForNode(FlowNode node, BizApprovalInstance instance) {
        SysFlowDef flowDef = flowDefMapper.selectById(instance.getFlowDefId());
        String flowName = flowDef != null ? flowDef.getFlowName() : instance.getBizType();
        String title = "[审批] " + node.getNodeName() + " - " + flowName;
        String content = "业务类型: " + instance.getBizType() + ", 单据ID: " + instance.getBizId();
        String todoBizType = "approval_" + instance.getBizType();

        if ("user".equals(node.getApproverType())) {
            todoService.createTodo(node.getApproverId(), todoBizType, instance.getId(), title, content);
        } else if ("role".equals(node.getApproverType())) {
            List<Integer> userIds = findUsersByRoleId(node.getApproverId());
            for (Integer uid : userIds) {
                todoService.createTodo(uid, todoBizType, instance.getId(), title, content);
            }
        } else if ("dept_leader".equals(node.getApproverType())) {
            SysUser initiator = sysUserMapper.selectById(instance.getInitiatorId());
            if (initiator != null && initiator.getDeptId() != null) {
                SysDept dept = sysDeptMapper.selectById(initiator.getDeptId());
                if (dept != null && dept.getLeaderId() != null) {
                    todoService.createTodo(dept.getLeaderId(), todoBizType, instance.getId(), title, content);
                }
            }
        }
    }

    /**
     * 查找某角色的所有用户
     */
    public List<Integer> findUsersByRoleId(Integer roleId) {
        return sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
        ).stream().map(SysUserRole::getUserId).collect(Collectors.toList());
    }

    private List<FlowNode> parseNodes(String nodesJson) {
        if (nodesJson == null || nodesJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(nodesJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * 审批流程节点
     */
    @Data
    public static class FlowNode {
        private Integer nodeOrder;
        private String nodeName;
        /** user / role / dept_leader */
        private String approverType;
        private Integer approverId;
    }
}
