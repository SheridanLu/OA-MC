package com.mochu.business.policy;

import com.mochu.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 项目状态操作边界矩阵
 * 定义每个状态下允许的操作，不在允许列表中的操作一律禁止
 */
@Slf4j
public class ProjectStatusPolicy {

    /** 操作常量 */
    public static final String OP_CREATE_CONTRACT = "create_contract";
    public static final String OP_CREATE_PURCHASE = "create_purchase";
    public static final String OP_CREATE_CHANGE = "create_change";
    public static final String OP_CREATE_PAYMENT = "create_payment";
    public static final String OP_CREATE_PAYMENT_TAIL = "create_payment_tail";
    public static final String OP_INBOUND = "inbound";
    public static final String OP_OUTBOUND = "outbound";
    public static final String OP_LABOR_SETTLEMENT = "labor_settlement";
    public static final String OP_RECEIPT = "receipt";
    public static final String OP_INVOICE = "invoice";
    public static final String OP_COMPLETION_DOC = "completion_doc";
    public static final String OP_COMPLETION_DRAWING = "completion_drawing";
    public static final String OP_REPORT = "report";
    public static final String OP_PROGRESS = "progress";
    public static final String OP_EXPORT = "export";

    /** 状态-操作允许矩阵 */
    private static final Map<String, Set<String>> ALLOWED_OPS = new HashMap<>();

    static {
        // active — 全部允许
        ALLOWED_OPS.put("active", new HashSet<>(Arrays.asList(
                OP_CREATE_CONTRACT, OP_CREATE_PURCHASE, OP_CREATE_CHANGE,
                OP_CREATE_PAYMENT, OP_INBOUND, OP_OUTBOUND,
                OP_LABOR_SETTLEMENT, OP_RECEIPT, OP_INVOICE,
                OP_COMPLETION_DOC, OP_COMPLETION_DRAWING,
                OP_REPORT, OP_PROGRESS, OP_EXPORT
        )));

        // suspended — 仅允许查看和出入库
        ALLOWED_OPS.put("suspended", new HashSet<>(Arrays.asList(
                OP_INBOUND, OP_OUTBOUND, OP_REPORT, OP_EXPORT
        )));

        // completion_accepted — 禁止新合同/采购/变更
        ALLOWED_OPS.put("completion_accepted", new HashSet<>(Arrays.asList(
                OP_CREATE_PAYMENT_TAIL, OP_LABOR_SETTLEMENT, OP_RECEIPT,
                OP_INVOICE, OP_COMPLETION_DOC, OP_COMPLETION_DRAWING,
                OP_REPORT, OP_EXPORT
        )));

        // final_accepted — 同 completion_accepted
        ALLOWED_OPS.put("final_accepted", new HashSet<>(Arrays.asList(
                OP_CREATE_PAYMENT_TAIL, OP_LABOR_SETTLEMENT, OP_RECEIPT,
                OP_INVOICE, OP_COMPLETION_DOC, OP_COMPLETION_DRAWING,
                OP_REPORT, OP_EXPORT
        )));

        // audit_done — 收款/发票/文档/报表
        ALLOWED_OPS.put("audit_done", new HashSet<>(Arrays.asList(
                OP_RECEIPT, OP_INVOICE, OP_COMPLETION_DOC,
                OP_REPORT, OP_EXPORT
        )));

        // closed — 仅报表查看
        ALLOWED_OPS.put("closed", new HashSet<>(Arrays.asList(
                OP_REPORT, OP_EXPORT
        )));

        // terminated — 文档归档+报表
        ALLOWED_OPS.put("terminated", new HashSet<>(Arrays.asList(
                OP_COMPLETION_DOC, OP_REPORT, OP_EXPORT
        )));
    }

    /**
     * 校验操作是否允许
     * @param projectStatus 项目当前状态
     * @param operation     要执行的操作
     * @throws BusinessException 如果操作不允许
     */
    public static void checkAllowed(String projectStatus, String operation) {
        Set<String> allowed = ALLOWED_OPS.get(projectStatus);
        if (allowed == null || !allowed.contains(operation)) {
            throw new BusinessException(10001,
                    String.format("项目状态[%s]不允许执行[%s]操作",
                            projectStatus, operation));
        }
    }

    /**
     * 判断操作是否允许（不抛异常）
     */
    public static boolean isAllowed(String projectStatus, String operation) {
        Set<String> allowed = ALLOWED_OPS.get(projectStatus);
        return allowed != null && allowed.contains(operation);
    }
}
