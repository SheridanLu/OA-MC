package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.PurchaseListDTO;
import com.mochu.business.dto.SpotPurchaseDTO;
import com.mochu.business.entity.BizPurchaseList;
import com.mochu.business.entity.BizPurchaseListItem;
import com.mochu.business.entity.BizSpotPurchase;
import com.mochu.business.mapper.BizPurchaseListItemMapper;
import com.mochu.business.mapper.BizPurchaseListMapper;
import com.mochu.business.mapper.BizSpotPurchaseMapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final BizPurchaseListMapper purchaseListMapper;
    private final BizPurchaseListItemMapper purchaseListItemMapper;
    private final BizSpotPurchaseMapper spotPurchaseMapper;
    private final NoGeneratorService noGeneratorService;
    private final ApprovalService approvalService;

    // ==================== 采购清单 ====================

    public PageResult<BizPurchaseList> listPurchaseLists(String status, Integer projectId,
                                                         Integer page, Integer size) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        if (size == null || size < 1) size = Constants.DEFAULT_SIZE;

        Page<BizPurchaseList> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizPurchaseList> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isBlank()) {
            wrapper.eq(BizPurchaseList::getStatus, status);
        }
        if (projectId != null) {
            wrapper.eq(BizPurchaseList::getProjectId, projectId);
        }
        wrapper.orderByDesc(BizPurchaseList::getCreatedAt);

        purchaseListMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizPurchaseList getPurchaseListById(Integer id) {
        return purchaseListMapper.selectById(id);
    }

    public List<BizPurchaseListItem> getPurchaseListItems(Integer listId) {
        LambdaQueryWrapper<BizPurchaseListItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizPurchaseListItem::getListId, listId);
        wrapper.orderByAsc(BizPurchaseListItem::getId);
        return purchaseListItemMapper.selectList(wrapper);
    }

    @Transactional
    public void createPurchaseList(PurchaseListDTO dto) {
        BizPurchaseList entity = new BizPurchaseList();
        BeanUtils.copyProperties(dto, entity, "items");
        entity.setListNo(noGeneratorService.generate("PL"));
        entity.setStatus("draft");

        // 计算总金额
        BigDecimal total = calcTotal(dto.getItems());
        entity.setTotalAmount(total);

        purchaseListMapper.insert(entity);

        // 保存明细
        saveItems(entity.getId(), dto.getItems());
    }

    @Transactional
    public void updatePurchaseList(Integer id, PurchaseListDTO dto) {
        BizPurchaseList entity = purchaseListMapper.selectById(id);
        if (entity == null) throw new BusinessException("采购清单不存在");

        BeanUtils.copyProperties(dto, entity, "id", "items");

        BigDecimal total = calcTotal(dto.getItems());
        entity.setTotalAmount(total);

        purchaseListMapper.updateById(entity);

        // 先删除旧明细，再插入新明细
        LambdaQueryWrapper<BizPurchaseListItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(BizPurchaseListItem::getListId, id);
        purchaseListItemMapper.delete(delWrapper);

        saveItems(id, dto.getItems());
    }

    public void updatePurchaseListStatus(Integer id, String status) {
        BizPurchaseList entity = purchaseListMapper.selectById(id);
        if (entity == null) throw new BusinessException("采购清单不存在");
        entity.setStatus(status);
        purchaseListMapper.updateById(entity);
    }

    public void deletePurchaseList(Integer id) {
        purchaseListMapper.deleteById(id);
    }

    private BigDecimal calcTotal(List<PurchaseListDTO.ItemDTO> items) {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        return items.stream()
                .filter(i -> i.getSubtotal() != null)
                .map(PurchaseListDTO.ItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void saveItems(Integer listId, List<PurchaseListDTO.ItemDTO> items) {
        if (items == null || items.isEmpty()) return;
        for (PurchaseListDTO.ItemDTO itemDTO : items) {
            BizPurchaseListItem item = new BizPurchaseListItem();
            BeanUtils.copyProperties(itemDTO, item, "id");
            item.setListId(listId);
            if (item.getSubtotal() == null && item.getQuantity() != null && item.getEstimatedPrice() != null) {
                item.setSubtotal(item.getQuantity().multiply(item.getEstimatedPrice()));
            }
            purchaseListItemMapper.insert(item);
        }
    }

    // ==================== 零星采购 ====================

    public PageResult<BizSpotPurchase> listSpotPurchases(String status, Integer projectId,
                                                         Integer page, Integer size) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        if (size == null || size < 1) size = Constants.DEFAULT_SIZE;

        Page<BizSpotPurchase> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizSpotPurchase> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isBlank()) {
            wrapper.eq(BizSpotPurchase::getStatus, status);
        }
        if (projectId != null) {
            wrapper.eq(BizSpotPurchase::getProjectId, projectId);
        }
        wrapper.orderByDesc(BizSpotPurchase::getCreatedAt);

        spotPurchaseMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizSpotPurchase getSpotPurchaseById(Integer id) {
        return spotPurchaseMapper.selectById(id);
    }

    public void createSpotPurchase(SpotPurchaseDTO dto) {
        BizSpotPurchase entity = new BizSpotPurchase();
        BeanUtils.copyProperties(dto, entity);
        entity.setPurchaseNo(noGeneratorService.generate("SP"));
        entity.setStatus("draft");

        if (dto.getAmount() == null && dto.getQuantity() != null && dto.getUnitPrice() != null) {
            entity.setAmount(dto.getQuantity().multiply(dto.getUnitPrice()));
        }

        spotPurchaseMapper.insert(entity);
    }

    public void updateSpotPurchase(Integer id, SpotPurchaseDTO dto) {
        BizSpotPurchase entity = spotPurchaseMapper.selectById(id);
        if (entity == null) throw new BusinessException("零星采购不存在");
        BeanUtils.copyProperties(dto, entity, "id");

        if (dto.getAmount() == null && dto.getQuantity() != null && dto.getUnitPrice() != null) {
            entity.setAmount(dto.getQuantity().multiply(dto.getUnitPrice()));
        }

        spotPurchaseMapper.updateById(entity);
    }

    public void updateSpotPurchaseStatus(Integer id, String status) {
        BizSpotPurchase entity = spotPurchaseMapper.selectById(id);
        if (entity == null) throw new BusinessException("零星采购不存在");
        entity.setStatus(status);
        spotPurchaseMapper.updateById(entity);
    }

    public void deleteSpotPurchase(Integer id) {
        spotPurchaseMapper.deleteById(id);
    }

    // ==================== P6: 采购清单唯一性校验 ====================

    /**
     * P6 §4.8: 创建采购清单 — 同一项目仅允许1份有效(approved)采购清单
     */
    @Transactional
    public void createPurchaseListWithCheck(PurchaseListDTO dto, Integer userId) {
        // 校验：同一项目是否已有approved的采购清单
        long existCount = purchaseListMapper.selectCount(
                new LambdaQueryWrapper<BizPurchaseList>()
                        .eq(BizPurchaseList::getProjectId, dto.getProjectId())
                        .eq(BizPurchaseList::getStatus, "approved")
                        .eq(BizPurchaseList::getDeleted, 0));
        if (existCount > 0) {
            throw new BusinessException("该项目已有有效采购清单，不可重复创建");
        }

        // 调用原有创建逻辑
        createPurchaseList(dto);
    }

    // ==================== P6: 零星采购超预警阈值 ====================

    /**
     * P6 §4.8: 零星采购 — 超预警阈值时变更审批路径
     */
    @Transactional
    public void createSpotPurchaseWithApproval(SpotPurchaseDTO dto, Integer userId) {
        BizSpotPurchase entity = new BizSpotPurchase();
        BeanUtils.copyProperties(dto, entity);
        entity.setPurchaseNo(noGeneratorService.generate("LP"));
        entity.setStatus("draft");
        entity.setCreatorId(userId);

        if (dto.getAmount() == null && dto.getQuantity() != null && dto.getUnitPrice() != null) {
            entity.setAmount(dto.getQuantity().multiply(dto.getUnitPrice()));
        }

        spotPurchaseMapper.insert(entity);

        // 判断是否超预警阈值
        BigDecimal threshold = getSpotPurchaseThreshold();
        BigDecimal amount = entity.getAmount() != null ? entity.getAmount() : BigDecimal.ZERO;
        if (amount.compareTo(threshold) > 0) {
            // 超阈值审批路径
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("overThreshold", true);
            ctx.put("amount", amount);
            approvalService.submitForApproval("spot_purchase",
                    entity.getId(), userId, ctx);
        } else {
            // 正常审批路径
            approvalService.submitForApproval("spot_purchase",
                    entity.getId(), userId);
        }
    }

    /**
     * P6 §4.8: 材料编码 M+6位全局递增
     */
    public String generateMaterialCode() {
        return noGeneratorService.generateGlobal("M", 6);
    }

    /**
     * 获取零星采购预警阈值（默认5000）
     */
    private BigDecimal getSpotPurchaseThreshold() {
        // 可从 sys_config 表读取，默认5000
        return new BigDecimal("5000");
    }
}
