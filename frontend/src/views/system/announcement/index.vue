<template>
  <div class="announcement-page">
    <!-- 搜索 -->
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="标题">
          <el-input v-model="queryForm.title" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.type" placeholder="全部" clearable>
            <el-option label="通知" value="notice" />
            <el-option label="制度" value="policy" />
            <el-option label="活动" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已下线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd" v-permission="'system:announcement-manage'">新增公告</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type]" size="small">{{ typeNameMap[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status]" size="small">{{ statusNameMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.is_top === 1" type="danger" size="small">顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisher_name" label="发布人" width="100" />
        <el-table-column prop="publish_time" label="发布时间" width="170" />
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)" v-permission="'system:announcement-manage'">编辑</el-button>
            <el-button link type="success" v-if="row.status === 'draft'" @click="handlePublish(row)" v-permission="'system:announcement-manage'">发布</el-button>
            <el-button link type="warning" v-if="row.status === 'published'" @click="handleOffline(row)" v-permission="'system:announcement-manage'">下线</el-button>
            <el-button link :type="row.is_top === 1 ? 'info' : 'primary'" @click="handleToggleTop(row)" v-permission="'system:announcement-manage'">
              {{ row.is_top === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-permission="'system:announcement-manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type">
            <el-option label="通知" value="notice" />
            <el-option label="制度" value="policy" />
            <el-option label="活动" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="form.expire_time" type="datetime" placeholder="选择过期时间" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="可见范围">
          <el-input v-model="form.scope" placeholder="all 或逗号分隔部门ID" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持HTML富文本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAnnouncementList, createAnnouncement, updateAnnouncement,
  publishAnnouncement, offlineAnnouncement, toggleTopAnnouncement, deleteAnnouncement
} from '@/api/announcement'

const typeNameMap = { notice: '通知', policy: '制度', activity: '活动' }
const typeTagMap = { notice: '', policy: 'warning', activity: 'success' }
const statusNameMap = { draft: '草稿', published: '已发布', offline: '已下线', expired: '已过期' }
const statusTagMap = { draft: 'info', published: 'success', offline: 'warning', expired: 'danger' }

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)

const queryForm = reactive({ title: '', type: '', status: '', page: 1, size: 20 })
const form = reactive({ id: null, title: '', content: '', type: 'notice', expire_time: null, is_top: 0, scope: 'all' })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAnnouncementList(queryForm)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => {
  Object.assign(queryForm, { title: '', type: '', status: '', page: 1, size: 20 })
  fetchData()
}

const handleAdd = () => {
  Object.assign(form, { id: null, title: '', content: '', type: 'notice', expire_time: null, is_top: 0, scope: 'all' })
  dialogTitle.value = '新增公告'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, {
    id: row.id, title: row.title, content: row.content, type: row.type,
    expire_time: row.expire_time, is_top: row.is_top, scope: row.scope
  })
  dialogTitle.value = '编辑公告'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateAnnouncement(form.id, form)
    } else {
      await createAnnouncement(form)
    }
    ElMessage.success(form.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

const handlePublish = (row) => {
  ElMessageBox.confirm(`确定发布公告 "${row.title}" 吗？`, '提示').then(async () => {
    await publishAnnouncement(row.id)
    ElMessage.success('发布成功')
    fetchData()
  }).catch(() => {})
}

const handleOffline = (row) => {
  ElMessageBox.confirm(`确定下线公告 "${row.title}" 吗？`, '提示').then(async () => {
    await offlineAnnouncement(row.id)
    ElMessage.success('已下线')
    fetchData()
  }).catch(() => {})
}

const handleToggleTop = async (row) => {
  await toggleTopAnnouncement(row.id)
  ElMessage.success(row.is_top === 1 ? '已取消置顶' : '已置顶')
  fetchData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除公告 "${row.title}" 吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.search-card { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
