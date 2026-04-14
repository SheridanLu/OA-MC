package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.ContractDTO;
import com.mochu.business.entity.BizContract;
import com.mochu.business.entity.BizContractFieldValue;
import com.mochu.business.entity.BizInvoice;
import com.mochu.business.entity.BizPaymentApply;
import com.mochu.business.entity.BizPurchaseList;
import com.mochu.business.entity.SysContractTplField;
import com.mochu.business.entity.SysContractTplVersion;
import com.mochu.business.enums.ContractTypeEnum;
import com.mochu.business.mapper.BizContractFieldValueMapper;
import com.mochu.business.mapper.BizContractMapper;
import com.mochu.business.mapper.BizInboundOrderMapper;
import com.mochu.business.mapper.BizInvoiceMapper;
import com.mochu.business.mapper.BizPaymentApplyMapper;
import com.mochu.business.mapper.BizPurchaseListMapper;
import com.mochu.business.dto.ContractMaterialDTO;
import com.mochu.common.constant.Constants;
import com.mochu.common.enums.ErrorCode;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.util.QueryParamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final BizContractMapper contractMapper;
    private final BizContractFieldValueMapper fieldValueMapper;
    private final BizPaymentApplyMapper paymentApplyMapper;
    private final BizInvoiceMapper invoiceMapper;
    private final BizInboundOrderMapper inboundOrderMapper;
    private final BizPurchaseListMapper purchaseListMapper;
    private final NoGeneratorService noGeneratorService;
    private final ContractTplService tplService;
    private final ApprovalService approvalService;
    private final ContractVersionService contractVersionService;
    private final ContractCheckService contractCheckService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "created_at", "updated_at", "id", "contract_no", "amount_with_tax", "status", "sign_date");

    public PageResult<BizContract> list(String contractName, String contractType, String status,
                                         Integer projectId, Integer page, Integer size,
                                         String sortField, String sortOrder) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        size = QueryParamUtils.normalizeSize(size);

        Page<BizContract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizContract> wrapper = new LambdaQueryWrapper<>();

        if (contractName != null && !contractName.isBlank()) {
            wrapper.like(BizContract::getContractName, contractName);
        }
        if (contractType != null && !contractType.isBlank()) {
            wrapper.eq(BizContract::getContractType, contractType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizContract::getStatus, status);
        }
        if (projectId != null) {
            wrapper.eq(BizContract::getProjectId, projectId);
        }

        // V3.2: sort_field/sort_order 支持
        String orderClause = QueryParamUtils.buildOrderClause(sortField, sortOrder, ALLOWED_SORT_FIELDS);
        if (orderClause != null) {
            wrapper.last(orderClause);
        } else {
            wrapper.orderByDesc(BizContract::getCreatedAt);
        }

        contractMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizContract getById(Integer id) {
        return contractMapper.selectById(id);
    }

    /**
     * 获取合同详情（含字段值）
     */
    public Map<String, Object> getDetail(Integer id) {
        BizContract contract = contractMapper.selectById(id);
        if (contract == null) throw new BusinessException("合同不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contract", contract);

        // 字段值
        List<BizContractFieldValue> values = fieldValueMapper.selectList(
                new LambdaQueryWrapper<BizContractFieldValue>()
                        .eq(BizContractFieldValue::getContractId, id));
        Map<String, String> fieldValues = values.stream()
                .collect(Collectors.toMap(BizContractFieldValue::getFieldKey, v -> v.getFieldValue() != null ? v.getFieldValue() : ""));
        result.put("field_values", fieldValues);

        // 模板字段定义（若有模板版本）
        if (contract.getTplVersionId() != null) {
            List<SysContractTplField> fields = tplService.listFields(contract.getTplVersionId());
            result.put("field_defs", fields);
            result.put("tpl_html", tplService.getPreviewHtml(contract.getTplVersionId()));
        }

        return result;
    }

    /**
     * 创建合同 — 模板驱动 + 字段校验 + 提交审批（均可选降级）
     */
    @Transactional
    public void create(ContractDTO dto, Integer initiatorId) {
        // 1. 校验合同类型
        if (!ContractTypeEnum.isValid(dto.getContractType())) {
            throw new BusinessException("无效的合同类型，必须为七类标准类型之一");
        }

        // 2. 查找该类型当前启用模板版本（可选）
        SysContractTplVersion activeVersion = null;
        List<SysContractTplField> fieldDefs = List.of();
        try {
            activeVersion = tplService.getActiveVersion(dto.getContractType());
            if (activeVersion != null) {
                fieldDefs = tplService.listFields(activeVersion.getId());
                validateFieldValues(dto.getFieldValues(), fieldDefs);
            }
        } catch (BusinessException e) {
            throw e; // 校验失败直接抛出
        } catch (Exception e) {
            log.warn("模板加载失败，跳过模板绑定: {}", e.getMessage());
        }

        // 3. 创建合同主记录
        BizContract entity = new BizContract();
        BeanUtils.copyProperties(dto, entity, "fieldValues");
        entity.setContractNo(noGeneratorService.generate("CT"));
        entity.setCreatorId(initiatorId);
        if (activeVersion != null) {
            entity.setTemplateId(activeVersion.getTplId());
            entity.setTplVersionId(activeVersion.getId());
        }
        if (dto.getTaxAmount() == null && dto.getAmountWithTax() != null && dto.getAmountWithoutTax() != null) {
            entity.setTaxAmount(dto.getAmountWithTax().subtract(dto.getAmountWithoutTax()));
        }

        // 4. 检查审批流程并设置状态
        boolean hasFlow = approvalService.hasFlowDef("contract");
        entity.setStatus(hasFlow ? "pending" : "draft");
        contractMapper.insert(entity);

        // 5. 批量写入字段值
        if (dto.getFieldValues() != null && activeVersion != null) {
            for (Map.Entry<String, String> entry : dto.getFieldValues().entrySet()) {
                if (entry.getValue() == null) continue;
                BizContractFieldValue fv = new BizContractFieldValue();
                fv.setContractId(entity.getId());
                fv.setFieldKey(entry.getKey());
                fv.setFieldValue(entry.getValue());
                fv.setCreatedAt(LocalDateTime.now());
                fieldValueMapper.insert(fv);
            }
        }

        // 6. 提交审批
        if (hasFlow) {
            try {
                Map<String, Object> bizContext = new HashMap<>();
                bizContext.put("contract_type", dto.getContractType());
                approvalService.submitForApproval("contract", entity.getId(), initiatorId, bizContext);
            } catch (Exception e) {
                log.warn("合同审批提交失败，保存为草稿: {}", e.getMessage());
                entity.setStatus("draft");
                contractMapper.updateById(entity);
            }
        }
    }

    /**
     * 更新合同 — 仅允许 draft/rejected 状态
     */
    @Transactional
    public void update(Integer id, ContractDTO dto) {
        BizContract entity = contractMapper.selectById(id);
        if (entity == null) throw new BusinessException("合同不存在");
        if (!"draft".equals(entity.getStatus()) && !"rejected".equals(entity.getStatus())) {
            throw new BusinessException("仅草稿或已驳回状态的合同可以修改");
        }

        // 校验合同类型
        if (!ContractTypeEnum.isValid(dto.getContractType())) {
            throw new BusinessException("无效的合同类型");
        }

        BeanUtils.copyProperties(dto, entity, "id", "fieldValues");
        if (dto.getTaxAmount() == null && dto.getAmountWithTax() != null && dto.getAmountWithoutTax() != null) {
            entity.setTaxAmount(dto.getAmountWithTax().subtract(dto.getAmountWithoutTax()));
        }
        contractMapper.updateById(entity);

        // 更新字段值
        if (dto.getFieldValues() != null && entity.getTplVersionId() != null) {
            List<SysContractTplField> fieldDefs = tplService.listFields(entity.getTplVersionId());
            validateFieldValues(dto.getFieldValues(), fieldDefs);

            // 删除旧值重建
            fieldValueMapper.delete(
                    new LambdaQueryWrapper<BizContractFieldValue>()
                            .eq(BizContractFieldValue::getContractId, id));
            for (Map.Entry<String, String> entry : dto.getFieldValues().entrySet()) {
                BizContractFieldValue fv = new BizContractFieldValue();
                fv.setContractId(id);
                fv.setFieldKey(entry.getKey());
                fv.setFieldValue(entry.getValue());
                fv.setCreatedAt(LocalDateTime.now());
                fieldValueMapper.insert(fv);
            }
        }
    }

    public void updateStatus(Integer id, String status) {
        BizContract entity = contractMapper.selectById(id);
        if (entity == null) throw new BusinessException("合同不存在");
        if (!"approved".equals(entity.getStatus()) && !"terminated".equals(entity.getStatus())) {
            throw new BusinessException("合同尚未审批通过，无法手动变更状态");
        }
        entity.setStatus(status);
        contractMapper.updateById(entity);
    }

    public void delete(Integer id) {
        BizContract entity = contractMapper.selectById(id);
        if (entity == null) throw new BusinessException("合同不存在");
        if ("pending".equals(entity.getStatus()) || "approved".equals(entity.getStatus())) {
            throw new BusinessException("审批中或已审批的合同不可删除");
        }
        contractMapper.deleteById(id);
    }

    /**
     * 提交合同审批 — 仅 draft/rejected 状态可提交
     */
    public void submitContract(Integer id, Integer initiatorId) {
        BizContract entity = contractMapper.selectById(id);
        if (entity == null) throw new BusinessException("合同不存在");
        if (!"draft".equals(entity.getStatus()) && !"rejected".equals(entity.getStatus())) {
            throw new BusinessException("仅草稿或已驳回状态的合同可以提交审批");
        }

        entity.setStatus("pending");
        contractMapper.updateById(entity);

        try {
            Map<String, Object> bizContext = new HashMap<>();
            bizContext.put("contract_type", entity.getContractType());
            approvalService.submitForApproval("contract", entity.getId(), initiatorId, bizContext);
        } catch (Exception e) {
            log.warn("合同审批提交失败: {}", e.getMessage());
            entity.setStatus("draft");
            contractMapper.updateById(entity);
            throw new BusinessException("审批提交失败: " + e.getMessage());
        }
    }

    /**
     * 终止合同 — 仅 approved/executing 状态可终止
     */
    public void terminateContract(Integer id, String reason, Integer terminatorId) {
        BizContract entity = contractMapper.selectById(id);
        if (entity == null) throw new BusinessException("合同不存在");
        if (!"approved".equals(entity.getStatus()) && !"executing".equals(entity.getStatus())) {
            throw new BusinessException("仅已审批或执行中的合同可以终止");
        }
        // 终止前生成版本快照
        contractVersionService.createSnapshot(entity, "terminate", "合同终止", terminatorId);
        entity.setStatus("terminated");
        entity.setTerminateReason(reason);
        entity.setTerminateTime(LocalDateTime.now());
        entity.setTerminatorId(terminatorId);
        contractMapper.updateById(entity);
    }

    /**
     * 补充协议审批通过回调 — 生成版本快照
     */
    public void onSupplementApproved(Integer contractId, Integer operatorId) {
        BizContract contract = contractMapper.selectById(contractId);
        contractVersionService.createSnapshot(
                contract, "supplement", "补充协议审批通过", operatorId);
        // ... 更新合同金额等
    }

    /**
     * 查询合同的补充协议（parentContractId = contractId）
     */
    public List<BizContract> listSupplements(Integer contractId) {
        return contractMapper.selectList(
                new LambdaQueryWrapper<BizContract>()
                        .eq(BizContract::getParentContractId, contractId)
                        .orderByDesc(BizContract::getCreatedAt));
    }

    /**
     * 创建补充协议
     */
    @Transactional
    public void createSupplement(Integer parentContractId, ContractDTO dto, Integer initiatorId) {
        BizContract parent = contractMapper.selectById(parentContractId);
        if (parent == null) throw new BusinessException("主合同不存在");

        dto.setParentContractId(parentContractId);
        if (dto.getProjectId() == null) {
            dto.setProjectId(parent.getProjectId());
        }
        create(dto, initiatorId);
    }

    // ===================== 合同关联查询 =====================

    /**
     * 查询合同关联的付款申请
     */
    public List<BizPaymentApply> listPaymentsByContract(Integer contractId) {
        return paymentApplyMapper.selectList(
                new LambdaQueryWrapper<BizPaymentApply>()
                        .eq(BizPaymentApply::getContractId, contractId)
                        .orderByDesc(BizPaymentApply::getCreatedAt));
    }

    /**
     * 查询合同关联的发票（bizType='contract', bizId=contractId）
     */
    public List<BizInvoice> listInvoicesByContract(Integer contractId) {
        return invoiceMapper.selectList(
                new LambdaQueryWrapper<BizInvoice>()
                        .eq(BizInvoice::getBizType, "contract")
                        .eq(BizInvoice::getBizId, contractId)
                        .orderByDesc(BizInvoice::getInvoiceDate));
    }

    /**
     * 超量检查 — 比较采购清单总金额与合同金额
     */
    public Map<String, Object> checkOverquantity(Integer contractId) {
        BizContract contract = contractMapper.selectById(contractId);
        if (contract == null) throw new BusinessException("合同不存在");

        // 查询关联采购清单的总金额
        BigDecimal purchaseTotal = BigDecimal.ZERO;
        if (contract.getPurchaseListId() != null) {
            BizPurchaseList purchaseList = purchaseListMapper.selectById(contract.getPurchaseListId());
            if (purchaseList != null && purchaseList.getTotalAmount() != null) {
                purchaseTotal = purchaseList.getTotalAmount();
            }
        }

        BigDecimal contractAmount = contract.getAmountWithTax() != null
                ? contract.getAmountWithTax() : BigDecimal.ZERO;
        boolean overquantity = purchaseTotal.compareTo(contractAmount) > 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractId", contractId);
        result.put("contractAmount", contractAmount);
        result.put("purchaseTotal", purchaseTotal);
        result.put("overquantity", overquantity);
        if (overquantity) {
            result.put("excessAmount", purchaseTotal.subtract(contractAmount));
        }
        return result;
    }

    // ===================== P6: 合同终止前置校验 =====================

    /**
     * P6 §4.6: 终止合同 — 增加前置校验（20004待付款/20005待入库）
     */
    @Transactional
    public void terminateContractWithCheck(Integer contractId, String reason, Integer userId) {
        BizContract contract = contractMapper.selectById(contractId);
        if (contract == null) throw new BusinessException("合同不存在");
        if (!"approved".equals(contract.getStatus()) && !"executing".equals(contract.getStatus())) {
            throw new BusinessException("仅已审批或执行中的合同可以终止");
        }

        // 校验1: 无pending付款申请 (20004)
        long pendingPayments = paymentApplyMapper.selectCount(
                new LambdaQueryWrapper<BizPaymentApply>()
                        .eq(BizPaymentApply::getContractId, contractId)
                        .eq(BizPaymentApply::getStatus, "pending")
                        .eq(BizPaymentApply::getDeleted, 0));
        if (pendingPayments > 0) {
            throw new BusinessException(ErrorCode.PENDING_PAYMENT_EXISTS.getCode(),
                    ErrorCode.PENDING_PAYMENT_EXISTS.getMessage());
        }

        // 校验2: 无未完成入库单 (20005)
        long pendingInbounds = inboundOrderMapper.selectCount(
                new LambdaQueryWrapper<BizInboundOrder>()
                        .eq(BizInboundOrder::getContractId, contractId)
                        .ne(BizInboundOrder::getStatus, "completed")
                        .ne(BizInboundOrder::getStatus, "cancelled")
                        .eq(BizInboundOrder::getDeleted, 0));
        if (pendingInbounds > 0) {
            throw new BusinessException(ErrorCode.PENDING_INBOUND_EXISTS.getCode(),
                    ErrorCode.PENDING_INBOUND_EXISTS.getMessage());
        }

        // 生成版本快照
        contractVersionService.createSnapshot(contract, "terminate", "合同终止", userId);

        // 终止
        contract.setStatus("terminated");
        contract.setTerminateReason(reason);
        contract.setTerminateTime(LocalDateTime.now());
        contract.setTerminatorId(userId);
        contractMapper.updateById(contract);
    }

    // ===================== P6: 支出合同超量/超价校验集成 =====================

    /**
     * P6 §4.6: 创建支出合同 — 集成超量/超价校验
     * 根据校验结果决定审批流程
     */
    @Transactional
    public void createExpenseContract(ContractDTO dto, List<ContractMaterialDTO> materials,
                                       Integer initiatorId) {
        // 超量校验
        boolean overQty = contractCheckService.checkOverQuantity(
                dto.getProjectId(), materials);
        // 超价校验
        boolean overPrice = contractCheckService.checkOverPrice(materials);

        // 创建合同
        BizContract entity = new BizContract();
        BeanUtils.copyProperties(dto, entity, "fieldValues");
        entity.setContractNo(noGeneratorService.generate("EC", 2)); // 支出合同 EC+日期+2位
        entity.setCreatorId(initiatorId);
        entity.setStatus("pending");
        contractMapper.insert(entity);

        // 根据校验结果决定审批流程
        if (overQty) {
            // 动态插入预算员审批节点（20002）
            Map<String, Object> context = new HashMap<>();
            context.put("overQuantity", true);
            approvalService.submitForApproval("expense_contract",
                    entity.getId(), initiatorId, context);
        } else if (overPrice) {
            // 触发总经理审批（20003）
            Map<String, Object> context = new HashMap<>();
            context.put("overPrice", true);
            approvalService.submitForApproval("expense_contract",
                    entity.getId(), initiatorId, context);
        } else {
            approvalService.submitForApproval("expense_contract",
                    entity.getId(), initiatorId);
        }
    }

    // ===================== 字段值校验 =====================

    private void validateFieldValues(Map<String, String> fieldValues, List<SysContractTplField> fieldDefs) {
        if (fieldDefs.isEmpty()) return;

        Set<String> definedKeys = fieldDefs.stream()
                .map(SysContractTplField::getFieldKey).collect(Collectors.toSet());

        // 白名单校验：拒绝未定义字段
        if (fieldValues != null) {
            for (String key : fieldValues.keySet()) {
                if (!definedKeys.contains(key)) {
                    throw new BusinessException("包含未定义的字段: " + key);
                }
            }
        }

        // 必填校验 + 格式校验
        for (SysContractTplField def : fieldDefs) {
            String value = fieldValues != null ? fieldValues.get(def.getFieldKey()) : null;

            if (def.getRequired() != null && def.getRequired() == 1) {
                if (value == null || value.isBlank()) {
                    throw new BusinessException("必填字段未填写: " + def.getFieldName());
                }
            }

            if (value != null && !value.isBlank()) {
                // 长度校验
                if (def.getMaxLength() != null && value.length() > def.getMaxLength()) {
                    throw new BusinessException(def.getFieldName() + "超出最大长度" + def.getMaxLength());
                }
                // 正则校验
                if (def.getValidationRule() != null && !def.getValidationRule().isBlank()) {
                    if (!Pattern.matches(def.getValidationRule(), value)) {
                        throw new BusinessException(def.getFieldName() + "格式不正确");
                    }
                }
                // 类型校验
                if ("number".equals(def.getFieldType())) {
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        throw new BusinessException(def.getFieldName() + "必须为数字");
                    }
                }
                // select 选项校验
                if ("select".equals(def.getFieldType()) && def.getOptionsJson() != null) {
                    if (!def.getOptionsJson().contains("\"" + value + "\"")) {
                        throw new BusinessException(def.getFieldName() + "的值不在可选范围内");
                    }
                }
            }
        }
    }
}
