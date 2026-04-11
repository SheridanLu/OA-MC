<template>
  <el-dialog v-model="visible" title="代码预览" width="900px" destroy-on-close>
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else>
      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane
          v-for="(code, path) in previewData"
          :key="path"
          :label="fileName(path)"
          :name="path"
        >
          <div style="position: relative;">
            <el-button
              size="small"
              style="position: absolute; right: 8px; top: 8px; z-index: 10;"
              @click="copyCode(code)"
            >复制</el-button>
            <pre style="background:#1e1e1e; color:#d4d4d4; padding:16px; border-radius:4px; overflow:auto; max-height:500px; font-size:13px;"><code>{{ code }}</code></pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { previewCode } from '@/api/codegen'

const visible = ref(false)
const loading = ref(false)
const previewData = ref({})
const activeTab = ref('')

async function open(tableId) {
  visible.value = true
  loading.value = true
  previewData.value = {}
  try {
    const res = await previewCode(tableId)
    previewData.value = res.data || {}
    const keys = Object.keys(previewData.value)
    if (keys.length) activeTab.value = keys[0]
  } finally {
    loading.value = false
  }
}

function fileName(path) {
  return path.split('/').pop()
}

async function copyCode(code) {
  await navigator.clipboard.writeText(code)
  ElMessage.success('已复制到剪贴板')
}

defineExpose({ open })
</script>
