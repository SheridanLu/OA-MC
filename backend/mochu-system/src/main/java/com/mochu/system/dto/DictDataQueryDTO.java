package com.mochu.system.dto;

import lombok.Data;

@Data
public class DictDataQueryDTO {

    private String dictType;

    private Integer status;

    private Integer page;

    private Integer size;
}
