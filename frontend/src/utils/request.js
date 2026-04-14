import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import CryptoJS from 'crypto-js'
import { checkWritePermission } from './network'
import { getClientType } from '@/config/mobile-features'

/**
 * 生成 UUID v4
 */
function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/**
 * camelCase → snake_case 转换
 * 后端 Jackson 配置了 property-naming-strategy: SNAKE_CASE，
 * 前端表单使用 camelCase，发送时需要转换
 */
function toSnakeCase(str) {
  return str.replace(/([A-Z])/g, '_$1').toLowerCase()
}

function convertKeysToSnakeCase(obj) {
  if (obj === null || obj === undefined || typeof obj !== 'object') {
    return obj
  }
  if (Array.isArray(obj)) {
    return obj.map(item => convertKeysToSnakeCase(item))
  }
  const result = {}
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      let val = obj[key]
      // 空字符串转 null，防止 Jackson 对 LocalDate/Integer/BigDecimal 等非 String 类型反序列化失败
      if (val === '') val = null
      result[toSnakeCase(key)] = convertKeysToSnakeCase(val)
    }
  }
  return result
}

const request = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器 — 对照 V3.2 §3.2
request.interceptors.request.use(
  (config) => {
    // P7: 离线写操作拦截
    if (!checkWritePermission(config.method || 'GET')) {
      return Promise.reject(new Error('OFFLINE_WRITE_BLOCKED'))
    }

    // P7: 文件上传 → 超时 30 秒
    if (config.headers['Content-Type'] === 'multipart/form-data'
        || config.url?.includes('/upload')) {
      config.timeout = 30000
    }

    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // P7: 自动添加 X-Client-Type（pc/h5/wxapp）
    config.headers['X-Client-Type'] = getClientType()
    // 请求追踪ID — V3.2 spec
    config.headers['X-Request-Id'] = uuid()
    // 写操作幂等键 — V3.2 spec
    const method = (config.method || '').toUpperCase()
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      config.headers['X-Idempotency-Key'] = uuid()
    }
    // 请求体 camelCase → snake_case 转换（适配后端 Jackson SNAKE_CASE 策略）
    // 必须在 HMAC 签名之前完成，保证签名 body 与实际发送 body 一致
    if (config.data && typeof config.data === 'object' && !(config.data instanceof FormData)) {
      config.data = convertKeysToSnakeCase(config.data)
    }
    // HMAC-SHA256 签名 — V3.2 §3.2 敏感接口安全
    // 注意: sign_secret 存储在 sessionStorage 中（关闭标签页即清除），
    // 降低 XSS 持久化攻击风险。后续应改为登录时由后端下发临时密钥。
    if (config.sign) {
      const timestamp = String(Date.now())
      const nonce = uuid()
      const body = config.data ? JSON.stringify(config.data) : ''
      const signPayload = `${method}\n${config.url}\n${timestamp}\n${nonce}\n${body}`
      const secret = sessionStorage.getItem('sign_secret') || ''
      const sign = CryptoJS.HmacSHA256(signPayload, secret).toString(CryptoJS.enc.Hex)
      config.headers['X-Timestamp'] = timestamp
      config.headers['X-Nonce'] = nonce
      config.headers['X-Sign'] = sign
    }
    // FormData 不设 Content-Type，由浏览器自动设置 multipart boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // Blob 响应（文件下载）不走 code 检查，直接返回原始 response
    if (response.config.responseType === 'blob') {
      return response
    }

    // Token 自动刷新 — 同步到 localStorage 和 Pinia
    const newToken = response.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
      // 延迟导入避免循环依赖，更新 Pinia store
      import('@/stores/user').then(({ useUserStore }) => {
        const userStore = useUserStore()
        userStore.token = newToken
      })
    }

    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        import('@/stores/user').then(({ useUserStore }) => {
          useUserStore().resetState()
        })
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const config = error.config

    // P7: 超时自动重试 1 次
    if (error.code === 'ECONNABORTED' && config && !config._retried) {
      config._retried = true
      console.warn('[retry] 请求超时，自动重试:', config.url)
      return request(config)
    }

    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        import('@/stores/user').then(({ useUserStore }) => {
          useUserStore().resetState()
        })
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('您没有权限执行此操作')
      } else if (status === 404) {
        ElMessage.error(data?.message || '数据不存在或已被删除')
      } else if (status === 409) {
        ElMessage.error(data?.message || '数据已被修改，请刷新后重试')
      } else if (status === 423) {
        ElMessage.error(data?.message || '账号已锁定')
      } else if (status === 429) {
        ElMessage.error(data?.message || '请求过于频繁')
      } else if (status === 500) {
        ElMessage.error('系统繁忙，请稍后重试')
      } else if (status === 502) {
        ElMessage.error('服务暂时不可用')
      } else {
        ElMessage.error(data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络连接异常')
    }
    return Promise.reject(error)
  }
)

export default request
