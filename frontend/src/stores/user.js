import { defineStore } from 'pinia'
import { loginByPassword, loginBySms, logout as logoutApi, getCurrentUser } from '@/api/auth'
import router from '@/router'
import { usePermissionStore } from './permission'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    permissions: [],
    roles: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userId: (state) => state.userInfo?.id,
    username: (state) => state.userInfo?.username,
    realName: (state) => state.userInfo?.real_name
  },

  actions: {
    async loginByPassword(data) {
      const res = await loginByPassword(data)
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
      await this.fetchUserInfo()
      return res
    },

    async loginBySms(data) {
      const res = await loginBySms(data)
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
      await this.fetchUserInfo()
      return res
    },

    async fetchUserInfo() {
      const res = await getCurrentUser()
      this.userInfo = res.data
      this.permissions = res.data.permissions || []
      this.roles = res.data.role_codes || []
      return res.data
    },

    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 即使后端失败也清除本地状态
      }
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
      localStorage.removeItem('token')
      const permissionStore = usePermissionStore()
      permissionStore.resetState()
      router.push('/login')
    },

    resetState() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
      localStorage.removeItem('token')
      const permissionStore = usePermissionStore()
      permissionStore.resetState()
    }
  }
})
