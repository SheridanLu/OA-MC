package com.mochu.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * P6: 虚拟项目DTO
 */
@Data
public class VirtualProjectDTO {

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    private BigDecimal investLimit;

    private LocalDate bidTime;

    private String remark;
}
