package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.dto.InventoryAlertDTO;
import com.mochu.business.dto.InventoryTransferDTO;
import com.mochu.business.entity.BizInventory;
import com.mochu.business.entity.BizInventoryAlert;
import com.mochu.business.entity.BizInventoryTransfer;
import com.mochu.business.mapper.BizInventoryAlertMapper;
import com.mochu.business.mapper.BizInventoryMapper;
import com.mochu.business.mapper.BizInventoryTransferMapper;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存增强服务 — 预警 + 调拨 + 进销存报表 + 库龄分析
 */
@Service
@RequiredArgsConstructor
public class InventoryEnhanceService {

    private final BizInventoryAlertMapper alertMapper;
    private final BizInventoryTransferMapper transferMapper;
    private final BizInventoryMapper inventoryMapper;
    private final NoGeneratorService noGeneratorService;

    // ==================== 库存预警 ====================

    public List<BizInventoryAlert> listAlerts(Integer projectId) {
        LambdaQueryWrapper<BizInventoryAlert> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) wrapper.eq(BizInventoryAlert::getProjectId, projectId);
        return alertMapper.selectList(wrapper);
    }

    public void saveAlert(InventoryAlertDTO dto) {
        BizInventoryAlert existing = alertMapper.selectOne(
                new LambdaQueryWrapper<BizInventoryAlert>()
                        .eq(BizInventoryAlert::getProjectId, dto.getProjectId())
                        .eq(BizInventoryAlert::getMaterialId, dto.getMaterialId()));
        if (existing != null) {
            existing.setSafetyQty(dto.getSafetyQty());
            existing.setMinQty(dto.getMinQty());
            existing.setAlertEnabled(dto.getAlertEnabled() != null ? dto.getAlertEnabled() : 1);
            alertMapper.updateById(existing);
        } else {
            BizInventoryAlert alert = new BizInventoryAlert();
            alert.setProjectId(dto.getProjectId());
            alert.setMaterialId(dto.getMaterialId());
            alert.setMaterialName(dto.getMaterialName() != null ? dto.getMaterialName() : "");
            alert.setUnit(dto.getUnit() != null ? dto.getUnit() : "");
            alert.setSafetyQty(dto.getSafetyQty() != null ? dto.getSafetyQty() : BigDecimal.ZERO);
            alert.setMinQty(dto.getMinQty() != null ? dto.getMinQty() : BigDecimal.ZERO);
            alert.setAlertEnabled(dto.getAlertEnabled() != null ? dto.getAlertEnabled() : 1);
            alert.setCreatorId(SecurityUtils.getCurrentUserId());
            alertMapper.insert(alert);
        }
    }

    public void deleteAlert(Integer id) {
        alertMapper.deleteById(id);
    }

    /**
     * 查询触发预警的库存（当前库存 < 安全库存）
     */
    public List<Object> listTriggeredAlerts(Integer projectId) {
        List<BizInventoryAlert> alerts = listAlerts(projectId);
        return alerts.stream()
                .filter(a -> a.getAlertEnabled() != null && a.getAlertEnabled() == 1)
                .filter(a -> {
                    BizInventory inv = inventoryMapper.selectOne(
                            new LambdaQueryWrapper<BizInventory>()
                                    .eq(BizInventory::getProjectId, a.getProjectId())
                                    .eq(BizInventory::getMaterialId, a.getMaterialId()));
                    if (inv == null) return a.getMinQty().compareTo(BigDecimal.ZERO) > 0;
                    return inv.getCurrentQuantity().compareTo(a.getSafetyQty()) < 0;
                })
                .collect(Collectors.toList());
    }

    // ==================== 库存调拨 ====================

    public List<BizInventoryTransfer> listTransfers(Integer projectId, String status) {
        LambdaQueryWrapper<BizInventoryTransfer> wrapper = new LambdaQueryWrapper<BizInventoryTransfer>()
                .orderByDesc(BizInventoryTransfer::getCreatedAt);
        if (projectId != null) {
            wrapper.and(w -> w.eq(BizInventoryTransfer::getFromProjectId, projectId)
                    .or().eq(BizInventoryTransfer::getToProjectId, projectId));
        }
        if (status != null && !status.isBlank()) wrapper.eq(BizInventoryTransfer::getStatus, status);
        return transferMapper.selectList(wrapper);
    }

    @Transactional
    public void createTransfer(InventoryTransferDTO dto) {
        // 检查源库存是否充足
        BizInventory fromInv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<BizInventory>()
                        .eq(BizInventory::getProjectId, dto.getFromProjectId())
                        .eq(BizInventory::getMaterialId, dto.getMaterialId()));
        if (fromInv == null || fromInv.getCurrentQuantity().compareTo(dto.getQty()) < 0) {
            throw new BusinessException("源项目库存不足，无法调拨");
        }

        BizInventoryTransfer transfer = new BizInventoryTransfer();
        transfer.setTransferNo(noGeneratorService.generate("TR"));
        transfer.setFromProjectId(dto.getFromProjectId());
        transfer.setToProjectId(dto.getToProjectId());
        transfer.setMaterialId(dto.getMaterialId());
        transfer.setMaterialName(fromInv.getMaterialName());
        transfer.setUnit(fromInv.getUnit());
        transfer.setQty(dto.getQty());
        transfer.setAvgPrice(fromInv.getAvgPrice() != null ? fromInv.getAvgPrice() : BigDecimal.ZERO);
        transfer.setTotalAmount(transfer.getAvgPrice().multiply(dto.getQty()));
        transfer.setStatus("draft");
        transfer.setRemark(dto.getRemark());
        transfer.setCreatorId(SecurityUtils.getCurrentUserId());
        transferMapper.insert(transfer);
    }

    @Transactional
    public void confirmTransfer(Integer id) {
        BizInventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new BusinessException("调拨单不存在");
        if (!"draft".equals(transfer.getStatus())) throw new BusinessException("当前状态不可确认");

        // 扣减源库存 — 使用乐观更新防止并发超扣
        BizInventory fromInv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<BizInventory>()
                        .eq(BizInventory::getProjectId, transfer.getFromProjectId())
                        .eq(BizInventory::getMaterialId, transfer.getMaterialId()));
        if (fromInv == null || fromInv.getCurrentQuantity().compareTo(transfer.getQty()) < 0) {
            throw new BusinessException("源库存不足，无法确认调拨");
        }
        BigDecimal newFromQty = fromInv.getCurrentQuantity().subtract(transfer.getQty());
        BigDecimal fromAvgPrice = fromInv.getAvgPrice() != null ? fromInv.getAvgPrice() : BigDecimal.ZERO;
        fromInv.setCurrentQuantity(newFromQty);
        fromInv.setTotalAmount(newFromQty.multiply(fromAvgPrice));
        // 乐观锁检查：只有当数量仍然足够时才扣减成功
        int affected = inventoryMapper.update(fromInv,
                new LambdaQueryWrapper<BizInventory>()
                        .eq(BizInventory::getId, fromInv.getId())
                        .ge(BizInventory::getCurrentQuantity, transfer.getQty()));
        if (affected == 0) {
            throw new BusinessException("库存已被其他操作修改，请刷新后重试");
        }

        // 增加目标库存（加权平均法）
        BizInventory toInv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<BizInventory>()
                        .eq(BizInventory::getProjectId, transfer.getToProjectId())
                        .eq(BizInventory::getMaterialId, transfer.getMaterialId()));
        if (toInv == null) {
            toInv = new BizInventory();
            toInv.setProjectId(transfer.getToProjectId());
            toInv.setMaterialId(transfer.getMaterialId());
            toInv.setMaterialName(transfer.getMaterialName());
            toInv.setUnit(transfer.getUnit());
            toInv.setCurrentQuantity(transfer.getQty());
            toInv.setAvgPrice(transfer.getAvgPrice());
            toInv.setTotalAmount(transfer.getTotalAmount());
            toInv.setCreatorId(SecurityUtils.getCurrentUserId());
            inventoryMapper.insert(toInv);
        } else {
            BigDecimal oldTotal = toInv.getTotalAmount() != null ? toInv.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal oldQty = toInv.getCurrentQuantity();
            BigDecimal newQty = oldQty.add(transfer.getQty());
            BigDecimal newTotal = oldTotal.add(transfer.getTotalAmount());
            toInv.setCurrentQuantity(newQty);
            toInv.setTotalAmount(newTotal);
            if (newQty.compareTo(BigDecimal.ZERO) > 0) {
                toInv.setAvgPrice(newTotal.divide(newQty, 6, java.math.RoundingMode.HALF_UP));
            }
            // 乐观锁：确保目标库存未被并发修改
            int toAffected = inventoryMapper.update(toInv,
                    new LambdaQueryWrapper<BizInventory>()
                            .eq(BizInventory::getId, toInv.getId())
                            .eq(BizInventory::getCurrentQuantity, oldQty));
            if (toAffected == 0) {
                throw new BusinessException("目标库存已被其他操作修改，请刷新后重试");
            }
        }

        transfer.setStatus("confirmed");
        transfer.setConfirmTime(LocalDateTime.now());
        transferMapper.updateById(transfer);
    }

    public void cancelTransfer(Integer id) {
        BizInventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new BusinessException("调拨单不存在");
        if (!"draft".equals(transfer.getStatus())) throw new BusinessException("已确认的调拨单无法取消");
        transfer.setStatus("cancelled");
        transferMapper.updateById(transfer);
    }
}
