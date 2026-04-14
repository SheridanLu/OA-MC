package com.mochu.business.controller;

import com.mochu.business.dto.ReportTemplateDTO;
import com.mochu.business.entity.SysReportTemplate;
import com.mochu.business.service.ReportTemplateService;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportTemplateService reportTemplateService;

    // ==================== 模板 CRUD ====================

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('report:view')")
    public R<List<SysReportTemplate>> listTemplates(@RequestParam(required = false) String category) {
        return R.ok(reportTemplateService.listTemplates(category));
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('report:view')")
    public R<SysReportTemplate> getTemplate(@PathVariable Integer id) {
        return R.ok(reportTemplateService.getTemplate(id));
    }

    @Idempotent
    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('report:template-manage')")
    public R<Void> createTemplate(@Valid @RequestBody ReportTemplateDTO dto) {
        reportTemplateService.createTemplate(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('report:template-manage')")
    public R<Void> updateTemplate(@PathVariable Integer id, @Valid @RequestBody ReportTemplateDTO dto) {
        reportTemplateService.updateTemplate(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('report:template-manage')")
    public R<Void> deleteTemplate(@PathVariable Integer id) {
        reportTemplateService.deleteTemplate(id);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/templates/{id}/execute")
    @PreAuthorize("hasAuthority('report:view')")
    public R<Map<String, Object>> executeTemplate(@PathVariable Integer id,
                                                   @RequestBody(required = false) Map<String, Object> params) {
        return R.ok(reportTemplateService.executeTemplate(id, params));
    }

    // ==================== 内置报表 ====================

    @GetMapping("/stock-flow")
    @PreAuthorize("hasAuthority('report:view')")
    public R<List<Map<String, Object>>> stockFlowReport(@RequestParam(required = false) Integer projectId) {
        return R.ok(reportTemplateService.stockFlowReport(projectId));
    }

    @GetMapping("/stock-aging")
    @PreAuthorize("hasAuthority('report:view')")
    public R<List<Map<String, Object>>> stockAgingReport(@RequestParam(required = false) Integer projectId) {
        return R.ok(reportTemplateService.stockAgingReport(projectId));
    }

    @GetMapping("/purchase-price")
    @PreAuthorize("hasAuthority('report:view')")
    public R<List<Map<String, Object>>> purchasePriceComparison(@RequestParam(required = false) Integer materialId) {
        return R.ok(reportTemplateService.purchasePriceComparison(materialId));
    }
}
