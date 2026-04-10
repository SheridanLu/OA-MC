import request from '@/utils/request'

// ====== 字典类型 ======

export function getDictTypeList(params) {
  return request.get('/api/v1/system/dict/types', { params })
}

export function getDictTypeById(id) {
  return request.get(`/api/v1/system/dict/types/${id}`)
}

export function createDictType(data) {
  return request.post('/api/v1/system/dict/types', data)
}

export function updateDictType(id, data) {
  return request.put(`/api/v1/system/dict/types/${id}`, data)
}

export function deleteDictType(id) {
  return request.delete(`/api/v1/system/dict/types/${id}`)
}

// ====== 字典数据 ======

export function getDictDataList(params) {
  return request.get('/api/v1/system/dict/data', { params })
}

export function getDictDataByType(dictType) {
  return request.get(`/api/v1/system/dict/data/type/${dictType}`)
}

export function createDictData(data) {
  return request.post('/api/v1/system/dict/data', data)
}

export function updateDictData(id, data) {
  return request.put(`/api/v1/system/dict/data/${id}`, data)
}

export function deleteDictData(id) {
  return request.delete(`/api/v1/system/dict/data/${id}`)
}
