package com.mochu.business.controller;

import com.mochu.business.dto.ChangeOrderDTO;
import com.mochu.business.dto.GanttTaskDTO;
import com.mochu.business.dto.IncomeSplitDTO;
import com.mochu.business.dto.MilestoneDTO;
import com.mochu.business.dto.ProgressCorrectDTO;
import com.mochu.business.dto.ProgressReportDTO;
import com.mochu.business.dto.ProgressStatementDTO;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.entity.BizChangeDetail;
import com.mochu.business.entity.BizChangeOrder;
import com.mochu.business.entity.BizGanttTask;
import com.mochu.business.entity.BizIncomeSplit;
import com.mochu.business.entity.BizProgressReport;
import com.mochu.business.entity.BizProgressStatement;
import com.mochu.business.service.ProgressService;
import com.mochu.business.vo.MilestoneVO;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 进度管理 & 变更管理接口
 */
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    // ===================== 甘特图任务 /gantt =====================

    @GetMapping("/gantt")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<PageResult<BizGanttTask>> listGanttTasks(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer taskType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(progressService.listGanttTasksPaged(projectId, taskType, page, size));
    }

    @GetMapping("/gantt/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<BizGanttTask> getGanttTask(@PathVariable Integer id) {
        BizGanttTask task = progressService.getGanttTaskById(id);
        if (task == null) {
            return R.fail(404, "甘特图任务不存在");
        }
        return R.ok(task);
    }

    @Idempotent
    @PostMapping("/gantt")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> createGanttTask(@Valid @RequestBody GanttTaskDTO dto) {
        progressService.createGanttTask(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/gantt/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateGanttTask(@PathVariable Integer id, @Valid @RequestBody GanttTaskDTO dto) {
        progressService.updateGanttTask(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/gantt/{id}/status")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateGanttTaskStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        progressService.updateGanttTaskStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/gantt/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> deleteGanttTask(@PathVariable Integer id) {
        progressService.deleteGanttTask(id);
        return R.ok();
    }

    // ===================== 里程碑 /milestones =====================

    @GetMapping("/milestones")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<PageResult<MilestoneVO>> listMilestones(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(progressService.listMilestones(projectId, page, size));
    }

    @GetMapping("/milestones/all")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<List<MilestoneVO>> listAllMilestones(
            @RequestParam(required = false) Integer projectId) {
        return R.ok(progressService.listAllMilestones(projectId));
    }

    @Idempotent
    @PostMapping("/milestones")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> createMilestone(@Valid @RequestBody MilestoneDTO dto) {
        progressService.createMilestone(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/milestones/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateMilestone(@PathVariable Integer id, @Valid @RequestBody MilestoneDTO dto) {
        progressService.updateMilestone(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/milestones/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> deleteMilestone(@PathVariable Integer id) {
        progressService.deleteMilestone(id);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/milestones/{id}/status")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateMilestoneStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        progressService.updateMilestoneStatus(id, dto.getStatus());
        return R.ok();
    }

    @GetMapping("/milestones/{id}/deps")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<List<Integer>> getMilestoneDeps(@PathVariable Integer id) {
        return R.ok(progressService.getMilestoneDeps(id));
    }

    // ===================== 变更单 /changes =====================

    @GetMapping("/changes")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<PageResult<BizChangeOrder>> listChangeOrders(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(progressService.listChangeOrders(projectId, changeType, status, page, size));
    }

    @GetMapping("/changes/{id}")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<BizChangeOrder> getChangeOrder(@PathVariable Integer id) {
        BizChangeOrder order = progressService.getChangeOrderById(id);
        if (order == null) {
            return R.fail(404, "变更单不存在");
        }
        return R.ok(order);
    }

    @GetMapping("/changes/{id}/details")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<List<BizChangeDetail>> listChangeDetails(@PathVariable Integer id) {
        return R.ok(progressService.listChangeDetails(id));
    }

    @Idempotent
    @PostMapping("/changes")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<Void> createChangeOrder(@Valid @RequestBody ChangeOrderDTO dto) {
        progressService.createChangeOrder(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/changes/{id}")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<Void> updateChangeOrder(@PathVariable Integer id, @Valid @RequestBody ChangeOrderDTO dto) {
        progressService.updateChangeOrder(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/changes/{id}/status")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<Void> updateChangeOrderStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        progressService.updateChangeOrderStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/changes/{id}")
    @PreAuthorize("hasAuthority('change:apply')")
    public R<Void> deleteChangeOrder(@PathVariable Integer id) {
        progressService.deleteChangeOrder(id);
        return R.ok();
    }

    // ===================== 产值报表 /statements =====================

    @GetMapping("/statements")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<PageResult<BizProgressStatement>> listStatements(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(progressService.listStatements(projectId, period, page, size));
    }

    @GetMapping("/statements/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<BizProgressStatement> getStatement(@PathVariable Integer id) {
        BizProgressStatement entity = progressService.getStatementById(id);
        if (entity == null) {
            return R.fail(404, "产值报表不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/statements")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> createStatement(@Valid @RequestBody ProgressStatementDTO dto) {
        progressService.createStatement(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/statements/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateStatement(@PathVariable Integer id, @Valid @RequestBody ProgressStatementDTO dto) {
        progressService.updateStatement(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/statements/{id}/status")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateStatementStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        progressService.updateStatementStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/statements/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> deleteStatement(@PathVariable Integer id) {
        progressService.deleteStatement(id);
        return R.ok();
    }

    // ===================== 收入拆分 /income-split =====================

    @GetMapping("/income-split")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<PageResult<BizIncomeSplit>> listIncomeSplits(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer contractId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(progressService.listIncomeSplits(projectId, contractId, page, size));
    }

    @GetMapping("/income-split/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<BizIncomeSplit> getIncomeSplit(@PathVariable Integer id) {
        BizIncomeSplit entity = progressService.getIncomeSplitById(id);
        if (entity == null) {
            return R.fail(404, "收入拆分不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/income-split")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> createIncomeSplit(@Valid @RequestBody IncomeSplitDTO dto) {
        progressService.createIncomeSplit(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/income-split/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateIncomeSplit(@PathVariable Integer id, @Valid @RequestBody IncomeSplitDTO dto) {
        progressService.updateIncomeSplit(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/income-split/{id}/status")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateIncomeSplitStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        progressService.updateIncomeSplitStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/income-split/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> deleteIncomeSplit(@PathVariable Integer id) {
        progressService.deleteIncomeSplit(id);
        return R.ok();
    }

    // ===================== 进度汇报 /reports =====================

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<PageResult<BizProgressReport>> listReports(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(progressService.listReports(projectId, page, size));
    }

    @GetMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<BizProgressReport> getReport(@PathVariable Integer id) {
        BizProgressReport entity = progressService.getReportById(id);
        if (entity == null) {
            return R.fail(404, "进度汇报不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/reports")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> createReport(@Valid @RequestBody ProgressReportDTO dto) {
        progressService.createReport(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateReport(@PathVariable Integer id, @Valid @RequestBody ProgressReportDTO dto) {
        progressService.updateReport(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/reports/{id}/status")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> updateReportStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        progressService.updateReportStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> deleteReport(@PathVariable Integer id) {
        progressService.deleteReport(id);
        return R.ok();
    }

    // ===================== 进度校正 =====================

    @Idempotent
    @PostMapping("/correct")
    @PreAuthorize("hasAuthority('progress:report')")
    public R<Void> correctProgress(@Valid @RequestBody ProgressCorrectDTO dto) {
        progressService.correctProgress(dto.getProjectId(), dto.getActualRate(), dto.getRemark());
        return R.ok();
    }
}
