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
import com.mochu.framework.annotation.Idempotent;
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

    @Idempotent
    @PostMapping("/flows")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<Void> createFlowDef(@Valid @RequestBody FlowDefDTO dto) {
        approvalService.createFlowDef(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/flows/{id}")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<Void> updateFlowDef(@PathVariable Integer id, @Valid @RequestBody FlowDefDTO dto) {
        approvalService.updateFlowDef(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/flows/{id}")
    @PreAuthorize("hasAuthority('approval:flow-manage')")
    public R<Void> deleteFlowDef(@PathVariable Integer id) {
        approvalService.deleteFlowDef(id);
        return R.ok();
    }

    // ===================== 审批操作 =====================

    @Idempotent
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public R<Void> submitForApproval(@Valid @RequestBody ApprovalActionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.submitForApproval(dto.getBizType(), dto.getBizId(), userId);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/approve")
    @PreAuthorize("isAuthenticated()")
    public R<Void> approve(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalOpinionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.approve(instanceId, userId, dto.getOpinion());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/reject")
    @PreAuthorize("isAuthenticated()")
    public R<Void> reject(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalOpinionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.reject(instanceId, userId, dto.getOpinion());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/withdraw")
    @PreAuthorize("isAuthenticated()")
    public R<Void> withdraw(@PathVariable Integer instanceId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.withdraw(instanceId, userId);
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/transfer")
    @PreAuthorize("isAuthenticated()")
    public R<Void> transfer(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalTransferDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.transfer(instanceId, userId, dto.getTargetUserId(), dto.getOpinion());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/cosign")
    @PreAuthorize("isAuthenticated()")
    public R<Void> addCosigner(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalTransferDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.addCosigner(instanceId, userId, dto.getTargetUserId(), dto.getOpinion());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/cosign/{cosignId}/approve")
    @PreAuthorize("isAuthenticated()")
    public R<Void> approveCosign(@PathVariable Integer cosignId, @Valid @RequestBody ApprovalOpinionDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.approveCosign(cosignId, userId, dto.getOpinion());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/read-handle")
    @PreAuthorize("isAuthenticated()")
    public R<Void> sendReadHandle(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalTransferDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.sendReadHandle(instanceId, userId, dto.getTargetUserId());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/{instanceId}/cc")
    @PreAuthorize("isAuthenticated()")
    public R<Void> sendCc(@PathVariable Integer instanceId, @Valid @RequestBody ApprovalCcDTO dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.sendCc(instanceId, userId, dto.getUserIds());
        return R.ok();
    }

    @Idempotent
    @PostMapping("/cc/{ccId}/handle")
    @PreAuthorize("isAuthenticated()")
    public R<Void> markHandled(@PathVariable Integer ccId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        approvalService.markHandled(ccId, userId);
        return R.ok();
    }

    // ===================== 查询 =====================

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<Map<String, Object>>> getMyPending(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(approvalService.getMyPending(userId, page, size));
    }

    @GetMapping("/initiated")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<Map<String, Object>>> getMyInitiated(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(approvalService.getMyInitiated(userId, page, size));
    }

    @GetMapping("/done")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<Map<String, Object>>> getMyDone(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(approvalService.getMyDone(userId, page, size));
    }

    @GetMapping("/cc/list")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<Map<String, Object>>> getCcList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(approvalService.getCcList(userId, page, size));
    }

    @GetMapping("/{instanceId}")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getInstanceDetail(@PathVariable Integer instanceId) {
        return R.ok(approvalService.getInstanceDetail(instanceId));
    }
}
