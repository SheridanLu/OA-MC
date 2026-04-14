package com.mochu.business.controller;

import com.mochu.business.service.ReportSubscribeService;
import com.mochu.common.result.R;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.framework.annotation.Idempotent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 报表订阅接口 — 对照 V3.2 报表订阅模块
 */
@RestController
@RequestMapping("/api/v1/report/subscribe")
@RequiredArgsConstructor
public class ReportSubscribeController {

    private final ReportSubscribeService reportSubscribeService;

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<Map<String, Object>>> listSubscriptions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(reportSubscribeService.listByUser(userId, page, size));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    public R<Void> subscribe(@RequestBody Map<String, Object> body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        reportSubscribeService.subscribe(userId, body);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    public R<Void> unsubscribe(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        reportSubscribeService.unsubscribe(id, userId);
        return R.ok();
    }
}
