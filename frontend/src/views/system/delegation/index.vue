<template>
  <div class="delegation-page">
    <!-- 搜索区域 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="有效" :value="1" />
            <el-option label="已撤销" :value="0" />
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
        <el-button type="primary" @click="handleAdd" v-permission="'approval:delegation-manage'">新增委托</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="delegator_name" label="委托人" width="120" />
        <el-table-column prop="delegatee_name" label="被委托人" width="120" />
        <el-table-column prop="start_time" label="生效时间" width="170" />
        <el-table-column prop="end_time" label="到期时间" width="170" />
        <el-table-column prop="remark" label="说明" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '有效' : '已撤销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="warning" link size="small" @click="handleRevoke(row)" v-permission="'approval:delegation-manage'">撤销</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)" v-permission="'approval:delegation-manage'">删除</el-button>
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

    <!-- 新增弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增委托" width="550px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="被委托人ID" prop="delegateeId">
          <el-input-number v-model="form.delegateeId" :min="1" placeholder="用户ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择生效时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择到期时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="委托说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入委托说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDelegationList, createDelegation, revokeDelegation, deleteDelegation } from '@/api/delegation'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref(null)

const queryForm = reactive({
  status: null,
  page: 1,
  size: 20
})

const form = reactive({
  delegateeId: null,
  startTime: '',
  endTime: '',
  remark: '',
  permissionCodes: []
})

const rules = {
  delegateeId: [{ required: true, message: '请输入被委托人ID', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择到期时间', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDelegationList(queryForm)
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
  queryForm.status = null
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
  form.delegateeId = null
  form.startTime = ''
  form.endTime = ''
  form.remark = ''
  form.permissionCodes = []
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    await createDelegation(form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

const handleRevoke = async (row) => {
  await ElMessageBox.confirm('确定撤销该委托？', '提示', { type: 'warning' })
  await revokeDelegation(row.id)
  ElMessage.success('已撤销')
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该委托记录？', '提示', { type: 'warning' })
  await deleteDelegation(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-card {
  margin-bottom: 0;
}
</style>
