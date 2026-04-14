package com.mochu.business.scheduler;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志归档
 * Cron: 0 0 2 1 * ? — 每月1日 02:00
 * 功能: 归档12个月前的审计日志，物理删除原表数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogArchiveScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("auditLogArchiveJob")
    public void archiveOldLogs() {
        String lockKey = "scheduler:lock:audit_archive";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(30));
        if (Boolean.FALSE.equals(locked)) return;

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMonths(12);
            String cutoffStr = cutoff.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("=== 审计日志归档开始, 截止日期: {} ===", cutoffStr);

            // 确保归档表存在
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS sys_audit_log_archive LIKE sys_audit_log");

            // 归档：插入到归档表
            int archived = jdbcTemplate.update(
                    "INSERT INTO sys_audit_log_archive SELECT * FROM sys_audit_log " +
                    "WHERE created_at < ?", cutoffStr);

            // 删除原表数据
            if (archived > 0) {
                int deleted = jdbcTemplate.update(
                        "DELETE FROM sys_audit_log WHERE created_at < ?", cutoffStr);
                log.info("=== 归档完成: 归档{}条, 删除{}条 ===", archived, deleted);
            } else {
                log.info("=== 无需归档的日志 ===");
            }
        } catch (Exception e) {
            log.error("审计日志归档异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
