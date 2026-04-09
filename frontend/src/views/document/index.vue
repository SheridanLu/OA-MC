<template>
  <div class="page-container">
    <page-header title="文档管理">
      <el-button type="primary" v-permission="'doc:upload'" @click="uploadVisible = true"><el-icon><Upload /></el-icon>上传文档</el-button>
    </page-header>

    <el-form :model="query" inline class="search-wrapper">
      <el-form-item label="关键字"><el-input v-model="query.keyword" placeholder="文件名" clearable @keyup.enter="handleSearch" /></el-form-item>
      <el-form-item label="关联项目"><project-select v-model="query.project_id" style="width:200px" /></el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.category" placeholder="全部" clearable>
          <el-option label="施工资料" value="construction" />
          <el-option label="合同文件" value="contract" />
          <el-option label="竣工资料" value="completion" />
          <el-option label="设计文件" value="design" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="file_name" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" align="center">
        <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
      </el-table-column>
      <el-table-column prop="project_name" label="关联项目" width="160" show-overflow-tooltip />
      <el-table-column prop="file_size" label="大小" width="100" align="right">
        <template #default="{ row }">{{ formatFileSize(row.file_size) }}</template>
      </el-table-column>
      <el-table-column prop="uploader_name" label="上传人" width="100" />
      <el-table-column prop="created_at" label="上传时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'doc:download'" link type="primary" @click="handleDownload(row)">下载</el-button>
          <el-button v-permission="'doc:manage'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[20,50,100]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
    </div>

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadVisible" title="上传文档" width="500px" destroy-on-close>
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" style="width:100%">
            <el-option label="施工资料" value="construction" />
            <el-option label="合同文件" value="contract" />
            <el-option label="竣工资料" value="completion" />
            <el-option label="设计文件" value="design" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目"><project-select v-model="uploadForm.project_id" /></el-form-item>
        <el-form-item label="文件">
          <file-upload v-model="uploadForm.files" :limit="10" tip="支持常见文件格式，单文件50MB以内" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { formatFileSize } from '@/utils/format'
import { getDocumentList, deleteDocument } from '@/api/document'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', project_id: '', category: '' })
const uploadVisible = ref(false)
const uploadForm = reactive({ category: 'other', project_id: '', files: [] })

const categoryMap = { construction: '施工资料', contract: '合同文件', completion: '竣工资料', design: '设计文件', other: '其他' }

async function fetchData() {
  loading.value = true
  try {
    const res = await getDocumentList(query)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; fetchData() }
function handleReset() { Object.assign(query, { page: 1, keyword: '', project_id: '', category: '' }); fetchData() }

function handleDownload(row) {
  window.open(row.file_url, '_blank')
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除此文档？')
  await deleteDocument(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.page-container { background: #fff; border-radius: 4px; padding: 20px; }
</style>
