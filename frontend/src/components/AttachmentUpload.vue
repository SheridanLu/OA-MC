<template>
  <div class="attachment-upload">
    <el-upload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :data="uploadData"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      :limit="10"
      :accept="acceptTypes"
      multiple
      :disabled="disabled"
    >
      <el-button type="primary" :disabled="disabled">
        <el-icon><Upload /></el-icon>
        上传附件
      </el-button>
      <template #tip>
        <div class="el-upload__tip">
          支持 jpg/png/gif/webp/pdf/doc/docx/xls/xlsx/txt/zip/rar，
          单文件不超过 50MB
        </div>
      </template>
    </el-upload>

    <!-- 附件列表 -->
    <el-table v-if="fileList.length" :data="fileList" style="margin-top: 12px">
      <el-table-column prop="file_name" label="文件名" />
      <el-table-column prop="file_size" label="大小" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.file_size) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDownload(row)">
            下载
          </el-button>
          <el-button
            v-if="!readonly"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  bizType: { type: String, required: true },
  bizId: { type: Number, default: null },
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['change'])

const uploadRef = ref()
const fileList = ref([])

const uploadUrl = computed(() =>
  `${import.meta.env.VITE_API_BASE_URL || ''}/api/v1/attachments/upload`)

const headers = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const uploadData = computed(() => ({
  bizType: props.bizType,
  bizId: props.bizId
}))

const acceptTypes = '.jpg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip,.rar'

const MAX_SIZE = 50 * 1024 * 1024 // 50MB

function beforeUpload(file) {
  if (file.size > MAX_SIZE) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  const ext = file.name.split('.').pop().toLowerCase()
  const allowed = ['jpg','png','gif','webp','pdf','doc','docx',
                    'xls','xlsx','txt','zip','rar']
  if (!allowed.includes(ext)) {
    ElMessage.error('不支持的文件类型')
    return false
  }
  return true
}

function onSuccess(response) {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    loadAttachments()
    emit('change')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function onError() {
  ElMessage.error('上传失败，请重试')
}

async function loadAttachments() {
  if (!props.bizId) return
  const res = await request.get('/api/v1/attachments/biz', {
    params: { bizType: props.bizType, bizId: props.bizId }
  })
  fileList.value = res.data || []
}

async function handleDownload(row) {
  const res = await request.get(
    `/api/v1/attachments/${row.id}/url`)
  if (res.data?.url) {
    window.open(res.data.url, '_blank')
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该附件？', '提示', {
    type: 'warning'
  })
  await request.delete(`/api/v1/attachments/${row.id}`)
  ElMessage.success('删除成功')
  loadAttachments()
  emit('change')
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

onMounted(() => {
  loadAttachments()
})

defineExpose({ loadAttachments })
</script>
