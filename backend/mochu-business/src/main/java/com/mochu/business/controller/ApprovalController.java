package com.mochu.business.controller;

import com.mochu.business.dto.ApprovalActionDTO;
import com.mochu.business.dto.ApprovalCcDTO;
import com.mochu.business.dto.ApprovalOpinionDTO;
import com.mochu.business.dto.ApprovalTransferDTO;
import com.mochu.business.dto.FlowDefDTO;
import com.mochu.business.entity.SysFlowDef;
import com.mochu.business.service.ApprovalService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审批流程管理接口 — 对照 V3.2 审批引擎
 */
@RestController
@RequestMapping("/api/v1/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    // ===================== 流程定义 CRUD =====================

    @GetMapping("/flows")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<PageResult<SysFlowDef>> listFlowDefs(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(approvalService.listFlowDefs(bizType, status, page, size));
    }

    @GetMapping("/flows/{id}")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<SysFlowDef> getFlowDef(@PathVariable Integer id) {
        SysFlowDef def = approvalService.getFlowDefById(id);
        if (def == null) return R.fail(404, "流程定义不存在");
        return R.ok(def);
    }

    @PostMapping("/flows")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<Void> createFlowDef(@Valid @RequestBody FlowDefDTO dto) {
        approvalService.createFlowDef(dto);
        return R.ok();
    }

    @PutMapping("/flows/{id}")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<Void> updateFlowDef(@PathVariable Integer id, @Valid @RequestBody FlowDefDTO dto) {
        approvalService.updateFlowDef(id, dto);
        return R.ok();
    }

    @DeleteMapping("/flows/{id}")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<Void> deleteFlowDef(@PathVariable Integer id) {
        approvalService.deleteFlowDef(id);
        return R.ok();
    }

    // ===================== 审批操作 =====================

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> submitForApproval(@Valid @RequestBody ApprovalActionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.submitForApproval(dto.getBizType(), dto.getBizId(), userId);
        return R.ok();
    }

    @PostMapping("/{instanceId}/approve")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> approve(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalOpinionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.approve(instanceId, userId, dto.getOpinion());
        return R.ok();
    }

    @PostMapping("/{instanceId}/reject")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> reject(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalOpinionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.reject(instanceId, userId, dto.getOpinion());
        return R.ok();
    }

    @PostMapping("/{instanceId}/withdraw")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> withdraw(@PathVariable Integer instanceId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.withdraw(instanceId, userId);
        return R.ok();
    }

    @PostMapping("/{instanceId}/transfer")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> transfer(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalTransferDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.transfer(instanceId, userId, dto.getTargetUserId(), dto.getOpinion());
        return R.ok();
    }

    @PostMapping("/{instanceId}/cosign")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> addCosigner(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalTransferDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.addCosigner(instanceId, userId, dto.getTargetUserId(), dto.getOpinion());
        return R.ok();
    }

    @PostMapping("/cosign/{cosignId}/approve")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> approveCosign(@PathVariable Integer cosignId, @Valid @RequestBody ApprovalOpinionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.approveCosign(cosignId, userId, dto.getOpinion());
        return R.ok();
    }

    @PostMapping("/{instanceId}/read-handle")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> sendReadHandle(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalTransferDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.sendReadHandle(instanceId, userId, dto.getTargetUserId());
        return R.ok();
    }

    @PostMapping("/{instanceId}/cc")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> sendCc(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalCcDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.sendCc(instanceId, userId, dto.getUserIds());
        return R.ok();
    }

    @PostMapping("/cc/{ccId}/handle")
    @PreAuthorize("hasAuthority('approval:operate')")
    public R<Void> markHandled(@PathVariable Integer ccId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.markHandled(ccId, userId);
        return R.ok();
    }

    // ===================== 查询 =====================

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('approval:view')")
    public R<PageResult<Map<String, Object>>> getMyPending(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(approvalService.getMyPending(userId, page, size));
    }

    @GetMapping("/initiated")
    @PreAuthorize("hasAuthority('approval:view')")
    public R<PageResult<Map<String, Object>>> getMyInitiated(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(approvalService.getMyInitiated(userId, page, size));
    }

    @GetMapping("/{instanceId}")
    @PreAuthorize("hasAuthority('approval:view')")
    public R<Map<String, Object>> getInstanceDetail(@PathVariable Integer instanceId) {
        return R.ok(approvalService.getInstanceDetail(instanceId));
    }
}
