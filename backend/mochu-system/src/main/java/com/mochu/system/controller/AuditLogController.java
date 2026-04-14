package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.entity.SysAuditLog;
import com.mochu.system.service.AuditLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

/**
 * 审计日志接口 — 只读查询
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:audit-log')")
    public R<PageResult<SysAuditLog>> list(
            @RequestParam(required = false) String operateModule,
            @RequestParam(required = false) String operateType,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(auditLogService.list(operateModule, operateType, userId, startDate, endDate, page, size));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('system:log-view','system:audit-log')")
    @Idempotent
    public void exportLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        auditLogService.exportLogs(module, startDate, endDate, response);
    }
}
