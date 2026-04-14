package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mochu.business.entity.BizAssetTransfer;
import com.mochu.business.entity.BizHrEntry;
import com.mochu.business.entity.BizHrResign;
import com.mochu.business.event.ApprovalCompletedEvent;
import com.mochu.business.mapper.BizAssetTransferMapper;
import com.mochu.business.mapper.BizHrEntryMapper;
import com.mochu.business.mapper.BizHrResignMapper;
import com.mochu.system.entity.SysUser;
import com.mochu.system.entity.SysUserRole;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.mapper.SysUserRoleMapper;
import com.mochu.system.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * P6 §4.18: HR 入离职自动化事件监听
 * - 入职审批通过 → 自动创建系统账号 + 企业邮箱
 * - 离职审批通过 → 禁用账号 + 清Token + 检查资产
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrEventListener {

    private final BizHrEntryMapper entryMapper;
    private final BizHrResignMapper resignMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BizAssetTransferMapper assetTransferMapper;
    private final TodoService todoService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    // ===================== 入职审批通过 → 自动创建账号 =====================

    /**
     * P6: 入职审批通过 → 自动创建 sys_user 账号
     */
    @EventListener
    @Transactional
    public void onEntryApproved(ApprovalCompletedEvent event) {
        if (!"hr_entry".equals(event.getBizType())
                || !"approved".equals(event.getFinalStatus())) {
            return;
        }

        BizHrEntry entry = entryMapper.selectById(event.getBizId());
        if (entry == null) return;

        // 1. 创建 sys_user 账号
        SysUser user = new SysUser();
        user.setUsername(generateUsername(entry));
        user.setRealName(entry.getApplicantName());
        user.setPhone(entry.getPhone());
        user.setDeptId(entry.getDeptId());
        String initPassword = generateInitPassword();
        // #7 fix: SysUser 字段名是 passwordHash 而非 password
        user.setPasswordHash(passwordEncoder.encode(initPassword));
        user.setForceChangePwd(1); // 首次登录强制改密
        user.setStatus(1);
        user.setFlagContact(1);
        userMapper.insert(user);

        // 2. 分配默认角色（如有 position 对应的角色）
        // SysUserRole userRole = new SysUserRole();
        // userRole.setUserId(user.getId());
        // userRole.setRoleId(getRoleIdByPosition(entry.getPosition()));
        // userRoleMapper.insert(userRole);

        // V3.2 §4.2: 入职 → 通讯录缓存失效
        clearContactCache();

        log.info("入职自动创建账号: username={}, userId={}", user.getUsername(), user.getId());
    }

    // ===================== 离职审批通过 → 禁用账号 =====================

    /**
     * P6: 离职审批通过 → 禁用账号 + 清除Token + 检查资产
     */
    @EventListener
    @Transactional
    public void onResignApproved(ApprovalCompletedEvent event) {
        if (!"hr_resign".equals(event.getBizType())
                || !"approved".equals(event.getFinalStatus())) {
            return;
        }

        BizHrResign resign = resignMapper.selectById(event.getBizId());
        if (resign == null) return;
        Integer userId = resign.getUserId();

        // 1. 禁用账号
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setStatus(0);
            userMapper.updateById(user);
        }

        // 2. #8 fix: 清除所有客户端类型的 Redis Token（与 AuthService.clearAllTokens 一致）
        try {
            for (String clientType : List.of("pc", "h5", "wxapp")) {
                redisTemplate.delete("auth:token:" + userId + ":" + clientType);
            }
            redisTemplate.delete("user:permissions:" + userId);
            // 同时清除用户信息缓存
            redisTemplate.delete("auth:token:info:" + userId);
        } catch (Exception e) {
            log.warn("清除Token失败: {}", e.getMessage());
        }

        // 3. 检查资产移交
        long pendingTransfers = assetTransferMapper.selectCount(
                new LambdaQueryWrapper<BizAssetTransfer>()
                        .eq(BizAssetTransfer::getUserId, userId)
                        .ne(BizAssetTransfer::getStatus, "completed"));
        if (pendingTransfers > 0) {
            // 创建待办通知HR
            String realName = user != null ? user.getRealName() : String.valueOf(userId);
            todoService.createTodo(1, "hr_resign_asset", resign.getId(),
                    "【提醒】离职员工" + realName + "有未完成的资产移交",
                    "请尽快处理资产移交事宜");
            log.warn("员工{}有{}项资产未完成移交", userId, pendingTransfers);
        }

        // V3.2 §4.2: 离职 → 通讯录缓存失效
        clearContactCache();

        log.info("离职处理完成: userId={}", userId);
    }

    // ===================== 内部辅助方法 =====================

    /**
     * V3.2 §4.2: 人事变动时清除通讯录 Redis 缓存
     * 直接通过 RedisTemplate 删除 contact:* 前缀的 key，
     * 避免跨模块依赖 system 模块的 ContactService
     */
    private void clearContactCache() {
        try {
            Set<String> keys = redisTemplate.keys("contact:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("人事变动触发通讯录缓存清除, 共{}个key", keys.size());
            }
        } catch (Exception e) {
            log.warn("清除通讯录缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 生成用户名: 姓名拼音首字母 + 4位随机数
     */
    private String generateUsername(BizHrEntry entry) {
        String name = entry.getApplicantName() != null ? entry.getApplicantName() : "user";
        String base = name.length() > 4 ? name.substring(0, 4) : name;
        int random = new SecureRandom().nextInt(9000) + 1000;
        return base.toLowerCase() + random;
    }

    /**
     * 生成初始密码: 8位随机字母数字
     */
    private String generateInitPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
