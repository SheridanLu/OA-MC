package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.BizProject;
import com.mochu.business.entity.BizStatement;
import com.mochu.business.mapper.BizProjectMapper;
import com.mochu.business.mapper.BizStatementMapper;
import com.mochu.business.service.NoGeneratorService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 对账单自动生成
 * Cron: 0 0 6 26 * ? — 每月26日 06:00
 * 功能: 为所有活跃项目自动生成当月对账单草稿
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatementGenerateScheduler {

    private final BizProjectMapper projectMapper;
    private final BizStatementMapper statementMapper;
    private final NoGeneratorService noGeneratorService;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("statementGenerateJob")
    public void generateMonthlyStatements() {
        String lockKey = "scheduler:lock:statement_generate";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(15));
        if (Boolean.FALSE.equals(locked)) return;

        try {
            YearMonth month = YearMonth.now();
            log.info("=== {}月对账单自动生成开始 ===", month);

            List<BizProject> projects = projectMapper.selectList(
                    new LambdaQueryWrapper<BizProject>()
                            .eq(BizProject::getStatus, "active")
                            .eq(BizProject::getDeleted, 0));

            int count = 0;
            for (BizProject project : projects) {
                try {
                    // 汇总当月材料出库金额
                    LocalDate monthStart = month.atDay(1);
                    LocalDate monthEnd = month.atEndOfMonth();

                    BigDecimal materialAmount = querySum(
                            "SELECT COALESCE(SUM(oi.subtotal), 0) FROM biz_outbound_order o " +
                            "JOIN biz_outbound_order_item oi ON oi.outbound_id = o.id " +
                            "WHERE o.project_id = ? AND o.status = 'approved' " +
                            "AND o.outbound_date BETWEEN ? AND ? AND o.deleted = 0",
                            project.getId(), monthStart, monthEnd);

                    BizStatement statement = new BizStatement();
                    statement.setProjectId(project.getId());
                    statement.setStatementNo(noGeneratorService.generate("DZ"));
                    statement.setYearMonth(month.toString());
                    statement.setMaterialAmount(materialAmount);
                    statement.setTotalAmount(materialAmount);
                    statement.setStatus("draft");
                    statementMapper.insert(statement);
                    count++;
                } catch (Exception e) {
                    log.error("项目{}对账单生成失败", project.getProjectNo(), e);
                }
            }
            log.info("=== 对账单生成完成: {}条 ===", count);
        } catch (Exception e) {
            log.error("对账单生成异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private BigDecimal querySum(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
