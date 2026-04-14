package com.mochu.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.BizReportSubscribe;
import com.mochu.business.mapper.BizReportSubscribeMapper;
import com.mochu.system.entity.SysTodo;
import com.mochu.system.mapper.SysTodoMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 日报推送
 * Cron: 0 30 6 * * ? — 每日 06:30
 * 功能: 查询daily订阅，创建待办通知
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportPushScheduler {

    private final BizReportSubscribeMapper subscribeMapper;
    private final SysTodoMapper todoMapper;

    private static final Map<String, String> REPORT_NAMES = Map.of(
            "project_cost", "项目成本汇总",
            "income_expense", "收支对比",
            "purchase_progress", "采购进度",
            "inventory_summary", "物资库存"
    );

    @XxlJob("dailyReportPushJob")
    public void pushDailyReports() {
        try {
            List<BizReportSubscribe> subs = subscribeMapper.selectList(
                    new LambdaQueryWrapper<BizReportSubscribe>()
                            .eq(BizReportSubscribe::getFrequency, "daily")
                            .eq(BizReportSubscribe::getStatus, 1));

            for (BizReportSubscribe sub : subs) {
                SysTodo todo = new SysTodo();
                todo.setUserId(sub.getUserId());
                todo.setTitle(String.format("[日报] %s 报表已更新",
                        REPORT_NAMES.getOrDefault(sub.getReportType(), sub.getReportType())));
                todo.setContent("请前往报表模块查看最新数据");
                todo.setBizType("report_push");
                todo.setBizId(sub.getId());
                todo.setStatus(0);
                todoMapper.insert(todo);
            }
            log.info("日报推送完成: {}条", subs.size());
        } catch (Exception e) {
            log.error("日报推送异常", e);
        }
    }
}
