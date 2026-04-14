package com.mochu.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统待办表 — 对照 V3.2 附录P.12
 */
@Data
@TableName("sys_todo")
public class SysTodo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    /** 待办关联业务类型 */
    private String bizType;

    /** 待办关联业务单据ID */
    private Integer bizId;

    private String title;

    private String content;

    /** 0待处理/1已处理 */
    private Integer status;

    /** 已读时间 */
    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
