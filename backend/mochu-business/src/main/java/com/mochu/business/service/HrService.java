package com.mochu.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.business.dto.*;
import com.mochu.business.entity.*;
import com.mochu.business.mapper.*;
import com.mochu.common.constant.Constants;
import com.mochu.common.enums.ErrorCode;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * 人力资源管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrService {

    private final BizSalaryMapper salaryMapper;
    private final BizHrContractMapper contractMapper;
    private final BizHrCertificateMapper certificateMapper;
    private final BizHrEntryMapper entryMapper;
    private final BizHrResignMapper resignMapper;
    private final BizSalaryConfigMapper salaryConfigMapper;
    private final BizSocialInsuranceMapper socialInsuranceMapper;
    private final BizSocialInsuranceConfigMapper socialInsuranceConfigMapper;
    private final BizTaxRateMapper taxRateMapper;
    private final BizAssetTransferMapper assetTransferMapper;
    private final SysUserMapper sysUserMapper;
    private final NoGeneratorService noGeneratorService;

    // ======================= 薪资 =======================

    public PageResult<BizSalary> listSalaries(SalaryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizSalary> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizSalary> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(BizSalary::getUserId, query.getUserId());
        }
        if (query.getSalaryMonth() != null && !query.getSalaryMonth().isBlank()) {
            wrapper.eq(BizSalary::getSalaryMonth, query.getSalaryMonth());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizSalary::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizSalary::getCreatedAt);

        salaryMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizSalary getSalaryById(Integer id) {
        return salaryMapper.selectById(id);
    }

    public void createSalary(SalaryDTO dto) {
        BizSalary entity = new BizSalary();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setStatus("draft");
        salaryMapper.insert(entity);
    }

    public void updateSalary(Integer id, SalaryDTO dto) {
        BizSalary entity = salaryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("薪资记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        salaryMapper.updateById(entity);
    }

    public void updateSalaryStatus(Integer id, String status) {
        BizSalary entity = salaryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("薪资记录不存在");
        }
        entity.setStatus(status);
        salaryMapper.updateById(entity);
    }

    public void deleteSalary(Integer id) {
        salaryMapper.deleteById(id);
    }

    // ======================= 劳动合同 =======================

    public PageResult<BizHrContract> listContracts(HrContractDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizHrContract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizHrContract> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(BizHrContract::getUserId, query.getUserId());
        }
        if (query.getContractType() != null && !query.getContractType().isBlank()) {
            wrapper.eq(BizHrContract::getContractType, query.getContractType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizHrContract::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizHrContract::getCreatedAt);

        contractMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizHrContract getContractById(Integer id) {
        return contractMapper.selectById(id);
    }

    public void createContract(HrContractDTO dto) {
        BizHrContract entity = new BizHrContract();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setStatus("active");
        contractMapper.insert(entity);
    }

    public void updateContract(Integer id, HrContractDTO dto) {
        BizHrContract entity = contractMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("合同不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        contractMapper.updateById(entity);
    }

    public void updateContractStatus(Integer id, String status) {
        BizHrContract entity = contractMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("合同不存在");
        }
        entity.setStatus(status);
        contractMapper.updateById(entity);
    }

    public void deleteContract(Integer id) {
        contractMapper.deleteById(id);
    }

    // ======================= 证书管理 =======================

    public PageResult<BizHrCertificate> listCertificates(HrCertificateDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizHrCertificate> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizHrCertificate> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(BizHrCertificate::getUserId, query.getUserId());
        }
        if (query.getCertType() != null && !query.getCertType().isBlank()) {
            wrapper.eq(BizHrCertificate::getCertType, query.getCertType());
        }
        if (query.getCertName() != null && !query.getCertName().isBlank()) {
            wrapper.like(BizHrCertificate::getCertName, query.getCertName());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizHrCertificate::getStatus, query.getStatus());
        }
        if (query.getWarnStatus() != null && !query.getWarnStatus().isBlank()) {
            wrapper.eq(BizHrCertificate::getWarnStatus, query.getWarnStatus());
        }
        wrapper.orderByDesc(BizHrCertificate::getCreatedAt);

        certificateMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizHrCertificate getCertificateById(Integer id) {
        return certificateMapper.selectById(id);
    }

    public void createCertificate(HrCertificateDTO dto) {
        BizHrCertificate entity = new BizHrCertificate();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setStatus("active");
        entity.setWarnStatus("normal");
        certificateMapper.insert(entity);
    }

    public void updateCertificate(Integer id, HrCertificateDTO dto) {
        BizHrCertificate entity = certificateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("证书不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        certificateMapper.updateById(entity);
    }

    public void updateCertificateStatus(Integer id, String status) {
        BizHrCertificate entity = certificateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("证书不存在");
        }
        entity.setStatus(status);
        certificateMapper.updateById(entity);
    }

    public void deleteCertificate(Integer id) {
        certificateMapper.deleteById(id);
    }

    // ======================= 入职申请 =======================

    public PageResult<BizHrEntry> listEntries(HrEntryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizHrEntry> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizHrEntry> wrapper = new LambdaQueryWrapper<>();

        if (query.getApplicantName() != null && !query.getApplicantName().isBlank()) {
            wrapper.like(BizHrEntry::getApplicantName, query.getApplicantName());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(BizHrEntry::getDeptId, query.getDeptId());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizHrEntry::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizHrEntry::getCreatedAt);

        entryMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizHrEntry getEntryById(Integer id) {
        return entryMapper.selectById(id);
    }

    public void createEntry(HrEntryDTO dto) {
        BizHrEntry entity = new BizHrEntry();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setEntryNo(noGeneratorService.generate("EN"));
        entity.setStatus("draft");
        entryMapper.insert(entity);
    }

    public void updateEntry(Integer id, HrEntryDTO dto) {
        BizHrEntry entity = entryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("入职申请不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size", "entryNo");
        entryMapper.updateById(entity);
    }

    public void updateEntryStatus(Integer id, String status) {
        BizHrEntry entity = entryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("入职申请不存在");
        }
        entity.setStatus(status);
        entryMapper.updateById(entity);
    }

    public void deleteEntry(Integer id) {
        entryMapper.deleteById(id);
    }

    // ======================= 离职申请 =======================

    public PageResult<BizHrResign> listResigns(HrResignDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizHrResign> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizHrResign> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(BizHrResign::getUserId, query.getUserId());
        }
        if (query.getResignType() != null && !query.getResignType().isBlank()) {
            wrapper.eq(BizHrResign::getResignType, query.getResignType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizHrResign::getStatus, query.getStatus());
        }
        if (query.getHandoverStatus() != null && !query.getHandoverStatus().isBlank()) {
            wrapper.eq(BizHrResign::getHandoverStatus, query.getHandoverStatus());
        }
        wrapper.orderByDesc(BizHrResign::getCreatedAt);

        resignMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizHrResign getResignById(Integer id) {
        return resignMapper.selectById(id);
    }

    public void createResign(HrResignDTO dto) {
        BizHrResign entity = new BizHrResign();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setResignNo(noGeneratorService.generate("RS"));
        entity.setStatus("draft");
        entity.setHandoverStatus("pending");
        resignMapper.insert(entity);
    }

    public void updateResign(Integer id, HrResignDTO dto) {
        BizHrResign entity = resignMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("离职申请不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size", "resignNo");
        resignMapper.updateById(entity);
    }

    public void updateResignStatus(Integer id, String status) {
        BizHrResign entity = resignMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("离职申请不存在");
        }
        entity.setStatus(status);
        resignMapper.updateById(entity);
    }

    public void deleteResign(Integer id) {
        resignMapper.deleteById(id);
    }

    // ======================= 薪资配置 =======================

    public PageResult<BizSalaryConfig> listSalaryConfigs(SalaryConfigDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizSalaryConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizSalaryConfig> wrapper = new LambdaQueryWrapper<>();

        if (query.getGrade() != null && !query.getGrade().isBlank()) {
            wrapper.eq(BizSalaryConfig::getGrade, query.getGrade());
        }
        if (query.getGradeName() != null && !query.getGradeName().isBlank()) {
            wrapper.like(BizSalaryConfig::getGradeName, query.getGradeName());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizSalaryConfig::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizSalaryConfig::getCreatedAt);

        salaryConfigMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizSalaryConfig getSalaryConfigById(Integer id) {
        return salaryConfigMapper.selectById(id);
    }

    public void createSalaryConfig(SalaryConfigDTO dto) {
        BizSalaryConfig entity = new BizSalaryConfig();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setStatus("active");
        salaryConfigMapper.insert(entity);
    }

    public void updateSalaryConfig(Integer id, SalaryConfigDTO dto) {
        BizSalaryConfig entity = salaryConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("薪资配置不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        salaryConfigMapper.updateById(entity);
    }

    public void updateSalaryConfigStatus(Integer id, String status) {
        BizSalaryConfig entity = salaryConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("薪资配置不存在");
        }
        entity.setStatus(status);
        salaryConfigMapper.updateById(entity);
    }

    public void deleteSalaryConfig(Integer id) {
        salaryConfigMapper.deleteById(id);
    }

    // ======================= 社保配置 =======================

    public PageResult<BizSocialInsurance> listSocialInsurances(SocialInsuranceDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizSocialInsurance> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizSocialInsurance> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(BizSocialInsurance::getUserId, query.getUserId());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizSocialInsurance::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizSocialInsurance::getCreatedAt);

        socialInsuranceMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizSocialInsurance getSocialInsuranceById(Integer id) {
        return socialInsuranceMapper.selectById(id);
    }

    public void createSocialInsurance(SocialInsuranceDTO dto) {
        BizSocialInsurance entity = new BizSocialInsurance();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setStatus("active");
        socialInsuranceMapper.insert(entity);
    }

    public void updateSocialInsurance(Integer id, SocialInsuranceDTO dto) {
        BizSocialInsurance entity = socialInsuranceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("社保配置不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        socialInsuranceMapper.updateById(entity);
    }

    public void updateSocialInsuranceStatus(Integer id, String status) {
        BizSocialInsurance entity = socialInsuranceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("社保配置不存在");
        }
        entity.setStatus(status);
        socialInsuranceMapper.updateById(entity);
    }

    public void deleteSocialInsurance(Integer id) {
        socialInsuranceMapper.deleteById(id);
    }

    // ======================= 个税税率 =======================

    public PageResult<BizTaxRate> listTaxRates(TaxRateDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizTaxRate> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizTaxRate> wrapper = new LambdaQueryWrapper<>();

        if (query.getLevel() != null) {
            wrapper.eq(BizTaxRate::getLevel, query.getLevel());
        }
        wrapper.orderByAsc(BizTaxRate::getLevel);

        taxRateMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizTaxRate getTaxRateById(Integer id) {
        return taxRateMapper.selectById(id);
    }

    public void createTaxRate(TaxRateDTO dto) {
        BizTaxRate entity = new BizTaxRate();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        taxRateMapper.insert(entity);
    }

    public void updateTaxRate(Integer id, TaxRateDTO dto) {
        BizTaxRate entity = taxRateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("税率记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        taxRateMapper.updateById(entity);
    }

    public void deleteTaxRate(Integer id) {
        taxRateMapper.deleteById(id);
    }

    // ======================= 资产交接 =======================

    public PageResult<BizAssetTransfer> listAssetTransfers(AssetTransferDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<BizAssetTransfer> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BizAssetTransfer> wrapper = new LambdaQueryWrapper<>();

        if (query.getUserId() != null) {
            wrapper.eq(BizAssetTransfer::getUserId, query.getUserId());
        }
        if (query.getTransferType() != null && !query.getTransferType().isBlank()) {
            wrapper.eq(BizAssetTransfer::getTransferType, query.getTransferType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(BizAssetTransfer::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizAssetTransfer::getCreatedAt);

        assetTransferMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    public BizAssetTransfer getAssetTransferById(Integer id) {
        return assetTransferMapper.selectById(id);
    }

    public void createAssetTransfer(AssetTransferDTO dto) {
        BizAssetTransfer entity = new BizAssetTransfer();
        BeanUtils.copyProperties(dto, entity, "id", "page", "size");
        entity.setTransferNo(noGeneratorService.generate("AT"));
        entity.setStatus("draft");
        assetTransferMapper.insert(entity);
    }

    public void updateAssetTransfer(Integer id, AssetTransferDTO dto) {
        BizAssetTransfer entity = assetTransferMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("资产交接记录不存在");
        }
        BeanUtils.copyProperties(dto, entity, "id", "page", "size", "transferNo");
        assetTransferMapper.updateById(entity);
    }

    public void updateAssetTransferStatus(Integer id, String status) {
        BizAssetTransfer entity = assetTransferMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("资产交接记录不存在");
        }
        entity.setStatus(status);
        assetTransferMapper.updateById(entity);
    }

    public void deleteAssetTransfer(Integer id) {
        assetTransferMapper.deleteById(id);
    }

    // ======================= P6: 薪资配置生效日期校验 =======================

    /**
     * P6 §4.15: 创建薪资配置 — effective_date 不早于当月 + 员工校验
     */
    public void createSalaryConfigWithValidation(SalaryConfigDTO dto, Integer userId) {
        // 校验员工存在且在职 (11001)
        SysUser user = sysUserMapper.selectById(dto.getUserId());
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND.getCode(),
                    ErrorCode.EMPLOYEE_NOT_FOUND.getMessage());
        }

        // effective_date 不早于当月 (11002)
        if (dto.getEffectiveDate() != null) {
            YearMonth configMonth = YearMonth.from(dto.getEffectiveDate());
            YearMonth currentMonth = YearMonth.now();
            if (configMonth.isBefore(currentMonth)) {
                throw new BusinessException(ErrorCode.EFFECTIVE_DATE_INVALID.getCode(),
                        ErrorCode.EFFECTIVE_DATE_INVALID.getMessage());
            }
        }

        // 旧配置 → inactive
        salaryConfigMapper.update(null, new LambdaUpdateWrapper<BizSalaryConfig>()
                .eq(BizSalaryConfig::getUserId, dto.getUserId())
                .eq(BizSalaryConfig::getStatus, "active")
                .set(BizSalaryConfig::getStatus, "inactive"));

        // 新建
        BizSalaryConfig config = new BizSalaryConfig();
        BeanUtils.copyProperties(dto, config, "id", "page", "size");
        config.setStatus("active");
        config.setCreatorId(userId);
        salaryConfigMapper.insert(config);
    }

    // ======================= P6: 社保详细配置计算 =======================

    /**
     * P6 §4.15: 计算社保个人扣款总额
     */
    public BigDecimal calculatePersonalDeduction(BizSocialInsuranceConfig config) {
        BigDecimal pension = config.getPensionBase()
                .multiply(config.getPensionPersonalRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal medical = config.getMedicalBase()
                .multiply(config.getMedicalPersonalRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal unemployment = config.getUnemploymentBase()
                .multiply(config.getUnemploymentPersonalRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal housing = config.getHousingBase()
                .multiply(config.getHousingPersonalRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return pension.add(medical).add(unemployment).add(housing);
    }

    /**
     * P6 §4.15: 计算社保企业承担总额
     */
    public BigDecimal calculateCompanyContribution(BizSocialInsuranceConfig config) {
        BigDecimal pension = config.getPensionBase()
                .multiply(config.getPensionCompanyRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal medical = config.getMedicalBase()
                .multiply(config.getMedicalCompanyRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal unemployment = config.getUnemploymentBase()
                .multiply(config.getUnemploymentCompanyRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal housing = config.getHousingBase()
                .multiply(config.getHousingCompanyRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return pension.add(medical).add(unemployment).add(housing);
    }

    /**
     * P6: 查询用户当前有效社保配置
     */
    public BizSocialInsuranceConfig getActiveSocialInsuranceConfig(Integer userId) {
        return socialInsuranceConfigMapper.selectOne(
                new LambdaQueryWrapper<BizSocialInsuranceConfig>()
                        .eq(BizSocialInsuranceConfig::getUserId, userId)
                        .eq(BizSocialInsuranceConfig::getStatus, "active")
                        .orderByDesc(BizSocialInsuranceConfig::getEffectiveDate)
                        .last("LIMIT 1"));
    }
}
