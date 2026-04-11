package com.mochu.infra.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CodegenTableVO {
    private Integer id;
    private String tableName;
    private String tableComment;
    private String moduleName;
    private String bizName;
    private String className;
    private Integer templateType;
    private String author;
    private String remark;
    private Integer creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CodegenColumnVO> columns;
}
