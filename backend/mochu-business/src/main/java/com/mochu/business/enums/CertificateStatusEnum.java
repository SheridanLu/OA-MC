package com.mochu.business.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资质状态枚举
 */
@Getter
public enum CertificateStatusEnum {
    VALID("valid", "有效"),
    EXPIRING("expiring", "即将过期"),
    EXPIRED("expired", "已过期");

    private final String code;
    private final String label;

    CertificateStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(String code) {
        return Arrays.stream(values()).anyMatch(e -> e.code.equals(code));
    }

    public static String getLabel(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .map(CertificateStatusEnum::getLabel)
                .findFirst().orElse(code);
    }

    public static List<Map<String, String>> toList() {
        return Arrays.stream(values())
                .map(e -> Map.of("code", e.code, "label", e.label))
                .collect(Collectors.toList());
    }
}
