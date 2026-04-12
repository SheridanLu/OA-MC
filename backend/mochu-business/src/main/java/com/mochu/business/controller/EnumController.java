package com.mochu.business.controller;

import com.mochu.business.enums.ContractTypeEnum;
import com.mochu.business.enums.MaterialCategoryEnum;
import com.mochu.business.enums.MaterialTaxRateEnum;
import com.mochu.business.enums.MaterialUnitEnum;
import com.mochu.common.result.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 枚举配置接口
 */
@RestController
@RequestMapping("/api/v1/enums")
public class EnumController {

    @GetMapping("/contract-types")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, String>>> contractTypes() {
        return R.ok(ContractTypeEnum.toList());
    }

    @GetMapping("/material-categories")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, String>>> materialCategories() {
        return R.ok(MaterialCategoryEnum.toList());
    }

    @GetMapping("/material-units")
    @PreAuthorize("isAuthenticated()")
    public R<List<String>> materialUnits() {
        return R.ok(MaterialUnitEnum.toList());
    }

    @GetMapping("/material-tax-rates")
    @PreAuthorize("isAuthenticated()")
    public R<List<Integer>> materialTaxRates() {
        return R.ok(MaterialTaxRateEnum.toList());
    }
}
