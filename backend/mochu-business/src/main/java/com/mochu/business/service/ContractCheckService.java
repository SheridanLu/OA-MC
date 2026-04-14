package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mochu.business.dto.ContractMaterialDTO;
import com.mochu.business.entity.BizMaterialBasePrice;
import com.mochu.business.mapper.BizMaterialBasePriceMapper;
import com.mochu.business.mapper.BizPurchaseListItemMapper;
import com.mochu.system.entity.SysConfig;
import com.mochu.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * P6 §4.6: 合同超量/超价校验
 * 合同签订前的超量/超价校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractCheckService {

    private final BizPurchaseListItemMapper purchaseItemMapper;
    private final BizMaterialBasePriceMapper basePriceMapper;
    private final SysConfigMapper configMapper;

    /**
     * 超量校验 — 支出合同物资数量 > 采购清单剩余量
     * @return true=超量需要额外审批
     */
    public boolean checkOverQuantity(Integer projectId,
                                      List<ContractMaterialDTO> materials) {
        boolean overQuantity = false;
        for (ContractMaterialDTO mat : materials) {
            // 查询采购清单中该材料的剩余计划量
            BigDecimal plannedQty = getPurchaseRemainingQty(
                    projectId, mat.getMaterialId());
            if (mat.getQuantity().compareTo(plannedQty) > 0) {
                log.warn("超量: materialId={}, 合同量={}, 剩余计划量={}",
                        mat.getMaterialId(), mat.getQuantity(), plannedQty);
                overQuantity = true;
            }
        }
        return overQuantity;
    }

    /**
     * 超价校验 — 单价 > 基准价 × (1 + 浮动阈值)
     * @return true=超价需要额外审批
     */
    public boolean checkOverPrice(List<ContractMaterialDTO> materials) {
        BigDecimal threshold = getPriceFloatThreshold(); // 默认5%
        boolean overPrice = false;

        for (ContractMaterialDTO mat : materials) {
            BizMaterialBasePrice basePrice = basePriceMapper.selectOne(
                    new LambdaQueryWrapper<BizMaterialBasePrice>()
                            .eq(BizMaterialBasePrice::getMaterialId, mat.getMaterialId())
                            .eq(BizMaterialBasePrice::getDeleted, 0)
                            .orderByDesc(BizMaterialBasePrice::getEffectiveDate)
                            .last("LIMIT 1"));

            if (basePrice != null) {
                BigDecimal maxPrice = basePrice.getBasePrice()
                        .multiply(BigDecimal.ONE.add(
                                threshold.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
                if (mat.getUnitPrice().compareTo(maxPrice) > 0) {
                    log.warn("超价: materialId={}, 合同价={}, 基准价={}, 上限={}",
                            mat.getMaterialId(), mat.getUnitPrice(),
                            basePrice.getBasePrice(), maxPrice);
                    overPrice = true;
                }
            }
        }
        return overPrice;
    }

    /**
     * 获取价格浮动阈值（默认5%，sys_config可配）
     */
    private BigDecimal getPriceFloatThreshold() {
        try {
            SysConfig config = configMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>()
                            .eq(SysConfig::getConfigKey, "contract.price.float.threshold"));
            if (config != null) return new BigDecimal(config.getConfigValue());
        } catch (Exception ignored) {}
        return new BigDecimal("5");
    }

    /**
     * 查询采购清单中材料的剩余可用量
     * 采购清单总量 - 已签合同中该材料的累计量
     */
    private BigDecimal getPurchaseRemainingQty(Integer projectId, Integer materialId) {
        // 需根据实际表结构实现
        // 查询该项目approved的采购清单中该材料的计划量
        // 减去已签合同中该材料的累计量
        // 返回剩余量
        return BigDecimal.ZERO;
    }
}
