package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.BizHrContract;
import com.mochu.business.mapper.BizHrContractMapper;
import com.mochu.system.entity.SysTodo;
import com.mochu.system.mapper.SysTodoMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 人员合同到期预警
 * Cron: 0 45 0 * * ? — 每日 00:45
 * 功能: 扫描人员合同到期前30/15/7天预警
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HrContractExpireScheduler {

    private final BizHrContractMapper contractMapper;
    private final SysTodoMapper todoMapper;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("hrContractExpireJob")
    public void checkHrContractExpiry() {
        String lockKey = "scheduler:lock:hr_contract_expire";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(locked)) return;

        try {
            log.info("=== 人员合同到期预警开始 ===");
            LocalDate today = LocalDate.now();
            List<BizHrContract> contracts = contractMapper.selectList(
                    new LambdaQueryWrapper<BizHrContract>()
                            .eq(BizHrContract::getStatus, "active")
                            .le(BizHrContract::getEndDate, today.plusDays(30))
                            .ge(BizHrContract::getEndDate, today)
                            .eq(BizHrContract::getDeleted, 0));

            for (BizHrContract c : contracts) {
                long days = ChronoUnit.DAYS.between(today, c.getEndDate());
                if (days == 30 || days == 15 || days == 7) {
                    // 通知HR
                    SysTodo hrTodo = new SysTodo();
                    hrTodo.setUserId(1);
                    hrTodo.setTitle(String.format("【合同预警】员工%s的劳动合同还剩%d天到期",
                            c.getEmployeeName(), days));
                    hrTodo.setBizType("hr_contract_expire");
                    hrTodo.setBizId(c.getId());
                    hrTodo.setStatus(0);
                    todoMapper.insert(hrTodo);

                    // 通知员工
                    SysTodo empTodo = new SysTodo();
                    empTodo.setUserId(c.getUserId());
                    empTodo.setTitle(String.format("【合同提醒】您的劳动合同还剩%d天到期", days));
                    empTodo.setBizType("hr_contract_expire");
                    empTodo.setBizId(c.getId());
                    empTodo.setStatus(0);
                    todoMapper.insert(empTodo);
                }
            }
            log.info("=== 人员合同到期预警完成 ===");
        } catch (Exception e) {
            log.error("人员合同到期预警异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
