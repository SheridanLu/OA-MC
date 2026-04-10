package com.mochu.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictTypeVO {

    private Integer id;

    private String dictType;

    private String dictName;

    private Integer status;

    private String remark;

    private Integer creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
