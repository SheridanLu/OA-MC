package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.framework.security.JwtUtils;
import com.mochu.system.dto.*;
import com.mochu.system.entity.SysUser;
import com.mochu.system.mapper.SysRoleMapper;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.vo.CheckAccountVO;
import com.mochu.system.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务 — 对照 V3.2 §4.1
 * 包含: check-account, send-sms, login-by-password, login-by-sms,
 *       logout, forgot-password, reset-password, retrieve-account
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查账号 — V3.2 §4.1 check-account
     * 账号不存在返回 404
     */
    public CheckAccountVO checkAccount(CheckAccountDTO dto) {
        SysUser user = findByUsernameOrPhone(dto.getUsername());
        if (user == null) {
            throw new BusinessException(404, "账号不存在");
        }
        CheckAccountVO vo = new CheckAccountVO();
        vo.setExists(true);
        vo.setLocked(user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now()));
        return vo;
    }

    /**
     * 发送短信验证码 — V3.2 §4.1 send-sms
     * 同一手机号 60 秒内限 1 次
     */
    public void sendSms(String phone) {
        // 限频检查
        String resendKey = Constants.REDIS_SMS_PREFIX + phone + ":lock";
        Boolean locked = redisTemplate.hasKey(resendKey);
        if (Boolean.TRUE.equals(locked)) {
            throw new BusinessException(429, "发送频率超限，请60秒后重试");
        }

        // 生成 6 位验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        log.info("短信验证码 [{}]: {}", phone, code);

        // 存入 Redis, TTL 5 分钟
        String smsKey = Constants.REDIS_SMS_PREFIX + phone;
        redisTemplate.opsForValue().set(smsKey, code, Constants.SMS_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 限频锁, TTL 60 秒
        redisTemplate.opsForValue().set(resendKey, "1", Constants.SMS_RESEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 密码登录 — V3.2 §4.1.2
     * 锁定规则: 5 次失败锁 30 分钟
     */
    public LoginVO loginByPassword(LoginByPasswordDTO dto, String clientType) {
        SysUser user = findByUsernameOrPhone(dto.getUsername());
        if (user == null) {
            throw new BusinessException(404, "账号不存在");
        }

        // 检查账号状态
        checkUserStatus(user);

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.warn("登录失败: 用户={}, 密码不匹配", dto.getUsername());
            handleLoginFail(user);
            throw new BusinessException(401, "密码错误");
        }

        // 登录成功
        return doLogin(user, clientType);
    }

    /**
     * 短信登录 — V3.2 §4.1 login-by-sms
     */
    public LoginVO loginBySms(LoginBySmsDTO dto, String clientType) {
        // 验证短信码
        verifySmsCode(dto.getPhone(), dto.getSmsCode());

        SysUser user = findByPhone(dto.getPhone());
        if (user == null) {
            throw new BusinessException(404, "该手机号未注册");
        }

        checkUserStatus(user);
        return doLogin(user, clientType);
    }

    /**
     * 退出登录 — V3.2 §4.1 logout
     * 删除当前设备的 Token
     */
    public void logout(Integer userId, String clientType) {
        String tokenKey = Constants.REDIS_TOKEN_PREFIX + userId + ":" + clientType;
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(Constants.REDIS_PERMISSIONS_PREFIX + userId);
        log.info("用户 {} 退出登录, clientType={}", userId, clientType);
    }

    /**
     * 忘记密码(发送验证码) — V3.2 §4.1 forgot-password
     */
    public void forgotPassword(String phone) {
        SysUser user = findByPhone(phone);
        if (user == null) {
            throw new BusinessException(404, "该手机号未注册");
        }
        sendSms(phone);
    }

    /**
     * 重置密码 — V3.2 §4.1 reset-password
     * 重置后清除所有设备 Token，强制重新登录
     */
    public void resetPassword(ResetPasswordDTO dto) {
        verifySmsCode(dto.getPhone(), dto.getSmsCode());

        SysUser user = findByPhone(dto.getPhone());
        if (user == null) {
            throw new BusinessException(404, "该手机号未注册");
        }

        // 密码规则: 最少 8 位，包含字母和数字
        validatePassword(dto.getNewPassword());

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setLoginAttempts(0);
        user.setLockUntil(null);
        sysUserMapper.updateById(user);

        // 清除所有设备 Token，强制重新登录
        clearAllTokens(user.getId());
    }

    /**
     * 找回账号 — V3.2 §4.1 retrieve-account
     * 根据手机号查询用户名
     */
    public String retrieveAccount(String phone) {
        SysUser user = findByPhone(phone);
        if (user == null) {
            throw new BusinessException(404, "该手机号未注册");
        }
        return user.getUsername();
    }

    // ==================== 私有方法 ====================

    private SysUser findByUsernameOrPhone(String input) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, input)
                        .or()
                        .eq(SysUser::getPhone, input)
        );
    }

    private SysUser findByPhone(String phone) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)
        );
    }

    private void checkUserStatus(SysUser user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(423, "账号已锁定，请30分钟后再试");
        }
    }

    private void handleLoginFail(SysUser user) {
        String failKey = Constants.REDIS_LOGIN_FAIL_PREFIX + user.getUsername();
        Long attempts = redisTemplate.opsForValue().increment(failKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(failKey, Constants.LOGIN_FAIL_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        if (attempts != null && attempts >= Constants.MAX_LOGIN_ATTEMPTS) {
            // 锁定账号
            user.setLockUntil(LocalDateTime.now().plusSeconds(Constants.LOCK_DURATION_SECONDS));
            user.setLoginAttempts(attempts.intValue());
            sysUserMapper.updateById(user);
            redisTemplate.delete(failKey);
            throw new BusinessException(423, "登录失败次数过多，账号已锁定30分钟");
        }
    }

    private LoginVO doLogin(SysUser user, String clientType) {
        if (clientType == null || clientType.isBlank()) {
            clientType = "pc";
        }

        // 清除失败计数
        redisTemplate.delete(Constants.REDIS_LOGIN_FAIL_PREFIX + user.getUsername());

        // 查询角色列表
        List<String> roles = loadUserRoles(user.getId());

        // 生成 Token（含 clientType）
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roles, clientType);

        // 存 Redis Token
        String tokenKey = Constants.REDIS_TOKEN_PREFIX + user.getId() + ":" + clientType;
        redisTemplate.opsForValue().set(tokenKey, token, Constants.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 存 Redis 权限
        Set<String> permissions = loadUserPermissions(user.getId());
        String permKey = Constants.REDIS_PERMISSIONS_PREFIX + user.getId();
        redisTemplate.opsForValue().set(permKey, permissions, Constants.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 存 Redis 用户信息（供 Filter 加载 dataScope/deptId）
        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("deptId", user.getDeptId());
        userInfoMap.put("realName", user.getRealName());
        // dataScope 取用户最高权限角色的 data_scope
        Integer dataScope = sysRoleMapper.selectMaxDataScopeByUserId(user.getId());
        userInfoMap.put("dataScope", dataScope != null ? dataScope : 4);
        String userInfoKey = Constants.REDIS_TOKEN_PREFIX + "info:" + user.getId();
        redisTemplate.opsForValue().set(userInfoKey, userInfoMap, Constants.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginAttempts(0);
        user.setLockUntil(null);
        sysUserMapper.updateById(user);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setForceChangePwd(user.getForceChangePwd() != null && user.getForceChangePwd() == 1);
        return vo;
    }

    private List<String> loadUserRoles(Integer userId) {
        return sysRoleMapper.selectRoleCodesByUserId(userId);
    }

    private Set<String> loadUserPermissions(Integer userId) {
        return sysRoleMapper.selectPermCodesByUserId(userId);
    }

    private void verifySmsCode(String phone, String code) {
        String smsKey = Constants.REDIS_SMS_PREFIX + phone;
        Object stored = redisTemplate.opsForValue().get(smsKey);
        if (stored == null || !code.equals(stored.toString())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        // 验证成功后删除
        redisTemplate.delete(smsKey);
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new BusinessException(400, "密码长度不能少于8位");
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException(400, "密码必须包含字母和数字");
        }
    }

    private void clearAllTokens(Integer userId) {
        for (String clientType : List.of("pc", "h5", "wxapp")) {
            String tokenKey = Constants.REDIS_TOKEN_PREFIX + userId + ":" + clientType;
            redisTemplate.delete(tokenKey);
        }
        redisTemplate.delete(Constants.REDIS_PERMISSIONS_PREFIX + userId);
    }
}
