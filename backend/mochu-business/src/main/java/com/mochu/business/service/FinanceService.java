package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.IncomeSplitItemDTO;
import com.mochu.business.dto.InvoiceDTO;
import com.mochu.business.dto.PaymentApplyDTO;
import com.mochu.business.dto.ReceiptDTO;
import com.mochu.business.dto.ReimburseDTO;
import com.mochu.business.dto.StatementDTO;
import com.mochu.business.entity.*;
import com.mochu.business.mapper.*;
import com.mochu.common.constant.Constants;
import com.mochu.common.enums.ErrorCode;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务管理服务 — 对账单 / 付款申请 / 发票 / 报销 / 成本台账 / 收款
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final BizStatementMapper statementMapper;
    private final BizPaymentApplyMapper paymentApplyMapper;
    private final BizInvoiceMapper invoiceMapper;
    private final BizReimburseMapper reimburseMapper;
    private final BizCostLedgerMapper costLedgerMapper;
    private final BizReceiptMapper receiptMapper;
    private final BizContractMapper contractMapper;
    private final BizIncomeSplitMapper incomeSplitMapper;
    private final BizIncomeSplitItemMapper incomeSplitItemMapper;
    private final NoGeneratorService noGeneratorService;
    private final ApprovalService approvalService;

    // ====================== 对账单 ======================

    public PageResult<BizStatement> listStatements(Integer projectId, Integer contractId, String status,
                                                    Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizStatement> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizStatement> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizStatement::getProjectId, projectId);
        }
        if (contractId != null) {
            wrapper.eq(BizStatement::getContractId, contractId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizStatement::getStatus, status);
        }
        wrapper.orderByDesc(BizStatement::getCreatedAt);

        statementMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizStatement getStatementById(Integer id) {
        return statementMapper.selectById(id);
    }

    public void createStatement(StatementDTO dto) {
        BizStatement entity = new BizStatement();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatementNo(noGeneratorService.generate("ST"));
        entity.setStatus("draft");
        statementMapper.insert(entity);
    }

    public void updateStatement(Integer id, StatementDTO dto) {
        BizStatement entity = statementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("对账单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        statementMapper.updateById(entity);
    }

    public void updateStatementStatus(Integer id, String status) {
        BizStatement entity = statementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("对账单不存在");
        }
        entity.setStatus(status);
        statementMapper.updateById(entity);
    }

    public void deleteStatement(Integer id) {
        statementMapper.deleteById(id);
    }

    // ====================== 付款申请 ======================

    public PageResult<BizPaymentApply> listPayments(Integer projectId, Integer contractId,
                                                     String paymentType, String status,
                                                     Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizPaymentApply> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizPaymentApply> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizPaymentApply::getProjectId, projectId);
        }
        if (contractId != null) {
            wrapper.eq(BizPaymentApply::getContractId, contractId);
        }
        if (paymentType != null && !paymentType.isBlank()) {
            wrapper.eq(BizPaymentApply::getPaymentType, paymentType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizPaymentApply::getStatus, status);
        }
        wrapper.orderByDesc(BizPaymentApply::getCreatedAt);

        paymentApplyMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizPaymentApply getPaymentById(Integer id) {
        return paymentApplyMapper.selectById(id);
    }

    public void createPayment(PaymentApplyDTO dto) {
        BizPaymentApply entity = new BizPaymentApply();
        BeanUtils.copyProperties(dto, entity);
        entity.setPaymentNo(noGeneratorService.generate("PA"));
        entity.setStatus("draft");
        paymentApplyMapper.insert(entity);
    }

    public void updatePayment(Integer id, PaymentApplyDTO dto) {
        BizPaymentApply entity = paymentApplyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("付款申请不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        paymentApplyMapper.updateById(entity);
    }

    public void updatePaymentStatus(Integer id, String status) {
        BizPaymentApply entity = paymentApplyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("付款申请不存在");
        }
        entity.setStatus(status);
        paymentApplyMapper.updateById(entity);
    }

    public void deletePayment(Integer id) {
        paymentApplyMapper.deleteById(id);
    }

    // ====================== 发票 ======================

    public PageResult<BizInvoice> listInvoices(String bizType, Integer bizId, String invoiceType,
                                                String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizInvoice> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizInvoice> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(BizInvoice::getBizType, bizType);
        }
        if (bizId != null) {
            wrapper.eq(BizInvoice::getBizId, bizId);
        }
        if (invoiceType != null && !invoiceType.isBlank()) {
            wrapper.eq(BizInvoice::getInvoiceType, invoiceType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizInvoice::getStatus, status);
        }
        wrapper.orderByDesc(BizInvoice::getCreatedAt);

        invoiceMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizInvoice getInvoiceById(Integer id) {
        return invoiceMapper.selectById(id);
    }

    public void createInvoice(InvoiceDTO dto) {
        BizInvoice entity = new BizInvoice();
        BeanUtils.copyProperties(dto, entity);
        // invoiceNo comes from user input (real invoice number), generate internal IV no
        entity.setInvoiceNo(dto.getInvoiceNo());
        entity.setStatus("active");
        invoiceMapper.insert(entity);
    }

    public void updateInvoice(Integer id, InvoiceDTO dto) {
        BizInvoice entity = invoiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        invoiceMapper.updateById(entity);
    }

    public void updateInvoiceStatus(Integer id, String status) {
        BizInvoice entity = invoiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("发票不存在");
        }
        entity.setStatus(status);
        invoiceMapper.updateById(entity);
    }

    public void deleteInvoice(Integer id) {
        invoiceMapper.deleteById(id);
    }

    // ====================== 报销 ======================

    public PageResult<BizReimburse> listReimburses(Integer deptId, Integer projectId,
                                                    String reimburseType, String status,
                                                    Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizReimburse> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizReimburse> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(BizReimburse::getDeptId, deptId);
        }
        if (projectId != null) {
            wrapper.eq(BizReimburse::getProjectId, projectId);
        }
        if (reimburseType != null && !reimburseType.isBlank()) {
            wrapper.eq(BizReimburse::getReimburseType, reimburseType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizReimburse::getStatus, status);
        }
        wrapper.orderByDesc(BizReimburse::getCreatedAt);

        reimburseMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizReimburse getReimburseById(Integer id) {
        return reimburseMapper.selectById(id);
    }

    public void createReimburse(ReimburseDTO dto) {
        BizReimburse entity = new BizReimburse();
        BeanUtils.copyProperties(dto, entity);
        entity.setReimburseNo(noGeneratorService.generate("RB"));
        entity.setStatus("draft");
        reimburseMapper.insert(entity);
    }

    public void updateReimburse(Integer id, ReimburseDTO dto) {
        BizReimburse entity = reimburseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("报销单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        reimburseMapper.updateById(entity);
    }

    public void updateReimburseStatus(Integer id, String status) {
        BizReimburse entity = reimburseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("报销单不存在");
        }
        entity.setStatus(status);
        reimburseMapper.updateById(entity);
    }

    public void deleteReimburse(Integer id) {
        reimburseMapper.deleteById(id);
    }

    // ====================== 成本台账 ======================

    public PageResult<BizCostLedger> listCostLedger(Integer projectId, String costType,
                                                     String costSubtype, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizCostLedger> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizCostLedger> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizCostLedger::getProjectId, projectId);
        }
        if (costType != null && !costType.isBlank()) {
            wrapper.eq(BizCostLedger::getCostType, costType);
        }
        if (costSubtype != null && !costSubtype.isBlank()) {
            wrapper.eq(BizCostLedger::getCostSubtype, costSubtype);
        }
        wrapper.orderByDesc(BizCostLedger::getCreatedAt);

        costLedgerMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizCostLedger getCostLedgerById(Integer id) {
        return costLedgerMapper.selectById(id);
    }

    public List<BizCostLedger> listCostLedgerByProject(Integer projectId) {
        return costLedgerMapper.selectList(
                new LambdaQueryWrapper<BizCostLedger>()
                        .eq(BizCostLedger::getProjectId, projectId)
                        .orderByDesc(BizCostLedger::getCollectTime));
    }

    // ====================== 收款 ======================

    public PageResult<BizReceipt> listReceipts(Integer projectId, Integer contractId, String status,
                                                Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizReceipt> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizReceipt> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizReceipt::getProjectId, projectId);
        }
        if (contractId != null) {
            wrapper.eq(BizReceipt::getContractId, contractId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizReceipt::getStatus, status);
        }
        wrapper.orderByDesc(BizReceipt::getCreatedAt);

        receiptMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizReceipt getReceiptById(Integer id) {
        return receiptMapper.selectById(id);
    }

    public void createReceipt(ReceiptDTO dto) {
        BizReceipt entity = new BizReceipt();
        BeanUtils.copyProperties(dto, entity);
        entity.setReceiptNo(noGeneratorService.generate("SK"));
        entity.setStatus("draft");
        receiptMapper.insert(entity);
    }

    public void updateReceipt(Integer id, ReceiptDTO dto) {
        BizReceipt entity = receiptMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("收款单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        receiptMapper.updateById(entity);
    }

    public void updateReceiptStatus(Integer id, String status) {
        BizReceipt entity = receiptMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("收款单不存在");
        }
        entity.setStatus(status);
        receiptMapper.updateById(entity);
    }

    public void deleteReceipt(Integer id) {
        receiptMapper.deleteById(id);
    }

    // ====================== 成本汇总 ======================

    public Map<String, BigDecimal> getCostSummary(Integer projectId) {
        List<BizCostLedger> list = costLedgerMapper.selectList(
                new LambdaQueryWrapper<BizCostLedger>()
                        .eq(BizCostLedger::getProjectId, projectId));
        return list.stream()
                .collect(Collectors.groupingBy(
                        BizCostLedger::getCostType,
                        Collectors.reducing(BigDecimal.ZERO, BizCostLedger::getAmount, BigDecimal::add)));
    }

    // ====================== P6: 收入合同拆分校验 ======================

    /**
     * P6 §4.12: 收入拆分校验 — 所有任务金额之和 = 收入合同含税金额
     */
    public void createIncomeSplit(Integer contractId, List<IncomeSplitItemDTO> items,
                                   Integer userId) {
        BizContract contract = contractMapper.selectById(contractId);
        if (contract == null) throw new BusinessException("合同不存在");

        BigDecimal totalSplit = items.stream()
                .map(IncomeSplitItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplit.compareTo(contract.getAmountWithTax()) != 0) {
            throw new BusinessException("拆分金额之和必须等于合同含税金额("
                    + contract.getAmountWithTax() + ")");
        }

        // 创建拆分主记录
        BizIncomeSplit split = new BizIncomeSplit();
        split.setContractId(contractId);
        split.setProjectId(contract.getProjectId());
        split.setAmount(totalSplit);
        split.setStatus("draft");
        split.setCreatorId(userId);
        incomeSplitMapper.insert(split);

        // 保存拆分明细
        for (IncomeSplitItemDTO item : items) {
            BizIncomeSplitItem splitItem = new BizIncomeSplitItem();
            splitItem.setSplitId(split.getId());
            splitItem.setTaskName(item.getTaskName());
            splitItem.setAmount(item.getAmount());
            splitItem.setRemark(item.getRemark());
            incomeSplitItemMapper.insert(splitItem);
        }
    }

    // ====================== P6: 付款编号按类型 ======================

    /**
     * P6 §4.12: 创建付款申请 — 按类型使用不同编号前缀
     * 人工费: PA / 材料款: MP
     */
    public void createPaymentWithTypeNo(PaymentApplyDTO dto, Integer userId) {
        // 根据付款类型使用不同编号
        String prefix;
        switch (dto.getPaymentType() != null ? dto.getPaymentType() : "labor") {
            case "material":
                prefix = "MP"; // 材料款
                break;
            case "labor":
            default:
                prefix = "PA"; // 人工费
                break;
        }
        String paymentNo = noGeneratorService.generate(prefix);

        // 人工费付款必须关联已审批对账单 (40002)
        if ("labor".equals(dto.getPaymentType())) {
            if (dto.getStatementId() == null) {
                throw new BusinessException(ErrorCode.NO_STATEMENT_TO_LINK.getCode(),
                        ErrorCode.NO_STATEMENT_TO_LINK.getMessage());
            }
            BizStatement stmt = statementMapper.selectById(dto.getStatementId());
            if (stmt == null || !"approved".equals(stmt.getStatus())) {
                throw new BusinessException(ErrorCode.NO_STATEMENT_TO_LINK.getCode(),
                        ErrorCode.NO_STATEMENT_TO_LINK.getMessage());
            }
        }

        // 付款金额 ≤ 合同可付余额 (40001)
        if (dto.getContractId() != null) {
            BizContract contract = contractMapper.selectById(dto.getContractId());
            if (contract != null && contract.getAmountWithTax() != null) {
                BigDecimal paid = getPaidAmount(dto.getContractId());
                BigDecimal remaining = contract.getAmountWithTax().subtract(paid);
                if (dto.getAmount() != null && dto.getAmount().compareTo(remaining) > 0) {
                    throw new BusinessException(ErrorCode.PAYMENT_EXCEED_BALANCE.getCode(),
                            ErrorCode.PAYMENT_EXCEED_BALANCE.getMessage());
                }
            }
        }

        // 创建付款申请
        BizPaymentApply apply = new BizPaymentApply();
        BeanUtils.copyProperties(dto, apply);
        apply.setPaymentNo(paymentNo);
        apply.setStatus("draft");
        apply.setCreatorId(userId);
        paymentApplyMapper.insert(apply);
    }

    /**
     * 获取合同已付金额
     */
    private BigDecimal getPaidAmount(Integer contractId) {
        List<BizPaymentApply> payments = paymentApplyMapper.selectList(
                new LambdaQueryWrapper<BizPaymentApply>()
                        .eq(BizPaymentApply::getContractId, contractId)
                        .in(BizPaymentApply::getStatus, "approved", "paid")
                        .eq(BizPaymentApply::getDeleted, 0));
        return payments.stream()
                .map(BizPaymentApply::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ====================== P6: 收款登记（无需审批）======================

    /**
     * P6 §4.12: 收款登记 — FINANCE 录入，无需审批
     */
    public void createReceiptConfirmed(ReceiptDTO dto, Integer userId) {
        BizReceipt receipt = new BizReceipt();
        BeanUtils.copyProperties(dto, receipt);
        receipt.setReceiptNo(noGeneratorService.generate("SK"));
        receipt.setStatus("confirmed"); // 直接确认，无审批流程
        receipt.setCreatorId(userId);
        receiptMapper.insert(receipt);
    }

    // ====================== P6: 日常报销 ======================

    /**
     * P6 §4.12: 日常报销 — 审批：员工→直接主管→财务审批→财务付款确认
     */
    public void createReimburseWithApproval(ReimburseDTO dto, Integer userId) {
        BizReimburse reimburse = new BizReimburse();
        BeanUtils.copyProperties(dto, reimburse);
        reimburse.setReimburseNo(noGeneratorService.generate("BX"));
        reimburse.setStatus("draft");
        reimburse.setCreatorId(userId);
        reimburseMapper.insert(reimburse);

        // 提交审批
        approvalService.submitForApproval("reimburse", reimburse.getId(), userId);
    }
}
