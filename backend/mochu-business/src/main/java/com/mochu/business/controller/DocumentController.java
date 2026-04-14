package com.mochu.business.controller;

import com.mochu.business.entity.BizAttachment;
import com.mochu.business.service.AttachmentService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文档管理接口 — 基于附件系统，bizType='document'
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final AttachmentService attachmentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<PageResult<BizAttachment>> list(
            @RequestParam(required = false) Integer bizId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(attachmentService.list("document", bizId, page, size));
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('doc:upload','doc:download','doc:manage')")
    public R<Void> delete(@PathVariable Integer id) throws Exception {
        attachmentService.delete(id);
        return R.ok();
    }
}
