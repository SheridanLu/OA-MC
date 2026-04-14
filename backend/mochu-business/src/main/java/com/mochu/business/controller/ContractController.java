package com.mochu.business.controller;

import com.mochu.business.dto.ContractDTO;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.dto.TerminateDTO;
import com.mochu.business.entity.BizContract;
import com.mochu.business.entity.BizInvoice;
import com.mochu.business.entity.BizPaymentApply;
import com.mochu.business.service.ContractService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.common.security.SecurityUtils;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('contract:view-all','contract:view-own')")
    public R<PageResult<BizContract>> list(
            @RequestParam(required = false) String contractName,
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return R.ok(contractService.list(contractName, contractType, status, projectId, page, size, sortField, sortOrder));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('contract:view-all','contract:view-own')")
    public R<Map<String, Object>> getById(@PathVariable Integer id) {
        return R.ok(contractService.getDetail(id));
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAnyAuthority('contract:sign-income','contract:sign-expense')")
    public R<Void> create(@Valid @RequestBody ContractDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        contractService.create(dto, userId);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('contract:sign-income','contract:sign-expense')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody ContractDTO dto) {
        contractService.update(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('contract:sign-income','contract:sign-expense')")
    public R<Void> updateStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        contractService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('contract:sign-income','contract:sign-expense')")
    public R<Void> delete(@PathVariable Integer id) {
        contractService.delete(id);
        return R.ok();
    }

    // ======================== 合同提交/终止 ========================

    @Idempotent
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('contract:sign-income','contract:sign-expense')")
    public R<Void> submit(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        contractService.submitContract(id, userId);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('contract:terminate')")
    public R<Void> terminate(@PathVariable Integer id, @Valid @RequestBody TerminateDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        contractService.terminateContract(id, dto.getReason(), userId);
        return R.ok();
    }

    // ======================== 补充协议 ========================

    @GetMapping("/{contractId}/supplements")
    @PreAuthorize("hasAnyAuthority('contract:view-all','contract:view-own')")
    public R<List<BizContract>> listSupplements(@PathVariable Integer contractId) {
        return R.ok(contractService.listSupplements(contractId));
    }

    @Idempotent
    @PostMapping("/{contractId}/supplements")
    @PreAuthorize("hasAnyAuthority('contract:sign-income','contract:sign-expense')")
    public R<Void> createSupplement(@PathVariable Integer contractId, @Valid @RequestBody ContractDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        contractService.createSupplement(contractId, dto, userId);
        return R.ok();
    }

    // ======================== 合同关联查询 ========================

    @GetMapping("/{contractId}/payments")
    @PreAuthorize("hasAnyAuthority('contract:view-all','contract:view-own')")
    public R<List<BizPaymentApply>> listPayments(@PathVariable Integer contractId) {
        return R.ok(contractService.listPaymentsByContract(contractId));
    }

    @GetMapping("/{contractId}/invoices")
    @PreAuthorize("hasAnyAuthority('contract:view-all','contract:view-own')")
    public R<List<BizInvoice>> listInvoices(@PathVariable Integer contractId) {
        return R.ok(contractService.listInvoicesByContract(contractId));
    }

    @GetMapping("/{contractId}/overquantity-check")
    @PreAuthorize("hasAnyAuthority('contract:view-all','contract:view-own')")
    public R<Map<String, Object>> checkOverquantity(@PathVariable Integer contractId) {
        return R.ok(contractService.checkOverquantity(contractId));
    }
}
