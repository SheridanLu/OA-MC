import { defineStore } from 'pinia'
import { useUserStore } from './user'

/**
 * 异步路由定义 — 在 router/async-routes.js 中维护，此处引入
 * 每条路由的 meta.permission 控制是否可见
 */
let asyncRoutes = []

/**
 * 根据权限码过滤路由
 */
function filterRoutes(routes, permissions) {
  const result = []
  for (const route of routes) {
    // 没有 permission 要求的路由直接通过
    if (!route.meta?.permission) {
      const r = { ...route }
      if (route.children?.length) {
        r.children = filterRoutes(route.children, permissions)
      }
      result.push(r)
      continue
    }

    // permission 可以是 string 或 string[]
    const required = Array.isArray(route.meta.permission)
      ? route.meta.permission
      : [route.meta.permission]

    // 满足任一权限即可访问
    const hasAccess = required.some(p => permissions.includes(p))
    if (hasAccess) {
      const r = { ...route }
      if (route.children?.length) {
        r.children = filterRoutes(route.children, permissions)
      }
      result.push(r)
    }
  }
  return result
}

/**
 * 从路由树构建菜单树
 * 只包含 meta.hidden !== true 的路由
 */
function buildMenuFromRoutes(routes, basePath = '') {
  const menus = []
  for (const route of routes) {
    const fullPath = basePath
      ? `${basePath}/${route.path}`.replace(/\/+/g, '/')
      : route.path

    // hidden 路由自身不作为菜单项，但其子路由要提升到当前层级
    if (route.meta?.hidden) {
      if (route.children?.length) {
        menus.push(...buildMenuFromRoutes(route.children, fullPath))
      }
      continue
    }

    const menu = {
      path: fullPath,
      title: route.meta?.title || '',
      icon: route.meta?.icon || '',
      children: []
    }

    if (route.children?.length) {
      menu.children = buildMenuFromRoutes(route.children, fullPath)
    }

    menus.push(menu)
  }
  return menus
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    // 用户拥有的权限码列表
    permissions: [],
    // 动态生成的路由（已过滤）
    accessRoutes: [],
    // 菜单树
    menuTree: [],
    // 路由是否已注入
    routesAdded: false
  }),

  getters: {
    /**
     * 检查是否拥有指定权限
     * @param {string} code 权限码
     */
    hasPermission: (state) => (code) => {
      if (!code) return true
      return state.permissions.includes(code)
    },

    /**
     * 检查是否拥有任一权限（OR）
     * @param {string[]} codes 权限码数组
     */
    hasAnyPermission: (state) => (codes) => {
      if (!codes || !codes.length) return true
      return codes.some(code => state.permissions.includes(code))
    },

    /**
     * 检查是否拥有全部权限（AND）
     * @param {string[]} codes 权限码数组
     */
    hasAllPermissions: (state) => (codes) => {
      if (!codes || !codes.length) return true
      return codes.every(code => state.permissions.includes(code))
    }
  },

  actions: {
    /**
     * 设置异步路由模块（由 router/index.js 调用）
     */
    setAsyncRoutes(routes) {
      asyncRoutes = routes
    },

    /**
     * 从用户信息同步权限码，并生成可访问路由和菜单
     * 在路由守卫中调用
     */
    generateRoutes() {
      const userStore = useUserStore()
      this.permissions = userStore.permissions || []

      // SUPER_ADMIN 角色直接获得所有路由，不受权限码过滤
      const isSuperAdmin = (userStore.roles || []).includes('SUPER_ADMIN')

      if (isSuperAdmin) {
        this.accessRoutes = asyncRoutes
      } else {
        // 过滤异步路由
        this.accessRoutes = filterRoutes(asyncRoutes, this.permissions)
      }

      // 构建菜单树 — 从 accessRoutes 中过滤 hidden 路由后生成
      this.menuTree = buildMenuFromRoutes(this.accessRoutes, '')

      return this.accessRoutes
    },

    /**
     * 标记路由已注入到 router
     */
    setRoutesAdded(val = true) {
      this.routesAdded = val
    },

    /**
     * 重置状态（登出时调用）
     */
    resetState() {
      this.permissions = []
      this.accessRoutes = []
      this.menuTree = []
      this.routesAdded = false
    }
  }
})
