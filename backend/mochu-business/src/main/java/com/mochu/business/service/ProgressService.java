package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.ChangeOrderDTO;
import com.mochu.business.dto.GanttTaskDTO;
import com.mochu.business.dto.IncomeSplitDTO;
import com.mochu.business.dto.MilestoneDTO;
import com.mochu.business.dto.ProgressReportDTO;
import com.mochu.business.dto.ProgressStatementDTO;
import com.mochu.business.entity.BizChangeDetail;
import com.mochu.business.entity.BizChangeOrder;
import com.mochu.business.entity.BizGanttTask;
import com.mochu.business.entity.BizIncomeSplit;
import com.mochu.business.entity.BizMilestoneDep;
import com.mochu.business.entity.BizProgressReport;
import com.mochu.business.entity.BizProgressStatement;
import com.mochu.business.mapper.BizChangeDetailMapper;
import com.mochu.business.mapper.BizChangeOrderMapper;
import com.mochu.business.mapper.BizGanttTaskMapper;
import com.mochu.business.mapper.BizIncomeSplitMapper;
import com.mochu.business.mapper.BizMilestoneDepMapper;
import com.mochu.business.mapper.BizProgressReportMapper;
import com.mochu.business.mapper.BizProgressStatementMapper;
import com.mochu.business.vo.MilestoneVO;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 进度管理 & 变更管理服务
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final BizGanttTaskMapper ganttTaskMapper;
    private final BizMilestoneDepMapper milestoneDepMapper;
    private final BizChangeOrderMapper changeOrderMapper;
    private final BizChangeDetailMapper changeDetailMapper;
    private final BizProgressStatementMapper progressStatementMapper;
    private final BizIncomeSplitMapper incomeSplitMapper;
    private final BizProgressReportMapper progressReportMapper;
    private final NoGeneratorService noGeneratorService;

    // ===================== 甘特图任务 =====================

    /**
     * 按项目查询甘特图任务树（按 sortOrder 排序）
     */
    public List<BizGanttTask> listGanttTasks(Integer projectId) {
        return ganttTaskMapper.selectList(
                new LambdaQueryWrapper<BizGanttTask>()
                        .eq(BizGanttTask::getProjectId, projectId)
                        .orderByAsc(BizGanttTask::getSortOrder)
                        .orderByAsc(BizGanttTask::getId));
    }

    /**
     * 分页查询甘特图任务，projectId 可选
     */
    public PageResult<BizGanttTask> listGanttTasksPaged(Integer projectId, Integer taskType, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizGanttTask> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizGanttTask> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(BizGanttTask::getProjectId, projectId);
        }
        if (taskType != null) {
            wrapper.eq(BizGanttTask::getTaskType, taskType);
        }
        wrapper.orderByAsc(BizGanttTask::getSortOrder)
               .orderByAsc(BizGanttTask::getId);

        ganttTaskMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizGanttTask getGanttTaskById(Integer id) {
        return ganttTaskMapper.selectById(id);
    }

    public void createGanttTask(GanttTaskDTO dto) {
        BizGanttTask entity = new BizGanttTask();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getParentId() == null) {
            entity.setParentId(0);
        }
        entity.setStatus("draft");
        ganttTaskMapper.insert(entity);
    }

    public void updateGanttTask(Integer id, GanttTaskDTO dto) {
        BizGanttTask entity = ganttTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("甘特图任务不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        ganttTaskMapper.updateById(entity);
    }

    public void updateGanttTaskStatus(Integer id, String status) {
        BizGanttTask entity = ganttTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("甘特图任务不存在");
        }
        entity.setStatus(status);
        ganttTaskMapper.updateById(entity);
    }

    public void deleteGanttTask(Integer id) {
        ganttTaskMapper.deleteById(id);
    }

    // ===================== 里程碑管理 =====================

    /**
     * 分页查询里程碑（含依赖信息）
     */
    public PageResult<MilestoneVO> listMilestones(Integer projectId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizGanttTask> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizGanttTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizGanttTask::getTaskType, 1);
        if (projectId != null) {
            wrapper.eq(BizGanttTask::getProjectId, projectId);
        }
        wrapper.orderByAsc(BizGanttTask::getSortOrder).orderByAsc(BizGanttTask::getId);
        ganttTaskMapper.selectPage(pageParam, wrapper);

        // 批量查询依赖关系
        List<Integer> milestoneIds = pageParam.getRecords().stream()
                .map(BizGanttTask::getId).collect(Collectors.toList());
        Map<Integer, List<Integer>> depMap = getDepMap(milestoneIds);

        // 批量获取所有里程碑名称（用于依赖显示）
        Map<Integer, String> nameMap = getMilestoneNameMap(projectId);

        List<MilestoneVO> voList = pageParam.getRecords().stream().map(task -> {
            MilestoneVO vo = new MilestoneVO();
            vo.setId(task.getId());
            vo.setProjectId(task.getProjectId());
            vo.setTaskName(task.getTaskName());
            vo.setPlanEndDate(task.getPlanEndDate());
            vo.setActualEndDate(task.getActualEndDate());
            vo.setProgressPct(task.getProgressPct());
            vo.setSortOrder(task.getSortOrder());
            vo.setStatus(task.getStatus());
            vo.setCreatedAt(task.getCreatedAt());

            List<Integer> depIds = depMap.getOrDefault(task.getId(), new ArrayList<>());
            vo.setDepMilestoneIds(depIds);
            vo.setDepMilestoneNames(depIds.stream()
                    .map(id -> nameMap.getOrDefault(id, "ID:" + id))
                    .collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageParam.getTotal(), p, s);
    }

    /**
     * 按项目查询所有里程碑（含依赖，用于时间线展示）
     */
    public List<MilestoneVO> listAllMilestones(Integer projectId) {
        LambdaQueryWrapper<BizGanttTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizGanttTask::getTaskType, 1);
        if (projectId != null) {
            wrapper.eq(BizGanttTask::getProjectId, projectId);
        }
        wrapper.orderByAsc(BizGanttTask::getSortOrder).orderByAsc(BizGanttTask::getId);
        List<BizGanttTask> tasks = ganttTaskMapper.selectList(wrapper);

        List<Integer> milestoneIds = tasks.stream().map(BizGanttTask::getId).collect(Collectors.toList());
        Map<Integer, List<Integer>> depMap = getDepMap(milestoneIds);
        Map<Integer, String> nameMap = getMilestoneNameMap(projectId);

        return tasks.stream().map(task -> {
            MilestoneVO vo = new MilestoneVO();
            vo.setId(task.getId());
            vo.setProjectId(task.getProjectId());
            vo.setTaskName(task.getTaskName());
            vo.setPlanEndDate(task.getPlanEndDate());
            vo.setActualEndDate(task.getActualEndDate());
            vo.setProgressPct(task.getProgressPct());
            vo.setSortOrder(task.getSortOrder());
            vo.setStatus(task.getStatus());
            vo.setCreatedAt(task.getCreatedAt());

            List<Integer> depIds = depMap.getOrDefault(task.getId(), new ArrayList<>());
            vo.setDepMilestoneIds(depIds);
            vo.setDepMilestoneNames(depIds.stream()
                    .map(id -> nameMap.getOrDefault(id, "ID:" + id))
                    .collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 创建里程碑（含依赖关系）
     */
    @Transactional
    public void createMilestone(MilestoneDTO dto) {
        BizGanttTask entity = new BizGanttTask();
        entity.setProjectId(dto.getProjectId());
        entity.setTaskName(dto.getMilestoneName());
        entity.setTaskType(1);
        entity.setParentId(0);
        entity.setPlanEndDate(dto.getDeadline());
        entity.setActualEndDate(dto.getActualEndDate());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setStatus("draft");
        ganttTaskMapper.insert(entity);

        saveDeps(entity.getId(), dto.getDepMilestoneIds());
    }

    /**
     * 更新里程碑（含依赖关系）
     */
    @Transactional
    public void updateMilestone(Integer id, MilestoneDTO dto) {
        BizGanttTask entity = ganttTaskMapper.selectById(id);
        if (entity == null || entity.getTaskType() != 1) {
            throw new BusinessException("里程碑不存在");
        }
        entity.setProjectId(dto.getProjectId());
        entity.setTaskName(dto.getMilestoneName());
        entity.setPlanEndDate(dto.getDeadline());
        entity.setActualEndDate(dto.getActualEndDate());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        ganttTaskMapper.updateById(entity);

        // 先删再建依赖
        milestoneDepMapper.delete(new LambdaQueryWrapper<BizMilestoneDep>()
                .eq(BizMilestoneDep::getMilestoneId, id));
        saveDeps(id, dto.getDepMilestoneIds());
    }

    /**
     * 删除里程碑（同时删除关联依赖）
     */
    @Transactional
    public void deleteMilestone(Integer id) {
        ganttTaskMapper.deleteById(id);
        milestoneDepMapper.delete(new LambdaQueryWrapper<BizMilestoneDep>()
                .eq(BizMilestoneDep::getMilestoneId, id)
                .or().eq(BizMilestoneDep::getDepMilestoneId, id));
    }

    /**
     * 更新里程碑状态
     */
    public void updateMilestoneStatus(Integer id, String status) {
        BizGanttTask entity = ganttTaskMapper.selectById(id);
        if (entity == null || entity.getTaskType() != 1) {
            throw new BusinessException("里程碑不存在");
        }
        entity.setStatus(status);
        ganttTaskMapper.updateById(entity);
    }

    /**
     * 查询单个里程碑的依赖ID列表
     */
    public List<Integer> getMilestoneDeps(Integer milestoneId) {
        return milestoneDepMapper.selectList(
                new LambdaQueryWrapper<BizMilestoneDep>()
                        .eq(BizMilestoneDep::getMilestoneId, milestoneId))
                .stream().map(BizMilestoneDep::getDepMilestoneId)
                .collect(Collectors.toList());
    }

    // ---- 内部方法 ----

    private void saveDeps(Integer milestoneId, List<Integer> depIds) {
        if (depIds == null || depIds.isEmpty()) return;
        for (Integer depId : depIds) {
            if (depId.equals(milestoneId)) continue; // 不允许自依赖
            BizMilestoneDep dep = new BizMilestoneDep();
            dep.setMilestoneId(milestoneId);
            dep.setDepMilestoneId(depId);
            dep.setCreatedAt(LocalDateTime.now());
            milestoneDepMapper.insert(dep);
        }
    }

    private Map<Integer, List<Integer>> getDepMap(List<Integer> milestoneIds) {
        if (milestoneIds.isEmpty()) return Map.of();
        List<BizMilestoneDep> deps = milestoneDepMapper.selectList(
                new LambdaQueryWrapper<BizMilestoneDep>()
                        .in(BizMilestoneDep::getMilestoneId, milestoneIds));
        return deps.stream().collect(Collectors.groupingBy(
                BizMilestoneDep::getMilestoneId,
                Collectors.mapping(BizMilestoneDep::getDepMilestoneId, Collectors.toList())));
    }

    private Map<Integer, String> getMilestoneNameMap(Integer projectId) {
        LambdaQueryWrapper<BizGanttTask> w = new LambdaQueryWrapper<>();
        w.eq(BizGanttTask::getTaskType, 1);
        if (projectId != null) w.eq(BizGanttTask::getProjectId, projectId);
        w.select(BizGanttTask::getId, BizGanttTask::getTaskName);
        return ganttTaskMapper.selectList(w).stream()
                .collect(Collectors.toMap(BizGanttTask::getId, BizGanttTask::getTaskName));
    }

    // ===================== 变更单 =====================

    /**
     * 分页查询变更单
     */
    public PageResult<BizChangeOrder> listChangeOrders(Integer projectId, String changeType,
                                                       String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizChangeOrder> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizChangeOrder> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(BizChangeOrder::getProjectId, projectId);
        }
        if (changeType != null && !changeType.isBlank()) {
            wrapper.eq(BizChangeOrder::getChangeType, changeType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizChangeOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizChangeOrder::getCreatedAt);

        changeOrderMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizChangeOrder getChangeOrderById(Integer id) {
        return changeOrderMapper.selectById(id);
    }

    /**
     * 查询变更单的明细列表
     */
    public List<BizChangeDetail> listChangeDetails(Integer changeId) {
        return changeDetailMapper.selectList(
                new LambdaQueryWrapper<BizChangeDetail>()
                        .eq(BizChangeDetail::getChangeId, changeId)
                        .orderByAsc(BizChangeDetail::getId));
    }

    @Transactional
    public void createChangeOrder(ChangeOrderDTO dto) {
        BizChangeOrder order = new BizChangeOrder();
        BeanUtils.copyProperties(dto, order, "details");
        order.setChangeNo(noGeneratorService.generate("CG"));
        order.setStatus("draft");
        changeOrderMapper.insert(order);

        // 保存明细
        if (dto.getDetails() != null) {
            for (ChangeOrderDTO.ChangeDetailItem item : dto.getDetails()) {
                BizChangeDetail detail = new BizChangeDetail();
                BeanUtils.copyProperties(item, detail);
                detail.setChangeId(order.getId());
                changeDetailMapper.insert(detail);
            }
        }
    }

    @Transactional
    public void updateChangeOrder(Integer id, ChangeOrderDTO dto) {
        BizChangeOrder order = changeOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("变更单不存在");
        }
        BeanUtils.copyProperties(dto, order, "id", "details");
        changeOrderMapper.updateById(order);

        // 先删除旧明细，再重新插入
        if (dto.getDetails() != null) {
            changeDetailMapper.delete(
                    new LambdaQueryWrapper<BizChangeDetail>()
                            .eq(BizChangeDetail::getChangeId, id));
            for (ChangeOrderDTO.ChangeDetailItem item : dto.getDetails()) {
                BizChangeDetail detail = new BizChangeDetail();
                BeanUtils.copyProperties(item, detail);
                detail.setId(null);
                detail.setChangeId(id);
                changeDetailMapper.insert(detail);
            }
        }
    }

    public void updateChangeOrderStatus(Integer id, String status) {
        BizChangeOrder order = changeOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("变更单不存在");
        }
        order.setStatus(status);
        changeOrderMapper.updateById(order);
    }

    public void deleteChangeOrder(Integer id) {
        changeOrderMapper.deleteById(id);
    }

    // ===================== 产值报表 =====================

    /**
     * 分页查询产值报表
     */
    public PageResult<BizProgressStatement> listStatements(Integer projectId, String period,
                                                           Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizProgressStatement> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizProgressStatement> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(BizProgressStatement::getProjectId, projectId);
        }
        if (period != null && !period.isBlank()) {
            wrapper.eq(BizProgressStatement::getPeriod, period);
        }
        wrapper.orderByDesc(BizProgressStatement::getCreatedAt);

        progressStatementMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizProgressStatement getStatementById(Integer id) {
        return progressStatementMapper.selectById(id);
    }

    public void createStatement(ProgressStatementDTO dto) {
        BizProgressStatement entity = new BizProgressStatement();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatementNo(noGeneratorService.generate("PS"));
        entity.setStatus("draft");
        progressStatementMapper.insert(entity);
    }

    public void updateStatement(Integer id, ProgressStatementDTO dto) {
        BizProgressStatement entity = progressStatementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("产值报表不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        progressStatementMapper.updateById(entity);
    }

    public void updateStatementStatus(Integer id, String status) {
        BizProgressStatement entity = progressStatementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("产值报表不存在");
        }
        entity.setStatus(status);
        progressStatementMapper.updateById(entity);
    }

    public void deleteStatement(Integer id) {
        progressStatementMapper.deleteById(id);
    }

    // ===================== 收入拆分 =====================

    /**
     * 分页查询收入拆分
     */
    public PageResult<BizIncomeSplit> listIncomeSplits(Integer projectId, Integer contractId,
                                                      Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizIncomeSplit> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizIncomeSplit> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(BizIncomeSplit::getProjectId, projectId);
        }
        if (contractId != null) {
            wrapper.eq(BizIncomeSplit::getContractId, contractId);
        }
        wrapper.orderByDesc(BizIncomeSplit::getCreatedAt);

        incomeSplitMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizIncomeSplit getIncomeSplitById(Integer id) {
        return incomeSplitMapper.selectById(id);
    }

    public void createIncomeSplit(IncomeSplitDTO dto) {
        BizIncomeSplit entity = new BizIncomeSplit();
        BeanUtils.copyProperties(dto, entity);
        entity.setSplitNo(noGeneratorService.generate("IS"));
        entity.setStatus("draft");
        incomeSplitMapper.insert(entity);
    }

    public void updateIncomeSplit(Integer id, IncomeSplitDTO dto) {
        BizIncomeSplit entity = incomeSplitMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("收入拆分不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        incomeSplitMapper.updateById(entity);
    }

    public void updateIncomeSplitStatus(Integer id, String status) {
        BizIncomeSplit entity = incomeSplitMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("收入拆分不存在");
        }
        entity.setStatus(status);
        incomeSplitMapper.updateById(entity);
    }

    public void deleteIncomeSplit(Integer id) {
        incomeSplitMapper.deleteById(id);
    }

    // ===================== 进度汇报 =====================

    /**
     * 分页查询进度汇报
     */
    public PageResult<BizProgressReport> listReports(Integer projectId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizProgressReport> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizProgressReport> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(BizProgressReport::getProjectId, projectId);
        }
        wrapper.orderByDesc(BizProgressReport::getCreatedAt);

        progressReportMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizProgressReport getReportById(Integer id) {
        return progressReportMapper.selectById(id);
    }

    public void createReport(ProgressReportDTO dto) {
        BizProgressReport entity = new BizProgressReport();
        BeanUtils.copyProperties(dto, entity);
        entity.setReportNo(noGeneratorService.generate("PR"));
        entity.setStatus("draft");
        if (entity.getReportDate() == null) {
            entity.setReportDate(LocalDate.now());
        }
        progressReportMapper.insert(entity);
    }

    public void updateReport(Integer id, ProgressReportDTO dto) {
        BizProgressReport entity = progressReportMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("进度汇报不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        progressReportMapper.updateById(entity);
    }

    public void updateReportStatus(Integer id, String status) {
        BizProgressReport entity = progressReportMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("进度汇报不存在");
        }
        entity.setStatus(status);
        progressReportMapper.updateById(entity);
    }

    public void deleteReport(Integer id) {
        progressReportMapper.deleteById(id);
    }

    // ===================== 进度校正 =====================

    /**
     * 进度校正 — 创建一条特殊的进度汇报记录
     */
    public void correctProgress(Integer projectId, BigDecimal actualRate, String remark) {
        BizProgressReport entity = new BizProgressReport();
        entity.setReportNo(noGeneratorService.generate("PR"));
        entity.setProjectId(projectId);
        entity.setReportDate(LocalDate.now());
        entity.setContent("[进度校正]");
        entity.setProgressRate(actualRate);
        entity.setRemark(remark);
        entity.setStatus("draft");
        progressReportMapper.insert(entity);
    }
}
