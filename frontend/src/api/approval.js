import request from '@/utils/request'

// ====== 流程定义 ======

export function getFlowDefList(params) {
  return request.get('/api/v1/approval/flows', { params })
}

export function getFlowDefById(id) {
  return request.get(`/api/v1/approval/flows/${id}`)
}

export function createFlowDef(data) {
  return request.post('/api/v1/approval/flows', data)
}

export function updateFlowDef(id, data) {
  return request.put(`/api/v1/approval/flows/${id}`, data)
}

export function deleteFlowDef(id) {
  return request.delete(`/api/v1/approval/flows/${id}`)
}

// ====== 审批操作 ======

export function submitApproval(data) {
  return request.post('/api/v1/approval/submit', data)
}

export function approveInstance(instanceId, opinion) {
  return request.post(`/api/v1/approval/${instanceId}/approve`, { opinion })
}

export function rejectInstance(instanceId, opinion) {
  return request.post(`/api/v1/approval/${instanceId}/reject`, { opinion })
}

export function withdrawInstance(instanceId) {
  return request.post(`/api/v1/approval/${instanceId}/withdraw`)
}

export function transferInstance(instanceId, data) {
  return request.post(`/api/v1/approval/${instanceId}/transfer`, data)
}

export function addCosigner(instanceId, data) {
  return request.post(`/api/v1/approval/${instanceId}/cosign`, data)
}

export function approveCosign(cosignId, opinion) {
  return request.post(`/api/v1/approval/cosign/${cosignId}/approve`, { opinion })
}

export function sendReadHandle(instanceId, data) {
  return request.post(`/api/v1/approval/${instanceId}/read-handle`, data)
}

export function sendCc(instanceId, data) {
  return request.post(`/api/v1/approval/${instanceId}/cc`, data)
}

export function markCcHandled(ccId) {
  return request.post(`/api/v1/approval/cc/${ccId}/handle`)
}

// ====== 审批查询 ======

export function getMyPending(params) {
  return request.get('/api/v1/approval/pending', { params })
}

export function getMyInitiated(params) {
  return request.get('/api/v1/approval/initiated', { params })
}

export function getInstanceDetail(instanceId) {
  return request.get(`/api/v1/approval/${instanceId}`)
}

// ====== 已办列表 ======

export function getMyDone(params) {
  return request.get('/api/v1/approval/done', { params })
}

// ====== 抄送列表 ======

export function getCcList(params) {
  return request.get('/api/v1/approval/cc/list', { params })
}
