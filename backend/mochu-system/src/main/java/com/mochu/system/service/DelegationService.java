package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.enums.ErrorCode;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.system.dto.DelegationDTO;
import com.mochu.system.entity.SysDelegation;
import com.mochu.system.mapper.SysDelegationMapper;
import com.mochu.system.mapper.SysRoleMapper;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.vo.DelegationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 委托代理服务 — P5 增强校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelegationService {

    private final SysDelegationMapper delegationMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper sysRoleMapper;
    private final ObjectMapper objectMapper;

    public PageResult<DelegationVO> list(Integer delegatorId, Integer status, Integer page, Integer size) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        if (size == null || size < 1) size = Constants.DEFAULT_SIZE;

        Page<SysDelegation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysDelegation> wrapper = new LambdaQueryWrapper<>();

        if (delegatorId != null) {
            wrapper.eq(SysDelegation::getDelegatorId, delegatorId);
        }
        if (status != null) {
            wrapper.eq(SysDelegation::getStatus, status);
        }
        wrapper.orderByDesc(SysDelegation::getCreatedAt);
        delegationMapper.selectPage(pageParam, wrapper);

        List<DelegationVO> voList = pageParam.getRecords().stream().map(d -> {
            DelegationVO vo = new DelegationVO();
            BeanUtils.copyProperties(d, vo);
            // Resolve user names
            var delegator = userMapper.selectById(d.getDelegatorId());
            var delegatee = userMapper.selectById(d.getDelegateeId());
            if (delegator != null) vo.setDelegatorName(delegator.getRealName());
            if (delegatee != null) vo.setDelegateeName(delegatee.getRealName());
            return vo;
        }).toList();

        return new PageResult<>(voList, pageParam.getTotal(), page, size);
    }

    /**
     * 创建委托 — P5 前置校验全部规则
     */
    @Transactional
    public void create(DelegationDTO dto) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();

        // 校验1: 委托时间范围有效 (70002)
        if (dto.getStartTime() == null || dto.getEndTime() == null
                || !dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BusinessException(ErrorCode.DELEGATION_TIME_INVALID);
        }

        // 校验2: 最多3条有效委托 (70003)
        long activeCount = delegationMapper.selectCount(
                new LambdaQueryWrapper<SysDelegation>()
                        .eq(SysDelegation::getDelegatorId, currentUserId)
                        .eq(SysDelegation::getStatus, 1)
                        .gt(SysDelegation::getEndTime, LocalDateTime.now()));
        if (activeCount >= 3) {
            throw new BusinessException(ErrorCode.DELEGATION_MAX_COUNT);
        }

        // 校验3: 委托权限不超出自身范围 (70001)
        if (dto.getPermissionCodes() != null && !dto.getPermissionCodes().isEmpty()) {
            Set<String> myPermissions = sysRoleMapper.selectPermCodesByUserId(currentUserId);
            for (String perm : dto.getPermissionCodes()) {
                if (!myPermissions.contains(perm)) {
                    throw new BusinessException(ErrorCode.DELEGATION_EXCEED_SCOPE);
                }
            }
        }

        // 校验4: 禁止链式委托 (70004)
        // 检查当前用户是否拥有来自他人委托的权限
        long delegatedToMe = delegationMapper.selectCount(
                new LambdaQueryWrapper<SysDelegation>()
                        .eq(SysDelegation::getDelegateeId, currentUserId)
                        .eq(SysDelegation::getStatus, 1)
                        .gt(SysDelegation::getEndTime, LocalDateTime.now()));
        if (delegatedToMe > 0 && dto.getPermissionCodes() != null) {
            Set<String> delegatedPerms = getDelegatedPermissions(currentUserId);
            for (String perm : dto.getPermissionCodes()) {
                if (delegatedPerms.contains(perm)) {
                    throw new BusinessException(ErrorCode.DELEGATION_CHAIN_FORBIDDEN);
                }
            }
        }

        // 创建委托
        SysDelegation entity = new SysDelegation();
        entity.setDelegatorId(currentUserId);
        entity.setDelegateeId(dto.getDelegateeId());
        try {
            if (dto.getPermissionCodes() == null || dto.getPermissionCodes().isEmpty()) {
                entity.setPermissionCodes("[]");
            } else {
                entity.setPermissionCodes(objectMapper.writeValueAsString(dto.getPermissionCodes()));
            }
        } catch (JsonProcessingException e) {
            entity.setPermissionCodes("[]");
        }
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setRemark(dto.getRemark());
        entity.setStatus(1);
        delegationMapper.insert(entity);
    }

    public SysDelegation getById(Integer id) {
        SysDelegation entity = delegationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("委托记录不存在");
        }
        return entity;
    }

    public void revoke(Integer id) {
        SysDelegation entity = delegationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("委托记录不存在");
        }
        entity.setStatus(0);
        delegationMapper.updateById(entity);
    }

    public void delete(Integer id) {
        delegationMapper.deleteById(id);
    }

    /**
     * 获取某用户被委托的权限编码集合（用于链式委托检测）
     */
    private Set<String> getDelegatedPermissions(Integer userId) {
        List<SysDelegation> delegations = delegationMapper.selectList(
                new LambdaQueryWrapper<SysDelegation>()
                        .eq(SysDelegation::getDelegateeId, userId)
                        .eq(SysDelegation::getStatus, 1)
                        .gt(SysDelegation::getEndTime, LocalDateTime.now()));
        Set<String> perms = new HashSet<>();
        for (SysDelegation d : delegations) {
            try {
                List<String> codes = objectMapper.readValue(
                        d.getPermissionCodes(), new TypeReference<List<String>>() {});
                perms.addAll(codes);
            } catch (JsonProcessingException e) {
                log.warn("解析委托权限编码失败: delegationId={}", d.getId());
            }
        }
        return perms;
    }
}
