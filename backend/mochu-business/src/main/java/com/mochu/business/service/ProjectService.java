package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.ProjectDTO;
import com.mochu.business.dto.ProjectMemberDTO;
import com.mochu.business.dto.ProjectQueryDTO;
import com.mochu.business.entity.BizProject;
import com.mochu.business.entity.BizProjectMember;
import com.mochu.business.mapper.BizProjectMapper;
import com.mochu.business.mapper.BizProjectMemberMapper;
import com.mochu.business.vo.ProjectVO;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.util.QueryParamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 项目管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final BizProjectMapper projectMapper;
    private final BizProjectMemberMapper memberMapper;
    private final NoGeneratorService noGeneratorService;
    private final ApprovalService approvalService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "created_at", "updated_at", "id", "project_no", "status", "plan_start_date", "plan_end_date");

    public PageResult<BizProject> list(ProjectQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = QueryParamUtils.normalizeSize(query.getSize());

        Page<BizProject> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizProject> wrapper = new LambdaQueryWrapper<>();

        if (query.getProjectName() != null && !query.getProjectName().isBlank()) {
            wrapper.like(BizProject::getProjectName, query.getProjectName());
        }
        if (query.getProjectNo() != null && !query.getProjectNo().isBlank()) {
            wrapper.like(BizProject::getProjectNo, query.getProjectNo());
        }
        if (query.getProjectType() != null) {
            wrapper.eq(BizProject::getProjectType, query.getProjectType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizProject::getStatus, query.getStatus());
        }
        if (query.getManagerId() != null) {
            wrapper.eq(BizProject::getManagerId, query.getManagerId());
        }

        // V3.2: sort_field/sort_order 支持
        String orderClause = QueryParamUtils.buildOrderClause(
                query.getSortField(), query.getSortOrder(), ALLOWED_SORT_FIELDS);
        if (orderClause != null) {
            wrapper.last(orderClause);
        } else {
            wrapper.orderByDesc(BizProject::getCreatedAt);
        }

        projectMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizProject getById(Integer id) {
        return projectMapper.selectById(id);
    }

    @Transactional
    public void create(ProjectDTO dto, Integer initiatorId) {
        BizProject entity = new BizProject();
        BeanUtils.copyProperties(dto, entity);
        entity.setProjectNo(noGeneratorService.generate("PJ"));
        entity.setCreatorId(initiatorId);

        // 检查是否已配置审批流程
        boolean hasFlow = approvalService.hasFlowDef("project");
        entity.setStatus(hasFlow ? "pending" : "draft");
        projectMapper.insert(entity);

        if (hasFlow) {
            try {
                approvalService.submitForApproval("project", entity.getId(), initiatorId);
            } catch (Exception e) {
                log.warn("项目审批提交失败，保存为草稿: {}", e.getMessage());
                entity.setStatus("draft");
                projectMapper.updateById(entity);
            }
        }
    }

    public void update(Integer id, ProjectDTO dto) {
        BizProject entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("项目不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        projectMapper.updateById(entity);
    }

    public void updateStatus(Integer id, String status) {
        BizProject entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("项目不存在");
        }
        // 只有审批通过(active)的项目才允许手动变更状态
        if (!"active".equals(entity.getStatus()) && !"completed".equals(entity.getStatus())) {
            throw new BusinessException("项目尚未审批通过，无法变更状态");
        }
        entity.setStatus(status);
        projectMapper.updateById(entity);
    }

    public void delete(Integer id) {
        projectMapper.deleteById(id);
    }

    /**
     * 查询所有项目（下拉选择用）
     */
    public List<BizProject> listAll() {
        return projectMapper.selectList(
                new LambdaQueryWrapper<BizProject>()
                        .select(BizProject::getId, BizProject::getProjectNo, BizProject::getProjectName, BizProject::getStatus)
                        .orderByDesc(BizProject::getCreatedAt));
    }

    // ======================== 项目生命周期 ========================

    public void convertProject(Integer id) {
        BizProject entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("项目不存在");
        }
        if (!"virtual".equals(entity.getStatus())) {
            throw new BusinessException("只有虚拟项目才能转为实体项目");
        }
        entity.setStatus("entity");
        projectMapper.updateById(entity);
    }

    public void suspendProject(Integer id) {
        BizProject entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("项目不存在");
        }
        if (!"active".equals(entity.getStatus())) {
            throw new BusinessException("只有进行中的项目才能暂停");
        }
        entity.setStatus("suspended");
        projectMapper.updateById(entity);
    }

    public void resumeProject(Integer id) {
        BizProject entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("项目不存在");
        }
        if (!"suspended".equals(entity.getStatus())) {
            throw new BusinessException("只有已暂停的项目才能恢复");
        }
        entity.setStatus("active");
        projectMapper.updateById(entity);
    }

    public void terminateProject(Integer id, String reason) {
        BizProject entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("项目不存在");
        }
        if ("terminated".equals(entity.getStatus())) {
            throw new BusinessException("项目已终止，无法重复操作");
        }
        entity.setStatus("terminated");
        entity.setRemark(reason);
        projectMapper.updateById(entity);
    }

    // ======================== 项目成员 ========================

    public List<BizProjectMember> listMembers(Integer projectId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<BizProjectMember>()
                        .eq(BizProjectMember::getProjectId, projectId));
    }

    public void addMember(Integer projectId, ProjectMemberDTO dto) {
        Long count = memberMapper.selectCount(
                new LambdaQueryWrapper<BizProjectMember>()
                        .eq(BizProjectMember::getProjectId, projectId)
                        .eq(BizProjectMember::getUserId, dto.getUserId()));
        if (count > 0) {
            throw new BusinessException("该成员已在项目中");
        }
        BizProjectMember member = new BizProjectMember();
        member.setProjectId(projectId);
        member.setUserId(dto.getUserId());
        member.setRole(dto.getRole());
        member.setJoinDate(LocalDate.now());
        memberMapper.insert(member);
    }

    public void removeMember(Integer projectId, Integer userId) {
        memberMapper.delete(
                new LambdaQueryWrapper<BizProjectMember>()
                        .eq(BizProjectMember::getProjectId, projectId)
                        .eq(BizProjectMember::getUserId, userId));
    }
}
