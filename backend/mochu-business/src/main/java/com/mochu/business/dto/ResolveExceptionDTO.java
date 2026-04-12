package com.mochu.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 异常工单解决 DTO
 */
@Data
public class ResolveExceptionDTO {
    @NotBlank(message = "解决说明不能为空")
    private String resolveRemark;
}
