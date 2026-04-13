package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.security.SecurityUtils;
import com.mochu.system.entity.SysTodo;
import com.mochu.system.mapper.SysTodoMapper;
import com.mochu.system.vo.AnnouncementVO;
import com.mochu.system.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 首页服务 — 对照 V3.2 §4.2, §5.9.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AnnouncementService announcementService;
    private final SysTodoMapper sysTodoMapper;

    /**
     * 获取首页数据
     */
    public HomeVO getHomeData() {
        Integer userId = SecurityUtils.getCurrentUserId();
        HomeVO vo = new HomeVO();

        // 待办数量（从 Redis 缓存读取，无缓存则返回 0）
        vo.setTodoCount(getTodoCount());

        // 最新公告（已发布，最多5条）
        try {
            List<AnnouncementVO> published = announcementService.listPublished(5);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            List<HomeVO.AnnouncementVO> annoList = published.stream().map(a -> {
                HomeVO.AnnouncementVO avo = new HomeVO.AnnouncementVO();
                avo.setId(a.getId());
                avo.setTitle(a.getTitle());
                avo.setPublishTime(a.getPublishTime() != null ? a.getPublishTime().format(fmt) : "");
                return avo;
            }).collect(Collectors.toList());
            vo.setAnnouncements(annoList);
        } catch (Exception e) {
            log.warn("加载首页公告失败: {}", e.getMessage());
            vo.setAnnouncements(new ArrayList<>());
        }

        // 快捷入口暂返回空列表
        vo.setShortcuts(new ArrayList<>());

        return vo;
    }

    /**
     * 待办数量 — V3.2 §5.9.2
     */
    public Integer getTodoCount() {
        Integer userId = SecurityUtils.getCurrentUserId();
        String todoKey = Constants.REDIS_TODO_COUNT_PREFIX + userId;
        Object todoCount = redisTemplate.opsForValue().get(todoKey);
        return todoCount instanceof Number n ? n.intValue() : 0;
    }

    /**
     * 待办列表 — V3.2 §5.9.2
     * 查询当前用户未处理的待办，按创建时间倒序，最多20条
     */
    public List<Map<String, Object>> getTodoList() {
        Integer userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<SysTodo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTodo::getUserId, userId)
               .eq(SysTodo::getStatus, 0)
               .orderByDesc(SysTodo::getCreatedAt)
               .last("LIMIT 20");
        List<SysTodo> todos = sysTodoMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysTodo todo : todos) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", todo.getId());
            item.put("bizType", todo.getBizType());
            item.put("bizId", todo.getBizId());
            item.put("title", todo.getTitle());
            item.put("content", todo.getContent());
            item.put("createdAt", todo.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    /**
     * 更新待办数量缓存
     */
    public void updateTodoCount(Integer userId, int count) {
        String todoKey = Constants.REDIS_TODO_COUNT_PREFIX + userId;
        redisTemplate.opsForValue().set(todoKey, count,
                Constants.TODO_COUNT_CACHE_SECONDS, TimeUnit.SECONDS);
    }
}
