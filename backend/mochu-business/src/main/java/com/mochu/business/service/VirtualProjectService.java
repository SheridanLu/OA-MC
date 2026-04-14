package com.mochu.business.service;

import com.mochu.business.dto.VirtualProjectDTO;
import com.mochu.business.entity.BizProject;
import com.mochu.business.mapper.BizProjectMapper;
import com.mochu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P6 §4.4: 虚拟项目管理
 * 创建/转换/中止/成本下挂
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualProjectService {

    private final BizProjectMapper projectMapper;
    private final NoGeneratorService noGeneratorService;
    private final ApprovalService approvalService;

    /**
     * 创建虚拟项目
     * 编号: V+YYMM+3位，每月重置
     */
    @Transactional
    public void create(VirtualProjectDTO dto, Integer userId) {
        BizProject project = new BizProject();
        project.setProjectNo(noGeneratorService.generateMonthly("V", 3));
        project.setProjectName(dto.getProjectName());
        project.setProjectType(0); // 0=虚拟
        project.setStatus("tracking");
        project.setInvestLimit(dto.getInvestLimit());
        project.setBidTime(dto.getBidTime());
        project.setCreatorId(userId);
        projectMapper.insert(project);

        // 提交审批（如已配置流程）
        if (approvalService.hasFlowDef("virtual_project")) {
            approvalService.submitForApproval("virtual_project",
                    project.getId(), userId);
        }
    }

    /**
     * 虚拟→实体项目转换
     */
    @Transactional
    public Integer convertToEntity(Integer virtualId, Integer userId) {
        BizProject virtual = projectMapper.selectById(virtualId);
        if (virtual == null || !"tracking".equals(virtual.getStatus())) {
            throw new BusinessException("虚拟项目不存在或状态不允许转换");
        }

        // 生成实体项目
        BizProject entity = new BizProject();
        entity.setProjectNo(noGeneratorService.generate("P")); // P+YYMMDD+3位
        entity.setProjectName(virtual.getProjectName());
        entity.setProjectType(1); // 1=实体
        entity.setStatus("active");
        entity.setSourceProjectId(virtualId); // 关联原虚拟项目
        entity.setInvestLimit(virtual.getInvestLimit());
        entity.setCreatorId(userId);
        projectMapper.insert(entity);

        // 原虚拟项目 → converted
        virtual.setStatus("converted");
        projectMapper.updateById(virtual);

        log.info("虚拟项目转实体: {} → {}", virtual.getProjectNo(), entity.getProjectNo());
        return entity.getId();
    }

    /**
     * 中止虚拟项目
     */
    public void terminate(Integer virtualId, Integer userId) {
        BizProject virtual = projectMapper.selectById(virtualId);
        if (virtual == null || !"tracking".equals(virtual.getStatus())) {
            throw new BusinessException("虚拟项目不存在或已终止");
        }
        virtual.setStatus("terminated");
        projectMapper.updateById(virtual);
    }

    /**
     * 成本下挂至实体项目
     */
    public void attachCostTarget(Integer virtualId, Integer targetProjectId) {
        BizProject virtual = projectMapper.selectById(virtualId);
        if (virtual == null) throw new BusinessException("虚拟项目不存在");

        BizProject target = projectMapper.selectById(targetProjectId);
        if (target == null || target.getProjectType() != 1) {
            throw new BusinessException("目标实体项目不存在");
        }

        virtual.setCostTargetProjectId(targetProjectId);
        projectMapper.updateById(virtual);
    }
}
