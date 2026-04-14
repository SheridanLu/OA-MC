package com.mochu.business.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同状态枚举
 */
@Getter
public enum ContractStatusEnum {
    DRAFT("draft", "草稿"),
    PENDING("pending", "待审批"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已驳回"),
    TERMINATED("terminated", "已终止");

    private final String code;
    private final String label;

    ContractStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(String code) {
        return Arrays.stream(values()).anyMatch(e -> e.code.equals(code));
    }

    public static String getLabel(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .map(ContractStatusEnum::getLabel)
                .findFirst().orElse(code);
    }

    public static List<Map<String, String>> toList() {
        return Arrays.stream(values())
                .map(e -> Map.of("code", e.code, "label", e.label))
                .collect(Collectors.toList());
    }
}
