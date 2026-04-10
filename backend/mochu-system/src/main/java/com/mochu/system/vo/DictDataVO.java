package com.mochu.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictDataVO {

    private Integer id;

    private String dictType;

    private String dictLabel;

    private String dictValue;

    private Integer dictSort;

    private String cssClass;

    private String listClass;

    private String colorHex;

    private Integer isDefault;

    private Integer status;

    private String remark;

    private Integer creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
