package com.mochu.common.enums;

import lombok.Getter;

/**
 * 业务错误码枚举 —— 按模块分段
 * 10xxx 项目, 20xxx 合同, 30xxx 物资, 40xxx 财务, 50xxx 编号,
 * 60xxx 审批, 70xxx 委托, 80xxx 盘点, 90xxx 工单,
 * 11xxx 人力, 12xxx 公告, 13xxx 报表, 15xxx 附件, 16xxx 导入
 */
@Getter
public enum ErrorCode {

    // ===== 项目管理 10xxx =====
    PROJECT_STATUS_NOT_ALLOWED(10001, "项目状态不允许此操作"),

    // ===== 合同管理 20xxx =====
    CONTRACT_AMOUNT_EXCEED(20001, "合同金额超限"),
    MATERIAL_OVER_QUANTITY(20002, "物资超量需预算员审批"),
    PRICE_OVER_BASE(20003, "单价超基准价"),
    PENDING_PAYMENT_EXISTS(20004, "存在待审批付款申请"),
    PENDING_INBOUND_EXISTS(20005, "存在未完成入库单"),

    // ===== 物资管理 30xxx =====
    INBOUND_EXCEED_CONTRACT(30001, "入库数量超合同约定"),
    OUTBOUND_EXCEED_STOCK(30002, "出库数量超库存可用量"),
    STOCK_INSUFFICIENT(30003, "库存不足"),

    // ===== 财务管理 40xxx =====
    PAYMENT_EXCEED_BALANCE(40001, "付款金额超合同可付余额"),
    NO_STATEMENT_TO_LINK(40002, "无可关联对账单"),
    PAYMENT_STATUS_INVALID(40003, "付款状态不允许确认"),

    // ===== 编号生成 50xxx =====
    NO_GENERATE_FAILED(50001, "编号生成失败"),

    // ===== 审批流程 60xxx =====
    APPROVAL_ALREADY_ENDED(60001, "审批流程已结束"),
    NOT_CURRENT_APPROVER(60002, "非当前审批人"),
    APPROVAL_OPERATED(60003, "审批已操作不可撤回"),
    NOT_INITIATOR(60004, "非发起人无权撤回"),
    NOT_READ_HANDLER(60005, "非阅办人无权操作"),
    TRANSFER_TARGET_INVALID(60006, "转办目标用户不存在"),

    // ===== 权限委托 70xxx =====
    DELEGATION_EXCEED_SCOPE(70001, "委托权限超出自身范围"),
    DELEGATION_TIME_INVALID(70002, "委托时间范围无效"),
    DELEGATION_MAX_COUNT(70003, "超出最大委托数量"),
    DELEGATION_CHAIN_FORBIDDEN(70004, "禁止链式委托"),

    // ===== 库存盘点 80xxx =====
    CHECK_NOT_APPROVED(80001, "盘点差异未审批"),
    CHECK_ALREADY_CLOSED(80002, "盘点单已关闭"),

    // ===== 异常工单 90xxx =====
    TASK_ALREADY_HANDLED(90001, "工单已处理"),

    // ===== 人力资源 11xxx =====
    EMPLOYEE_NOT_FOUND(11001, "员工不存在或已离职"),
    EFFECTIVE_DATE_INVALID(11002, "生效日期不合法"),
    TAX_RATE_INCOMPLETE(11003, "税率表配置不完整"),

    // ===== 通知公告 12xxx =====
    ANNOUNCEMENT_NOT_FOUND(12001, "公告不存在"),

    // ===== 报表管理 13xxx =====
    REPORT_GENERATING(13001, "报表数据生成中"),

    // ===== 附件管理 15xxx =====
    FILE_TYPE_NOT_ALLOWED(15001, "文件类型不允许"),
    FILE_SIZE_EXCEED(15002, "文件大小超过限制"),

    // ===== 导入导出 16xxx =====
    IMPORT_TEMPLATE_MISMATCH(16001, "导入模板格式不匹配"),
    IMPORT_DATA_INVALID(16002, "导入数据校验失败"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
