package com.mochu.system.service;

import com.mochu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 密码策略服务 —— 复杂度校验、过期检查、登录失败锁定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /** 密码复杂度正则：8位以上，包含大写、小写、数字、特殊字符 */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    /** 最大失败次数 */
    private static final int MAX_FAILURES = 5;

    /** 锁定时间（分钟） */
    private static final int LOCK_MINUTES = 30;

    /** 密码有效期（天） */
    private static final int PASSWORD_EXPIRE_DAYS = 90;

    private static final String FAIL_COUNT_KEY = "login:fail:";

    /**
     * 校验密码复杂度
     */
    public void validateComplexity(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(400,
                    "密码必须8位以上，包含大写字母、小写字母、数字和特殊字符");
        }
    }

    /**
     * 检查密码是否过期（超过90天）
     */
    public boolean isPasswordExpired(LocalDateTime passwordChangedAt) {
        if (passwordChangedAt == null) {
            return true;
        }
        return Duration.between(passwordChangedAt, LocalDateTime.now()).toDays()
                > PASSWORD_EXPIRE_DAYS;
    }

    /**
     * 检查账号是否被锁定
     */
    public void checkLocked(String username, LocalDateTime lockUntil) {
        if (lockUntil != null && lockUntil.isAfter(LocalDateTime.now())) {
            long minutes = Duration.between(LocalDateTime.now(), lockUntil).toMinutes();
            throw new BusinessException(423,
                    String.format("账号已锁定，请%d分钟后重试", minutes + 1));
        }
    }

    /**
     * 记录登录失败次数
     * @return 当前失败次数
     */
    public int recordFailure(String username) {
        String key = FAIL_COUNT_KEY + username;
        Long count = redisTemplate.opsForValue().increment(key);
        // 首次设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, LOCK_MINUTES, TimeUnit.MINUTES);
        }
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取当前失败次数
     */
    public int getFailureCount(String username) {
        String val = redisTemplate.opsForValue().get(FAIL_COUNT_KEY + username);
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * 是否应该锁定（达到5次）
     */
    public boolean shouldLock(String username) {
        return getFailureCount(username) >= MAX_FAILURES;
    }

    /**
     * 计算锁定截止时间
     */
    public LocalDateTime calculateLockUntil() {
        return LocalDateTime.now().plusMinutes(LOCK_MINUTES);
    }

    /**
     * 登录成功后重置失败计数
     */
    public void resetFailureCount(String username) {
        redisTemplate.delete(FAIL_COUNT_KEY + username);
    }

    /**
     * 获取剩余允许次数
     */
    public int getRemainingAttempts(String username) {
        return Math.max(0, MAX_FAILURES - getFailureCount(username));
    }

    /**
     * 检查密码历史 — 新密码不能与最近5次使用过的密码相同
     */
    public void checkPasswordHistory(Integer userId, String rawPassword) {
        List<String> recentHashes = jdbcTemplate.queryForList(
                "SELECT password_hash FROM sys_password_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 5",
                String.class, userId);
        for (String historicalHash : recentHashes) {
            if (passwordEncoder.matches(rawPassword, historicalHash)) {
                throw new BusinessException(400, "新密码不能与最近5次使用过的密码相同");
            }
        }
    }

    /**
     * 保存密码历史记录
     */
    public void savePasswordHistory(Integer userId, String encodedPassword) {
        jdbcTemplate.update(
                "INSERT INTO sys_password_history (user_id, password_hash, created_at) VALUES (?, ?, NOW())",
                userId, encodedPassword);
    }
}
