/**
 * 网络状态管理 — 用于移动端离线检测
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const isOnline = ref(navigator.onLine)

// 监听网络状态变化
window.addEventListener('online', () => {
  isOnline.value = true
  ElMessage.success('网络已恢复，正在刷新数据...')
  // 网络恢复后自动刷新页面数据
  window.dispatchEvent(new CustomEvent('network-restore'))
})

window.addEventListener('offline', () => {
  isOnline.value = false
  ElMessage.warning('网络连接已断开，部分功能不可用')
})

/**
 * 写操作拦截 — 离线状态下所有写操作不可用
 * 在 axios 请求拦截器中调用
 */
export function checkWritePermission(method) {
  const writeMethods = ['POST', 'PUT', 'PATCH', 'DELETE']
  if (writeMethods.includes(method.toUpperCase()) && !isOnline.value) {
    ElMessage.error('当前网络不可用，写操作已禁止')
    return false
  }
  return true
}

export { isOnline }
