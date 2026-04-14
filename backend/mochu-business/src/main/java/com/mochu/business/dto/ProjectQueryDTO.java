package com.mochu.business.dto;

import lombok.Data;

@Data
public class ProjectQueryDTO {

    private String projectName;

    private String projectNo;

    private Integer projectType;

    private String status;

    private Integer managerId;

    private Integer page;

    private Integer size;

    /** V3.2: 排序字段 */
    private String sortField;

    /** V3.2: 排序方向 (asc/desc) */
    private String sortOrder;
}
