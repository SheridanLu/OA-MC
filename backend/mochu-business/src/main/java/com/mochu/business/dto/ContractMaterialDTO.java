package com.mochu.business.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * P6: 合同物资明细DTO (用于超量/超价校验)
 */
@Data
public class ContractMaterialDTO {

    private Integer materialId;

    private String materialName;

    private BigDecimal quantity;

    private BigDecimal unitPrice;
}
