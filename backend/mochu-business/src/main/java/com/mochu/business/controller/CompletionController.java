package com.mochu.business.controller;

import com.mochu.business.dto.CaseDTO;
import com.mochu.business.dto.CompletionDocDTO;
import com.mochu.business.dto.CompletionFinishDTO;
import com.mochu.business.dto.DrawingDTO;
import com.mochu.business.dto.ExceptionTaskDTO;
import com.mochu.business.dto.LaborSettlementDTO;
import com.mochu.business.dto.ResolveExceptionDTO;
import com.mochu.business.entity.BizCase;
import com.mochu.business.entity.BizCompletionDoc;
import com.mochu.business.entity.BizCompletionFinish;
import com.mochu.business.entity.BizDrawing;
import com.mochu.business.entity.BizExceptionTask;
import com.mochu.business.entity.BizLaborSettlement;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.service.CompletionService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 竣工验收模块接口 — 完工验收 / 劳务结算 / 案例管理 / 异常工单 / 竣工图纸 / 竣工资料
 */
@RestController
@RequestMapping("/api/v1/completion")
@RequiredArgsConstructor
public class CompletionController {

    private final CompletionService completionService;

    // ==================== 完工验收 /finish ====================

    @GetMapping("/finish")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<PageResult<BizCompletionFinish>> listFinish(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(completionService.listFinish(projectId, status, page, size));
    }

    @GetMapping("/finish/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<BizCompletionFinish> getFinishById(@PathVariable Integer id) {
        BizCompletionFinish entity = completionService.getFinishById(id);
        if (entity == null) {
            return R.fail(404, "完工验收记录不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/finish")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> createFinish(@Valid @RequestBody CompletionFinishDTO dto) {
        completionService.createFinish(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/finish/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> updateFinish(@PathVariable Integer id, @Valid @RequestBody CompletionFinishDTO dto) {
        completionService.updateFinish(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/finish/{id}/status")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> updateFinishStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        completionService.updateFinishStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/finish/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> deleteFinish(@PathVariable Integer id) {
        completionService.deleteFinish(id);
        return R.ok();
    }

    // ==================== 劳务结算 /labor ====================

    @GetMapping("/labor")
    @PreAuthorize("hasAnyAuthority('finance:payment-apply','finance:payment-confirm')")
    public R<PageResult<BizLaborSettlement>> listLabor(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(completionService.listLabor(projectId, status, page, size));
    }

    @GetMapping("/labor/{id}")
    @PreAuthorize("hasAnyAuthority('finance:payment-apply','finance:payment-confirm')")
    public R<BizLaborSettlement> getLaborById(@PathVariable Integer id) {
        BizLaborSettlement entity = completionService.getLaborById(id);
        if (entity == null) {
            return R.fail(404, "劳务结算记录不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/labor")
    @PreAuthorize("hasAnyAuthority('finance:payment-apply','finance:payment-confirm')")
    public R<Void> createLabor(@Valid @RequestBody LaborSettlementDTO dto) {
        completionService.createLabor(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/labor/{id}")
    @PreAuthorize("hasAnyAuthority('finance:payment-apply','finance:payment-confirm')")
    public R<Void> updateLabor(@PathVariable Integer id, @Valid @RequestBody LaborSettlementDTO dto) {
        completionService.updateLabor(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/labor/{id}/status")
    @PreAuthorize("hasAnyAuthority('finance:payment-apply','finance:payment-confirm')")
    public R<Void> updateLaborStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        completionService.updateLaborStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/labor/{id}")
    @PreAuthorize("hasAnyAuthority('finance:payment-apply','finance:payment-confirm')")
    public R<Void> deleteLabor(@PathVariable Integer id) {
        completionService.deleteLabor(id);
        return R.ok();
    }

    // ==================== 案例管理 /cases ====================

    @GetMapping("/cases")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<PageResult<BizCase>> listCase(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(completionService.listCase(projectId, status, page, size));
    }

    @GetMapping("/cases/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<BizCase> getCaseById(@PathVariable Integer id) {
        BizCase entity = completionService.getCaseById(id);
        if (entity == null) {
            return R.fail(404, "案例记录不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/cases")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> createCase(@Valid @RequestBody CaseDTO dto) {
        completionService.createCase(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/cases/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> updateCase(@PathVariable Integer id, @Valid @RequestBody CaseDTO dto) {
        completionService.updateCase(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/cases/{id}/status")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> updateCaseStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        completionService.updateCaseStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/cases/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> deleteCase(@PathVariable Integer id) {
        completionService.deleteCase(id);
        return R.ok();
    }

    // ==================== 异常工单 /exceptions ====================

    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<PageResult<BizExceptionTask>> listException(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(completionService.listException(bizType, status, page, size));
    }

    @GetMapping("/exceptions/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<BizExceptionTask> getExceptionById(@PathVariable Integer id) {
        BizExceptionTask entity = completionService.getExceptionById(id);
        if (entity == null) {
            return R.fail(404, "异常工单不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/exceptions")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> createException(@Valid @RequestBody ExceptionTaskDTO dto) {
        completionService.createException(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/exceptions/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> updateException(@PathVariable Integer id, @Valid @RequestBody ExceptionTaskDTO dto) {
        completionService.updateException(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/exceptions/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> resolveException(@PathVariable Integer id, @Valid @RequestBody ResolveExceptionDTO dto) {
        completionService.resolveException(id, dto.getResolveRemark());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/exceptions/{id}")
    @PreAuthorize("hasAnyAuthority('progress:report','project:view-all')")
    public R<Void> deleteException(@PathVariable Integer id) {
        completionService.deleteException(id);
        return R.ok();
    }

    // ==================== 竣工图纸 /drawings ====================

    @GetMapping("/drawings")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<PageResult<BizDrawing>> listDrawing(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(completionService.listDrawing(projectId, status, page, size));
    }

    @GetMapping("/drawings/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<BizDrawing> getDrawingById(@PathVariable Integer id) {
        BizDrawing entity = completionService.getDrawingById(id);
        if (entity == null) {
            return R.fail(404, "竣工图纸记录不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/drawings")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> createDrawing(@Valid @RequestBody DrawingDTO dto) {
        completionService.createDrawing(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/drawings/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> updateDrawing(@PathVariable Integer id, @Valid @RequestBody DrawingDTO dto) {
        completionService.updateDrawing(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/drawings/{id}/status")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> updateDrawingStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        completionService.updateDrawingStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/drawings/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> deleteDrawing(@PathVariable Integer id) {
        completionService.deleteDrawing(id);
        return R.ok();
    }

    // ==================== 竣工资料 /docs ====================

    @GetMapping("/docs")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<PageResult<BizCompletionDoc>> listDoc(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(completionService.listDoc(projectId, status, page, size));
    }

    @GetMapping("/docs/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<BizCompletionDoc> getDocById(@PathVariable Integer id) {
        BizCompletionDoc entity = completionService.getDocById(id);
        if (entity == null) {
            return R.fail(404, "竣工资料记录不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/docs")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> createDoc(@Valid @RequestBody CompletionDocDTO dto) {
        completionService.createDoc(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/docs/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> updateDoc(@PathVariable Integer id, @Valid @RequestBody CompletionDocDTO dto) {
        completionService.updateDoc(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/docs/{id}/status")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> updateDocStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        completionService.updateDocStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/docs/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> deleteDoc(@PathVariable Integer id) {
        completionService.deleteDoc(id);
        return R.ok();
    }
}
