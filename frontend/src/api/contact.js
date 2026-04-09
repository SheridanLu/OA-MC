import request from '@/utils/request'

export function getExternalContacts(params) {
  return request.get('/api/v1/contacts/external', { params })
}

export function createExternalContact(data) {
  return request.post('/api/v1/contacts/external', data)
}

export function updateExternalContact(id, data) {
  return request.put(`/api/v1/contacts/external/${id}`, data)
}

export function deleteExternalContact(id) {
  return request.delete(`/api/v1/contacts/external/${id}`)
}
