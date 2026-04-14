package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.entity.BizAttachment;
import com.mochu.business.entity.BizContract;
import com.mochu.business.entity.BizProject;
import com.mochu.business.entity.BizPurchaseList;
import com.mochu.business.entity.BizProjectMember;
import com.mochu.business.mapper.BizAttachmentMapper;
import com.mochu.business.mapper.BizContractMapper;
import com.mochu.business.mapper.BizProjectMapper;
import com.mochu.business.mapper.BizPurchaseListMapper;
import com.mochu.business.mapper.BizProjectMemberMapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.constant.FileConstants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.common.util.FileMd5Util;
import com.mochu.common.util.FileSecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 附件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final BizAttachmentMapper attachmentMapper;
    private final BizProjectMapper projectMapper;
    private final BizContractMapper contractMapper;
    private final BizPurchaseListMapper purchaseListMapper;
    private final BizProjectMemberMapper projectMemberMapper;
    private final MinioService minioService;
    private final ApprovalService approvalService;

    /**
     * 上传附件
     * 1) 安全校验（大小 + 扩展名 + 魔数）
     * 2) MD5 秒传检查
     * 3) 上传 MinIO
     * 4) 保存 biz_attachment 记录
     */
    @Transactional
    public BizAttachment upload(MultipartFile file, String bizType, Integer bizId) throws Exception {
        assertAttachmentAccess(bizType, bizId);

        // P7: 文件安全校验（大小 + 扩展名 + 文件头魔数）
        FileSecurityUtil.validate(file);

        // P7: MD5 秒传检查
        String md5;
        try (InputStream is = file.getInputStream()) {
            md5 = FileMd5Util.computeMd5(is);
        }

        BizAttachment existing = attachmentMapper.selectOne(
                new LambdaQueryWrapper<BizAttachment>()
                        .eq(BizAttachment::getMd5, md5)
                        .eq(BizAttachment::getStatus, 1)
                        .last("LIMIT 1"));

        if (existing != null) {
            // 秒传：复用已有文件，创建新的关联记录
            BizAttachment attachment = new BizAttachment();
            attachment.setFileName(file.getOriginalFilename());
            attachment.setFilePath(existing.getFilePath());
            attachment.setFileSize(existing.getFileSize());
            attachment.setFileType(existing.getFileType());
            attachment.setFileExt(existing.getFileExt());
            attachment.setMd5(md5);
            attachment.setBizType(bizType);
            attachment.setBizId(bizId);
            attachment.setStatus(1);
            attachment.setCreatorId(SecurityUtils.getCurrentUserId());
            attachmentMapper.insert(attachment);
            log.info("秒传成功: md5={}, bizType={}, bizId={}", md5, bizType, bizId);
            return attachment;
        }

        // 上传到 MinIO
        String filePath = minioService.upload(file, bizType);

        String ext = FileSecurityUtil.getExtension(file.getOriginalFilename());

        BizAttachment attachment = new BizAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setFileExt(ext);
        attachment.setMd5(md5);
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setStatus(1);
        attachment.setCreatorId(SecurityUtils.getCurrentUserId());
        attachmentMapper.insert(attachment);

        log.info("附件上传成功: id={}, path={}", attachment.getId(), filePath);
        return attachment;
    }

    /**
     * 获取下载URL
     */
    public String getDownloadUrl(Integer id) throws Exception {
        BizAttachment attachment = getAccessibleAttachment(id);
        return minioService.getPresignedUrl(attachment.getFilePath());
    }

    /**
     * 查询业务附件列表
     */
    public List<BizAttachment> listByBiz(String bizType, Integer bizId) {
        assertAttachmentAccess(bizType, bizId);
        return attachmentMapper.selectList(
                new LambdaQueryWrapper<BizAttachment>()
                        .eq(BizAttachment::getBizType, bizType)
                        .eq(BizAttachment::getBizId, bizId)
                        .eq(BizAttachment::getStatus, 1)
                        .orderByDesc(BizAttachment::getCreatedAt));
    }

    /**
     * 分页查询附件
     */
    public PageResult<BizAttachment> list(String bizType, Integer bizId, Integer page, Integer size) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        if (size == null || size < 1) size = Constants.DEFAULT_SIZE;
        if (bizType != null && !bizType.isBlank() && bizId != null) {
            assertAttachmentAccess(bizType, bizId);
        }

        Page<BizAttachment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizAttachment> wrapper = new LambdaQueryWrapper<>();

        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(BizAttachment::getBizType, bizType);
        }
        if (bizId != null) {
            wrapper.eq(BizAttachment::getBizId, bizId);
        }
        wrapper.eq(BizAttachment::getStatus, 1);
        wrapper.orderByDesc(BizAttachment::getCreatedAt);

        attachmentMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    /**
     * 删除附件 — 已审批通过的业务单据附件不可删除
     */
    @Transactional
    public void delete(Integer id) throws Exception {
        BizAttachment attachment = getAccessibleAttachment(id);

        // P7: 审批通过保护 — 检查关联业务单据是否已审批通过
        checkApprovalProtection(attachment);

        minioService.delete(attachment.getFilePath());
        attachmentMapper.deleteById(id);
        log.info("附件已删除: id={}, userId={}", id, SecurityUtils.getCurrentUserId());
    }

    /**
     * 替换附件：原文件保留并标记 replaced（status=0），上传新文件
     */
    @Transactional
    public BizAttachment replace(Integer oldAttachmentId, MultipartFile newFile) throws Exception {
        BizAttachment oldAtt = getAccessibleAttachment(oldAttachmentId);

        // 审批通过保护
        checkApprovalProtection(oldAtt);

        // 标记原文件为已替换
        oldAtt.setStatus(0); // 0=已替换
        attachmentMapper.updateById(oldAtt);

        // 上传新文件，继承 bizType 和 bizId
        return upload(newFile, oldAtt.getBizType(), oldAtt.getBizId());
    }

    /**
     * 批量关联已上传的附件到指定业务单据
     */
    @Transactional
    public void bindBatch(List<Integer> attachmentIds, String bizType, Integer bizId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        for (Integer attId : attachmentIds) {
            BizAttachment att = attachmentMapper.selectById(attId);
            if (att == null) {
                continue;
            }
            // 只允许关联本人上传的未绑定附件
            if (!currentUserId.equals(att.getCreatorId())) {
                throw new BusinessException(403, "无权操作他人附件");
            }
            att.setBizType(bizType);
            att.setBizId(bizId);
            attachmentMapper.updateById(att);
        }
    }

    /**
     * 检查附件关联的业务单据是否已审批通过
     * 已审批通过 → 不可删除/替换附件（§7.8）
     */
    private void checkApprovalProtection(BizAttachment att) {
        boolean approved = approvalService.isApproved(att.getBizType(), att.getBizId());
        if (approved) {
            throw new BusinessException(400, "业务单据已审批通过，附件不可删除");
        }
    }

    private BizAttachment getAccessibleAttachment(Integer id) {
        BizAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        assertAttachmentAccess(attachment.getBizType(), attachment.getBizId());
        return attachment;
    }

    private void assertAttachmentAccess(String bizType, Integer bizId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        boolean allowed = switch (bizType) {
            case "project" -> hasProjectAccess(bizId, currentUserId);
            case "contract" -> hasContractAccess(bizId, currentUserId);
            case "purchase", "purchase_list" -> hasPurchaseAccess(bizId, currentUserId);
            case "document" -> true; // 文档管理附件对所有登录用户开放
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException("无权访问该业务附件");
        }
    }

    private boolean hasProjectAccess(Integer bizId, Integer currentUserId) {
        BizProject project = projectMapper.selectById(bizId);
        if (project == null) {
            return false;
        }
        if (currentUserId.equals(project.getCreatorId()) || currentUserId.equals(project.getManagerId())) {
            return true;
        }
        Long memberCount = projectMemberMapper.selectCount(
                new LambdaQueryWrapper<BizProjectMember>()
                        .eq(BizProjectMember::getProjectId, bizId)
                        .eq(BizProjectMember::getUserId, currentUserId));
        return memberCount != null && memberCount > 0;
    }

    private boolean hasContractAccess(Integer bizId, Integer currentUserId) {
        BizContract contract = contractMapper.selectById(bizId);
        if (contract == null || contract.getProjectId() == null) {
            return false;
        }
        return hasProjectAccess(contract.getProjectId(), currentUserId);
    }

    private boolean hasPurchaseAccess(Integer bizId, Integer currentUserId) {
        BizPurchaseList purchase = purchaseListMapper.selectById(bizId);
        if (purchase == null || purchase.getProjectId() == null) {
            return false;
        }
        return hasProjectAccess(purchase.getProjectId(), currentUserId);
    }
}
