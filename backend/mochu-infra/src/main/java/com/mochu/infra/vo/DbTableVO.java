package com.mochu.infra.vo;

import lombok.Data;

@Data
public class DbTableVO {
    private String tableName;
    private String tableComment;
    private String createTime;
}
