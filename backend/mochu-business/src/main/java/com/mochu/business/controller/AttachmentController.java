package com.mochu.business.controller;

import com.mochu.business.entity.BizAttachment;
import com.mochu.business.service.AttachmentService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 附件管理接口
 */
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public R<BizAttachment> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam String bizType,
                                    @RequestParam Integer bizId) throws Exception {
        return R.ok(attachmentService.upload(file, bizType, bizId));
    }

    @GetMapping("/{id}/url")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, String>> getDownloadUrl(@PathVariable Integer id) throws Exception {
        String url = attachmentService.getDownloadUrl(id);
        return R.ok(Map.of("url", url));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<BizAttachment>> list(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer bizId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(attachmentService.list(bizType, bizId, page, size));
    }

    @GetMapping("/biz")
    @PreAuthorize("isAuthenticated()")
    public R<List<BizAttachment>> listByBiz(@RequestParam String bizType,
                                             @RequestParam Integer bizId) {
        return R.ok(attachmentService.listByBiz(bizType, bizId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> delete(@PathVariable Integer id) throws Exception {
        attachmentService.delete(id);
        return R.ok();
    }
}
