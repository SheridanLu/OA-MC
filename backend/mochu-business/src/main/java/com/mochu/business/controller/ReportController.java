package com.mochu.business.controller;

import com.mochu.business.service.ReportService;
import com.mochu.common.result.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 报表汇总接口
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/project")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public R<Map<String, Object>> projectSummary() {
        return R.ok(reportService.getProjectSummary());
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public R<Map<String, Object>> financeSummary() {
        return R.ok(reportService.getFinanceSummary());
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public R<Map<String, Object>> inventorySummary() {
        return R.ok(reportService.getInventorySummary());
    }

    @GetMapping("/contract")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public R<Map<String, Object>> contractSummary() {
        return R.ok(reportService.getContractSummary());
    }

    @GetMapping("/cost")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public R<Map<String, Object>> costSummary() {
        return R.ok(reportService.getCostSummary());
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public R<Map<String, Object>> hrSummary() {
        return R.ok(reportService.getHrSummary());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('report:view-all','report:view-project')")
    public void exportReport(
            @RequestParam String type,
            @RequestParam(required = false) Integer projectId,
            HttpServletResponse response) throws IOException {
        reportService.exportReport(type, projectId, response);
    }
}
