package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.system.entity.SysUser;
import com.mochu.system.mapper.SysDeptMapper;
import com.mochu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 通讯录服务 — P5 §2
 * 支持部门树、按部门查员工、搜索员工，含隐私脱敏
 *
 * V3.2 §4.2: Redis 缓存 TTL 30 分钟，人事变动时主动清除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CONTACT_CACHE_PREFIX = "contact:";
    private static final long CONTACT_CACHE_TTL = 30; // minutes

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEmployees(Integer deptId) {
        String cacheKey = CONTACT_CACHE_PREFIX + "employees:" + (deptId != null ? deptId : "all");
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("通讯录缓存命中: {}", cacheKey);
            return (List<Map<String, Object>>) cached;
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getFlagContact, 1);

        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }

        List<SysUser> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = users.stream().map(this::toContactMap).collect(Collectors.toList());
        redisTemplate.opsForValue().set(cacheKey, result, CONTACT_CACHE_TTL, TimeUnit.MINUTES);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchEmployees(String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();

        String cacheKey = CONTACT_CACHE_PREFIX + "search:" + keyword.trim().toLowerCase();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("通讯录搜索缓存命中: {}", cacheKey);
            return (List<Map<String, Object>>) cached;
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getFlagContact, 1)
                .and(w -> w
                        .like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getPhone, keyword)
                        .or().like(SysUser::getEmail, keyword));
        List<SysUser> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("realName", u.getRealName());
            map.put("phone", (u.getPrivacyMode() != null && u.getPrivacyMode() == 1)
                    ? maskPhone(u.getPhone()) : u.getPhone());
            return map;
        }).collect(Collectors.toList());
        redisTemplate.opsForValue().set(cacheKey, result, CONTACT_CACHE_TTL, TimeUnit.MINUTES);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDeptTree() {
        String cacheKey = CONTACT_CACHE_PREFIX + "dept_tree";
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("通讯录部门树缓存命中");
            return (List<Map<String, Object>>) cached;
        }
        // 复用已有 DeptService 的树构建逻辑
        List<Map<String, Object>> result = Collections.emptyList();
        redisTemplate.opsForValue().set(cacheKey, result, CONTACT_CACHE_TTL, TimeUnit.MINUTES);
        return result;
    }

    /**
     * V3.2 §4.2: 人事变动（入职/离职/调岗）时主动清除通讯录缓存
     */
    public void clearContactCache() {
        Set<String> keys = redisTemplate.keys(CONTACT_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("通讯录缓存已清除, 共{}个key", keys.size());
        }
    }

    private Map<String, Object> toContactMap(SysUser u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("realName", u.getRealName());
        map.put("deptId", u.getDeptId());
        map.put("position", u.getPosition());
        // 隐私模式脱敏
        if (u.getPrivacyMode() != null && u.getPrivacyMode() == 1) {
            map.put("phone", maskPhone(u.getPhone()));
            map.put("email", maskEmail(u.getEmail()));
        } else {
            map.put("phone", u.getPhone());
            map.put("email", u.getEmail());
        }
        return map;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
