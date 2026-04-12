<template>
  <div class="config-page">
    <!-- 搜索区域 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="配置键">
          <el-input v-model="queryForm.configKey" placeholder="请输入配置键" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="配置分组">
          <el-input v-model="queryForm.configGroup" placeholder="请输入分组" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-button type="primary" @click="handleAdd" v-permission="'system:config-manage'">新增配置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="config_key" label="配置键" width="200" show-overflow-tooltip />
        <el-table-column prop="config_value" label="配置值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="config_desc" label="说明" width="200" show-overflow-tooltip />
        <el-table-column prop="config_group" label="分组" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)" v-permission="'system:config-manage'">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)" v-permission="'system:config-manage'">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="550px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如：system.title" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入配置值" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="说明" prop="configDesc">
          <el-input v-model="form.configDesc" placeholder="配置说明" />
        </el-form-item>
        <el-form-item label="分组" prop="configGroup">
          <el-input v-model="form.configGroup" placeholder="如：system / email / oss" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
import { getConfigList, createConfig, updateConfig, deleteConfig } from '@/api/config'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)

const queryForm = reactive({
  configKey: '',
  configGroup: '',
  status: null,
  page: 1,
  size: 20
})

const form = reactive({
  configKey: '',
  configValue: '',
  configDesc: '',
  configGroup: '',
  status: 1
})

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getConfigList(queryForm)
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
  queryForm.configKey = ''
  queryForm.configGroup = ''
  queryForm.status = null
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
  form.configKey = row.config_key
  form.configValue = row.config_value
  form.configDesc = row.config_desc || ''
  form.configGroup = row.config_group || ''
  form.status = row.status
  dialogVisible.value = true
}

const resetForm = () => {
  form.configKey = ''
  form.configValue = ''
  form.configDesc = ''
  form.configGroup = ''
  form.status = 1
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateConfig(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createConfig(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除配置 "${row.config_key}" ？`, '提示', { type: 'warning' })
  await deleteConfig(row.id)
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
