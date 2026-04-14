package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.BizGanttTask;
import com.mochu.business.entity.BizProjectMember;
import com.mochu.business.mapper.BizGanttTaskMapper;
import com.mochu.business.mapper.BizProjectMemberMapper;
import com.mochu.system.entity.SysConfig;
import com.mochu.system.entity.SysTodo;
import com.mochu.system.mapper.SysConfigMapper;
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
 * 进度预警
 * Cron: 0 15 0 * * ? — 每日 00:15
 * 功能: 扫描即将逾期的甘特图任务，通知项目经理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProgressWarningScheduler {

    private final BizGanttTaskMapper ganttTaskMapper;
    private final BizProjectMemberMapper memberMapper;
    private final SysConfigMapper configMapper;
    private final SysTodoMapper todoMapper;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("progressWarningJob")
    public void checkProgressWarnings() {
        String lockKey = "scheduler:lock:progress_warning";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (Boolean.FALSE.equals(locked)) return;

        try {
            log.info("=== 进度预警扫描开始 ===");
            int warningDays = getWarningDays();
            LocalDate threshold = LocalDate.now().plusDays(warningDays);

            List<BizGanttTask> tasks = ganttTaskMapper.selectList(
                    new LambdaQueryWrapper<BizGanttTask>()
                            .le(BizGanttTask::getPlanEndDate, threshold)
                            .ge(BizGanttTask::getPlanEndDate, LocalDate.now())
                            .ne(BizGanttTask::getStatus, "completed")
                            .eq(BizGanttTask::getDeleted, 0));

            int warned = 0;
            for (BizGanttTask task : tasks) {
                // 24小时内不重复预警
                String dedupKey = "progress:warned:" + task.getId();
                if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) continue;

                Integer managerId = findProjectManager(task.getProjectId());
                if (managerId == null) continue;

                long remainDays = ChronoUnit.DAYS
                        .between(LocalDate.now(), task.getPlanEndDate());

                SysTodo todo = new SysTodo();
                todo.setUserId(managerId);
                todo.setTitle(String.format("【进度预警】任务\"%s\"还剩%d天到期",
                        task.getTaskName(), remainDays));
                todo.setContent(String.format("计划结束日期: %s，当前进度: %s%%",
                        task.getPlanEndDate(), task.getProgress()));
                todo.setBizType("progress_warning");
                todo.setBizId(task.getId());
                todo.setStatus(0);
                todoMapper.insert(todo);

                redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofHours(24));
                warned++;
            }
            log.info("=== 进度预警完成: 预警{}条 ===", warned);
        } catch (Exception e) {
            log.error("进度预警异常", e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private int getWarningDays() {
        try {
            SysConfig config = configMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>()
                            .eq(SysConfig::getConfigKey, "progress.warning.days"));
            if (config != null) return Integer.parseInt(config.getConfigValue());
        } catch (Exception ignored) {}
        return 3; // 默认3天
    }

    private Integer findProjectManager(Integer projectId) {
        BizProjectMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<BizProjectMember>()
                        .eq(BizProjectMember::getProjectId, projectId)
                        .eq(BizProjectMember::getRole, "manager")
                        .last("LIMIT 1"));
        return member != null ? member.getUserId() : null;
    }
}
