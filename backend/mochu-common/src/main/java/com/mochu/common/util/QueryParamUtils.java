package com.mochu.common.util;

import com.mochu.common.exception.BusinessException;

import java.util.Set;

/**
 * 查询参数规范化工具 — P5 §6.1 / V3.2 §6.1
 * 分页 size 强制 <= 100, 排序字段白名单校验
 */
public class QueryParamUtils {

    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 20;

    /** 通用排序白名单 */
    private static final Set<String> SORT_WHITELIST = Set.of(
            "created_at", "updated_at", "id", "project_no", "contract_no",
            "amount", "amount_with_tax", "status", "plan_start_date",
            "plan_end_date", "sign_date", "inbound_date"
    );

    /**
     * 规范化分页 size — 超过100强制取100
     */
    public static int normalizeSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 校验排序字段是否在白名单中
     */
    public static void validateSortField(String sortField) {
        if (sortField != null && !sortField.isBlank()
                && !SORT_WHITELIST.contains(sortField)) {
            throw new BusinessException(400,
                    "不支持的排序字段: " + sortField);
        }
    }

    /**
     * V3.2: 校验排序字段是否在给定白名单中，通过则返回字段名，否则抛异常。
     * @param sortField  前端传入的排序字段
     * @param allowedFields 允许的字段集合
     * @return 经过校验的安全字段名
     */
    public static String validateSortField(String sortField, Set<String> allowedFields) {
        if (sortField == null || sortField.isBlank()) {
            return null;
        }
        if (!allowedFields.contains(sortField)) {
            throw new BusinessException(400, "不支持的排序字段: " + sortField);
        }
        return sortField;
    }

    /**
     * V3.2: 构建 ORDER BY SQL 片段（已白名单校验，安全拼接）。
     * 返回形如 "ORDER BY created_at DESC" 的字符串，可直接传给 wrapper.last()。
     * 如果 sortField 为空返回 null，调用方应使用默认排序。
     *
     * @param sortField     排序字段
     * @param sortOrder     排序方向 (asc/desc)，默认 desc
     * @param allowedFields 允许的字段白名单
     * @return ORDER BY 子句或 null
     */
    public static String buildOrderClause(String sortField, String sortOrder, Set<String> allowedFields) {
        String field = validateSortField(sortField, allowedFields);
        if (field == null) {
            return null;
        }
        String direction = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        return "ORDER BY " + field + " " + direction;
    }
}
