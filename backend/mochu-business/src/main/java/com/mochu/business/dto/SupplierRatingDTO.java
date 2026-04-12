package com.mochu.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierRatingDTO {
    @NotNull(message = "供应商不能为空")
    private Integer supplierId;
    private Integer purchaseId;
    private Integer projectId;
    @NotNull(message = "质量评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer qualityScore;
    @NotNull(message = "交付评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer deliveryScore;
    @NotNull(message = "服务评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer serviceScore;
    @NotNull(message = "价格评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer priceScore;
    private String commentText;
}
