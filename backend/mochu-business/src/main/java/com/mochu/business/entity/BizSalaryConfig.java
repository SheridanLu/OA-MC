package com.mochu.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mochu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪资配置表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_salary_config")
public class BizSalaryConfig extends BaseEntity {

    /** P6: 关联用户ID */
    private Integer userId;

    private String grade;

    private String gradeName;

    private BigDecimal baseSalary;

    private BigDecimal allowance;

    /** P6: 生效日期 */
    private LocalDate effectiveDate;

    private String remark;

    private String status;
}
