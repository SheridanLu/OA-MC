package com.mochu.business.enums;

import lombok.Getter;

/**
 * P6: 退库处置方式枚举
 */
@Getter
public enum ReturnDisposalType {

    ON_SITE("on_site", "现场处理"),
    RETURN_TO_SUPPLIER("return_to_supplier", "退回厂家"),
    TO_COMPANY_WAREHOUSE("to_company", "入公司库"),
    INTER_PROJECT_TRANSFER("transfer", "项目间调拨");

    private final String code;
    private final String label;

    ReturnDisposalType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(String code) {
        for (ReturnDisposalType type : values()) {
            if (type.code.equals(code)) return true;
        }
        return false;
    }
}
