package com.mochu.bpm.controller;

import com.mochu.bpm.dto.TaskActionDTO;
import com.mochu.bpm.service.BpmTaskService;
import com.mochu.bpm.vo.BpmTaskVO;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bpm/task")
@RequiredArgsConstructor
public class BpmTaskController {

    private final BpmTaskService bpmTaskService;

    @GetMapping("/todo")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<PageResult<BpmTaskVO>> listTodo(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(bpmTaskService.listTodoTasks(page, size));
    }

    @PostMapping("/{taskId}/claim")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Void> claim(@PathVariable String taskId) {
        bpmTaskService.claimTask(taskId);
        return R.ok();
    }

    @PostMapping("/{taskId}/complete")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Void> complete(@PathVariable String taskId, @Valid @RequestBody TaskActionDTO dto) {
        bpmTaskService.completeTask(taskId, dto);
        return R.ok();
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Void> reject(@PathVariable String taskId, @Valid @RequestBody TaskActionDTO dto) {
        bpmTaskService.rejectTask(taskId, dto);
        return R.ok();
    }

    @PostMapping("/{taskId}/transfer")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Void> transfer(@PathVariable String taskId, @Valid @RequestBody TaskActionDTO dto) {
        bpmTaskService.transferTask(taskId, dto);
        return R.ok();
    }

    @PostMapping("/{taskId}/delegate")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Void> delegate(@PathVariable String taskId, @Valid @RequestBody TaskActionDTO dto) {
        bpmTaskService.delegateTask(taskId, dto);
        return R.ok();
    }

    @PostMapping("/withdraw/{processInstId}")
    @PreAuthorize("hasAuthority('bpm:task-operate')")
    public R<Void> withdraw(@PathVariable String processInstId,
                            @RequestParam(required = false) String reason) {
        bpmTaskService.withdrawProcess(processInstId, reason);
        return R.ok();
    }
}
