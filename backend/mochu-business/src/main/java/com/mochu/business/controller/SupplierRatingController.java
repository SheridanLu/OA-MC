package com.mochu.business.controller;

import com.mochu.business.dto.SupplierRatingDTO;
import com.mochu.business.entity.BizSupplierRating;
import com.mochu.business.service.SupplierRatingService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/supplier/rating")
@RequiredArgsConstructor
public class SupplierRatingController {

    private final SupplierRatingService ratingService;

    @GetMapping
    @PreAuthorize("hasAuthority('supplier:rating')")
    public R<PageResult<BizSupplierRating>> list(
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(ratingService.listRatings(supplierId, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('supplier:view','supplier:rating')")
    public R<List<Map<String, Object>>> summary() {
        return R.ok(ratingService.supplierRatingSummary());
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('supplier:rating')")
    public R<Void> create(@Valid @RequestBody SupplierRatingDTO dto) {
        ratingService.createRating(dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:rating')")
    public R<Void> delete(@PathVariable Integer id) {
        ratingService.deleteRating(id);
        return R.ok();
    }
}
