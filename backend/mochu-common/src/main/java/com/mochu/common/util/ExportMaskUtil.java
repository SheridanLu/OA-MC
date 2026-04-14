package com.mochu.common.util;

import java.util.List;
import java.util.Set;

/**
 * 导出脱敏工具
 *
 * 规则（§7.9）：
 * - 一级数据：仅 GM 和 HR 可导出原文，其他角色显示 "***"
 * - 三级数据：导出时中间部分脱敏（如 138****1234）
 */
public final class ExportMaskUtil {

    private ExportMaskUtil() {}

    /** 一级数据安全等级角色白名单 */
    private static final Set<String> LEVEL1_ALLOWED_ROLES = Set.of("GM", "HR");

    /**
     * 一级脱敏：身份证、银行卡等
     * 非 GM/HR → 输出 "***"
     */
    public static String maskLevel1(String value, List<String> currentRoles) {
        if (value == null) return null;
        boolean allowed = currentRoles.stream()
                .anyMatch(LEVEL1_ALLOWED_ROLES::contains);
        return allowed ? value : "***";
    }

    /**
     * 三级脱敏：手机号
     * 所有角色导出均脱敏中间部分
     */
    public static String maskLevel3Phone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****"
                + phone.substring(phone.length() - 4);
    }

    /**
     * 三级脱敏：身份证号
     */
    public static String maskLevel3IdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********"
                + idCard.substring(idCard.length() - 4);
    }
}
