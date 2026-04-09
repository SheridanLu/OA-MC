import request from '@/utils/request'

// ====== 里程碑管理 ======

export function getMilestoneList(params) {
  return request.get('/api/v1/progress/milestones', { params })
}

export function getAllMilestones(params) {
  return request.get('/api/v1/progress/milestones/all', { params })
}

export function createMilestone(data) {
  return request.post('/api/v1/progress/milestones', data)
}

export function updateMilestone(id, data) {
  return request.put(`/api/v1/progress/milestones/${id}`, data)
}

export function deleteMilestone(id) {
  return request.delete(`/api/v1/progress/milestones/${id}`)
}

export function updateMilestoneStatus(id, status) {
  return request.patch(`/api/v1/progress/milestones/${id}/status`, { status })
}

export function getMilestoneDeps(id) {
  return request.get(`/api/v1/progress/milestones/${id}/deps`)
}

// ====== 甘特任务 / 里程碑 ======

export function getGanttTaskList(params) {
  return request.get('/api/v1/progress/gantt', { params })
}

export function getGanttTaskById(id) {
  return request.get(`/api/v1/progress/gantt/${id}`)
}

export function createGanttTask(data) {
  return request.post('/api/v1/progress/gantt', data)
}

export function updateGanttTask(id, data) {
  return request.put(`/api/v1/progress/gantt/${id}`, data)
}

export function updateGanttTaskStatus(id, status) {
  return request.patch(`/api/v1/progress/gantt/${id}/status`, { status })
}

export function deleteGanttTask(id) {
  return request.delete(`/api/v1/progress/gantt/${id}`)
}

// ====== 变更管理 ======

export function getChangeOrderList(params) {
  return request.get('/api/v1/progress/changes', { params })
}

export function getChangeOrderById(id) {
  return request.get(`/api/v1/progress/changes/${id}`)
}

export function getChangeOrderDetails(id) {
  return request.get(`/api/v1/progress/changes/${id}/details`)
}

export function createChangeOrder(data) {
  return request.post('/api/v1/progress/changes', data)
}

export function updateChangeOrder(id, data) {
  return request.put(`/api/v1/progress/changes/${id}`, data)
}

export function updateChangeOrderStatus(id, status) {
  return request.patch(`/api/v1/progress/changes/${id}/status`, { status })
}

export function deleteChangeOrder(id) {
  return request.delete(`/api/v1/progress/changes/${id}`)
}

// V3.2 新增 - 变更提交
export function submitChangeOrder(id) {
  return request.post(`/api/v1/progress/changes/${id}/submit`)
}

// 进度填报
export function reportProgress(taskId, data) {
  return request.post(`/api/v1/progress/gantt/${taskId}/report`, data)
}

// 进度纠偏
export function correctProgress(taskId, data) {
  return request.post(`/api/v1/progress/gantt/${taskId}/correct`, data)
}

// 对账单
export function getStatementList(params) {
  return request.get('/api/v1/progress/statements', { params })
}

export function createStatement(data) {
  return request.post('/api/v1/progress/statements', data)
}

export function submitStatement(id) {
  return request.post(`/api/v1/progress/statements/${id}/submit`)
}

// 收入拆分
export function getIncomeSplitList(params) {
  return request.get('/api/v1/progress/income-splits', { params })
}

export function createIncomeSplit(data) {
  return request.post('/api/v1/progress/income-splits', data)
}
