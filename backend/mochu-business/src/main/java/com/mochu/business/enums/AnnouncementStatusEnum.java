package com.mochu.business.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公告状态枚举
 */
@Getter
public enum AnnouncementStatusEnum {
    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    OFFLINE("offline", "已下线"),
    EXPIRED("expired", "已过期");

    private final String code;
    private final String label;

    AnnouncementStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(String code) {
        return Arrays.stream(values()).anyMatch(e -> e.code.equals(code));
    }

    public static String getLabel(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .map(AnnouncementStatusEnum::getLabel)
                .findFirst().orElse(code);
    }

    public static List<Map<String, String>> toList() {
        return Arrays.stream(values())
                .map(e -> Map.of("code", e.code, "label", e.label))
                .collect(Collectors.toList());
    }
}
