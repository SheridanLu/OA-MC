<template>
  <div>
    <div class="toolbar">
      <el-button v-permission="'progress:change-manage'" type="primary" @click="handleAdd">新建变更单</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="change_no" label="变更编号" width="140" />
      <el-table-column label="变更类型" width="110">
        <template #default="{ row }">{{ changeTypeLabel[row.change_type] || row.change_type }}</template>
      </el-table-column>
      <el-table-column prop="title" label="变更标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="变更金额" width="130" align="right">
        <template #default="{ row }">
          <money-text v-if="row.total_amount" :value="row.total_amount" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-permission="'progress:change-manage'" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-dropdown v-permission="'progress:change-manage'" @command="(cmd) => handleStatusUpdate(row, cmd)" style="margin-left: 4px">
            <el-button type="warning" link size="small">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pending">提交审批</el-dropdown-item>
                <el-dropdown-item command="approved">审批通过</el-dropdown-item>
                <el-dropdown-item command="rejected">驳回</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-permission="'progress:change-manage'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next"
        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <!-- 变更单对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑变更单' : '新建变更单'" width="850px" @closed="resetForm">
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId"><project-select v-model="changeForm.projectId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="变更类型" prop="changeType">
              <el-select v-model="changeForm.changeType" style="width: 100%">
                <el-option v-for="opt in changeTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联合同">
              <el-input-number v-model="changeForm.contractId" :min="1" controls-position="right" placeholder="合同ID" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="变更标题" prop="title">
          <el-input v-model="changeForm.title" />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="changeForm.description" type="textarea" :rows="2" />
        </el-form-item>

        <el-divider content-position="left">变更明细</el-divider>
        <el-button type="primary" size="small" style="margin-bottom: 12px" @click="addDetail">添加明细行</el-button>
        <el-table :data="changeForm.details" border size="small">
          <el-table-column label="项目名称" min-width="120">
            <template #default="{ row }"><el-input v-model="row.itemName" size="small" /></template>
          </el-table-column>
          <el-table-column label="规格型号" width="100">
            <template #default="{ row }"><el-input v-model="row.specModel" size="small" /></template>
          </el-table-column>
          <el-table-column label="单位" width="70">
            <template #default="{ row }"><el-input v-model="row.unit" size="small" /></template>
          </el-table-column>
          <el-table-column label="计划量" width="90">
            <template #default="{ row }"><el-input-number v-model="row.planQuantity" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" /></template>
          </el-table-column>
          <el-table-column label="实际量" width="90">
            <template #default="{ row }"><el-input-number v-model="row.actualQuantity" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" /></template>
          </el-table-column>
          <el-table-column label="单价" width="90">
            <template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" /></template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">{{ (((row.actualQuantity || 0) - (row.planQuantity || 0)) * (row.unitPrice || 0)).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ $index }">
              <el-button type="danger" link size="small" @click="changeForm.details.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitChange">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import {
  getChangeOrderList, createChangeOrder, updateChangeOrder, updateChangeOrderStatus,
  deleteChangeOrder, getChangeOrderDetails
} from '@/api/progress'
import { submitApproval } from '@/api/approval'

const props = defineProps({
  projectId: { type: [Number, String], default: null }
})

// ====== Constants ======
const changeTypeLabel = { visa: '签证', owner_change: '业主变更', overage: '超量', labor_visa: '劳务签证' }
const changeTypeOptions = [
  { value: 'visa', label: '签证' },
  { value: 'owner_change', label: '业主变更' },
  { value: 'overage', label: '超量' },
  { value: 'labor_visa', label: '劳务签证' }
]
const changeStatusLabel = { draft: '草稿', pending: '审批中', approved: '已审批', rejected: '已驳回' }

// ====== Table ======
const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(getChangeOrderList, { projectId: null })

// Sync project filter from parent
watch(() => props.projectId, (val) => {
  query.projectId = val
  query.page = 1
  fetchData()
})

// ====== Form state ======
const changeFormRef = ref(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const createEmptyDetail = () => ({ itemName: '', specModel: '', unit: '', planQuantity: null, actualQuantity: null, unitPrice: null })

const changeForm = reactive({
  projectId: null, contractId: null, changeType: '', title: '', description: '',
  details: [createEmptyDetail()]
})

const changeRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入变更标题', trigger: 'blur' }]
}

const addDetail = () => { changeForm.details.push(createEmptyDetail()) }

// ====== CRUD ======
const handleAdd = () => {
  isEdit.value = false; editId.value = null
  Object.assign(changeForm, {
    projectId: props.projectId, contractId: null, changeType: '', title: '', description: '',
    details: [createEmptyDetail()]
  })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true; editId.value = row.id
  let existingDetails = [createEmptyDetail()]
  try {
    const res = await getChangeOrderDetails(row.id)
    const details = res.data || []
    if (details.length > 0) {
      existingDetails = details.map(d => ({
        itemName: d.item_name || '', specModel: d.spec_model || '', unit: d.unit || '',
        planQuantity: d.plan_quantity, actualQuantity: d.actual_quantity, unitPrice: d.unit_price
      }))
    }
  } catch { /* ignore */ }
  Object.assign(changeForm, {
    projectId: row.project_id, contractId: row.contract_id || null, changeType: row.change_type || '',
    title: row.title || '', description: row.description || '', details: existingDetails
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(changeForm, {
    projectId: null, contractId: null, changeType: '', title: '', description: '',
    details: [createEmptyDetail()]
  })
  changeFormRef.value?.resetFields()
}

const submitChange = async () => {
  await changeFormRef.value.validate()
  const details = changeForm.details.filter(d => d.itemName).map(d => {
    const diff = (d.actualQuantity || 0) - (d.planQuantity || 0)
    return { ...d, diffQuantity: diff, subtotal: diff * (d.unitPrice || 0) }
  })
  const totalAmount = details.reduce((s, d) => s + (d.subtotal || 0), 0)
  const payload = {
    projectId: changeForm.projectId, contractId: changeForm.contractId,
    changeType: changeForm.changeType, title: changeForm.title,
    description: changeForm.description, totalAmount, details
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateChangeOrder(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createChangeOrder(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

const handleStatusUpdate = async (row, status) => {
  if (status === 'pending') {
    // 走审批流程
    await ElMessageBox.confirm('确定提交审批？提交后将进入审批流程。', '提交审批', { type: 'info' })
    await submitApproval({ bizType: 'change_order', bizId: row.id, action: 'submit' })
    ElMessage.success('已提交审批')
  } else {
    await ElMessageBox.confirm(`确定将状态改为"${changeStatusLabel[status]}"？`, '提示', { type: 'warning' })
    await updateChangeOrderStatus(row.id, status)
    ElMessage.success('状态已更新')
  }
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该变更单？', '提示', { type: 'warning' })
  await deleteChangeOrder(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  query.projectId = props.projectId
  fetchData()
})
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
