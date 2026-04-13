<template>
  <div class="app-container">
    <search-form>
      <el-form :model="query" inline>
        <el-form-item label="表名">
          <el-input v-model="query.tableName" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </search-form>

    <page-header title="代码生成">
      <el-button type="primary" @click="openImport" v-permission="'infra:codegen'">导入表</el-button>
    </page-header>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="table_name" label="表名" min-width="160" />
      <el-table-column prop="table_comment" label="表描述" min-width="160" />
      <el-table-column prop="module_name" label="模块" width="120" />
      <el-table-column prop="biz_name" label="业务名" width="120" />
      <el-table-column prop="class_name" label="类名" width="160" />
      <el-table-column prop="author" label="作者" width="100" />
      <el-table-column prop="created_at" label="创建时间" width="160" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goEdit(row)" v-permission="'infra:codegen'">编辑</el-button>
          <el-button link type="success" @click="handlePreview(row)">预览</el-button>
          <el-button link type="warning" @click="handleDownload(row)">下载</el-button>
          <el-button link type="danger" @click="handleDelete(row)" v-permission="'infra:codegen'">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0" :current-page="query.page" :page-size="query.size"
      :total="total" layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50]" @size-change="handleSizeChange" @current-change="handleCurrentChange"
      style="margin-top: 16px; justify-content: flex-end;"
    />

    <!-- 导入表对话框 -->
    <el-dialog v-model="importVisible" title="选择要导入的表" width="700px" destroy-on-close>
      <el-table v-loading="dbLoading" :data="dbTables" border stripe @selection-change="selectedDbTables = $event">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="table_name" label="表名" min-width="180" />
        <el-table-column prop="table_comment" label="表描述" min-width="200" />
        <el-table-column prop="create_time" label="创建时间" width="160" />
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!selectedDbTables.length" @click="handleImport">
          导入 {{ selectedDbTables.length ? `(${selectedDbTables.length}张)` : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 代码预览对话框 -->
    <CodegenPreview ref="previewRef" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCodegenTableList, getDbTableList, importDbTable, deleteCodegenTable, downloadCode } from '@/api/codegen'
import { useTable } from '@/composables/useTable'
import CodegenPreview from './preview.vue'

const router = useRouter()
const { loading, tableData, total, query, fetchData, handleSearch, handleReset, handleSizeChange, handleCurrentChange } =
  useTable(getCodegenTableList, { tableName: '' })

onMounted(() => { fetchData() })

const importVisible = ref(false)
const dbLoading = ref(false)
const dbTables = ref([])
const selectedDbTables = ref([])
const importing = ref(false)
const previewRef = ref(null)

async function openImport() {
  importVisible.value = true
  dbLoading.value = true
  try {
    const res = await getDbTableList()
    dbTables.value = res.data || []
  } finally {
    dbLoading.value = false
  }
}

async function handleImport() {
  importing.value = true
  try {
    for (const t of selectedDbTables.value) {
      await importDbTable(t.table_name)
    }
    ElMessage.success(`成功导入 ${selectedDbTables.value.length} 张表`)
    importVisible.value = false
    fetchData()
  } finally {
    importing.value = false
  }
}

function goEdit(row) {
  router.push(`/infra/codegen/${row.id}/edit`)
}

async function handlePreview(row) {
  previewRef.value.open(row.id)
}

async function handleDownload(row) {
  try {
    const res = await downloadCode(row.id)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.table_name}_codegen.zip`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败：' + (e.message || '未知错误'))
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.table_name}」的代码生成配置？`, '提示', { type: 'warning' })
  await deleteCodegenTable(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>
