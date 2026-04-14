package com.mochu.business.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表预聚合
 * Cron: 0 0 3 * * ? — 每日 03:00
 * 功能: 预聚合4种报表数据到 biz_report_cache
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportCacheScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("reportCacheJob")
    public void generateReportCache() {
        String lockKey = "scheduler:lock:report_cache";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(30));
        if (Boolean.FALSE.equals(locked)) return;

        try {
            log.info("=== 报表预聚合开始 ===");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiredAt = now.plusDays(1);

            // 清理过期缓存
            jdbcTemplate.update("DELETE FROM biz_report_cache WHERE expired_at < NOW()");

            // 1. 项目成本汇总
            aggregateReport("project_cost",
                    "SELECT project_id, category_l1, category_l2, " +
                    "SUM(amount) as total_amount, COUNT(*) as record_count " +
                    "FROM biz_cost_ledger WHERE deleted = 0 " +
                    "GROUP BY project_id, category_l1, category_l2",
                    now, expiredAt);

            // 2. 收支对比
            aggregateReport("income_expense",
                    "SELECT p.id as project_id, p.project_name, " +
                    "COALESCE(SUM(CASE WHEN pa.status='collected' THEN pa.amount END), 0) as expense, " +
                    "COALESCE(SUM(CASE WHEN r.id IS NOT NULL THEN r.amount END), 0) as income " +
                    "FROM biz_project p " +
                    "LEFT JOIN biz_payment_apply pa ON pa.project_id = p.id AND pa.deleted = 0 " +
                    "LEFT JOIN biz_receipt r ON r.project_id = p.id AND r.deleted = 0 " +
                    "WHERE p.deleted = 0 GROUP BY p.id, p.project_name",
                    now, expiredAt);

            // 3. 采购进度
            aggregateReport("purchase_progress",
                    "SELECT pl.project_id, COUNT(*) as total_items, " +
                    "SUM(CASE WHEN pli.inbound_quantity >= pli.quantity THEN 1 ELSE 0 END) as completed_items " +
                    "FROM biz_purchase_list pl " +
                    "JOIN biz_purchase_list_item pli ON pli.purchase_list_id = pl.id " +
                    "WHERE pl.deleted = 0 AND pl.status = 'approved' " +
                    "GROUP BY pl.project_id",
                    now, expiredAt);

            // 4. 物资库存汇总
            aggregateReport("inventory_summary",
                    "SELECT project_id, COUNT(*) as material_count, " +
                    "SUM(quantity) as total_quantity, " +
                    "SUM(quantity * avg_price) as total_value " +
                    "FROM biz_inventory WHERE deleted = 0 AND quantity > 0 " +
                    "GROUP BY project_id",
                    now, expiredAt);

            log.info("=== 报表预聚合完成 ===");
        } catch (Exception e) {
            log.error("报表预聚合异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void aggregateReport(String reportType, String sql,
                                  LocalDateTime generatedAt, LocalDateTime expiredAt) {
        try {
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            String json = objectMapper.writeValueAsString(data);
            jdbcTemplate.update(
                    "INSERT INTO biz_report_cache (report_type, data_json, generated_at, expired_at) " +
                    "VALUES (?, ?, ?, ?)",
                    reportType, json, generatedAt, expiredAt);
            log.info("报表[{}]预聚合完成, 数据{}条", reportType, data.size());
        } catch (Exception e) {
            log.error("报表[{}]预聚合失败", reportType, e);
        }
    }
}
