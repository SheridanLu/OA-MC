package com.mochu.business.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * P6: 变更台账查询DTO
 */
@Data
public class ChangeLedgerQueryDTO {

    private Integer projectId;

    private String changeType;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer page;

    private Integer size;
}
