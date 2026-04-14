package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.ChangeLedgerQueryDTO;
import com.mochu.business.dto.ChangeOrderDTO;
import com.mochu.business.entity.BizChangeOrder;
import com.mochu.business.mapper.BizChangeOrderMapper;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * P6 §4.11: 变更管理
 * 4类变更（现场签证/甲方变更/超量采购/劳务签证）各有不同编号和审批流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeOrderService {

    private final BizChangeOrderMapper changeOrderMapper;
    private final NoGeneratorService noGeneratorService;
    private final ApprovalService approvalService;

    /** 编号前缀映射 */
    private static final Map<String, String> NO_PREFIX = Map.of(
            "site_visa", "VS",           // 现场签证
            "owner_change", "CH",        // 甲方变更
            "over_purchase", "CH",       // 超量采购变更
            "labor_visa", "VS"           // 劳务签证
    );

    /** 审批流程映射 */
    private static final Map<String, String> FLOW_TYPE = Map.of(
            "site_visa", "change_site_visa",
            "owner_change", "change_owner",
            "over_purchase", "change_over_purchase",
            "labor_visa", "change_labor_visa"
    );

    /**
     * 创建变更单
     */
    @Transactional
    public void create(ChangeOrderDTO dto, Integer userId) {
        BizChangeOrder order = new BizChangeOrder();
        BeanUtils.copyProperties(dto, order);

        String prefix = NO_PREFIX.getOrDefault(dto.getChangeType(), "CH");
        order.setChangeNo(noGeneratorService.generate(prefix));
        order.setStatus("draft");
        order.setCreatorId(userId);
        changeOrderMapper.insert(order);

        // 按变更类型走不同审批流程
        String flowType = FLOW_TYPE.getOrDefault(dto.getChangeType(), "change_order");
        if (approvalService.hasFlowDef(flowType)) {
            approvalService.submitForApproval(flowType, order.getId(), userId);
        }
    }

    /**
     * 变更台账查询 — 支持组合查询
     */
    public PageResult<BizChangeOrder> getLedger(ChangeLedgerQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? 1 : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? 20 : query.getSize();

        LambdaQueryWrapper<BizChangeOrder> wrapper = new LambdaQueryWrapper<>();
        if (query.getProjectId() != null) {
            wrapper.eq(BizChangeOrder::getProjectId, query.getProjectId());
        }
        if (query.getChangeType() != null) {
            wrapper.eq(BizChangeOrder::getChangeType, query.getChangeType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(BizChangeOrder::getCreatedAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(BizChangeOrder::getCreatedAt, query.getEndDate().plusDays(1).atStartOfDay());
        }
        wrapper.eq(BizChangeOrder::getDeleted, 0)
                .orderByDesc(BizChangeOrder::getCreatedAt);

        Page<BizChangeOrder> p = new Page<>(page, size);
        changeOrderMapper.selectPage(p, wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }
}
