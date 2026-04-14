package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.InboundOrderDTO;
import com.mochu.business.dto.InventoryCheckDTO;
import com.mochu.business.dto.OutboundOrderDTO;
import com.mochu.business.dto.ReturnOrderDTO;
import com.mochu.business.entity.BizInboundOrder;
import com.mochu.business.entity.BizInboundOrderItem;
import com.mochu.business.entity.BizInventory;
import com.mochu.business.entity.BizInventoryCheck;
import com.mochu.business.entity.BizOutboundOrder;
import com.mochu.business.entity.BizOutboundOrderItem;
import com.mochu.business.entity.BizReturnOrder;
import com.mochu.business.mapper.BizInboundOrderMapper;
import com.mochu.business.mapper.BizInventoryCheckMapper;
import com.mochu.business.mapper.BizInventoryMapper;
import com.mochu.business.mapper.BizOutboundOrderMapper;
import com.mochu.business.mapper.BizReturnOrderMapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.enums.ErrorCode;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存管理服务 — 入库/出库/退库/盘点/库存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BizInboundOrderMapper inboundOrderMapper;
    private final BizOutboundOrderMapper outboundOrderMapper;
    private final BizReturnOrderMapper returnOrderMapper;
    private final BizInventoryCheckMapper checkMapper;
    private final BizInventoryMapper inventoryMapper;
    private final NoGeneratorService noGeneratorService;
    private final InventoryRecordService inventoryRecordService;
    private final ApprovalService approvalService;

    // ======================== 入库单 ========================

    public PageResult<BizInboundOrder> listInbound(Integer page, Integer size,
                                                   Integer projectId, String status) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizInboundOrder> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizInboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizInboundOrder::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizInboundOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizInboundOrder::getCreatedAt);

        inboundOrderMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizInboundOrder getInboundById(Integer id) {
        return inboundOrderMapper.selectById(id);
    }

    public void createInbound(InboundOrderDTO dto) {
        BizInboundOrder entity = new BizInboundOrder();
        BeanUtils.copyProperties(dto, entity);
        entity.setInboundNo(noGeneratorService.generate("IB"));
        entity.setStatus("draft");
        inboundOrderMapper.insert(entity);
    }

    public void updateInbound(Integer id, InboundOrderDTO dto) {
        BizInboundOrder entity = inboundOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("入库单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        inboundOrderMapper.updateById(entity);
    }

    public void updateInboundStatus(Integer id, String status) {
        BizInboundOrder entity = inboundOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("入库单不存在");
        }
        entity.setStatus(status);
        inboundOrderMapper.updateById(entity);
    }

    public void deleteInbound(Integer id) {
        inboundOrderMapper.deleteById(id);
    }

    // ======================== 出库单 ========================

    public PageResult<BizOutboundOrder> listOutbound(Integer page, Integer size,
                                                     Integer projectId, String status) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizOutboundOrder> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizOutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizOutboundOrder::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizOutboundOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizOutboundOrder::getCreatedAt);

        outboundOrderMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizOutboundOrder getOutboundById(Integer id) {
        return outboundOrderMapper.selectById(id);
    }

    public void createOutbound(OutboundOrderDTO dto) {
        BizOutboundOrder entity = new BizOutboundOrder();
        BeanUtils.copyProperties(dto, entity);
        entity.setOutboundNo(noGeneratorService.generate("OB"));
        entity.setStatus("draft");
        outboundOrderMapper.insert(entity);
    }

    public void updateOutbound(Integer id, OutboundOrderDTO dto) {
        BizOutboundOrder entity = outboundOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("出库单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        outboundOrderMapper.updateById(entity);
    }

    public void updateOutboundStatus(Integer id, String status) {
        BizOutboundOrder entity = outboundOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("出库单不存在");
        }
        entity.setStatus(status);
        outboundOrderMapper.updateById(entity);
    }

    public void deleteOutbound(Integer id) {
        outboundOrderMapper.deleteById(id);
    }

    // ======================== 退库单 ========================

    public PageResult<BizReturnOrder> listReturn(Integer page, Integer size,
                                                  Integer projectId, String status) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizReturnOrder> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizReturnOrder> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizReturnOrder::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizReturnOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizReturnOrder::getCreatedAt);

        returnOrderMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizReturnOrder getReturnById(Integer id) {
        return returnOrderMapper.selectById(id);
    }

    public void createReturn(ReturnOrderDTO dto) {
        BizReturnOrder entity = new BizReturnOrder();
        BeanUtils.copyProperties(dto, entity);
        entity.setReturnNo(noGeneratorService.generate("RT"));
        entity.setStatus("draft");
        returnOrderMapper.insert(entity);
    }

    public void updateReturn(Integer id, ReturnOrderDTO dto) {
        BizReturnOrder entity = returnOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("退库单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        returnOrderMapper.updateById(entity);
    }

    public void updateReturnStatus(Integer id, String status) {
        BizReturnOrder entity = returnOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("退库单不存在");
        }
        entity.setStatus(status);
        returnOrderMapper.updateById(entity);
    }

    public void deleteReturn(Integer id) {
        returnOrderMapper.deleteById(id);
    }

    // ======================== 盘点 ========================

    public PageResult<BizInventoryCheck> listCheck(Integer page, Integer size,
                                                    Integer projectId, String status) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizInventoryCheck> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizInventoryCheck> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizInventoryCheck::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizInventoryCheck::getStatus, status);
        }
        wrapper.orderByDesc(BizInventoryCheck::getCreatedAt);

        checkMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizInventoryCheck getCheckById(Integer id) {
        return checkMapper.selectById(id);
    }

    public void createCheck(InventoryCheckDTO dto) {
        BizInventoryCheck entity = new BizInventoryCheck();
        BeanUtils.copyProperties(dto, entity);
        entity.setCheckNo(noGeneratorService.generate("CK"));
        entity.setGainCount(0);
        entity.setLossCount(0);
        entity.setStatus("draft");
        checkMapper.insert(entity);
    }

    public void updateCheck(Integer id, InventoryCheckDTO dto) {
        BizInventoryCheck entity = checkMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("盘点单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        checkMapper.updateById(entity);
    }

    public void updateCheckStatus(Integer id, String status) {
        BizInventoryCheck entity = checkMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("盘点单不存在");
        }
        entity.setStatus(status);
        checkMapper.updateById(entity);
    }

    public void deleteCheck(Integer id) {
        checkMapper.deleteById(id);
    }

    // ======================== 库存 ========================

    public PageResult<BizInventory> listStock(Integer page, Integer size,
                                               Integer projectId, Integer materialId) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizInventory> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizInventory> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizInventory::getProjectId, projectId);
        }
        if (materialId != null) {
            wrapper.eq(BizInventory::getMaterialId, materialId);
        }
        wrapper.orderByDesc(BizInventory::getUpdatedAt);

        inventoryMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizInventory getStockById(Integer id) {
        return inventoryMapper.selectById(id);
    }

    // ======================== P6: 加权平均法入库 ========================

    /**
     * P6 §4.9: 入库时更新加权平均单价
     * 新均价 = (当前库存金额 + 本次入库金额) / (当前库存数量 + 本次入库数量)
     */
    @Transactional
    public void processInbound(BizInboundOrder inbound, List<BizInboundOrderItem> items) {
        for (BizInboundOrderItem item : items) {
            // 入库数量校验 (30001)
            BigDecimal contractRemaining = getContractRemainingQty(
                    inbound.getContractId(), item.getMaterialId());
            if (item.getQuantity().compareTo(contractRemaining) > 0) {
                throw new BusinessException(ErrorCode.INBOUND_EXCEED_CONTRACT.getCode(),
                        ErrorCode.INBOUND_EXCEED_CONTRACT.getMessage());
            }

            // 查询当前库存
            BizInventory inventory = getOrCreateInventory(
                    inbound.getProjectId(), item.getMaterialId());
            BigDecimal beforeQty = inventory.getCurrentQuantity();
            BigDecimal beforeAmount = inventory.getCurrentQuantity()
                    .multiply(inventory.getAvgPrice() != null ? inventory.getAvgPrice() : BigDecimal.ZERO);

            // 计算新的加权平均单价
            BigDecimal inboundAmount = item.getQuantity().multiply(item.getUnitPrice());
            BigDecimal newQty = beforeQty.add(item.getQuantity());
            BigDecimal newAvgPrice = BigDecimal.ZERO;
            if (newQty.compareTo(BigDecimal.ZERO) > 0) {
                newAvgPrice = beforeAmount.add(inboundAmount)
                        .divide(newQty, 2, RoundingMode.HALF_UP);
            }

            // 更新库存
            inventory.setCurrentQuantity(newQty);
            inventory.setAvgPrice(newAvgPrice);
            inventory.setTotalAmount(newQty.multiply(newAvgPrice));
            inventoryMapper.updateById(inventory);

            // 记录库存流水
            inventoryRecordService.record(inbound.getProjectId(), item.getMaterialId(),
                    "inbound", inbound.getId(), inbound.getInboundNo(),
                    1, item.getQuantity(), beforeQty, newQty, item.getUnitPrice());
        }
    }

    // ======================== P6: 加权平均法出库 ========================

    /**
     * P6 §4.9: 出库 — 出库成本 = 出库数量 × 当前加权平均单价
     */
    @Transactional
    public void processOutbound(BizOutboundOrder outbound, List<BizOutboundOrderItem> items) {
        for (BizOutboundOrderItem item : items) {
            BizInventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<BizInventory>()
                            .eq(BizInventory::getProjectId, outbound.getProjectId())
                            .eq(BizInventory::getMaterialId, item.getMaterialId()));

            if (inventory == null || inventory.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT.getCode(),
                        ErrorCode.STOCK_INSUFFICIENT.getMessage());
            }
            if (item.getQuantity().compareTo(inventory.getCurrentQuantity()) > 0) {
                throw new BusinessException(ErrorCode.OUTBOUND_EXCEED_STOCK.getCode(),
                        ErrorCode.OUTBOUND_EXCEED_STOCK.getMessage());
            }

            BigDecimal beforeQty = inventory.getCurrentQuantity();
            // 出库成本 = 数量 × 加权平均单价（均价不变）
            BigDecimal outboundCost = item.getQuantity().multiply(inventory.getAvgPrice());
            item.setAvgPrice(inventory.getAvgPrice());
            item.setSubtotal(outboundCost);

            BigDecimal newQty = beforeQty.subtract(item.getQuantity());
            inventory.setCurrentQuantity(newQty);
            inventory.setTotalAmount(newQty.multiply(inventory.getAvgPrice()));
            inventoryMapper.updateById(inventory);

            inventoryRecordService.record(outbound.getProjectId(), item.getMaterialId(),
                    "outbound", outbound.getId(), outbound.getOutboundNo(),
                    -1, item.getQuantity(), beforeQty, inventory.getCurrentQuantity(),
                    inventory.getAvgPrice());
        }
    }

    // ======================== P6: 盘点差异>10%强制GM审批 ========================

    /**
     * P6 §4.9: 提交盘点审批 — 超阈值时强制总经理节点
     */
    public void submitCheckForApproval(Integer checkId, Integer userId) {
        BizInventoryCheck check = checkMapper.selectById(checkId);
        if (check == null) throw new BusinessException("盘点单不存在");

        Map<String, Object> context = new HashMap<>();
        if (check.getIsOverThreshold() != null && check.getIsOverThreshold() == 1) {
            // 差异>10%: 强制包含总经理审批节点
            context.put("forceGmApproval", true);
        }
        approvalService.submitForApproval("inventory_check", checkId, userId, context);
    }

    // ======================== P6: 内部辅助方法 ========================

    /**
     * 查询或创建库存记录
     */
    private BizInventory getOrCreateInventory(Integer projectId, Integer materialId) {
        BizInventory inventory = inventoryMapper.selectOne(
                new LambdaQueryWrapper<BizInventory>()
                        .eq(BizInventory::getProjectId, projectId)
                        .eq(BizInventory::getMaterialId, materialId));
        if (inventory == null) {
            inventory = new BizInventory();
            inventory.setProjectId(projectId);
            inventory.setMaterialId(materialId);
            inventory.setCurrentQuantity(BigDecimal.ZERO);
            inventory.setAvgPrice(BigDecimal.ZERO);
            inventory.setTotalAmount(BigDecimal.ZERO);
            inventoryMapper.insert(inventory);
        }
        return inventory;
    }

    /**
     * 查询合同中该材料的剩余可入库量
     */
    private BigDecimal getContractRemainingQty(Integer contractId, Integer materialId) {
        // 需根据实际表结构实现:
        // 合同约定量 - 已入库量 = 可入库剩余量
        return BigDecimal.valueOf(999999);
    }
}
