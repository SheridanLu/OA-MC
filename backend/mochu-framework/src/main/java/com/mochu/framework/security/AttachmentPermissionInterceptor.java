package com.mochu.framework.security;

import com.mochu.business.entity.BizAttachment;
import com.mochu.business.mapper.BizAttachmentMapper;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 附件权限继承拦截器
 *
 * 规则：附件的访问权限继承自关联业务实体
 * 用户有权查看某合同 → 有权查看和下载该合同的所有附件
 *
 * 在 WebMvcConfigurer 中注册拦截路径：
 * /api/v1/attachment/{id}/download
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttachmentPermissionInterceptor implements HandlerInterceptor {

    private final BizAttachmentMapper attachmentMapper;
    private final DataPermissionChecker dataPermissionChecker;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String uri = request.getRequestURI();
        // 仅拦截附件下载和查看请求
        if (!uri.matches("/api/v1/attachment/\\d+/download")) {
            return true;
        }

        // 提取附件 ID
        String[] parts = uri.split("/");
        Integer attachmentId = Integer.parseInt(parts[4]);

        BizAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null || att.getDeleted() == 1) {
            throw new BusinessException(404, "附件不存在");
        }

        // 检查当前用户是否有权访问关联的业务实体
        Integer userId = SecurityUtils.getCurrentUserId();
        boolean hasAccess = dataPermissionChecker.canAccess(
                att.getBizType(), att.getBizId(), userId);

        if (!hasAccess) {
            throw new BusinessException(403, "无权访问该附件");
        }

        return true;
    }
}
