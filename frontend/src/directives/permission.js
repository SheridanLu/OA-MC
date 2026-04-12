import { usePermissionStore } from '@/stores/permission'

/**
 * v-permission 指令 — V3.2 权限控制
 *
 * 用法:
 *   v-permission="'material:inbound'"            — 单个权限
 *   v-permission="['a:b', 'c:d']"                — 满足任一即可 (OR)
 *   v-permission="{ all: ['a:b', 'c:d'] }"       — 必须同时满足 (AND)
 */
function checkPermission(el, binding) {
  const { value } = binding
  if (!value) return

  const permissionStore = usePermissionStore()
  let hasPermission = false

  if (typeof value === 'string') {
    hasPermission = permissionStore.hasPermission(value)
  } else if (Array.isArray(value)) {
    hasPermission = permissionStore.hasAnyPermission(value)
  } else if (value && typeof value === 'object' && Array.isArray(value.all)) {
    hasPermission = permissionStore.hasAllPermissions(value.all)
  }

  // 使用 display:none 代替 removeChild，避免永久移除 DOM 导致权限变更后无法恢复
  if (!hasPermission) {
    el.style.display = 'none'
    el.setAttribute('aria-hidden', 'true')
  } else {
    el.style.display = ''
    el.removeAttribute('aria-hidden')
  }
}

export const permissionDirective = {
  mounted(el, binding) {
    // 保存原始 display 值，以便恢复
    el._originalDisplay = el.style.display
    checkPermission(el, binding)
  },
  updated(el, binding) {
    checkPermission(el, binding)
  }
}
