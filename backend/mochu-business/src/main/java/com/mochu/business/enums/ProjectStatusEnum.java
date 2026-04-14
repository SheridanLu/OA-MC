package com.mochu.business.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目状态枚举
 */
@Getter
public enum ProjectStatusEnum {
    ACTIVE("active", "进行中"),
    SUSPENDED("suspended", "已暂停"),
    COMPLETION_ACCEPTED("completion_accepted", "已完工验收"),
    FINAL_ACCEPTED("final_accepted", "已竣工验收"),
    AUDIT_DONE("audit_done", "已完成审计"),
    CLOSED("closed", "已关闭"),
    TERMINATED("terminated", "已终止"),
    TRACKING("tracking", "跟踪中"),
    CONVERTED("converted", "已转实体");

    private final String code;
    private final String label;

    ProjectStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(String code) {
        return Arrays.stream(values()).anyMatch(e -> e.code.equals(code));
    }

    public static String getLabel(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .map(ProjectStatusEnum::getLabel)
                .findFirst().orElse(code);
    }

    public static List<Map<String, String>> toList() {
        return Arrays.stream(values())
                .map(e -> Map.of("code", e.code, "label", e.label))
                .collect(Collectors.toList());
    }
}
