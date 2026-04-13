package com.mochu.business.listener;

import com.mochu.business.entity.*;
import com.mochu.business.event.ApprovalCompletedEvent;
import com.mochu.business.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审批完成事件监听器 — 审批通过/驳回后回写业务单据状态
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalCompletedListener {

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
    private final SysContractTplVersionMapper contractTplVersionMapper;

    @EventListener
    @Transactional
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        String bizType = event.getBizType();
        Integer bizId = event.getBizId();
        String finalStatus = event.getFinalStatus(); // "approved" or "rejected"

        String targetStatus;
        if ("approved".equals(finalStatus)) {
            targetStatus = resolveApprovedStatus(bizType);
        } else if ("rejected".equals(finalStatus)) {
            targetStatus = "rejected";
        } else {
            // cancelled or other → revert to draft
            targetStatus = "draft";
        }

        log.info("审批完成回调: bizType={}, bizId={}, finalStatus={}, targetStatus={}", bizType, bizId, finalStatus, targetStatus);

        switch (bizType) {
            case "project" -> {
                BizProject project = projectMapper.selectById(bizId);
                if (project != null) {
                    project.setStatus(targetStatus);
                    projectMapper.updateById(project);
                }
            }
            case "contract" -> {
                BizContract contract = contractMapper.selectById(bizId);
                if (contract != null) {
                    contract.setStatus(targetStatus);
                    contractMapper.updateById(contract);
                }
            }
            case "purchase" -> {
                BizPurchaseList pl = purchaseListMapper.selectById(bizId);
                if (pl != null) {
                    pl.setStatus(targetStatus);
                    purchaseListMapper.updateById(pl);
                }
            }
            case "spot_purchase" -> {
                BizSpotPurchase sp = spotPurchaseMapper.selectById(bizId);
                if (sp != null) {
                    sp.setStatus(targetStatus);
                    spotPurchaseMapper.updateById(sp);
                }
            }
            case "inbound" -> {
                BizInboundOrder io = inboundOrderMapper.selectById(bizId);
                if (io != null) {
                    io.setStatus(targetStatus);
                    inboundOrderMapper.updateById(io);
                }
            }
            case "outbound" -> {
                BizOutboundOrder oo = outboundOrderMapper.selectById(bizId);
                if (oo != null) {
                    oo.setStatus(targetStatus);
                    outboundOrderMapper.updateById(oo);
                }
            }
            case "return_order" -> {
                BizReturnOrder ro = returnOrderMapper.selectById(bizId);
                if (ro != null) {
                    ro.setStatus(targetStatus);
                    returnOrderMapper.updateById(ro);
                }
            }
            case "inventory_check" -> {
                BizInventoryCheck ic = inventoryCheckMapper.selectById(bizId);
                if (ic != null) {
                    ic.setStatus(targetStatus);
                    inventoryCheckMapper.updateById(ic);
                }
            }
            case "gantt_task" -> {
                BizGanttTask gt = ganttTaskMapper.selectById(bizId);
                if (gt != null) {
                    gt.setStatus(targetStatus);
                    ganttTaskMapper.updateById(gt);
                }
            }
            case "change_order" -> {
                BizChangeOrder co = changeOrderMapper.selectById(bizId);
                if (co != null) {
                    co.setStatus(targetStatus);
                    changeOrderMapper.updateById(co);
                }
            }
            case "statement" -> {
                BizStatement st = statementMapper.selectById(bizId);
                if (st != null) {
                    st.setStatus(targetStatus);
                    statementMapper.updateById(st);
                }
            }
            case "payment" -> {
                BizPaymentApply pa = paymentApplyMapper.selectById(bizId);
                if (pa != null) {
                    pa.setStatus(targetStatus);
                    paymentApplyMapper.updateById(pa);
                }
            }
            case "reimburse" -> {
                BizReimburse re = reimburseMapper.selectById(bizId);
                if (re != null) {
                    re.setStatus(targetStatus);
                    reimburseMapper.updateById(re);
                }
            }
            case "completion" -> {
                BizCompletionFinish cf = completionFinishMapper.selectById(bizId);
                if (cf != null) {
                    cf.setStatus(targetStatus);
                    completionFinishMapper.updateById(cf);
                }
            }
            case "labor_settlement" -> {
                BizLaborSettlement ls = laborSettlementMapper.selectById(bizId);
                if (ls != null) {
                    ls.setStatus(targetStatus);
                    laborSettlementMapper.updateById(ls);
                }
            }
            case "salary" -> {
                BizSalary sal = salaryMapper.selectById(bizId);
                if (sal != null) {
                    sal.setStatus(targetStatus);
                    salaryMapper.updateById(sal);
                }
            }
            case "contract_tpl" -> {
                SysContractTplVersion version = contractTplVersionMapper.selectById(bizId);
                if (version != null) {
                    version.setStatus(resolveContractTplStatus(finalStatus));
                    contractTplVersionMapper.updateById(version);
                }
            }
            default -> log.warn("未知的业务类型: {}", bizType);
        }
    }

    /**
     * 审批通过后的目标状态
     */
    private String resolveApprovedStatus(String bizType) {
        return switch (bizType) {
            case "project" -> "active";
            default -> "approved";
        };
    }

    private Integer resolveContractTplStatus(String finalStatus) {
        return switch (finalStatus) {
            case "approved" -> 1;
            default -> 0;
        };
    }

    // resolveDraftStatus removed — rejection/cancellation logic now handled inline in onApprovalCompleted
}
