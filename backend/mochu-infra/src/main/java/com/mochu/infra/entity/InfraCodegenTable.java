package com.mochu.infra.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mochu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_codegen_table")
public class InfraCodegenTable extends BaseEntity {

    private String tableName;
    private String tableComment;
    private String moduleName;
    private String bizName;
    private String className;
    private Integer templateType;
    private String author;
    private String remark;
}
