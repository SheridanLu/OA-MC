package com.mochu.business.controller;

import com.mochu.business.dto.InventoryAlertDTO;
import com.mochu.business.dto.InventoryTransferDTO;
import com.mochu.business.entity.BizInventoryAlert;
import com.mochu.business.entity.BizInventoryTransfer;
import com.mochu.business.service.InventoryEnhanceService;
import com.mochu.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryEnhanceController {

    private final InventoryEnhanceService enhanceService;

    // ==================== 库存预警 ====================

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('inventory:alert-manage')")
    public R<List<BizInventoryAlert>> listAlerts(@RequestParam(required = false) Integer projectId) {
        return R.ok(enhanceService.listAlerts(projectId));
    }

    @GetMapping("/alerts/triggered")
    @PreAuthorize("hasAuthority('inventory:stock-view')")
    public R<List<Object>> listTriggeredAlerts(@RequestParam(required = false) Integer projectId) {
        return R.ok(enhanceService.listTriggeredAlerts(projectId));
    }

    @PostMapping("/alerts")
    @PreAuthorize("hasAuthority('inventory:alert-manage')")
    public R<Void> saveAlert(@Valid @RequestBody InventoryAlertDTO dto) {
        enhanceService.saveAlert(dto);
        return R.ok();
    }

    @DeleteMapping("/alerts/{id}")
    @PreAuthorize("hasAuthority('inventory:alert-manage')")
    public R<Void> deleteAlert(@PathVariable Integer id) {
        enhanceService.deleteAlert(id);
        return R.ok();
    }

    // ==================== 库存调拨 ====================

    @GetMapping("/transfers")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public R<List<BizInventoryTransfer>> listTransfers(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status) {
        return R.ok(enhanceService.listTransfers(projectId, status));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public R<Void> createTransfer(@Valid @RequestBody InventoryTransferDTO dto) {
        enhanceService.createTransfer(dto);
        return R.ok();
    }

    @PutMapping("/transfers/{id}/confirm")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public R<Void> confirmTransfer(@PathVariable Integer id) {
        enhanceService.confirmTransfer(id);
        return R.ok();
    }

    @PutMapping("/transfers/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public R<Void> cancelTransfer(@PathVariable Integer id) {
        enhanceService.cancelTransfer(id);
        return R.ok();
    }
}
