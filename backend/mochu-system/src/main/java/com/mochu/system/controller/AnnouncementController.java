package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.AnnouncementDTO;
import com.mochu.system.dto.AnnouncementQueryDTO;
import com.mochu.system.service.AnnouncementService;
import com.mochu.system.vo.AnnouncementVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理接口 — 对照 V3.2 公告模块
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 公告列表（管理端） — GET /api/v1/admin/announcements
     */
    @GetMapping("/admin/announcements")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<PageResult<AnnouncementVO>> list(AnnouncementQueryDTO dto) {
        return R.ok(announcementService.list(dto));
    }

    /**
     * 公告详情 — GET /api/v1/admin/announcements/{id}
     */
    @GetMapping("/admin/announcements/{id}")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<AnnouncementVO> detail(@PathVariable Integer id) {
        return R.ok(announcementService.getById(id));
    }

    /**
     * 创建公告 — POST /api/v1/admin/announcements
     */
    @Idempotent
    @PostMapping("/admin/announcements")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<Integer> create(@Valid @RequestBody AnnouncementDTO dto) {
        return R.ok(announcementService.create(dto));
    }

    /**
     * 更新公告 — PUT /api/v1/admin/announcements/{id}
     */
    @Idempotent
    @PutMapping("/admin/announcements/{id}")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody AnnouncementDTO dto) {
        dto.setId(id);
        announcementService.update(dto);
        return R.ok();
    }

    /**
     * 发布公告 — PATCH /api/v1/admin/announcements/{id}/publish
     */
    @Idempotent
    @PatchMapping("/admin/announcements/{id}/publish")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<Void> publish(@PathVariable Integer id) {
        announcementService.publish(id);
        return R.ok();
    }

    /**
     * 下线公告 — PATCH /api/v1/admin/announcements/{id}/offline
     */
    @Idempotent
    @PatchMapping("/admin/announcements/{id}/offline")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<Void> offline(@PathVariable Integer id) {
        announcementService.offline(id);
        return R.ok();
    }

    /**
     * 置顶/取消置顶 — PATCH /api/v1/admin/announcements/{id}/toggle-top
     */
    @Idempotent
    @PatchMapping("/admin/announcements/{id}/toggle-top")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<Void> toggleTop(@PathVariable Integer id) {
        announcementService.toggleTop(id);
        return R.ok();
    }

    /**
     * 删除公告 — DELETE /api/v1/admin/announcements/{id}
     */
    @Idempotent
    @DeleteMapping("/admin/announcements/{id}")
    @PreAuthorize("hasAuthority('system:announcement-manage')")
    public R<Void> delete(@PathVariable Integer id) {
        announcementService.delete(id);
        return R.ok();
    }

    /**
     * 前台已发布公告（所有登录用户） — GET /api/v1/announcements
     */
    @GetMapping("/announcements")
    public R<List<AnnouncementVO>> listPublished(@RequestParam(defaultValue = "10") int limit) {
        return R.ok(announcementService.listPublished(limit));
    }
}
