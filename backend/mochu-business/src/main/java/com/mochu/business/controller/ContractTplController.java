package com.mochu.business.controller;

import com.mochu.business.dto.ContractTplDTO;
import com.mochu.business.dto.TplFieldUpdateDTO;
import com.mochu.business.dto.VersionStatusDTO;
import com.mochu.business.entity.SysContractTpl;
import com.mochu.business.entity.SysContractTplAudit;
import com.mochu.business.entity.SysContractTplField;
import com.mochu.business.entity.SysContractTplVersion;
import com.mochu.business.service.ContractTplService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.common.security.SecurityUtils;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 合同模板管理接口
 */
@RestController
@RequestMapping("/api/v1/contract-tpl")
@RequiredArgsConstructor
public class ContractTplController {

    private final ContractTplService tplService;

    // ===================== 模板 CRUD =====================

    @GetMapping
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<PageResult<SysContractTpl>> list(
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(tplService.list(contractType, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<SysContractTpl> getById(@PathVariable Integer id) {
        SysContractTpl tpl = tplService.getById(id);
        if (tpl == null) return R.fail(404, "模板不存在");
        return R.ok(tpl);
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<Integer> create(@Valid @RequestBody ContractTplDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Integer tplId = tplService.create(dto, userId);
        return R.ok(tplId);
    }

    /**
     * 创建模板并同时上传模板文件（合并操作）
     */
    @Idempotent
    @PostMapping("/with-file")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<SysContractTplVersion> createWithFile(
            @RequestParam String contractType,
            @RequestParam String tplName,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ContractTplDTO dto = new ContractTplDTO();
        dto.setContractType(contractType);
        dto.setTplName(tplName);
        dto.setDescription(description);
        Integer tplId = tplService.create(dto, userId);
        SysContractTplVersion version = tplService.uploadVersion(tplId, file, userId);
        return R.ok(version);
    }

    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody ContractTplDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        tplService.update(id, dto, userId);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<Void> delete(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        tplService.delete(id, userId);
        return R.ok();
    }

    // ===================== 版本管理 =====================

    @Idempotent
    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<SysContractTplVersion> uploadVersion(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SysContractTplVersion version = tplService.uploadVersion(id, file, userId);
        return R.ok(version);
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<List<SysContractTplVersion>> listVersions(@PathVariable Integer id) {
        return R.ok(tplService.listVersions(id));
    }

    @GetMapping("/versions/{versionId}")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<SysContractTplVersion> getVersion(@PathVariable Integer versionId) {
        SysContractTplVersion version = tplService.getVersionById(versionId);
        if (version == null) return R.fail(404, "版本不存在");
        return R.ok(version);
    }

    @Idempotent
    @PatchMapping("/versions/{versionId}/status")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<Void> updateVersionStatus(@PathVariable Integer versionId, @Valid @RequestBody VersionStatusDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        tplService.updateVersionStatus(versionId, dto.getStatus(), userId);
        return R.ok();
    }

    /**
     * 提交版本启用审批（电子流审批）
     */
    @Idempotent
    @PostMapping("/versions/{versionId}/submit-approval")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<Void> submitVersionApproval(@PathVariable Integer versionId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        tplService.submitVersionApproval(versionId, userId);
        return R.ok();
    }

    @GetMapping("/versions/{versionId}/preview")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<String> preview(@PathVariable Integer versionId) {
        return R.ok(tplService.getPreviewHtml(versionId));
    }

    @GetMapping("/versions/{versionId}/download")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<String> download(@PathVariable Integer versionId) {
        return R.ok(tplService.getDownloadUrl(versionId));
    }

    // ===================== 字段管理 =====================

    @GetMapping("/versions/{versionId}/fields")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<List<SysContractTplField>> listFields(@PathVariable Integer versionId) {
        return R.ok(tplService.listFields(versionId));
    }

    @Idempotent
    @PutMapping("/versions/{versionId}/fields")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<Void> updateFields(@PathVariable Integer versionId, @Valid @RequestBody TplFieldUpdateDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        tplService.updateFields(versionId, dto, userId);
        return R.ok();
    }

    // ===================== 审计日志 =====================

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAuthority('system:tpl-manage')")
    public R<PageResult<SysContractTplAudit>> auditLogs(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(tplService.listAuditLogs(id, page, size));
    }

    // ===================== 公开查询（供合同创建选择模板用） =====================

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('system:tpl-manage','contract:create')")
    public R<SysContractTplVersion> getActiveVersion(@RequestParam String contractType) {
        SysContractTplVersion version = tplService.getActiveVersion(contractType);
        if (version == null) return R.fail(404, "该合同类型尚未配置模板");
        return R.ok(version);
    }
}
