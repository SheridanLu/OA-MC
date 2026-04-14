package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.RoleDTO;
import com.mochu.system.service.RoleService;
import com.mochu.system.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理接口 — 对照 V3.2 §5.9.2
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色列表 — GET /api/v1/admin/roles
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<PageResult<RoleVO>> list(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(roleService.listRoles(roleName, status, page, size));
    }

    /**
     * 角色详情 — GET /api/v1/admin/roles/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<RoleVO> detail(@PathVariable Integer id) {
        return R.ok(roleService.getRoleById(id));
    }

    /**
     * 创建角色 — POST /api/v1/admin/roles
     */
    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<Integer> create(@Valid @RequestBody RoleDTO dto) {
        return R.ok(roleService.createRole(dto));
    }

    /**
     * 更新角色 — PUT /api/v1/admin/roles/{id}
     */
    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody RoleDTO dto) {
        dto.setId(id);
        roleService.updateRole(dto);
        return R.ok();
    }

    /**
     * 删除角色 — DELETE /api/v1/admin/roles/{id}
     */
    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<Void> delete(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    /**
     * 查询角色权限 — GET /api/v1/admin/roles/{id}/permissions — V3.2 §5.9.5
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<List<Integer>> getPermissions(@PathVariable Integer id) {
        return R.ok(roleService.getRolePermissionIds(id));
    }

    /**
     * 配置角色权限 — PUT /api/v1/admin/roles/{id}/permissions — V3.2 §5.9.5
     */
    @Idempotent
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<Void> updatePermissions(@PathVariable Integer id, @RequestBody Map<String, List<Integer>> body) {
        List<Integer> permissionIds = body.get("permission_ids");
        roleService.updateRolePermissions(id, permissionIds != null ? permissionIds : List.of());
        return R.ok();
    }

    /**
     * 互斥角色对 — GET /api/v1/admin/roles/mutual-exclusions — V3.2
     */
    @GetMapping("/mutual-exclusions")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<List<List<String>>> getMutualExclusions() {
        return R.ok(roleService.getMutualExclusions());
    }

    /**
     * 查询角色数据权限 — GET /api/v1/admin/roles/{id}/data-scope — V3.2
     */
    @GetMapping("/{id}/data-scope")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<Integer> getDataScope(@PathVariable Integer id) {
        RoleVO role = roleService.getRoleById(id);
        return R.ok(role.getDataScope());
    }

    /**
     * 更新角色数据权限 — PUT /api/v1/admin/roles/{id}/data-scope — V3.2
     */
    @Idempotent
    @PutMapping("/{id}/data-scope")
    @PreAuthorize("hasAuthority('system:role-manage')")
    public R<Void> updateDataScope(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        roleService.updateDataScope(id, body.get("data_scope"));
        return R.ok();
    }
}
