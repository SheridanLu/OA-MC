package com.mochu.business.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryAlertDTO {
    @NotNull(message = "项目不能为空")
    private Integer projectId;
    @NotNull(message = "材料不能为空")
    private Integer materialId;
    private String materialName;
    private String unit;
    private BigDecimal safetyQty;
    private BigDecimal minQty;
    private Integer alertEnabled;
}
