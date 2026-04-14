package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mochu.system.entity.SysDelegation;
import com.mochu.system.mapper.SysDelegationMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 委托到期回收
 * Cron: 0 5 0 * * ? — 每日 00:05
 * 功能: 扫描已过期委托，回收权限，清理Redis缓存
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DelegationExpireScheduler {

    private final SysDelegationMapper delegationMapper;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("delegationExpireJob")
    public void revokeExpiredDelegations() {
        String lockKey = "scheduler:lock:delegation_expire";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(locked)) {
            log.info("委托到期回收任务已有实例在运行，跳过");
            return;
        }

        try {
            log.info("=== 委托到期回收任务开始 ===");
            List<SysDelegation> expired = delegationMapper.selectList(
                    new LambdaQueryWrapper<SysDelegation>()
                            .eq(SysDelegation::getStatus, 1)
                            .lt(SysDelegation::getEndTime, LocalDateTime.now()));

            if (expired.isEmpty()) {
                log.info("无过期委托");
                return;
            }

            // 批量更新状态
            List<Integer> ids = expired.stream()
                    .map(SysDelegation::getId).collect(Collectors.toList());
            delegationMapper.update(null, new LambdaUpdateWrapper<SysDelegation>()
                    .in(SysDelegation::getId, ids)
                    .set(SysDelegation::getStatus, 0));

            // 清理被委托人的权限缓存
            expired.stream()
                    .map(SysDelegation::getDelegateeId)
                    .distinct()
                    .forEach(uid -> {
                        redisTemplate.delete("user:permissions:" + uid);
                        log.info("已清理用户{}的权限缓存", uid);
                    });

            log.info("=== 委托到期回收完成: 回收{}条 ===", expired.size());
        } catch (Exception e) {
            log.error("委托到期回收异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
