package com.mochu.business.controller;

import com.mochu.business.dto.ExternalContactDTO;
import com.mochu.business.entity.BizExternalContact;
import com.mochu.business.service.ContactService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/external")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<PageResult<BizExternalContact>> listExternal(
            @RequestParam(required = false) String contactType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(contactService.listExternal(contactType, keyword, page, size));
    }

    @GetMapping("/external/{id}")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<BizExternalContact> getExternalById(@PathVariable Integer id) {
        BizExternalContact contact = contactService.getExternalById(id);
        if (contact == null) return R.fail(404, "联系人不存在");
        return R.ok(contact);
    }

    @Idempotent
    @PostMapping("/external")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> createExternal(@Valid @RequestBody ExternalContactDTO dto) {
        contactService.createExternal(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/external/{id}")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> updateExternal(@PathVariable Integer id, @Valid @RequestBody ExternalContactDTO dto) {
        contactService.updateExternal(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/external/{id}")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> deleteExternal(@PathVariable Integer id) {
        contactService.deleteExternal(id);
        return R.ok();
    }
}
