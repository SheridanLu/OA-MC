package com.mochu.business.controller;

import com.mochu.business.dto.SupplierDTO;
import com.mochu.business.entity.BizSupplier;
import com.mochu.business.service.SupplierService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('contract:sign-expense')")
    public R<PageResult<BizSupplier>> list(
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return R.ok(supplierService.list(supplierName, status, page, size, sortField, sortOrder));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('contract:sign-expense')")
    public R<List<BizSupplier>> listAll() {
        return R.ok(supplierService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contract:sign-expense')")
    public R<BizSupplier> getById(@PathVariable Integer id) {
        BizSupplier supplier = supplierService.getById(id);
        if (supplier == null) return R.fail(404, "供应商不存在");
        return R.ok(supplier);
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('contract:sign-expense')")
    public R<Void> create(@Valid @RequestBody SupplierDTO dto) {
        supplierService.create(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('contract:sign-expense')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody SupplierDTO dto) {
        supplierService.update(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('contract:sign-expense')")
    public R<Void> delete(@PathVariable Integer id) {
        supplierService.delete(id);
        return R.ok();
    }
}
