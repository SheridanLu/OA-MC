package com.mochu.system.dto;

import lombok.Data;

@Data
public class DictDataDTO {

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
}
