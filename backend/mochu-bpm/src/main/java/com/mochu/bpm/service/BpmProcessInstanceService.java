package com.mochu.bpm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochu.bpm.dto.StartProcessDTO;
import com.mochu.bpm.entity.BpmOaRule;
import com.mochu.bpm.entity.BpmTaskExt;
import com.mochu.bpm.mapper.BpmOaRuleMapper;
import com.mochu.bpm.mapper.BpmTaskExtMapper;
import com.mochu.bpm.vo.ProcessInstanceVO;
import com.mochu.bpm.vo.TaskRecordVO;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.system.entity.SysUser;
import com.mochu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程实例服务 — 启动/查询/监控
 */
@Service
@RequiredArgsConstructor
public class BpmProcessInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final BpmTaskExtMapper taskExtMapper;
    private final BpmOaRuleMapper oaRuleMapper;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    /**
     * 根据业务类型启动流程实例
     */
    @Transactional
    public String startProcess(StartProcessDTO dto) {
        BpmOaRule rule = oaRuleMapper.selectOne(
                new LambdaQueryWrapper<BpmOaRule>()
                        .eq(BpmOaRule::getBizType, dto.getBizType())
                        .eq(BpmOaRule::getEnabled, 1));
        if (rule == null) {
            throw new BusinessException("业务类型[" + dto.getBizType() + "]未配置Flowable流程规则");
        }

        // 解析流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiatorId", SecurityUtils.getCurrentUserId());
        variables.put("bizType", dto.getBizType());
        variables.put("bizId", dto.getBizId());
        if (dto.getVariablesJson() != null && !dto.getVariablesJson().isBlank()) {
            try {
                Map<String, Object> extra = objectMapper.readValue(dto.getVariablesJson(),
                        new TypeReference<>() {});
                variables.putAll(extra);
            } catch (Exception ignored) {}
        }

        org.flowable.engine.runtime.ProcessInstance instance = runtimeService
                .startProcessInstanceByKey(rule.getProcessDefKey(), variables);

        // 记录扩展信息
        BpmTaskExt ext = new BpmTaskExt();
        ext.setProcessInstId(instance.getId());
        ext.setBizType(dto.getBizType());
        ext.setBizId(dto.getBizId());
        ext.setBizNo(dto.getBizNo() != null ? dto.getBizNo() : "");
        ext.setInitiatorId(SecurityUtils.getCurrentUserId());
        ext.setResult(0);
        ext.setCreatorId(SecurityUtils.getCurrentUserId());
        taskExtMapper.insert(ext);

        return instance.getId();
    }

    /**
     * 查询我发起的流程分页
     */
    public PageResult<ProcessInstanceVO> listMyInstances(Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 10 : size;

        List<BpmTaskExt> exts = taskExtMapper.selectList(
                new LambdaQueryWrapper<BpmTaskExt>()
                        .eq(BpmTaskExt::getInitiatorId, SecurityUtils.getCurrentUserId())
                        .orderByDesc(BpmTaskExt::getCreatedAt));
        long total = exts.size();
        int fromIdx = (p - 1) * s;
        List<BpmTaskExt> pageExts = fromIdx < exts.size()
                ? exts.subList(fromIdx, Math.min(fromIdx + s, exts.size()))
                : List.of();

        return new PageResult<>(buildInstanceVOs(pageExts), total, p, s);
    }

    /**
     * 查询所有流程实例（管理员监控）
     */
    public PageResult<ProcessInstanceVO> listAllInstances(String bizType, Integer result, Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 10 : size;

        LambdaQueryWrapper<BpmTaskExt> wrapper = new LambdaQueryWrapper<BpmTaskExt>()
                .orderByDesc(BpmTaskExt::getCreatedAt);
        if (bizType != null && !bizType.isBlank()) wrapper.eq(BpmTaskExt::getBizType, bizType);
        if (result != null) wrapper.eq(BpmTaskExt::getResult, result);
        long total = taskExtMapper.selectCount(wrapper);
        wrapper.last("LIMIT " + ((p - 1) * s) + ", " + s);
        List<BpmTaskExt> exts = taskExtMapper.selectList(wrapper);

        return new PageResult<>(buildInstanceVOs(exts), total, p, s);
    }

    /**
     * 获取流程实例详情（含审批记录）
     */
    public ProcessInstanceVO getInstanceDetail(String processInstId) {
        BpmTaskExt ext = taskExtMapper.selectOne(
                new LambdaQueryWrapper<BpmTaskExt>()
                        .eq(BpmTaskExt::getProcessInstId, processInstId));
        if (ext == null) throw new BusinessException("流程实例不存在");

        ProcessInstanceVO vo = toInstanceVO(ext);

        // 历史任务记录
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();
        List<TaskRecordVO> records = historicTasks.stream().map(t -> {
            TaskRecordVO r = new TaskRecordVO();
            r.setTaskId(t.getId());
            r.setTaskName(t.getName());
            if (t.getAssignee() != null) {
                try { r.setAssigneeId(Integer.parseInt(t.getAssignee())); } catch (Exception ignored) {}
            }
            r.setStartTime(t.getStartTime() != null ?
                    t.getStartTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            r.setEndTime(t.getEndTime() != null ?
                    t.getEndTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            // 审批意见
            r.setComment(null);
            return r;
        }).collect(Collectors.toList());

        // 补充用户名
        List<Integer> uids = records.stream()
                .filter(r -> r.getAssigneeId() != null)
                .map(TaskRecordVO::getAssigneeId).distinct().collect(Collectors.toList());
        if (!uids.isEmpty()) {
            Map<Integer, String> nameMap = sysUserMapper.selectBatchIds(uids).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            records.forEach(r -> { if (r.getAssigneeId() != null) r.setAssigneeName(nameMap.get(r.getAssigneeId())); });
        }
        vo.setRecords(records);
        return vo;
    }

    /**
     * 获取流程高亮活动节点ID（用于前端流程图着色）
     */
    public Map<String, Object> getHighlightNodes(String processInstId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<String> completed = activities.stream()
                .filter(a -> a.getEndTime() != null)
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());
        List<String> running = activities.stream()
                .filter(a -> a.getEndTime() == null)
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());

        return Map.of("completedNodes", completed, "runningNodes", running);
    }

    // ==================== OA 规则管理 ====================

    public List<BpmOaRule> listOaRules() {
        return oaRuleMapper.selectList(null);
    }

    @Transactional
    public void saveOaRule(com.mochu.bpm.dto.OaRuleDTO dto) {
        BpmOaRule existing = oaRuleMapper.selectOne(
                new LambdaQueryWrapper<BpmOaRule>().eq(BpmOaRule::getBizType, dto.getBizType()));
        if (existing != null) {
            existing.setProcessDefKey(dto.getProcessDefKey());
            if (dto.getBizName() != null) existing.setBizName(dto.getBizName());
            if (dto.getEnabled() != null) existing.setEnabled(dto.getEnabled());
            oaRuleMapper.updateById(existing);
        } else {
            BpmOaRule rule = new BpmOaRule();
            rule.setBizType(dto.getBizType());
            rule.setBizName(dto.getBizName() != null ? dto.getBizName() : "");
            rule.setProcessDefKey(dto.getProcessDefKey());
            rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
            rule.setCreatorId(SecurityUtils.getCurrentUserId());
            oaRuleMapper.insert(rule);
        }
    }

    public void deleteOaRule(Integer id) {
        oaRuleMapper.deleteById(id);
    }

    /**
     * 检查某业务类型是否配置了 Flowable 流程（用于迁移策略路由）
     */
    public boolean hasFlowableRule(String bizType) {
        return oaRuleMapper.selectCount(
                new LambdaQueryWrapper<BpmOaRule>()
                        .eq(BpmOaRule::getBizType, bizType)
                        .eq(BpmOaRule::getEnabled, 1)) > 0;
    }

    // ==================== 私有方法 ====================

    private List<ProcessInstanceVO> buildInstanceVOs(List<BpmTaskExt> exts) {
        if (exts.isEmpty()) return List.of();
        List<Integer> initiatorIds = exts.stream()
                .filter(e -> e.getInitiatorId() != null)
                .map(BpmTaskExt::getInitiatorId).distinct().collect(Collectors.toList());
        Map<Integer, String> nameMap = initiatorIds.isEmpty() ? Map.of() :
                sysUserMapper.selectBatchIds(initiatorIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        return exts.stream().map(e -> {
            ProcessInstanceVO vo = toInstanceVO(e);
            if (e.getInitiatorId() != null) vo.setInitiatorName(nameMap.get(e.getInitiatorId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private ProcessInstanceVO toInstanceVO(BpmTaskExt e) {
        ProcessInstanceVO vo = new ProcessInstanceVO();
        vo.setProcessInstId(e.getProcessInstId());
        vo.setBizType(e.getBizType());
        vo.setBizId(e.getBizId());
        vo.setBizNo(e.getBizNo());
        vo.setInitiatorId(e.getInitiatorId());
        vo.setResult(e.getResult());
        vo.setEndTime(e.getEndTime());
        vo.setStartTime(e.getCreatedAt());
        return vo;
    }
}
