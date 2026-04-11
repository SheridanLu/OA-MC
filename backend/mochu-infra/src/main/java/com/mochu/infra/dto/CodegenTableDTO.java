package com.mochu.infra.dto;

import lombok.Data;

@Data
public class CodegenTableDTO {
    private String moduleName;
    private String bizName;
    private String className;
    private Integer templateType;
    private String author;
    private String remark;
}
