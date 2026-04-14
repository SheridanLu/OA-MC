package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.system.dto.AnnouncementDTO;
import com.mochu.system.dto.AnnouncementQueryDTO;
import com.mochu.system.entity.SysAnnouncement;
import com.mochu.system.entity.SysUser;
import com.mochu.system.mapper.SysAnnouncementMapper;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.vo.AnnouncementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告服务 — 对照 V3.2 公告管理
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final SysAnnouncementMapper announcementMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 公告分页查询
     */
    public PageResult<AnnouncementVO> list(AnnouncementQueryDTO dto) {
        int page = (dto.getPage() != null && dto.getPage() > 0) ? dto.getPage() : Constants.DEFAULT_PAGE;
        int size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : Constants.DEFAULT_SIZE;

        Page<SysAnnouncement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<>();

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            wrapper.like(SysAnnouncement::getTitle, dto.getTitle());
        }
        if (dto.getType() != null && !dto.getType().isBlank()) {
            wrapper.eq(SysAnnouncement::getType, dto.getType());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            wrapper.eq(SysAnnouncement::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(SysAnnouncement::getIsTop).orderByDesc(SysAnnouncement::getCreatedAt);

        announcementMapper.selectPage(pageParam, wrapper);
        List<AnnouncementVO> voList = pageParam.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(voList, pageParam.getTotal(), page, size);
    }

    /**
     * 公告详情
     */
    public AnnouncementVO getById(Integer id) {
        SysAnnouncement entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return toVO(entity);
    }

    /**
     * 创建公告（草稿）
     */
    public Integer create(AnnouncementDTO dto) {
        SysAnnouncement entity = new SysAnnouncement();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("draft");
        entity.setPublisherId(SecurityUtils.getCurrentUserId());
        if (entity.getIsTop() == null) {
            entity.setIsTop(0);
        }
        if (entity.getScope() == null || entity.getScope().isBlank()) {
            entity.setScope("all");
        }
        announcementMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新公告
     */
    public void update(AnnouncementDTO dto) {
        SysAnnouncement entity = announcementMapper.selectById(dto.getId());
        if (entity == null) {
            throw new BusinessException(404, "公告不存在");
        }
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setType(dto.getType());
        entity.setExpireTime(dto.getExpireTime());
        entity.setIsTop(dto.getIsTop());
        entity.setScope(dto.getScope());
        announcementMapper.updateById(entity);
    }

    /**
     * 发布公告
     */
    public void publish(Integer id) {
        SysAnnouncement entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "公告不存在");
        }
        entity.setStatus("published");
        entity.setPublishTime(LocalDateTime.now());
        entity.setPublisherId(SecurityUtils.getCurrentUserId());
        announcementMapper.updateById(entity);
    }

    /**
     * 下线公告
     */
    public void offline(Integer id) {
        SysAnnouncement entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "公告不存在");
        }
        entity.setStatus("offline");
        announcementMapper.updateById(entity);
    }

    /**
     * 置顶/取消置顶
     */
    public void toggleTop(Integer id) {
        SysAnnouncement entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "公告不存在");
        }
        entity.setIsTop(entity.getIsTop() == 1 ? 0 : 1);
        announcementMapper.updateById(entity);
    }

    /**
     * 删除公告
     */
    public void delete(Integer id) {
        announcementMapper.deleteById(id);
    }

    /**
     * 前台已发布公告列表（首页调用）
     */
    public List<AnnouncementVO> listPublished(int limit) {
        LambdaQueryWrapper<SysAnnouncement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAnnouncement::getStatus, "published")
               .orderByDesc(SysAnnouncement::getIsTop)
               .orderByDesc(SysAnnouncement::getPublishTime)
               .last("LIMIT " + limit);
        return announcementMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    private AnnouncementVO toVO(SysAnnouncement entity) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getPublisherId() != null) {
            SysUser publisher = sysUserMapper.selectById(entity.getPublisherId());
            if (publisher != null) {
                vo.setPublisherName(publisher.getRealName());
            }
        }
        return vo;
    }

    // ===================== P6: 公告范围过滤 =====================

    /**
     * P6 §4.16: 查询对当前用户可见的公告（按部门ID过滤 scope）
     */
    public List<AnnouncementVO> getVisibleAnnouncements(Integer userDeptId) {
        List<SysAnnouncement> all = announcementMapper.selectList(
                new LambdaQueryWrapper<SysAnnouncement>()
                        .eq(SysAnnouncement::getStatus, "published")
                        .orderByDesc(SysAnnouncement::getIsTop)
                        .orderByDesc(SysAnnouncement::getCreatedAt));

        return all.stream().filter(a -> {
            if ("all".equals(a.getScope())) return true;
            if (a.getScope() == null || a.getScope().isBlank()) return true;
            String[] deptIds = a.getScope().split(",");
            return Arrays.asList(deptIds).contains(String.valueOf(userDeptId));
        }).map(this::toVO).collect(Collectors.toList());
    }

    // ===================== P6: 公告自动过期 =====================

    /**
     * P6 §4.16: 自动过期 — 每日 00:10 执行
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void expireAnnouncements() {
        announcementMapper.update(null, new LambdaUpdateWrapper<SysAnnouncement>()
                .eq(SysAnnouncement::getStatus, "published")
                .le(SysAnnouncement::getExpireTime, LocalDateTime.now())
                .isNotNull(SysAnnouncement::getExpireTime)
                .set(SysAnnouncement::getStatus, "expired"));
    }
}
