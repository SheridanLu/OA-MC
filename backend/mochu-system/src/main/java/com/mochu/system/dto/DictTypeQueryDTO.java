package com.mochu.system.dto;

import lombok.Data;

@Data
public class DictTypeQueryDTO {

    private String dictType;

    private String dictName;

    private Integer status;

    private Integer page;

    private Integer size;
}
