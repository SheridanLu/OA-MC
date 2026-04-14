package com.mochu.business.controller;

import com.mochu.business.dto.PurchaseListDTO;
import com.mochu.business.dto.SpotPurchaseDTO;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.entity.BizPurchaseList;
import com.mochu.business.entity.BizPurchaseListItem;
import com.mochu.business.entity.BizSpotPurchase;
import com.mochu.business.service.PurchaseService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    // ==================== 采购清单 /api/v1/purchases ====================

    @GetMapping("/api/v1/purchases")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<PageResult<BizPurchaseList>> listPurchases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return R.ok(purchaseService.listPurchaseLists(status, projectId, page, size, sortField, sortOrder));
    }

    @GetMapping("/api/v1/purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<BizPurchaseList> getPurchaseList(@PathVariable Integer id) {
        BizPurchaseList entity = purchaseService.getPurchaseListById(id);
        if (entity == null) return R.fail(404, "采购清单不存在");
        return R.ok(entity);
    }

    @GetMapping("/api/v1/purchases/{id}/items")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<List<BizPurchaseListItem>> getPurchaseListItems(@PathVariable Integer id) {
        return R.ok(purchaseService.getPurchaseListItems(id));
    }

    @Idempotent
    @PostMapping("/api/v1/purchases")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> createPurchaseList(@Valid @RequestBody PurchaseListDTO dto) {
        purchaseService.createPurchaseList(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/api/v1/purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> updatePurchaseList(@PathVariable Integer id, @Valid @RequestBody PurchaseListDTO dto) {
        purchaseService.updatePurchaseList(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/api/v1/purchases/{id}/status")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> updatePurchaseListStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        purchaseService.updatePurchaseListStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/api/v1/purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> deletePurchaseList(@PathVariable Integer id) {
        purchaseService.deletePurchaseList(id);
        return R.ok();
    }

    // ==================== 零星采购 /api/v1/spot-purchases ====================

    @GetMapping("/api/v1/spot-purchases")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<PageResult<BizSpotPurchase>> listSpotPurchases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(purchaseService.listSpotPurchases(status, projectId, page, size));
    }

    @GetMapping("/api/v1/spot-purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<BizSpotPurchase> getSpotPurchase(@PathVariable Integer id) {
        BizSpotPurchase entity = purchaseService.getSpotPurchaseById(id);
        if (entity == null) return R.fail(404, "零星采购不存在");
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/api/v1/spot-purchases")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> createSpotPurchase(@Valid @RequestBody SpotPurchaseDTO dto) {
        purchaseService.createSpotPurchase(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/api/v1/spot-purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> updateSpotPurchase(@PathVariable Integer id, @Valid @RequestBody SpotPurchaseDTO dto) {
        purchaseService.updateSpotPurchase(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/api/v1/spot-purchases/{id}/status")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> updateSpotPurchaseStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        purchaseService.updateSpotPurchaseStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/api/v1/spot-purchases/{id}")
    @PreAuthorize("hasAuthority('purchase:list-manage')")
    public R<Void> deleteSpotPurchase(@PathVariable Integer id) {
        purchaseService.deleteSpotPurchase(id);
        return R.ok();
    }
}
