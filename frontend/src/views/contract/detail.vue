<template>
  <div class="page-container" v-loading="loading">
    <div class="detail-header">
      <el-button link @click="$router.push('/contracts')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <h2>{{ contract.contract_name }}</h2>
      <div class="header-actions">
        <status-tag :status="contract.status" />
        <el-button v-if="contract.status==='approved'" v-permission="'contract:terminate'" type="danger" size="small" @click="handleTerminate">终止合同</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="合同编号"><numbering-display :value="contract.contract_no" /></el-descriptions-item>
          <el-descriptions-item label="合同名称">{{ contract.contract_name }}</el-descriptions-item>
          <el-descriptions-item label="合同类型">{{ typeLabel }}</el-descriptions-item>
          <el-descriptions-item label="关联项目">{{ contract.project_name }}</el-descriptions-item>
          <el-descriptions-item label="签约日期">{{ contract.sign_date }}</el-descriptions-item>
          <el-descriptions-item label="状态"><status-tag :status="contract.status" /></el-descriptions-item>
          <el-descriptions-item label="合同金额"><money-text :value="contract.amount" /></el-descriptions-item>
          <el-descriptions-item label="已付/已收"><money-text :value="contract.settled_amount" /></el-descriptions-item>
          <el-descriptions-item v-if="contract.contract_type==='expense'" label="供应商">{{ contract.supplier_name }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ contract.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 超量预警 -->
        <el-alert v-if="overquantityWarning" type="warning" :closable="false" style="margin-top:12px">
          {{ overquantityWarning }}
        </el-alert>
      </el-tab-pane>

      <el-tab-pane label="关联付款" name="payments">
        <el-table :data="payments" border>
          <el-table-column prop="payment_no" label="付款单号" width="160" />
          <el-table-column prop="amount" label="金额" width="130" align="right">
            <template #default="{ row }"><money-text :value="row.amount" /></template>
          </el-table-column>
          <el-table-column prop="payment_date" label="日期" width="120" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }"><status-tag :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="关联发票" name="invoices">
        <el-table :data="invoices" border>
          <el-table-column prop="invoice_no" label="发票号" width="180" />
          <el-table-column prop="amount" label="金额" width="130" align="right">
            <template #default="{ row }"><money-text :value="row.amount" /></template>
          </el-table-column>
          <el-table-column prop="invoice_date" label="开票日期" width="120" />
          <el-table-column prop="invoice_type" label="类型" width="120" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }"><status-tag :status="row.status" /></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="补充协议" name="supplements">
        <div style="margin-bottom:12px">
          <el-button type="primary" size="small" v-permission="'contract:create'" @click="supplementVisible = true"><el-icon><Plus /></el-icon>新增补充协议</el-button>
        </div>
        <el-table :data="supplements" border>
          <el-table-column prop="supplement_no" label="协议编号" width="160" />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="amount_change" label="金额变更" width="130" align="right">
            <template #default="{ row }"><money-text :value="row.amount_change" /></template>
          </el-table-column>
          <el-table-column prop="created_at" label="创建时间" width="170" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }"><status-tag :status="row.status" /></template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="supplementVisible" title="新增补充协议" width="600px" destroy-on-close>
          <el-form ref="supFormRef" :model="supForm" :rules="supRules" label-width="100px">
            <el-form-item label="标题" prop="title"><el-input v-model="supForm.title" /></el-form-item>
            <el-form-item label="金额变更" prop="amount_change"><money-input v-model="supForm.amount_change" /></el-form-item>
            <el-form-item label="说明"><el-input v-model="supForm.description" type="textarea" :rows="3" /></el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="supplementVisible = false">取消</el-button>
            <el-button type="primary" :loading="supSubmitting" @click="submitSupplement">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getContractById, terminateContract, getSupplements, createSupplement, checkOverquantity, getContractPayments, getContractInvoices } from '@/api/contract'
import { CONTRACT_TYPES } from '@/utils/dict'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'

const route = useRoute()
const contractId = route.params.id
const loading = ref(false)
const activeTab = ref('info')
const contract = ref({})
const payments = ref([])
const invoices = ref([])
const supplements = ref([])
const overquantityWarning = ref('')

const typeLabel = computed(() => CONTRACT_TYPES[contract.value.contract_type] || contract.value.contract_type)

// 补充协议表单
const supplementVisible = ref(false)
const supFormRef = ref(null)
const supSubmitting = ref(false)
const supForm = reactive({ title: '', amount_change: '', description: '' })
const supRules = { title: [{ required: true, message: '必填' }] }

async function fetchContract() {
  loading.value = true
  try {
    const res = await getContractById(contractId)
    contract.value = res.data || {}
  } finally {
    loading.value = false
  }
}

async function fetchPayments() {
  try {
    const res = await getContractPayments(contractId)
    payments.value = res.data?.records || res.data || []
  } catch { /* endpoint may not exist */ }
}

async function fetchInvoices() {
  try {
    const res = await getContractInvoices(contractId)
    invoices.value = res.data?.records || res.data || []
  } catch { /* endpoint may not exist */ }
}

async function fetchSupplements() {
  try {
    const res = await getSupplements(contractId)
    supplements.value = res.data || []
  } catch { /* endpoint may not exist */ }
}

async function fetchOverquantity() {
  try {
    const res = await checkOverquantity(contractId)
    if (res.data?.warning) overquantityWarning.value = res.data.warning
  } catch { /* endpoint may not exist */ }
}

async function handleTerminate() {
  const { value } = await ElMessageBox.prompt('请输入终止原因', '终止合同', { inputType: 'textarea' })
  await terminateContract(contractId, { reason: value })
  ElMessage.success('合同已终止')
  fetchContract()
}

async function submitSupplement() {
  await supFormRef.value.validate()
  supSubmitting.value = true
  try {
    await createSupplement(contractId, supForm)
    ElMessage.success('创建成功')
    supplementVisible.value = false
    fetchSupplements()
  } finally {
    supSubmitting.value = false
  }
}

onMounted(() => {
  fetchContract()
  fetchPayments()
  fetchInvoices()
  fetchSupplements()
  fetchOverquantity()
})
</script>

<style scoped lang="scss">
.page-container { background: #fff; border-radius: 4px; padding: 20px; }
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  h2 { margin: 0; flex: 1; }
}
.header-actions { display: flex; align-items: center; gap: 8px; }
</style>
