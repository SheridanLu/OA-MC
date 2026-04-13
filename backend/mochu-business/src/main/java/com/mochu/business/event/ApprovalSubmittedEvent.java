package com.mochu.business.event;

import org.springframework.context.ApplicationEvent;

/**
 * 审批提交事件 — 通知业务模块将单据状态改为 pending
 */
public class ApprovalSubmittedEvent extends ApplicationEvent {

    private final String bizType;
    private final Integer bizId;

    public ApprovalSubmittedEvent(Object source, String bizType, Integer bizId) {
        super(source);
        this.bizType = bizType;
        this.bizId = bizId;
    }

    public String getBizType() { return bizType; }
    public Integer getBizId() { return bizId; }
}
