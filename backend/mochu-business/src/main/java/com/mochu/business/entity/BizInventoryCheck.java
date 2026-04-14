package com.mochu.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mochu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 盘点单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_inventory_check")
public class BizInventoryCheck extends BaseEntity {

    private String checkNo;

    private Integer projectId;

    private LocalDate checkDate;

    private Integer gainCount;

    private Integer lossCount;

    private String remark;

    private Integer isOverThreshold;

    private String status;
}
