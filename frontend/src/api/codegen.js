import request from '@/utils/request'

// ==================== 已导入表 ====================

export function getCodegenTableList(params) {
  return request({ url: '/api/v1/infra/codegen/tables', method: 'get', params })
}

export function getCodegenTableDetail(id) {
  return request({ url: `/api/v1/infra/codegen/tables/${id}`, method: 'get' })
}

export function updateCodegenTable(id, data) {
  return request({ url: `/api/v1/infra/codegen/tables/${id}`, method: 'put', data })
}

export function deleteCodegenTable(id) {
  return request({ url: `/api/v1/infra/codegen/tables/${id}`, method: 'delete' })
}

// ==================== 数据库表（未导入） ====================

export function getDbTableList() {
  return request({ url: '/api/v1/infra/codegen/db-tables', method: 'get' })
}

export function importDbTable(tableName, author) {
  return request({
    url: '/api/v1/infra/codegen/tables/import',
    method: 'post',
    params: { tableName, author }
  })
}

// ==================== 列配置 ====================

export function getCodegenColumnList(tableId) {
  return request({ url: `/api/v1/infra/codegen/tables/${tableId}/columns`, method: 'get' })
}

export function updateCodegenColumns(tableId, data) {
  return request({ url: `/api/v1/infra/codegen/tables/${tableId}/columns`, method: 'put', data })
}

// ==================== 代码生成 ====================

export function previewCode(tableId) {
  return request({ url: `/api/v1/infra/codegen/tables/${tableId}/preview`, method: 'get' })
}

export function downloadCode(tableId) {
  return request({
    url: `/api/v1/infra/codegen/tables/${tableId}/download`,
    method: 'get',
    responseType: 'blob'
  })
}
