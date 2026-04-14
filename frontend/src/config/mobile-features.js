/**
 * 移动端功能裁剪配置（§4.20）
 *
 * 规则：
 * - 合同签订 → 仅 PC 端
 * - 系统管理 → 仅 PC 端
 * - 甘特图 → 移动端仅列表视图
 * - 进度填报 → 移动端仅单节点快捷填报
 */

/** PC-only 路由路径 — 移动端隐藏入口并路由守卫拦截 */
export const PC_ONLY_ROUTES = [
  '/contract/sign',          // 合同签订
  '/system/user',            // 系统管理 - 用户
  '/system/role',            // 系统管理 - 角色
  '/system/config',          // 系统管理 - 系统配置
  '/system/dept',            // 系统管理 - 部门管理
  '/system/flow',            // 系统管理 - 流程定义
  '/system/audit-log',       // 系统管理 - 审计日志
]

/** 移动端降级组件映射 */
export const MOBILE_DEGRADE_MAP = {
  // 甘特图 → 移动端列表视图
  'GanttChart': 'GanttListView',
  // 进度填报 → 单节点快捷模式
  'ProgressBatchReport': 'ProgressSingleReport',
}

/**
 * 判断是否移动端
 */
export function isMobile() {
  return /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent)
    || window.innerWidth <= 768
}

/**
 * 获取客户端类型标识（对应 X-Client-Type）
 */
export function getClientType() {
  if (window.__wxjs_environment === 'miniprogram') return 'wxapp'
  if (isMobile()) return 'h5'
  return 'pc'
}
