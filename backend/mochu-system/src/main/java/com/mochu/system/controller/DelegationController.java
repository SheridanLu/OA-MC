package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.DelegationDTO;
import com.mochu.system.service.DelegationService;
import com.mochu.system.vo.DelegationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 委托代理接口
 */
@RestController
@RequestMapping("/api/v1/admin/delegations")
@RequiredArgsConstructor
public class DelegationController {

    private final DelegationService delegationService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:delegation')")
    public R<PageResult<DelegationVO>> list(
            @RequestParam(required = false) Integer delegatorId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(delegationService.list(delegatorId, status, page, size));
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('system:delegation:edit')")
    public R<Void> create(@Valid @RequestBody DelegationDTO dto) {
        delegationService.create(dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('system:delegation:edit')")
    public R<Void> revoke(@PathVariable Integer id) {
        delegationService.revoke(id);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:delegation:edit')")
    public R<Void> delete(@PathVariable Integer id) {
        delegationService.delete(id);
        return R.ok();
    }
}
