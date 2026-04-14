package com.mochu.business.controller;

import com.mochu.business.dto.ProjectDTO;
import com.mochu.business.dto.ProjectMemberDTO;
import com.mochu.business.dto.ProjectQueryDTO;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.dto.TerminateDTO;
import com.mochu.business.entity.BizProject;
import com.mochu.business.entity.BizProjectMember;
import com.mochu.business.service.ProjectService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.common.security.SecurityUtils;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理接口
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('project:view-all','project:view-own')")
    public R<PageResult<BizProject>> list(ProjectQueryDTO query) {
        return R.ok(projectService.list(query));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('project:view-all','project:view-own')")
    public R<List<BizProject>> listAll() {
        return R.ok(projectService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('project:view-all','project:view-own')")
    public R<BizProject> getById(@PathVariable Integer id) {
        BizProject project = projectService.getById(id);
        if (project == null) {
            return R.fail(404, "项目不存在");
        }
        return R.ok(project);
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('project:create')")
    public R<Void> create(@Valid @RequestBody ProjectDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        projectService.create(dto, userId);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:edit')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody ProjectDTO dto) {
        projectService.update(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('project:edit')")
    public R<Void> updateStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        projectService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('project:delete')")
    public R<Void> delete(@PathVariable Integer id) {
        projectService.delete(id);
        return R.ok();
    }

    // ======================== 项目生命周期 ========================

    @Idempotent
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('project:suspend')")
    public R<Void> convert(@PathVariable Integer id) {
        projectService.convertProject(id);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('project:suspend')")
    public R<Void> suspend(@PathVariable Integer id) {
        projectService.suspendProject(id);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('project:suspend')")
    public R<Void> resume(@PathVariable Integer id) {
        projectService.resumeProject(id);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('project:suspend')")
    public R<Void> terminate(@PathVariable Integer id, @Valid @RequestBody TerminateDTO dto) {
        projectService.terminateProject(id, dto.getReason());
        return R.ok();
    }

    // ======================== 项目成员 ========================

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyAuthority('project:view-all','project:view-own')")
    public R<List<BizProjectMember>> listMembers(@PathVariable Integer id) {
        return R.ok(projectService.listMembers(id));
    }

    @Idempotent
    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('project:edit')")
    public R<Void> addMember(@PathVariable Integer id, @Valid @RequestBody ProjectMemberDTO dto) {
        projectService.addMember(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('project:edit')")
    public R<Void> removeMember(@PathVariable Integer id, @PathVariable Integer userId) {
        projectService.removeMember(id, userId);
        return R.ok();
    }
}
