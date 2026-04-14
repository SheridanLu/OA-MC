package com.mochu.business.service;

import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报表订阅服务 — 使用 JdbcTemplate 操作 biz_report_subscribe 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSubscribeService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询用户订阅列表（分页）
     */
    public PageResult<Map<String, Object>> listByUser(Integer userId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;
        int offset = (p - 1) * s;

        // 查询总数
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_report_subscribe WHERE user_id = ? AND deleted = 0",
                Long.class, userId);
        if (total == null) total = 0L;

        // 查询分页数据
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, user_id, report_type, report_name, cron_expr, email, "
                + "created_at, updated_at "
                + "FROM biz_report_subscribe "
                + "WHERE user_id = ? AND deleted = 0 "
                + "ORDER BY created_at DESC "
                + "LIMIT ? OFFSET ?",
                userId, s, offset);

        return new PageResult<>(rows, total, p, s);
    }

    /**
     * 创建订阅
     */
    public void subscribe(Integer userId, Map<String, Object> body) {
        String reportType = (String) body.get("reportType");
        String reportName = (String) body.get("reportName");
        String cronExpr = (String) body.get("cronExpr");
        String email = (String) body.get("email");

        if (reportType == null || reportType.isBlank()) {
            throw new BusinessException("报表类型不能为空");
        }

        jdbcTemplate.update(
                "INSERT INTO biz_report_subscribe (user_id, report_type, report_name, cron_expr, email, deleted, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
                userId, reportType, reportName, cronExpr, email,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 取消订阅（逻辑删除，校验所有权）
     */
    public void unsubscribe(Integer id, Integer userId) {
        int affected = jdbcTemplate.update(
                "UPDATE biz_report_subscribe SET deleted = 1, updated_at = ? WHERE id = ? AND user_id = ? AND deleted = 0",
                LocalDateTime.now(), id, userId);
        if (affected == 0) {
            throw new BusinessException("订阅记录不存在或无权操作");
        }
    }
}
