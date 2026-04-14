package com.mochu.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mochu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪资配置表 — V3.2 P.54 per-employee model
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_salary_config")
public class BizSalaryConfig extends BaseEntity {

    /** V3.2: 关联员工ID (per-employee) */
    private Integer userId;

    private String grade;

    private String gradeName;

    private BigDecimal baseSalary;

    /** V3.2: 岗位工资 */
    private BigDecimal positionSalary;

    /** V3.2: 绩效基数 */
    private BigDecimal performance;

    private BigDecimal allowance;

    /** V3.2: 生效日期 */
    private LocalDate effectiveDate;

    private String remark;

    private String status;
}
