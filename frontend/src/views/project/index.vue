<template>
  <div class="project-page">
    <!-- 搜索区域 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="项目名称">
          <el-input v-model="queryForm.projectName" placeholder="请输入项目名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="项目编号">
          <el-input v-model="queryForm.projectNo" placeholder="请输入编号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="queryForm.projectType" placeholder="全部" clearable style="width: 120px">
            <el-option label="虚拟项目" :value="1" />
            <el-option label="实体项目" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="pending" />
            <el-option label="进行中" value="active" />
            <el-option label="已完工" value="completed" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never" style="margin-top: 12px">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleAdd" v-permission="'project:create'">新建项目</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="project_no" label="项目编号" width="140" />
        <el-table-column prop="project_name" label="项目名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="project_type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.project_type === 1 ? 'warning' : 'primary'" size="small">
              {{ row.project_type === 1 ? '虚拟' : '实体' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contract_type" label="合同类型" width="110" />
        <el-table-column prop="client_name" label="甲方" width="150" show-overflow-tooltip />
        <el-table-column prop="amount_with_tax" label="含税金额" width="130" align="right">
          <template #default="{ row }">
            {{ row.amount_with_tax ? Number(row.amount_with_tax).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)" :disabled="row.status === 'pending'" v-permission="'project:edit'">编辑</el-button>
            <el-dropdown v-if="row.status === 'active' || row.status === 'completed'" @command="(cmd) => handleStatusChange(row, cmd)" style="margin-left: 8px" v-permission="'project:edit'">
              <el-button type="warning" link size="small">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="active">进行中</el-dropdown-item>
                  <el-dropdown-item command="completed">已完工</el-dropdown-item>
                  <el-dropdown-item command="closed">已关闭</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-if="row.status !== 'pending'" type="danger" link size="small" @click="handleDelete(row)" v-permission="'project:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top: 16px; justify-content: flex-end"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="750px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="form.projectName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目别名">
              <el-input v-model="form.projectAlias" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目类型" prop="projectType">
              <el-select v-model="form.projectType" style="width: 100%">
                <el-option label="虚拟项目" :value="1" />
                <el-option label="实体项目" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="form.contractType" style="width: 100%" placeholder="请选择合同类型">
                <el-option v-for="t in contractTypeOptions" :key="t.code" :label="t.label" :value="t.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="甲方信息">
              <el-input v-model="form.clientName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目地点">
              <el-input v-model="form.location" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="含税金额">
              <el-input-number v-model="form.amountWithTax" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不含税金额">
              <el-input-number v-model="form.amountWithoutTax" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="税率(%)">
              <el-input-number v-model="form.taxRate" :precision="2" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="计划开始">
              <el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划结束">
              <el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保修到期">
              <el-date-picker v-model="form.warrantyDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存' : '提交' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getProjectList, createProject, updateProject, updateProjectStatus, deleteProject } from '@/api/project'
import { getContractTypes } from '@/api/contractTpl'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)
const contractTypeOptions = ref([])

const statusMap = {
  draft: { text: '草稿', type: 'info' },
  pending: { text: '待审批', type: 'warning' },
  active: { text: '进行中', type: 'primary' },
  completed: { text: '已完工', type: 'success' },
  closed: { text: '已关闭', type: 'danger' }
}

const queryForm = reactive({
  projectName: '',
  projectNo: '',
  projectType: null,
  status: '',
  page: 1,
  size: 20
})

const form = reactive({
  projectName: '',
  projectAlias: '',
  projectType: 2,
  contractType: '',
  clientName: '',
  location: '',
  amountWithTax: null,
  amountWithoutTax: null,
  taxRate: null,
  planStartDate: null,
  planEndDate: null,
  warrantyDate: null,
  remark: ''
})

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  contractType: [{ required: true, message: '请输入合同类型', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getProjectList(queryForm)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.projectName = ''
  queryForm.projectNo = ''
  queryForm.projectType = null
  queryForm.status = ''
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  editId.value = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.projectName = row.project_name
  form.projectAlias = row.project_alias || ''
  form.projectType = row.project_type
  form.contractType = row.contract_type
  form.clientName = row.client_name || ''
  form.location = row.location || ''
  form.amountWithTax = row.amount_with_tax
  form.amountWithoutTax = row.amount_without_tax
  form.taxRate = row.tax_rate
  form.planStartDate = row.plan_start_date || null
  form.planEndDate = row.plan_end_date || null
  form.warrantyDate = row.warranty_date || null
  form.remark = row.remark || ''
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    projectName: '', projectAlias: '', projectType: 2, contractType: null,
    clientName: '', location: '', amountWithTax: null, amountWithoutTax: null,
    taxRate: null, planStartDate: null, planEndDate: null, warrantyDate: null, remark: ''
  })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProject(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createProject(form)
      ElMessage.success('已提交审批')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

const handleStatusChange = async (row, status) => {
  await ElMessageBox.confirm(`确定将项目状态改为"${statusMap[status]?.text}"？`, '提示', { type: 'warning' })
  await updateProjectStatus(row.id, status)
  ElMessage.success('状态已更新')
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除项目"${row.project_name}"？`, '提示', { type: 'warning' })
  await deleteProject(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchData()
  getContractTypes().then(res => { contractTypeOptions.value = res.data || [] })
})
</script>

<style scoped>
.search-card {
  margin-bottom: 0;
}
</style>
