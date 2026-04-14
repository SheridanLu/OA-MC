import { ref } from 'vue'

const CACHE_KEY = 'mochu_todo_cache'
const CACHE_TTL = 5 * 60 * 1000 // 5 分钟

/**
 * 待办列表本地缓存
 *
 * 规则（§4.20）：
 * - 已缓存的待办列表（5分钟本地缓存）离线可查看
 * - 网络恢复后自动刷新
 */
export function useTodoCache() {
  const todoList = ref([])

  /**
   * 获取待办列表 — 优先读缓存
   */
  function getTodoList(fetchFn) {
    const cached = readCache()
    if (cached) {
      todoList.value = cached
      // 后台静默刷新（如果在线）
      if (navigator.onLine) {
        fetchFn().then(data => {
          todoList.value = data
          writeCache(data)
        }).catch(() => {})
      }
      return Promise.resolve(cached)
    }

    // 无缓存 → 在线获取
    return fetchFn().then(data => {
      todoList.value = data
      writeCache(data)
      return data
    })
  }

  /**
   * 网络恢复时强制刷新
   */
  function onNetworkRestore(fetchFn) {
    window.addEventListener('network-restore', () => {
      fetchFn().then(data => {
        todoList.value = data
        writeCache(data)
      })
    })
  }

  function readCache() {
    try {
      const raw = localStorage.getItem(CACHE_KEY)
      if (!raw) return null
      const { data, expiry } = JSON.parse(raw)
      if (Date.now() > expiry) {
        localStorage.removeItem(CACHE_KEY)
        return null
      }
      return data
    } catch {
      return null
    }
  }

  function writeCache(data) {
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify({
        data,
        expiry: Date.now() + CACHE_TTL
      }))
    } catch {}
  }

  return { todoList, getTodoList, onNetworkRestore }
}
