package com.mochu.business.controller;

import com.mochu.business.dto.*;
import com.mochu.business.entity.*;
import com.mochu.business.service.HrService;
import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 人力资源管理接口
 */
@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    // ======================= 薪资 =======================

    @GetMapping("/salaries")
    @PreAuthorize("hasAuthority('hr:salary-adjust')")
    public R<PageResult<BizSalary>> listSalaries(SalaryDTO query) {
        return R.ok(hrService.listSalaries(query));
    }

    @GetMapping("/salaries/{id}")
    @PreAuthorize("hasAuthority('hr:salary-adjust')")
    public R<BizSalary> getSalaryById(@PathVariable Integer id) {
        BizSalary salary = hrService.getSalaryById(id);
        if (salary == null) {
            return R.fail(404, "薪资记录不存在");
        }
        return R.ok(salary);
    }

    @Idempotent
    @PostMapping("/salaries")
    @PreAuthorize("hasAuthority('hr:salary-adjust')")
    public R<Void> createSalary(@Valid @RequestBody SalaryDTO dto) {
        hrService.createSalary(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/salaries/{id}")
    @PreAuthorize("hasAuthority('hr:salary-adjust')")
    public R<Void> updateSalary(@PathVariable Integer id, @Valid @RequestBody SalaryDTO dto) {
        hrService.updateSalary(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/salaries/{id}/status")
    @PreAuthorize("hasAuthority('hr:salary-adjust')")
    public R<Void> updateSalaryStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateSalaryStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/salaries/{id}")
    @PreAuthorize("hasAuthority('hr:salary-adjust')")
    public R<Void> deleteSalary(@PathVariable Integer id) {
        hrService.deleteSalary(id);
        return R.ok();
    }

    // ======================= 劳动合同 =======================

    @GetMapping("/contracts")
    @PreAuthorize("hasAuthority('hr:contract-manage')")
    public R<PageResult<BizHrContract>> listContracts(HrContractDTO query) {
        return R.ok(hrService.listContracts(query));
    }

    @GetMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('hr:contract-manage')")
    public R<BizHrContract> getContractById(@PathVariable Integer id) {
        BizHrContract contract = hrService.getContractById(id);
        if (contract == null) {
            return R.fail(404, "合同不存在");
        }
        return R.ok(contract);
    }

    @Idempotent
    @PostMapping("/contracts")
    @PreAuthorize("hasAuthority('hr:contract-manage')")
    public R<Void> createContract(@Valid @RequestBody HrContractDTO dto) {
        hrService.createContract(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('hr:contract-manage')")
    public R<Void> updateContract(@PathVariable Integer id, @Valid @RequestBody HrContractDTO dto) {
        hrService.updateContract(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/contracts/{id}/status")
    @PreAuthorize("hasAuthority('hr:contract-manage')")
    public R<Void> updateContractStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateContractStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('hr:contract-manage')")
    public R<Void> deleteContract(@PathVariable Integer id) {
        hrService.deleteContract(id);
        return R.ok();
    }

    // ======================= 证书管理 =======================

    @GetMapping("/certificates")
    @PreAuthorize("hasAuthority('hr:certificate-manage')")
    public R<PageResult<BizHrCertificate>> listCertificates(HrCertificateDTO query) {
        return R.ok(hrService.listCertificates(query));
    }

    @GetMapping("/certificates/{id}")
    @PreAuthorize("hasAuthority('hr:certificate-manage')")
    public R<BizHrCertificate> getCertificateById(@PathVariable Integer id) {
        BizHrCertificate certificate = hrService.getCertificateById(id);
        if (certificate == null) {
            return R.fail(404, "证书不存在");
        }
        return R.ok(certificate);
    }

    @Idempotent
    @PostMapping("/certificates")
    @PreAuthorize("hasAuthority('hr:certificate-manage')")
    public R<Void> createCertificate(@Valid @RequestBody HrCertificateDTO dto) {
        hrService.createCertificate(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/certificates/{id}")
    @PreAuthorize("hasAuthority('hr:certificate-manage')")
    public R<Void> updateCertificate(@PathVariable Integer id, @Valid @RequestBody HrCertificateDTO dto) {
        hrService.updateCertificate(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/certificates/{id}/status")
    @PreAuthorize("hasAuthority('hr:certificate-manage')")
    public R<Void> updateCertificateStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateCertificateStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/certificates/{id}")
    @PreAuthorize("hasAuthority('hr:certificate-manage')")
    public R<Void> deleteCertificate(@PathVariable Integer id) {
        hrService.deleteCertificate(id);
        return R.ok();
    }

    // ======================= 入职申请 =======================

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('hr:entry-process')")
    public R<PageResult<BizHrEntry>> listEntries(HrEntryDTO query) {
        return R.ok(hrService.listEntries(query));
    }

    @GetMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('hr:entry-process')")
    public R<BizHrEntry> getEntryById(@PathVariable Integer id) {
        BizHrEntry entry = hrService.getEntryById(id);
        if (entry == null) {
            return R.fail(404, "入职申请不存在");
        }
        return R.ok(entry);
    }

    @Idempotent
    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('hr:entry-process')")
    public R<Void> createEntry(@Valid @RequestBody HrEntryDTO dto) {
        hrService.createEntry(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('hr:entry-process')")
    public R<Void> updateEntry(@PathVariable Integer id, @Valid @RequestBody HrEntryDTO dto) {
        hrService.updateEntry(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/entries/{id}/status")
    @PreAuthorize("hasAuthority('hr:entry-process')")
    public R<Void> updateEntryStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateEntryStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('hr:entry-process')")
    public R<Void> deleteEntry(@PathVariable Integer id) {
        hrService.deleteEntry(id);
        return R.ok();
    }

    // ======================= 离职申请 =======================

    @GetMapping("/resigns")
    @PreAuthorize("hasAuthority('hr:resign-process')")
    public R<PageResult<BizHrResign>> listResigns(HrResignDTO query) {
        return R.ok(hrService.listResigns(query));
    }

    @GetMapping("/resigns/{id}")
    @PreAuthorize("hasAuthority('hr:resign-process')")
    public R<BizHrResign> getResignById(@PathVariable Integer id) {
        BizHrResign resign = hrService.getResignById(id);
        if (resign == null) {
            return R.fail(404, "离职申请不存在");
        }
        return R.ok(resign);
    }

    @Idempotent
    @PostMapping("/resigns")
    @PreAuthorize("hasAuthority('hr:resign-process')")
    public R<Void> createResign(@Valid @RequestBody HrResignDTO dto) {
        hrService.createResign(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/resigns/{id}")
    @PreAuthorize("hasAuthority('hr:resign-process')")
    public R<Void> updateResign(@PathVariable Integer id, @Valid @RequestBody HrResignDTO dto) {
        hrService.updateResign(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/resigns/{id}/status")
    @PreAuthorize("hasAuthority('hr:resign-process')")
    public R<Void> updateResignStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateResignStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/resigns/{id}")
    @PreAuthorize("hasAuthority('hr:resign-process')")
    public R<Void> deleteResign(@PathVariable Integer id) {
        hrService.deleteResign(id);
        return R.ok();
    }

    // ======================= 薪资配置 =======================

    @GetMapping("/salary-config")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<PageResult<BizSalaryConfig>> listSalaryConfigs(SalaryConfigDTO query) {
        return R.ok(hrService.listSalaryConfigs(query));
    }

    @GetMapping("/salary-config/{id}")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<BizSalaryConfig> getSalaryConfigById(@PathVariable Integer id) {
        BizSalaryConfig salaryConfig = hrService.getSalaryConfigById(id);
        if (salaryConfig == null) {
            return R.fail(404, "薪资配置不存在");
        }
        return R.ok(salaryConfig);
    }

    @Idempotent
    @PostMapping("/salary-config")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> createSalaryConfig(@Valid @RequestBody SalaryConfigDTO dto) {
        hrService.createSalaryConfig(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/salary-config/{id}")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> updateSalaryConfig(@PathVariable Integer id, @Valid @RequestBody SalaryConfigDTO dto) {
        hrService.updateSalaryConfig(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/salary-config/{id}/status")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> updateSalaryConfigStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateSalaryConfigStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/salary-config/{id}")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> deleteSalaryConfig(@PathVariable Integer id) {
        hrService.deleteSalaryConfig(id);
        return R.ok();
    }

    // ======================= 社保配置 =======================

    @GetMapping("/social-insurance")
    @PreAuthorize("hasAuthority('hr:social-insurance-config')")
    public R<PageResult<BizSocialInsurance>> listSocialInsurances(SocialInsuranceDTO query) {
        return R.ok(hrService.listSocialInsurances(query));
    }

    @GetMapping("/social-insurance/{id}")
    @PreAuthorize("hasAuthority('hr:social-insurance-config')")
    public R<BizSocialInsurance> getSocialInsuranceById(@PathVariable Integer id) {
        BizSocialInsurance socialInsurance = hrService.getSocialInsuranceById(id);
        if (socialInsurance == null) {
            return R.fail(404, "社保配置不存在");
        }
        return R.ok(socialInsurance);
    }

    @Idempotent
    @PostMapping("/social-insurance")
    @PreAuthorize("hasAuthority('hr:social-insurance-config')")
    public R<Void> createSocialInsurance(@Valid @RequestBody SocialInsuranceDTO dto) {
        hrService.createSocialInsurance(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/social-insurance/{id}")
    @PreAuthorize("hasAuthority('hr:social-insurance-config')")
    public R<Void> updateSocialInsurance(@PathVariable Integer id, @Valid @RequestBody SocialInsuranceDTO dto) {
        hrService.updateSocialInsurance(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/social-insurance/{id}/status")
    @PreAuthorize("hasAuthority('hr:social-insurance-config')")
    public R<Void> updateSocialInsuranceStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateSocialInsuranceStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/social-insurance/{id}")
    @PreAuthorize("hasAuthority('hr:social-insurance-config')")
    public R<Void> deleteSocialInsurance(@PathVariable Integer id) {
        hrService.deleteSocialInsurance(id);
        return R.ok();
    }

    // ======================= 个税税率 =======================

    @GetMapping("/tax-rate")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<PageResult<BizTaxRate>> listTaxRates(TaxRateDTO query) {
        return R.ok(hrService.listTaxRates(query));
    }

    @GetMapping("/tax-rate/{id}")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<BizTaxRate> getTaxRateById(@PathVariable Integer id) {
        BizTaxRate taxRate = hrService.getTaxRateById(id);
        if (taxRate == null) {
            return R.fail(404, "税率记录不存在");
        }
        return R.ok(taxRate);
    }

    @Idempotent
    @PostMapping("/tax-rate")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> createTaxRate(@Valid @RequestBody TaxRateDTO dto) {
        hrService.createTaxRate(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/tax-rate/{id}")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> updateTaxRate(@PathVariable Integer id, @Valid @RequestBody TaxRateDTO dto) {
        hrService.updateTaxRate(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/tax-rate/{id}")
    @PreAuthorize("hasAuthority('hr:salary-config')")
    public R<Void> deleteTaxRate(@PathVariable Integer id) {
        hrService.deleteTaxRate(id);
        return R.ok();
    }

    // ======================= 资产交接 =======================

    @GetMapping("/asset-transfer")
    @PreAuthorize("hasAuthority('hr:asset-transfer')")
    public R<PageResult<BizAssetTransfer>> listAssetTransfers(AssetTransferDTO query) {
        return R.ok(hrService.listAssetTransfers(query));
    }

    @GetMapping("/asset-transfer/{id}")
    @PreAuthorize("hasAuthority('hr:asset-transfer')")
    public R<BizAssetTransfer> getAssetTransferById(@PathVariable Integer id) {
        BizAssetTransfer assetTransfer = hrService.getAssetTransferById(id);
        if (assetTransfer == null) {
            return R.fail(404, "资产交接记录不存在");
        }
        return R.ok(assetTransfer);
    }

    @Idempotent
    @PostMapping("/asset-transfer")
    @PreAuthorize("hasAuthority('hr:asset-transfer')")
    public R<Void> createAssetTransfer(@Valid @RequestBody AssetTransferDTO dto) {
        hrService.createAssetTransfer(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/asset-transfer/{id}")
    @PreAuthorize("hasAuthority('hr:asset-transfer')")
    public R<Void> updateAssetTransfer(@PathVariable Integer id, @Valid @RequestBody AssetTransferDTO dto) {
        hrService.updateAssetTransfer(id, dto);
        return R.ok();
    }

    @Idempotent
    @PatchMapping("/asset-transfer/{id}/status")
    @PreAuthorize("hasAuthority('hr:asset-transfer')")
    public R<Void> updateAssetTransferStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateDTO dto) {
        hrService.updateAssetTransferStatus(id, dto.getStatus());
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/asset-transfer/{id}")
    @PreAuthorize("hasAuthority('hr:asset-transfer')")
    public R<Void> deleteAssetTransfer(@PathVariable Integer id) {
        hrService.deleteAssetTransfer(id);
        return R.ok();
    }
}
