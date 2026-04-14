package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.UserCreateDTO;
import com.mochu.system.dto.UserQueryDTO;
import com.mochu.system.dto.UserUpdateDTO;
import com.mochu.system.service.UserService;
import com.mochu.system.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口 — 对照 V3.2 §5.9.1
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户列表 — GET /api/v1/admin/users
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<PageResult<UserVO>> list(UserQueryDTO dto) {
        return R.ok(userService.listUsers(dto));
    }

    /**
     * 用户详情 — GET /api/v1/admin/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<UserVO> detail(@PathVariable Integer id) {
        return R.ok(userService.getUserById(id));
    }

    /**
     * 创建用户 — POST /api/v1/admin/users
     */
    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Integer> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.createUser(dto));
    }

    /**
     * 更新用户 — PUT /api/v1/admin/users/{id}
     */
    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        userService.updateUser(dto);
        return R.ok();
    }

    /**
     * 删除用户 — DELETE /api/v1/admin/users/{id}
     */
    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> delete(@PathVariable Integer id) {
        userService.deleteUser(id);
        return R.ok();
    }

    /**
     * 启用/禁用用户 — PATCH /api/v1/admin/users/{id}/status — V3.2 §5.9.4
     */
    @Idempotent
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        userService.updateUserStatus(id, status);
        return R.ok();
    }

    /**
     * 分配角色 — PUT /api/v1/admin/users/{id}/roles
     */
    @Idempotent
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> assignRoles(@PathVariable Integer id, @RequestBody Map<String, List<Integer>> body) {
        List<Integer> roleIds = body.get("role_ids");
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    /**
     * 重置密码 — POST /api/v1/admin/users/{id}/reset-password — V3.2 §5.9.4
     */
    @Idempotent
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user-manage')")
    public R<Void> resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("new_password");
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("new_password is required");
        }
        userService.resetUserPassword(id, newPassword);
        return R.ok();
    }
}
