<template>
  <div class="hr-page">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="薪资管理" name="salary"><salary-tab /></el-tab-pane>
        <el-tab-pane label="劳动合同" name="hrContract"><hr-contract-tab /></el-tab-pane>
        <el-tab-pane label="证书管理" name="certificate"><certificate-tab /></el-tab-pane>
        <el-tab-pane label="入职管理" name="entry"><entry-tab /></el-tab-pane>
        <el-tab-pane label="离职管理" name="resign"><resign-tab /></el-tab-pane>
        <el-tab-pane label="薪资配置" name="salaryConfig"><salary-config-tab /></el-tab-pane>
        <el-tab-pane label="社保配置" name="socialInsurance"><social-insurance-tab /></el-tab-pane>
        <el-tab-pane label="个税税率" name="taxRate"><tax-rate-tab /></el-tab-pane>
        <el-tab-pane label="资产交接" name="assetTransfer"><asset-transfer-tab /></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SalaryTab from './components/SalaryTab.vue'
import HrContractTab from './components/HrContractTab.vue'
import CertificateTab from './components/CertificateTab.vue'
import EntryTab from './components/EntryTab.vue'
import ResignTab from './components/ResignTab.vue'
import SalaryConfigTab from './components/SalaryConfigTab.vue'
import SocialInsuranceTab from './components/SocialInsuranceTab.vue'
import TaxRateTab from './components/TaxRateTab.vue'
import AssetTransferTab from './components/AssetTransferTab.vue'

const activeTab = ref('salary')
</script>
