package com.mochu.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportTemplateDTO {
    @NotBlank(message = "报表名称不能为空")
    private String reportName;
    private String category;
    private String chartType;
    @NotBlank(message = "SQL不能为空")
    private String sqlText;
    private String paramsJson;
    private String xField;
    private String yFields;
    private String description;
}
