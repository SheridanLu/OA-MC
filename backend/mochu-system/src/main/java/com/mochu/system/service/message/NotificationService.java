package com.mochu.system.service.message;

import com.mochu.common.message.EmailSender;
import com.mochu.common.message.SmsSender;
import com.mochu.common.message.WechatSender;
import com.mochu.system.entity.SysTodo;
import com.mochu.system.mapper.SysTodoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SysTodoMapper todoMapper;
    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final WechatSender wechatSender;

    /**
     * 统一通知 — 站内信(必发) + 可选短信/邮件/微信
     */
    public void notify(Integer userId, String title, String content,
                       String bizType, Integer bizId) {
        // 1. 站内信（必发）
        SysTodo todo = new SysTodo();
        todo.setUserId(userId);
        todo.setTitle(title);
        todo.setContent(content);
        todo.setBizType(bizType);
        todo.setBizId(bizId);
        todo.setStatus(0);
        todoMapper.insert(todo);

        // 2. 其他渠道（后续根据用户配置决定是否发送）
        // TODO: 查询用户通知偏好 sys_user_config
        // if (preferSms) smsSender.sendNotify(phone, template, params);
        // if (preferEmail) emailSender.send(email, title, content);
        // if (preferWechat) wechatSender.sendTextCard(wxId, title, content, url);
    }
}
