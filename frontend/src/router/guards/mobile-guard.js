import { isMobile, PC_ONLY_ROUTES } from '@/config/mobile-features'
import { ElMessage } from 'element-plus'

/**
 * 移动端路由守卫
 * 拦截 PC-only 路由，移动端访问时重定向到首页
 */
export function setupMobileGuard(router) {
  router.beforeEach((to, from, next) => {
    if (isMobile() && PC_ONLY_ROUTES.includes(to.path)) {
      ElMessage.warning('该功能仅支持电脑端操作')
      next('/home')
      return
    }
    next()
  })
}
