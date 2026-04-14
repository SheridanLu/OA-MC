<template>
  <div class="contract-page">
    <!-- 收入/支出 Tab -->
    <el-tabs v-model="contractTab" type="card" @tab-change="handleTabChange" style="margin-bottom:12px">
      <el-tab-pane label="全部合同" name="all" />
      <el-tab-pane label="收入合同" name="income" />
      <el-tab-pane label="支出合同" name="expense" />
    </el-tabs>

    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="合同名称">
          <el-input v-model="queryForm.contractName" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="queryForm.contractType" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="t in contractTypes" :key="t.code" :label="t.label" :value="t.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="pending" />
            <el-option label="已审批" value="approved" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="已终止" value="terminated" />
            <el-option label="已关闭" value="closed" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleAdd" v-permission="'contract:create'">新建合同</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="contract_no" label="合同编号" width="140" />
        <el-table-column prop="contract_name" label="合同名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contract_type" label="类型" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.contract_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount_with_tax" label="含税金额" width="130" align="right">
          <template #default="{ row }">{{ row.amount_with_tax ? Number(row.amount_with_tax).toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column prop="party_a" label="甲方" width="130" show-overflow-tooltip />
        <el-table-column prop="party_b" label="乙方" width="130" show-overflow-tooltip />
        <el-table-column prop="sign_date" label="签订日期" width="110" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/contracts/${row.id}`)">详情</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)"
              :disabled="row.status === 'pending' || row.status === 'approved'" v-permission="'contract:edit'">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)"
              v-if="row.status !== 'pending' && row.status !== 'approved'" v-permission="'contract:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top: 16px; justify-content: flex-end" background
        layout="total, sizes, prev, pager, next, jumper" :total="total" :page-sizes="[10, 20, 50]"
        v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
        @size-change="fetchData" @current-change="fetchData" />
    </el-card>

    <!-- 新建/编辑合同 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑合同' : '新建合同'" width="900px" @closed="resetForm" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="form.contractType" style="width: 100%" @change="onTypeChange" :disabled="isEdit">
                <el-option v-for="t in contractTypes" :key="t.code" :label="t.label" :value="t.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <el-select v-model="form.projectId" filterable placeholder="选择项目" style="width: 100%">
                <el-option v-for="p in projects" :key="p.id" :label="p.project_name || p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="含税金额" prop="amountWithTax">
              <el-input-number v-model="form.amountWithTax" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不含税金额" prop="amountWithoutTax">
              <el-input-number v-model="form.amountWithoutTax" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="税率(%)" prop="taxRate">
              <el-input-number v-model="form.taxRate" :precision="2" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="签订日期">
              <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="甲方"><el-input v-model="form.partyA" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="乙方"><el-input v-model="form.partyB" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>

        <!-- 模板字段填写区 -->
        <el-divider v-if="tplFields.length > 0">模板字段填写</el-divider>
        <div v-if="tplLoading" style="text-align: center; padding: 20px">
          <el-icon class="is-loading"><Loading /></el-icon> 加载模板字段...
        </div>
        <div v-if="tplError" style="padding: 10px">
          <el-alert :title="tplError" type="warning" show-icon :closable="false" />
        </div>
        <template v-for="field in tplFields" :key="field.field_key">
          <el-form-item :label="field.field_name" :required="field.required === 1">
            <!-- text -->
            <el-input v-if="field.field_type === 'text'" v-model="form.fieldValues[field.field_key]"
              :placeholder="field.placeholder || ''" :maxlength="field.max_length || undefined" />
            <!-- number -->
            <el-input-number v-else-if="field.field_type === 'number'" v-model="form.fieldValues[field.field_key]"
              controls-position="right" style="width: 100%" />
            <!-- date -->
            <el-date-picker v-else-if="field.field_type === 'date'" v-model="form.fieldValues[field.field_key]"
              type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            <!-- select -->
            <el-select v-else-if="field.field_type === 'select'" v-model="form.fieldValues[field.field_key]" style="width: 100%">
              <el-option v-for="opt in parseOptions(field.options_json)" :key="opt" :label="opt" :value="opt" />
            </el-select>
            <!-- textarea -->
            <el-input v-else-if="field.field_type === 'textarea'" v-model="form.fieldValues[field.field_key]"
              type="textarea" :rows="3" :placeholder="field.placeholder || ''" :maxlength="field.max_length || undefined" />
            <!-- fallback text -->
            <el-input v-else v-model="form.fieldValues[field.field_key]" :placeholder="field.placeholder || ''" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '提交审批' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getContractList, createContract, updateContract, deleteContract } from '@/api/contract'
import { getAllProjects } from '@/api/project'
import { getContractTypes, getActiveTplVersion, getVersionFields } from '@/api/contractTpl'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)
const projects = ref([])
const contractTypes = ref([])
const contractTab = ref('all')

const handleTabChange = (tab) => {
  if (tab === 'all') queryForm.contractType = ''
  else queryForm.contractType = tab
  queryForm.page = 1
  fetchData()
}

const tplFields = ref([])
const tplLoading = ref(false)
const tplError = ref('')

const statusMap = {
  draft: { text: '草稿', type: 'info' },
  pending: { text: '待审批', type: 'warning' },
  approved: { text: '已审批', type: 'success' },
  rejected: { text: '已驳回', type: 'danger' },
  terminated: { text: '已终止', type: 'info' },
  closed: { text: '已关闭', type: 'info' },
  completed: { text: '已完成', type: 'success' }
}

const queryForm = reactive({ contractName: '', contractType: '', status: '', page: 1, size: 20 })

const form = reactive({
  contractName: '', contractType: null, projectId: null, supplierId: null,
  amountWithTax: null, amountWithoutTax: null, taxRate: null,
  signDate: null, startDate: null, endDate: null, partyA: '', partyB: '', remark: '',
  fieldValues: {}
})

const rules = {
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  amountWithTax: [{ required: true, message: '请输入含税金额', trigger: 'blur' }],
  amountWithoutTax: [{ required: true, message: '请输入不含税金额', trigger: 'blur' }],
  taxRate: [{ required: true, message: '请输入税率', trigger: 'blur' }]
}

const typeLabel = (code) => {
  const t = contractTypes.value.find(i => i.code === code)
  return t ? t.label : code
}

const parseOptions = (json) => {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getContractList(queryForm)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const loadProjects = async () => {
  try { const res = await getAllProjects(); projects.value = res.data || [] } catch { /* ignore */ }
}

/**
 * 合同类型变更时自动加载模板字段
 */
const onTypeChange = async (type) => {
  tplFields.value = []
  tplError.value = ''
  form.fieldValues = {}
  if (!type) return

  tplLoading.value = true
  try {
    const res = await getActiveTplVersion(type)
    if (res.code === 200 && res.data) {
      const fieldRes = await getVersionFields(res.data.id)
      tplFields.value = fieldRes.data || []
      // 用默认值初始化
      for (const f of tplFields.value) {
        form.fieldValues[f.field_key] = f.default_value || ''
      }
    } else {
      tplError.value = '该合同类型尚未配置模板，请联系管理员'
    }
  } catch (e) {
    tplError.value = e?.response?.data?.message || '该合同类型尚未配置模板，请联系管理员'
  } finally { tplLoading.value = false }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => {
  queryForm.contractName = ''; queryForm.contractType = ''; queryForm.status = ''; queryForm.page = 1; fetchData()
}
const handleAdd = () => {
  isEdit.value = false; editId.value = null; tplFields.value = []; tplError.value = ''
  loadProjects(); dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; editId.value = row.id; loadProjects()
  Object.assign(form, {
    contractName: row.contract_name, contractType: row.contract_type,
    projectId: row.project_id, supplierId: row.supplier_id,
    amountWithTax: row.amount_with_tax, amountWithoutTax: row.amount_without_tax,
    taxRate: row.tax_rate, signDate: row.sign_date || null, startDate: row.start_date || null,
    endDate: row.end_date || null, partyA: row.party_a || '', partyB: row.party_b || '',
    remark: row.remark || '', fieldValues: {}
  })
  // 编辑时加载字段
  if (row.contract_type) onTypeChange(row.contract_type)
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    contractName: '', contractType: null, projectId: null, supplierId: null,
    amountWithTax: null, amountWithoutTax: null, taxRate: null,
    signDate: null, startDate: null, endDate: null, partyA: '', partyB: '', remark: '',
    fieldValues: {}
  })
  tplFields.value = []; tplError.value = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()

  // 校验模板必填字段
  for (const f of tplFields.value) {
    if (f.required === 1) {
      const val = form.fieldValues[f.field_key]
      if (!val && val !== 0) {
        ElMessage.warning(`请填写模板字段: ${f.field_name}`)
        return
      }
    }
  }

  // 将 fieldValues 中的值统一转为字符串（el-input-number 产出的是数字）
  const payload = { ...form }
  if (form.fieldValues && Object.keys(form.fieldValues).length > 0) {
    const strValues = {}
    for (const [key, val] of Object.entries(form.fieldValues)) {
      strValues[key] = val !== null && val !== undefined ? String(val) : ''
    }
    payload.fieldValues = strValues
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateContract(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createContract(payload)
      ElMessage.success('已提交')
    }
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除合同"${row.contract_name}"？`, '提示', { type: 'warning' })
  await deleteContract(row.id); ElMessage.success('删除成功'); fetchData()
}

onMounted(() => {
  fetchData()
  getContractTypes().then(res => { contractTypes.value = res.data || [] })
})
</script>

<style scoped>.search-card { margin-bottom: 0; }</style>
