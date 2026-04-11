package com.mochu.infra.dto;

import lombok.Data;

@Data
public class CodegenTableQueryDTO {
    private String tableName;
    private Integer page;
    private Integer size;
}
