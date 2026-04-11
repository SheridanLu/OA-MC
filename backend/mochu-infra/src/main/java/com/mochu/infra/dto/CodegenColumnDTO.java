package com.mochu.infra.dto;

import lombok.Data;

@Data
public class CodegenColumnDTO {
    private Integer tableId;
    private String columnName;
    private String columnComment;
    private String dataType;
    private String javaType;
    private String javaField;
    private String dictType;
    private String htmlType;
    private Integer pkFlag;
    private Integer nullableFlag;
    private Integer createOperation;
    private Integer updateOperation;
    private Integer listOperation;
    private Integer queryOperation;
    private String queryCondition;
    private Integer columnSort;
}
