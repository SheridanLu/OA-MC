package com.mochu.infra.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mochu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_codegen_column")
public class InfraCodegenColumn extends BaseEntity {

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
