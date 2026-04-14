package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.BizHrCertificate;
import com.mochu.business.mapper.BizHrCertificateMapper;
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
 * 资质到期预警
 * Cron: 0 30 0 * * ? — 每日 00:30
 * 功能: 扫描资质到期前60/30/15天分级预警
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateExpireScheduler {

    private final BizHrCertificateMapper certMapper;
    private final SysTodoMapper todoMapper;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("certificateExpireJob")
    public void checkCertificateExpiry() {
        String lockKey = "scheduler:lock:cert_expire";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(locked)) return;

        try {
            log.info("=== 资质到期预警开始 ===");
            LocalDate today = LocalDate.now();
            // 查询60天内到期的资质
            List<BizHrCertificate> certs = certMapper.selectList(
                    new LambdaQueryWrapper<BizHrCertificate>()
                            .le(BizHrCertificate::getExpireDate, today.plusDays(60))
                            .ge(BizHrCertificate::getExpireDate, today)
                            .ne(BizHrCertificate::getStatus, "expired")
                            .eq(BizHrCertificate::getDeleted, 0));

            int count = 0;
            for (BizHrCertificate cert : certs) {
                long remainDays = ChronoUnit.DAYS.between(today, cert.getExpireDate());

                if (remainDays <= 15) {
                    // 通知HR+员工+部门负责人
                    createTodo(cert, remainDays, "HR");
                    createTodo(cert, remainDays, "employee");
                    createTodo(cert, remainDays, "dept_leader");
                } else if (remainDays <= 30) {
                    createTodo(cert, remainDays, "HR");
                    createTodo(cert, remainDays, "employee");
                } else {
                    createTodo(cert, remainDays, "HR");
                }

                // 更新状态为expiring
                if (!"expiring".equals(cert.getStatus())) {
                    cert.setStatus("expiring");
                    certMapper.updateById(cert);
                }
                count++;
            }
            log.info("=== 资质到期预警完成: 处理{}条 ===", count);
        } catch (Exception e) {
            log.error("资质到期预警异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void createTodo(BizHrCertificate cert, long remainDays, String target) {
        String dedupKey = String.format("cert:warned:%d:%s:%d",
                cert.getId(), target, remainDays / 15);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) return;

        SysTodo todo = new SysTodo();
        todo.setUserId(resolveTargetUserId(cert, target));
        todo.setTitle(String.format("【资质预警】%s的%s还剩%d天到期",
                cert.getUserName(), cert.getCertName(), remainDays));
        todo.setBizType("cert_expire_warning");
        todo.setBizId(cert.getId());
        todo.setStatus(0);
        todoMapper.insert(todo);

        redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofDays(14));
    }

    private Integer resolveTargetUserId(BizHrCertificate cert, String target) {
        // 简化：HR通知管理员(ID=1)，employee通知证书持有人
        return "employee".equals(target) ? cert.getUserId() : 1;
    }
}
