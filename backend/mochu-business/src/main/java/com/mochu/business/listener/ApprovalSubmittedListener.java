package com.mochu.business.listener;

import com.mochu.business.entity.*;
import com.mochu.business.event.ApprovalSubmittedEvent;
import com.mochu.business.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 审批提交事件监听器 — 提交审批后将业务单据状态改为 pending
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalSubmittedListener {

    private final BizProjectMapper projectMapper;
    private final BizContractMapper contractMapper;
    private final BizPurchaseListMapper purchaseListMapper;
    private final BizSpotPurchaseMapper spotPurchaseMapper;
    private final BizInboundOrderMapper inboundOrderMapper;
    private final BizOutboundOrderMapper outboundOrderMapper;
    private final BizReturnOrderMapper returnOrderMapper;
    private final BizInventoryCheckMapper inventoryCheckMapper;
    private final BizGanttTaskMapper ganttTaskMapper;
    private final BizChangeOrderMapper changeOrderMapper;
    private final BizStatementMapper statementMapper;
    private final BizPaymentApplyMapper paymentApplyMapper;
    private final BizReimburseMapper reimburseMapper;
    private final BizCompletionFinishMapper completionFinishMapper;
    private final BizLaborSettlementMapper laborSettlementMapper;
    private final BizSalaryMapper salaryMapper;

    @EventListener
    public void onApprovalSubmitted(ApprovalSubmittedEvent event) {
        String bizType = event.getBizType();
        Integer bizId = event.getBizId();
        String targetStatus = "pending";

        log.info("审批提交回写: bizType={}, bizId={}, targetStatus={}", bizType, bizId, targetStatus);

        switch (bizType) {
            case "project" -> {
                BizProject entity = projectMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); projectMapper.updateById(entity); }
            }
            case "contract" -> {
                BizContract entity = contractMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); contractMapper.updateById(entity); }
            }
            case "purchase" -> {
                BizPurchaseList entity = purchaseListMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); purchaseListMapper.updateById(entity); }
            }
            case "spot_purchase" -> {
                BizSpotPurchase entity = spotPurchaseMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); spotPurchaseMapper.updateById(entity); }
            }
            case "inbound" -> {
                BizInboundOrder entity = inboundOrderMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); inboundOrderMapper.updateById(entity); }
            }
            case "outbound" -> {
                BizOutboundOrder entity = outboundOrderMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); outboundOrderMapper.updateById(entity); }
            }
            case "return_order" -> {
                BizReturnOrder entity = returnOrderMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); returnOrderMapper.updateById(entity); }
            }
            case "inventory_check" -> {
                BizInventoryCheck entity = inventoryCheckMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); inventoryCheckMapper.updateById(entity); }
            }
            case "gantt_task" -> {
                BizGanttTask entity = ganttTaskMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); ganttTaskMapper.updateById(entity); }
            }
            case "change_order" -> {
                BizChangeOrder entity = changeOrderMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); changeOrderMapper.updateById(entity); }
            }
            case "statement" -> {
                BizStatement entity = statementMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); statementMapper.updateById(entity); }
            }
            case "payment" -> {
                BizPaymentApply entity = paymentApplyMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); paymentApplyMapper.updateById(entity); }
            }
            case "reimburse" -> {
                BizReimburse entity = reimburseMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); reimburseMapper.updateById(entity); }
            }
            case "completion" -> {
                BizCompletionFinish entity = completionFinishMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); completionFinishMapper.updateById(entity); }
            }
            case "labor_settlement" -> {
                BizLaborSettlement entity = laborSettlementMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); laborSettlementMapper.updateById(entity); }
            }
            case "salary" -> {
                BizSalary entity = salaryMapper.selectById(bizId);
                if (entity != null) { entity.setStatus(targetStatus); salaryMapper.updateById(entity); }
            }
            default -> log.warn("审批提交回写: 未知的业务类型: {}", bizType);
        }
    }
}
