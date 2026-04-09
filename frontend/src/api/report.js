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
