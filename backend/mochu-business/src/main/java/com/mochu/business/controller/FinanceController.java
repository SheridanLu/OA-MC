package com.mochu.business.controller;

import com.mochu.business.dto.InvoiceDTO;
import com.mochu.business.dto.PaymentApplyDTO;
import com.mochu.business.dto.ReceiptDTO;
import com.mochu.business.dto.ReimburseDTO;
import com.mochu.business.dto.StatementDTO;
import com.mochu.business.dto.StatusUpdateDTO;
import com.mochu.business.entity.*;
import com.mochu.business.service.FinanceService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 财务管理接口
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    // ====================== 对账单 /statements ======================

    @GetMapping("/statements")
    @PreAuthorize("hasAuthority('finance:statement-manage')")
    public R<PageResult<BizStatement>> listStatements(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer contractId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(financeService.listStatements(projectId, contractId, status, page, size));
    }

    @GetMapping("/statements/{id}")
    @PreAuthorize("hasAuthority('finance:statement-manage')")
    public R<BizStatement> getStatement(@PathVariable Integer id) {
        BizStatement entity = financeService.getStatementById(id);
        if (entity == null) {
            return R.fail(404, "对账单不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/statements")
    @PreAuthorize("hasAuthority('finance:statement-manage')")
    public R<Void> createStatement(@Valid @RequestBody StatementDTO dto) {
        financeService.createStatement(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/statements/{id}")
    @PreAuthorize("hasAuthority('finance:statement-manage')")
    public R<Void> updateStatement(@PathVariable Integer id, @Valid @RequestBody StatementDTO dto) {
        financeService.updateStatement(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/statements/{id}/status")
    @PreAuthorize("hasAuthority('finance:statement-manage')")
    public R<Void> updateStatementStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        financeService.updateStatementStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/statements/{id}")
    @PreAuthorize("hasAuthority('finance:statement-manage')")
    public R<Void> deleteStatement(@PathVariable Integer id) {
        financeService.deleteStatement(id);
        return R.ok();
    }

    // ====================== 付款申请 /payments ======================

    @GetMapping("/payments")
    @PreAuthorize("hasAnyAuthority('finance:payment-create','finance:payment-confirm')")
    public R<PageResult<BizPaymentApply>> listPayments(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer contractId,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(financeService.listPayments(projectId, contractId, paymentType, status, page, size));
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAnyAuthority('finance:payment-create','finance:payment-confirm')")
    public R<BizPaymentApply> getPayment(@PathVariable Integer id) {
        BizPaymentApply entity = financeService.getPaymentById(id);
        if (entity == null) {
            return R.fail(404, "付款申请不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('finance:payment-create')")
    public R<Void> createPayment(@Valid @RequestBody PaymentApplyDTO dto) {
        financeService.createPayment(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('finance:payment-create')")
    public R<Void> updatePayment(@PathVariable Integer id, @Valid @RequestBody PaymentApplyDTO dto) {
        financeService.updatePayment(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/payments/{id}/status")
    @PreAuthorize("hasAuthority('finance:payment-confirm')")
    public R<Void> updatePaymentStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        financeService.updatePaymentStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('finance:payment-create')")
    public R<Void> deletePayment(@PathVariable Integer id) {
        financeService.deletePayment(id);
        return R.ok();
    }

    // ====================== 发票 /invoices ======================

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('finance:invoice-manage')")
    public R<PageResult<BizInvoice>> listInvoices(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer bizId,
            @RequestParam(required = false) String invoiceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(financeService.listInvoices(bizType, bizId, invoiceType, status, page, size));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('finance:invoice-manage')")
    public R<BizInvoice> getInvoice(@PathVariable Integer id) {
        BizInvoice entity = financeService.getInvoiceById(id);
        if (entity == null) {
            return R.fail(404, "发票不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/invoices")
    @PreAuthorize("hasAuthority('finance:invoice-manage')")
    public R<Void> createInvoice(@Valid @RequestBody InvoiceDTO dto) {
        financeService.createInvoice(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('finance:invoice-manage')")
    public R<Void> updateInvoice(@PathVariable Integer id, @Valid @RequestBody InvoiceDTO dto) {
        financeService.updateInvoice(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/invoices/{id}/status")
    @PreAuthorize("hasAuthority('finance:invoice-manage')")
    public R<Void> updateInvoiceStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        financeService.updateInvoiceStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('finance:invoice-manage')")
    public R<Void> deleteInvoice(@PathVariable Integer id) {
        financeService.deleteInvoice(id);
        return R.ok();
    }

    // ====================== 报销 /reimburses ======================

    @GetMapping("/reimburses")
    @PreAuthorize("hasAuthority('finance:reimburse-manage')")
    public R<PageResult<BizReimburse>> listReimburses(
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String reimburseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(financeService.listReimburses(deptId, projectId, reimburseType, status, page, size));
    }

    @GetMapping("/reimburses/{id}")
    @PreAuthorize("hasAuthority('finance:reimburse-manage')")
    public R<BizReimburse> getReimburse(@PathVariable Integer id) {
        BizReimburse entity = financeService.getReimburseById(id);
        if (entity == null) {
            return R.fail(404, "报销单不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/reimburses")
    @PreAuthorize("hasAuthority('finance:reimburse-manage')")
    public R<Void> createReimburse(@Valid @RequestBody ReimburseDTO dto) {
        financeService.createReimburse(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/reimburses/{id}")
    @PreAuthorize("hasAuthority('finance:reimburse-manage')")
    public R<Void> updateReimburse(@PathVariable Integer id, @Valid @RequestBody ReimburseDTO dto) {
        financeService.updateReimburse(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/reimburses/{id}/status")
    @PreAuthorize("hasAuthority('finance:reimburse-manage')")
    public R<Void> updateReimburseStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        financeService.updateReimburseStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/reimburses/{id}")
    @PreAuthorize("hasAuthority('finance:reimburse-manage')")
    public R<Void> deleteReimburse(@PathVariable Integer id) {
        financeService.deleteReimburse(id);
        return R.ok();
    }

    // ====================== 成本台账 /cost-ledger ======================

    @GetMapping("/cost-ledger")
    @PreAuthorize("hasAuthority('finance:cost-view')")
    public R<PageResult<BizCostLedger>> listCostLedger(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String costType,
            @RequestParam(required = false) String costSubtype,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(financeService.listCostLedger(projectId, costType, costSubtype, page, size));
    }

    @GetMapping("/cost-ledger/{id}")
    @PreAuthorize("hasAuthority('finance:cost-view')")
    public R<BizCostLedger> getCostLedger(@PathVariable Integer id) {
        BizCostLedger entity = financeService.getCostLedgerById(id);
        if (entity == null) {
            return R.fail(404, "成本台账记录不存在");
        }
        return R.ok(entity);
    }

    @GetMapping("/cost-ledger/project/{projectId}")
    @PreAuthorize("hasAuthority('finance:cost-view')")
    public R<List<BizCostLedger>> listCostLedgerByProject(@PathVariable Integer projectId) {
        return R.ok(financeService.listCostLedgerByProject(projectId));
    }

    // ====================== 收款 /receipts ======================

    @GetMapping("/receipts")
    @PreAuthorize("hasAuthority('finance:receipt-create')")
    public R<PageResult<BizReceipt>> listReceipts(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer contractId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(financeService.listReceipts(projectId, contractId, status, page, size));
    }

    @GetMapping("/receipts/{id}")
    @PreAuthorize("hasAuthority('finance:receipt-create')")
    public R<BizReceipt> getReceipt(@PathVariable Integer id) {
        BizReceipt entity = financeService.getReceiptById(id);
        if (entity == null) {
            return R.fail(404, "收款单不存在");
        }
        return R.ok(entity);
    }

    @Idempotent
    @PostMapping("/receipts")
    @PreAuthorize("hasAuthority('finance:receipt-create')")
    public R<Void> createReceipt(@Valid @RequestBody ReceiptDTO dto) {
        financeService.createReceipt(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/receipts/{id}")
    @PreAuthorize("hasAuthority('finance:receipt-create')")
    public R<Void> updateReceipt(@PathVariable Integer id, @Valid @RequestBody ReceiptDTO dto) {
        financeService.updateReceipt(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/receipts/{id}/status")
    @PreAuthorize("hasAuthority('finance:receipt-create')")
    public R<Void> updateReceiptStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        financeService.updateReceiptStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/receipts/{id}")
    @PreAuthorize("hasAuthority('finance:receipt-create')")
    public R<Void> deleteReceipt(@PathVariable Integer id) {
        financeService.deleteReceipt(id);
        return R.ok();
    }

    // ====================== 成本汇总 ======================

    @GetMapping("/cost-summary")
    @PreAuthorize("hasAuthority('finance:cost-summary')")
    public R<Map<String, BigDecimal>> getCostSummary(@RequestParam Integer projectId) {
        return R.ok(financeService.getCostSummary(projectId));
    }
}
