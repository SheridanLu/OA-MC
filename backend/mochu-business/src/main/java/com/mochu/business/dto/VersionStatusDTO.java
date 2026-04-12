package com.mochu.business.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模板版本状态更新 DTO
 */
@Data
public class VersionStatusDTO {
    @NotNull(message = "状态值不能为空")
    private Integer status;
}
