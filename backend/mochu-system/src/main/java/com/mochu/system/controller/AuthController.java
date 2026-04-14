package com.mochu.system.controller;

import com.mochu.common.result.R;
import com.mochu.common.security.SecurityUtils;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.*;
import com.mochu.system.service.AuthService;
import com.mochu.system.vo.CheckAccountVO;
import com.mochu.system.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口 — 对照 V3.2 §4.1（8 个公开端点）
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 检查账号 — POST /api/v1/auth/check-account
     */
    @Idempotent
    @PostMapping("/check-account")
    public R<CheckAccountVO> checkAccount(@Valid @RequestBody CheckAccountDTO dto) {
        return R.ok(authService.checkAccount(dto));
    }

    /**
     * 发送短信验证码 — POST /api/v1/auth/send-sms
     */
    @Idempotent
    @PostMapping("/send-sms")
    public R<Void> sendSms(@RequestParam String phone) {
        authService.sendSms(phone);
        return R.ok();
    }

    /**
     * 密码登录 — POST /api/v1/auth/login-by-password
     */
    @Idempotent
    @PostMapping("/login-by-password")
    public R<LoginVO> loginByPassword(@Valid @RequestBody LoginByPasswordDTO dto,
                                      HttpServletRequest request) {
        String clientType = getClientType(request);
        return R.ok(authService.loginByPassword(dto, clientType));
    }

    /**
     * 短信登录 — POST /api/v1/auth/login-by-sms
     */
    @Idempotent
    @PostMapping("/login-by-sms")
    public R<LoginVO> loginBySms(@Valid @RequestBody LoginBySmsDTO dto,
                                 HttpServletRequest request) {
        String clientType = getClientType(request);
        return R.ok(authService.loginBySms(dto, clientType));
    }

    /**
     * 退出登录 — POST /api/v1/auth/logout
     */
    @Idempotent
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        String clientType = getClientType(request);
        authService.logout(userId, clientType);
        return R.ok();
    }

    /**
     * 忘记密码(发送验证码) — POST /api/v1/auth/forgot-password
     */
    @Idempotent
    @PostMapping("/forgot-password")
    public R<Void> forgotPassword(@RequestParam String phone) {
        authService.forgotPassword(phone);
        return R.ok();
    }

    /**
     * 重置密码 — POST /api/v1/auth/reset-password
     */
    @Idempotent
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return R.ok();
    }

    /**
     * 找回账号 — POST /api/v1/auth/retrieve-account
     */
    @Idempotent
    @PostMapping("/retrieve-account")
    public R<String> retrieveAccount(@RequestParam String phone) {
        return R.ok(authService.retrieveAccount(phone));
    }

    private String getClientType(HttpServletRequest request) {
        String clientType = request.getHeader("X-Client-Type");
        return (clientType != null && !clientType.isBlank()) ? clientType : "pc";
    }
}
