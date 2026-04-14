package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.CaseDTO;
import com.mochu.business.dto.CompletionDocDTO;
import com.mochu.business.dto.CompletionFinishDTO;
import com.mochu.business.dto.DrawingDTO;
import com.mochu.business.dto.ExceptionTaskDTO;
import com.mochu.business.dto.LaborSettlementDTO;
import com.mochu.business.entity.BizAttachment;
import com.mochu.business.entity.BizCase;
import com.mochu.business.entity.BizCompletionDoc;
import com.mochu.business.entity.BizCompletionFinish;
import com.mochu.business.entity.BizDrawing;
import com.mochu.business.entity.BizExceptionTask;
import com.mochu.business.entity.BizLaborSettlement;
import com.mochu.business.entity.BizProject;
import com.mochu.business.event.ApprovalCompletedEvent;
import com.mochu.business.mapper.BizAttachmentMapper;
import com.mochu.business.mapper.BizCaseMapper;
import com.mochu.business.mapper.BizCompletionDocMapper;
import com.mochu.business.mapper.BizCompletionFinishMapper;
import com.mochu.business.mapper.BizDrawingMapper;
import com.mochu.business.mapper.BizExceptionTaskMapper;
import com.mochu.business.mapper.BizLaborSettlementMapper;
import com.mochu.business.mapper.BizProjectMapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 竣工验收模块服务 — 完工验收 / 劳务结算 / 案例管理 / 异常工单 / 竣工图纸 / 竣工资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompletionService {

    private final BizCompletionFinishMapper completionFinishMapper;
    private final BizLaborSettlementMapper laborSettlementMapper;
    private final BizCaseMapper caseMapper;
    private final BizExceptionTaskMapper exceptionTaskMapper;
    private final BizDrawingMapper drawingMapper;
    private final BizCompletionDocMapper completionDocMapper;
    private final BizProjectMapper projectMapper;
    private final BizAttachmentMapper attachmentMapper;
    private final NoGeneratorService noGeneratorService;

    // ==================== 完工验收 ====================

    public PageResult<BizCompletionFinish> listFinish(Integer projectId, String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizCompletionFinish> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizCompletionFinish> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizCompletionFinish::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizCompletionFinish::getStatus, status);
        }
        wrapper.orderByDesc(BizCompletionFinish::getCreatedAt);

        completionFinishMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizCompletionFinish getFinishById(Integer id) {
        return completionFinishMapper.selectById(id);
    }

    public void createFinish(CompletionFinishDTO dto) {
        BizCompletionFinish entity = new BizCompletionFinish();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("draft");
        completionFinishMapper.insert(entity);
    }

    public void updateFinish(Integer id, CompletionFinishDTO dto) {
        BizCompletionFinish entity = completionFinishMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("完工验收记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        completionFinishMapper.updateById(entity);
    }

    public void updateFinishStatus(Integer id, String status) {
        BizCompletionFinish entity = completionFinishMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("完工验收记录不存在");
        }
        entity.setStatus(status);
        completionFinishMapper.updateById(entity);
    }

    public void deleteFinish(Integer id) {
        completionFinishMapper.deleteById(id);
    }

    // ==================== 劳务结算 ====================

    public PageResult<BizLaborSettlement> listLabor(Integer projectId, String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizLaborSettlement> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizLaborSettlement> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizLaborSettlement::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizLaborSettlement::getStatus, status);
        }
        wrapper.orderByDesc(BizLaborSettlement::getCreatedAt);

        laborSettlementMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizLaborSettlement getLaborById(Integer id) {
        return laborSettlementMapper.selectById(id);
    }

    public void createLabor(LaborSettlementDTO dto) {
        BizLaborSettlement entity = new BizLaborSettlement();
        BeanUtils.copyProperties(dto, entity);
        entity.setSettlementNo(noGeneratorService.generate("LS"));
        entity.setStatus("draft");
        laborSettlementMapper.insert(entity);
    }

    public void updateLabor(Integer id, LaborSettlementDTO dto) {
        BizLaborSettlement entity = laborSettlementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("劳务结算记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        laborSettlementMapper.updateById(entity);
    }

    public void updateLaborStatus(Integer id, String status) {
        BizLaborSettlement entity = laborSettlementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("劳务结算记录不存在");
        }
        entity.setStatus(status);
        laborSettlementMapper.updateById(entity);
    }

    public void deleteLabor(Integer id) {
        laborSettlementMapper.deleteById(id);
    }

    // ==================== 案例管理 ====================

    public PageResult<BizCase> listCase(Integer projectId, String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizCase> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizCase> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizCase::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizCase::getStatus, status);
        }
        wrapper.orderByAsc(BizCase::getDisplayOrder)
               .orderByDesc(BizCase::getCreatedAt);

        caseMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizCase getCaseById(Integer id) {
        return caseMapper.selectById(id);
    }

    public void createCase(CaseDTO dto) {
        BizCase entity = new BizCase();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("draft");
        caseMapper.insert(entity);
    }

    public void updateCase(Integer id, CaseDTO dto) {
        BizCase entity = caseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("案例记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        caseMapper.updateById(entity);
    }

    public void updateCaseStatus(Integer id, String status) {
        BizCase entity = caseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("案例记录不存在");
        }
        entity.setStatus(status);
        caseMapper.updateById(entity);
    }

    public void deleteCase(Integer id) {
        caseMapper.deleteById(id);
    }

    // ==================== 异常工单 ====================

    public PageResult<BizExceptionTask> listException(String bizType, Integer status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizExceptionTask> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizExceptionTask> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(BizExceptionTask::getBizType, bizType);
        }
        if (status != null) {
            wrapper.eq(BizExceptionTask::getStatus, status);
        }
        wrapper.orderByDesc(BizExceptionTask::getCreatedAt);

        exceptionTaskMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizExceptionTask getExceptionById(Integer id) {
        return exceptionTaskMapper.selectById(id);
    }

    public void createException(ExceptionTaskDTO dto) {
        BizExceptionTask entity = new BizExceptionTask();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1); // 1=待处理
        exceptionTaskMapper.insert(entity);
    }

    public void updateException(Integer id, ExceptionTaskDTO dto) {
        BizExceptionTask entity = exceptionTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("异常工单不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        exceptionTaskMapper.updateById(entity);
    }

    public void resolveException(Integer id, String resolveRemark) {
        BizExceptionTask entity = exceptionTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("异常工单不存在");
        }
        entity.setStatus(2); // 2=已处理
        entity.setResolveRemark(resolveRemark);
        exceptionTaskMapper.updateById(entity);
    }

    public void deleteException(Integer id) {
        exceptionTaskMapper.deleteById(id);
    }

    // ==================== 竣工图纸 ====================

    public PageResult<BizDrawing> listDrawing(Integer projectId, String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizDrawing> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizDrawing> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizDrawing::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizDrawing::getStatus, status);
        }
        wrapper.orderByDesc(BizDrawing::getCreatedAt);

        drawingMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizDrawing getDrawingById(Integer id) {
        return drawingMapper.selectById(id);
    }

    public void createDrawing(DrawingDTO dto) {
        BizDrawing entity = new BizDrawing();
        BeanUtils.copyProperties(dto, entity);
        entity.setDrawingNo(noGeneratorService.generate("DW"));
        entity.setStatus("draft");
        drawingMapper.insert(entity);
    }

    public void updateDrawing(Integer id, DrawingDTO dto) {
        BizDrawing entity = drawingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("竣工图纸记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        drawingMapper.updateById(entity);
    }

    public void updateDrawingStatus(Integer id, String status) {
        BizDrawing entity = drawingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("竣工图纸记录不存在");
        }
        entity.setStatus(status);
        drawingMapper.updateById(entity);
    }

    public void deleteDrawing(Integer id) {
        drawingMapper.deleteById(id);
    }

    // ==================== 竣工资料 ====================

    public PageResult<BizCompletionDoc> listDoc(Integer projectId, String status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_SIZE : size;

        Page<BizCompletionDoc> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<BizCompletionDoc> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(BizCompletionDoc::getProjectId, projectId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizCompletionDoc::getStatus, status);
        }
        wrapper.orderByDesc(BizCompletionDoc::getCreatedAt);

        completionDocMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), p, s);
    }

    public BizCompletionDoc getDocById(Integer id) {
        return completionDocMapper.selectById(id);
    }

    public void createDoc(CompletionDocDTO dto) {
        BizCompletionDoc entity = new BizCompletionDoc();
        BeanUtils.copyProperties(dto, entity);
        entity.setDocNo(noGeneratorService.generate("CD"));
        entity.setStatus("draft");
        completionDocMapper.insert(entity);
    }

    public void updateDoc(Integer id, CompletionDocDTO dto) {
        BizCompletionDoc entity = completionDocMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("竣工资料记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id");
        completionDocMapper.updateById(entity);
    }

    public void updateDocStatus(Integer id, String status) {
        BizCompletionDoc entity = completionDocMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("竣工资料记录不存在");
        }
        entity.setStatus(status);
        completionDocMapper.updateById(entity);
    }

    public void deleteDoc(Integer id) {
        completionDocMapper.deleteById(id);
    }

    // ==================== P6: 完工验收审批→项目状态变更 ====================

    /**
     * P6 §4.14: 完工验收审批通过 → 项目 status → completion_accepted
     */
    @EventListener
    public void onCompletionApproved(ApprovalCompletedEvent event) {
        if ("completion_acceptance".equals(event.getBizType())
                && "approved".equals(event.getFinalStatus())) {
            BizProject project = projectMapper.selectById(event.getBizId());
            if (project != null) {
                project.setStatus("completion_accepted");
                projectMapper.updateById(project);
                log.info("项目已完工验收: {}", project.getProjectNo());
            }
        }
    }

    // ==================== P6: 竣工文档归档 ====================

    /**
     * P6 §4.14: 竣工文档归档 — 按类别自动归集
     */
    public Map<String, List<BizAttachment>> getArchiveByCategory(Integer projectId) {
        List<BizAttachment> allDocs = attachmentMapper.selectList(
                new LambdaQueryWrapper<BizAttachment>()
                        .eq(BizAttachment::getProjectId, projectId)
                        .eq(BizAttachment::getDeleted, 0));

        Map<String, List<BizAttachment>> archive = new LinkedHashMap<>();
        archive.put("合同文档", filterByBizType(allDocs, "contract"));
        archive.put("变更文档", filterByBizType(allDocs, "change", "change_order"));
        archive.put("进度文档", filterByBizType(allDocs, "progress", "progress_report"));
        archive.put("财务文档", filterByBizType(allDocs, "payment", "payment_apply", "invoice", "receipt", "statement"));
        archive.put("图纸文档", filterByBizType(allDocs, "drawing", "completion_drawing"));
        return archive;
    }

    private List<BizAttachment> filterByBizType(List<BizAttachment> all, String... types) {
        Set<String> typeSet = Set.of(types);
        return all.stream()
                .filter(a -> typeSet.contains(a.getBizType()))
                .collect(Collectors.toList());
    }

    // ==================== P6: 劳务结算编号 ====================

    /**
     * P6 §4.14: 劳务结算编号 LS+YYMMDD+3位
     */
    public String generateLaborSettlementNo() {
        return noGeneratorService.generate("LS");
    }
}
