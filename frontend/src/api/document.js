import request from '@/utils/request'

export function getDocumentList(params) {
  return request.get('/api/v1/documents', { params })
}

export function deleteDocument(id) {
  return request.delete(`/api/v1/documents/${id}`)
}
