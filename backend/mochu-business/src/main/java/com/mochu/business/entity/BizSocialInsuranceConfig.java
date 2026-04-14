package com.mochu.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mochu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * P6: 社保详细配置表 — 各险种单独配置个人/企业比例
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_social_insurance_config")
public class BizSocialInsuranceConfig extends BaseEntity {

    private Integer userId;

    /** 养老保险基数 */
    private BigDecimal pensionBase;
    /** 养老个人比例(如 8%) */
    private BigDecimal pensionPersonalRate;
    /** 养老企业比例(如 16%) */
    private BigDecimal pensionCompanyRate;

    /** 医疗保险基数 */
    private BigDecimal medicalBase;
    /** 医疗个人比例(如 2%) */
    private BigDecimal medicalPersonalRate;
    /** 医疗企业比例(如 9.5%) */
    private BigDecimal medicalCompanyRate;

    /** 失业保险基数 */
    private BigDecimal unemploymentBase;
    /** 失业个人比例(如 0.5%) */
    private BigDecimal unemploymentPersonalRate;
    /** 失业企业比例(如 0.5%) */
    private BigDecimal unemploymentCompanyRate;

    /** 住房公积金基数 */
    private BigDecimal housingBase;
    /** 公积金个人比例(如 12%) */
    private BigDecimal housingPersonalRate;
    /** 公积金企业比例(如 12%) */
    private BigDecimal housingCompanyRate;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** active/inactive */
    private String status;
}
