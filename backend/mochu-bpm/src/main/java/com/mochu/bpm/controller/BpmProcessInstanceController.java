package com.mochu.bpm.controller;

import com.mochu.bpm.dto.OaRuleDTO;
import com.mochu.bpm.dto.StartProcessDTO;
import com.mochu.bpm.entity.BpmOaRule;
import com.mochu.bpm.service.BpmProcessInstanceService;
import com.mochu.bpm.vo.ProcessInstanceVO;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bpm/instance")
@RequiredArgsConstructor
public class BpmProcessInstanceController {

    private final BpmProcessInstanceService instanceService;

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<String> start(@Valid @RequestBody StartProcessDTO dto) {
        return R.ok(instanceService.startProcess(dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<PageResult<ProcessInstanceVO>> listMy(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(instanceService.listMyInstances(page, size));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('bpm:instance-view')")
    public R<PageResult<ProcessInstanceVO>> listAll(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer result,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(instanceService.listAllInstances(bizType, result, page, size));
    }

    @GetMapping("/{processInstId}")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<ProcessInstanceVO> getDetail(@PathVariable String processInstId) {
        return R.ok(instanceService.getInstanceDetail(processInstId));
    }

    @GetMapping("/{processInstId}/highlight")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Map<String, Object>> getHighlight(@PathVariable String processInstId) {
        return R.ok(instanceService.getHighlightNodes(processInstId));
    }

    // ==================== OA 规则管理 ====================

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('bpm:rule-manage')")
    public R<List<BpmOaRule>> listRules() {
        return R.ok(instanceService.listOaRules());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('bpm:rule-manage')")
    public R<Void> saveRule(@Valid @RequestBody OaRuleDTO dto) {
        instanceService.saveOaRule(dto);
        return R.ok();
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('bpm:rule-manage')")
    public R<Void> deleteRule(@PathVariable Integer id) {
        instanceService.deleteOaRule(id);
        return R.ok();
    }
}
