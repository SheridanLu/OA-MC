package com.mochu.business.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * P6: 收入拆分明细项DTO
 */
@Data
public class IncomeSplitItemDTO {

    private String taskName;

    private BigDecimal amount;

    private String remark;
}
