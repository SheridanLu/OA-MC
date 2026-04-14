import request from '@/utils/request'

// 项目概览报表
export function getProjectOverview(params) { return request.get('/api/v1/reports/project', { params }) }

// 财务收支报表
export function getFinanceReport(params) { return request.get('/api/v1/reports/finance', { params }) }

// 物资库存报表
export function getInventoryReport(params) { return request.get('/api/v1/reports/inventory', { params }) }

// 合同执行报表
export function getContractReport(params) { return request.get('/api/v1/reports/contract', { params }) }

// 人力资源报表
export function getHrReport(params) { return request.get('/api/v1/reports/hr', { params }) }

// 成本分析报表
export function getCostAnalysis(params) { return request.get('/api/v1/reports/cost', { params }) }

// ==================== 报表模板（Phase 5）====================

export function getReportTemplateList(params) {
  return request({ url: '/api/v1/report/templates', method: 'get', params })
}

export function getReportTemplate(id) {
  return request({ url: `/api/v1/report/templates/${id}`, method: 'get' })
}

export function createReportTemplate(data) {
  return request({ url: '/api/v1/report/templates', method: 'post', data })
}

export function updateReportTemplate(id, data) {
  return request({ url: `/api/v1/report/templates/${id}`, method: 'put', data })
}

export function deleteReportTemplate(id) {
  return request({ url: `/api/v1/report/templates/${id}`, method: 'delete' })
}

export function executeReportTemplate(id, params) {
  return request({ url: `/api/v1/report/templates/${id}/execute`, method: 'post', data: params })
}

// 内置报表
export function getStockFlowReport(params) {
  return request({ url: '/api/v1/report/stock-flow', method: 'get', params })
}

export function getStockAgingReport(params) {
  return request({ url: '/api/v1/report/stock-aging', method: 'get', params })
}

export function getPurchasePriceComparison(params) {
  return request({ url: '/api/v1/report/purchase-price', method: 'get', params })
}

// ==================== 报表订阅 ====================

export function getSubscriptions(params) {
  return request.get('/api/v1/report/subscribe/list', { params })
}

export function subscribe(data) {
  return request.post('/api/v1/report/subscribe', data)
}

export function unsubscribe(id) {
  return request.delete(`/api/v1/report/subscribe/${id}`)
}

// ==================== 报表导出 ====================

export function exportReport(params) {
  return request.get('/api/v1/reports/export', { params, responseType: 'blob' })
}
