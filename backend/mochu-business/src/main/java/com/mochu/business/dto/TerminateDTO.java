package com.mochu.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目/合同中止 DTO
 */
@Data
public class TerminateDTO {
    @NotBlank(message = "中止原因不能为空")
    private String reason;
}
