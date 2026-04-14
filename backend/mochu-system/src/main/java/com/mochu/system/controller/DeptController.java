package com.mochu.system.controller;

import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.DeptDTO;
import com.mochu.system.service.DeptService;
import com.mochu.system.vo.DeptVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理接口 — 对照 V3.2 §5.9.3
 */
@RestController
@RequestMapping("/api/v1/admin/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 部门树 — GET /api/v1/admin/depts/tree
     * V3.2: 全部已认证用户可访问
     */
    @GetMapping("/tree")
    public R<List<DeptVO>> tree() {
        return R.ok(deptService.getDeptTree());
    }

    /**
     * 部门详情 — GET /api/v1/admin/depts/{id}
     */
    @GetMapping("/{id}")
    public R<DeptVO> detail(@PathVariable Integer id) {
        return R.ok(deptService.getDeptById(id));
    }

    /**
     * 创建部门 — POST /api/v1/admin/depts
     */
    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept-manage')")
    public R<Integer> create(@Valid @RequestBody DeptDTO dto) {
        return R.ok(deptService.createDept(dto));
    }

    /**
     * 更新部门 — PUT /api/v1/admin/depts/{id}
     */
    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept-manage')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody DeptDTO dto) {
        dto.setId(id);
        deptService.updateDept(dto);
        return R.ok();
    }

    /**
     * 启用/停用部门 — PATCH /api/v1/admin/depts/{id}/status — V3.2 §5.9.6
     */
    @Idempotent
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:dept-manage')")
    public R<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            throw new com.mochu.common.exception.BusinessException("status参数不能为空");
        }
        deptService.updateDeptStatus(id, status);
        return R.ok();
    }

    /**
     * 删除部门 — DELETE /api/v1/admin/depts/{id}
     */
    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept-manage')")
    public R<Void> delete(@PathVariable Integer id) {
        deptService.deleteDept(id);
        return R.ok();
    }
}
