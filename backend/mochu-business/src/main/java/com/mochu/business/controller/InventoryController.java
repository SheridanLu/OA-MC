package com.mochu.business.controller;

import com.mochu.business.dto.InboundOrderDTO;
import com.mochu.business.dto.InventoryCheckDTO;
import com.mochu.business.dto.OutboundOrderDTO;
import com.mochu.business.dto.ReturnOrderDTO;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.entity.BizInboundOrder;
import com.mochu.business.entity.BizInventory;
import com.mochu.business.entity.BizInventoryCheck;
import com.mochu.business.entity.BizOutboundOrder;
import com.mochu.business.entity.BizReturnOrder;
import com.mochu.business.service.InventoryService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存管理接口 — 入库/出库/退库/盘点/库存
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ======================== 入库单 /inbound ========================

    @GetMapping("/inbound")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public R<PageResult<BizInboundOrder>> listInbound(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status) {
        return R.ok(inventoryService.listInbound(page, size, projectId, status));
    }

    @GetMapping("/inbound/{id}")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public R<BizInboundOrder> getInbound(@PathVariable Integer id) {
        BizInboundOrder order = inventoryService.getInboundById(id);
        if (order == null) {
            return R.fail(404, "入库单不存在");
        }
        return R.ok(order);
    }

    @PostMapping("/inbound")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public R<Void> createInbound(@Valid @RequestBody InboundOrderDTO dto) {
        inventoryService.createInbound(dto);
        return R.ok();
    }

    @PutMapping("/inbound/{id}")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public R<Void> updateInbound(@PathVariable Integer id, @Valid @RequestBody InboundOrderDTO dto) {
        inventoryService.updateInbound(id, dto);
        return R.ok();
    }

    @PatchMapping("/inbound/{id}/status")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public R<Void> updateInboundStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        inventoryService.updateInboundStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/inbound/{id}")
    @PreAuthorize("hasAuthority('inventory:inbound')")
    public R<Void> deleteInbound(@PathVariable Integer id) {
        inventoryService.deleteInbound(id);
        return R.ok();
    }

    // ======================== 出库单 /outbound ========================

    @GetMapping("/outbound")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public R<PageResult<BizOutboundOrder>> listOutbound(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status) {
        return R.ok(inventoryService.listOutbound(page, size, projectId, status));
    }

    @GetMapping("/outbound/{id}")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public R<BizOutboundOrder> getOutbound(@PathVariable Integer id) {
        BizOutboundOrder order = inventoryService.getOutboundById(id);
        if (order == null) {
            return R.fail(404, "出库单不存在");
        }
        return R.ok(order);
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public R<Void> createOutbound(@Valid @RequestBody OutboundOrderDTO dto) {
        inventoryService.createOutbound(dto);
        return R.ok();
    }

    @PutMapping("/outbound/{id}")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public R<Void> updateOutbound(@PathVariable Integer id, @Valid @RequestBody OutboundOrderDTO dto) {
        inventoryService.updateOutbound(id, dto);
        return R.ok();
    }

    @PatchMapping("/outbound/{id}/status")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public R<Void> updateOutboundStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        inventoryService.updateOutboundStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/outbound/{id}")
    @PreAuthorize("hasAuthority('inventory:outbound')")
    public R<Void> deleteOutbound(@PathVariable Integer id) {
        inventoryService.deleteOutbound(id);
        return R.ok();
    }

    // ======================== 退库单 /return ========================

    @GetMapping("/return")
    @PreAuthorize("hasAuthority('inventory:return')")
    public R<PageResult<BizReturnOrder>> listReturn(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status) {
        return R.ok(inventoryService.listReturn(page, size, projectId, status));
    }

    @GetMapping("/return/{id}")
    @PreAuthorize("hasAuthority('inventory:return')")
    public R<BizReturnOrder> getReturn(@PathVariable Integer id) {
        BizReturnOrder order = inventoryService.getReturnById(id);
        if (order == null) {
            return R.fail(404, "退库单不存在");
        }
        return R.ok(order);
    }

    @PostMapping("/return")
    @PreAuthorize("hasAuthority('inventory:return')")
    public R<Void> createReturn(@Valid @RequestBody ReturnOrderDTO dto) {
        inventoryService.createReturn(dto);
        return R.ok();
    }

    @PutMapping("/return/{id}")
    @PreAuthorize("hasAuthority('inventory:return')")
    public R<Void> updateReturn(@PathVariable Integer id, @Valid @RequestBody ReturnOrderDTO dto) {
        inventoryService.updateReturn(id, dto);
        return R.ok();
    }

    @PatchMapping("/return/{id}/status")
    @PreAuthorize("hasAuthority('inventory:return')")
    public R<Void> updateReturnStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        inventoryService.updateReturnStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/return/{id}")
    @PreAuthorize("hasAuthority('inventory:return')")
    public R<Void> deleteReturn(@PathVariable Integer id) {
        inventoryService.deleteReturn(id);
        return R.ok();
    }

    // ======================== 盘点 /check ========================

    @GetMapping("/check")
    @PreAuthorize("hasAuthority('inventory:check')")
    public R<PageResult<BizInventoryCheck>> listCheck(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String status) {
        return R.ok(inventoryService.listCheck(page, size, projectId, status));
    }

    @GetMapping("/check/{id}")
    @PreAuthorize("hasAuthority('inventory:check')")
    public R<BizInventoryCheck> getCheck(@PathVariable Integer id) {
        BizInventoryCheck check = inventoryService.getCheckById(id);
        if (check == null) {
            return R.fail(404, "盘点单不存在");
        }
        return R.ok(check);
    }

    @PostMapping("/check")
    @PreAuthorize("hasAuthority('inventory:check')")
    public R<Void> createCheck(@Valid @RequestBody InventoryCheckDTO dto) {
        inventoryService.createCheck(dto);
        return R.ok();
    }

    @PutMapping("/check/{id}")
    @PreAuthorize("hasAuthority('inventory:check')")
    public R<Void> updateCheck(@PathVariable Integer id, @Valid @RequestBody InventoryCheckDTO dto) {
        inventoryService.updateCheck(id, dto);
        return R.ok();
    }

    @PatchMapping("/check/{id}/status")
    @PreAuthorize("hasAuthority('inventory:check')")
    public R<Void> updateCheckStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        inventoryService.updateCheckStatus(id, dto.getStatus());
        return R.ok();
    }

    @DeleteMapping("/check/{id}")
    @PreAuthorize("hasAuthority('inventory:check')")
    public R<Void> deleteCheck(@PathVariable Integer id) {
        inventoryService.deleteCheck(id);
        return R.ok();
    }

    // ======================== 库存 /stock ========================

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('inventory:stock-view')")
    public R<PageResult<BizInventory>> listStock(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer materialId) {
        return R.ok(inventoryService.listStock(page, size, projectId, materialId));
    }

    @GetMapping("/stock/{id}")
    @PreAuthorize("hasAuthority('inventory:stock-view')")
    public R<BizInventory> getStock(@PathVariable Integer id) {
        BizInventory inventory = inventoryService.getStockById(id);
        if (inventory == null) {
            return R.fail(404, "库存记录不存在");
        }
        return R.ok(inventory);
    }
}
