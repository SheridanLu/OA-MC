package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.entity.BizContract;
import com.mochu.business.entity.BizContractMaterial;
import com.mochu.business.entity.BizInboundOrder;
import com.mochu.business.entity.BizInboundOrderItem;
import com.mochu.business.event.ApprovalCompletedEvent;
import com.mochu.business.mapper.BizContractMapper;
import com.mochu.business.mapper.BizContractMaterialMapper;
import com.mochu.business.mapper.BizInboundOrderItemMapper;
import com.mochu.business.mapper.BizInboundOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * P6 §4.9: 支出合同审批通过 → 自动生成入库单草稿
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final BizContractMapper contractMapper;
    private final BizContractMaterialMapper contractMaterialMapper;
    private final BizInboundOrderMapper inboundMapper;
    private final BizInboundOrderItemMapper inboundItemMapper;
    private final NoGeneratorService noGeneratorService;

    /**
     * 支出合同审批通过 → 自动生成入库单草稿
     */
    @EventListener
    @Transactional
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (!"expense_contract".equals(event.getBizType())
                && !"contract".equals(event.getBizType())) {
            return;
        }
        if (!"approved".equals(event.getFinalStatus())) {
            return;
        }

        BizContract contract = contractMapper.selectById(event.getBizId());
        if (contract == null) return;

        // 仅采购类合同（支出类）自动生成入库单
        String ct = contract.getContractType();
        if (ct == null || (!ct.contains("purchase") && !ct.equals("subcontract"))) {
            return;
        }

        // 自动生成入库单草稿
        BizInboundOrder inbound = new BizInboundOrder();
        inbound.setInboundNo(noGeneratorService.generate("RK"));
        inbound.setProjectId(contract.getProjectId());
        inbound.setContractId(contract.getId());
        inbound.setStatus("draft");
        inboundMapper.insert(inbound);

        // 从合同材料明细生成入库明细
        List<BizContractMaterial> materials = contractMaterialMapper.selectList(
                new LambdaQueryWrapper<BizContractMaterial>()
                        .eq(BizContractMaterial::getContractId, contract.getId()));
        for (BizContractMaterial mat : materials) {
            BizInboundOrderItem item = new BizInboundOrderItem();
            item.setInboundId(inbound.getId());
            item.setMaterialId(mat.getMaterialId());
            item.setMaterialName(mat.getMaterialName());
            item.setQuantity(mat.getQuantity());
            item.setUnitPrice(mat.getUnitPrice());
            inboundItemMapper.insert(item);
        }

        log.info("支出合同审批通过，自动生成入库单草稿: {}", inbound.getInboundNo());
    }
}
