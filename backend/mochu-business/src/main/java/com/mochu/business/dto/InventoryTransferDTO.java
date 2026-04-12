package com.mochu.business.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryTransferDTO {
    @NotNull(message = "源项目不能为空")
    private Integer fromProjectId;
    @NotNull(message = "目标项目不能为空")
    private Integer toProjectId;
    @NotNull(message = "材料不能为空")
    private Integer materialId;
    @NotNull(message = "调拨数量不能为空")
    @DecimalMin(value = "0.01", message = "调拨数量必须大于0")
    private BigDecimal qty;
    private String remark;
}
