package com.mochu.business.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryConfigDTO {

    private Integer id;

    /** P6: 关联用户ID（用于个人薪资配置场景） */
    private Integer userId;

    @NotNull(message = "薪资等级不能为空")
    private String grade;

    @NotNull(message = "等级名称不能为空")
    private String gradeName;

    @NotNull(message = "基本工资不能为空")
    private BigDecimal baseSalary;

    private BigDecimal allowance;

    /** P6: 生效日期 — 不早于当月 */
    private LocalDate effectiveDate;

    private String remark;

    /* ---------- 查询条件 ---------- */

    private Integer page;

    private Integer size;

    private String status;
}
