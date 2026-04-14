package com.mochu.business.service;

import com.mochu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchApprovalService {

    private final ApprovalService approvalService;

    /**
     * 批量审批 — 全量事务模式
     *
     * 规则（§3.12）：
     * - 任一记录审批失败 → 全部回滚
     * - 使用 @Transactional 保证原子性
     *
     * @throws BusinessException 任一记录失败时抛出，触发事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(List<Integer> instanceIds, String opinion,
                             Integer userId) {
        for (int i = 0; i < instanceIds.size(); i++) {
            Integer instanceId = instanceIds.get(i);
            try {
                approvalService.approve(instanceId, opinion, userId);
            } catch (Exception e) {
                log.error("批量审批第{}条失败: instanceId={}, error={}",
                        i + 1, instanceId, e.getMessage());
                throw new BusinessException(400,
                        "批量审批失败（第" + (i + 1) + "条）: "
                                + e.getMessage());
            }
        }
        log.info("批量审批成功: count={}, userId={}", instanceIds.size(), userId);
    }

    /**
     * 批量删除 — 全量事务模式
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(String bizType, List<Integer> bizIds,
                            Integer userId) {
        for (int i = 0; i < bizIds.size(); i++) {
            Integer bizId = bizIds.get(i);
            try {
                // 各业务删除逻辑复用已有 Service
                approvalService.checkDeletable(bizType, bizId, userId);
            } catch (Exception e) {
                log.error("批量删除第{}条失败: bizType={}, bizId={}, error={}",
                        i + 1, bizType, bizId, e.getMessage());
                throw new BusinessException(400,
                        "批量删除失败（第" + (i + 1) + "条）: "
                                + e.getMessage());
            }
        }
    }
}
