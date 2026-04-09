import request from '@/utils/request'

export function getSalaryList(params) {
  return request.get('/api/v1/hr/salaries', { params })
}

export function createSalary(data) {
  return request.post('/api/v1/hr/salaries', data)
}

export function updateSalary(id, data) {
  return request.put(`/api/v1/hr/salaries/${id}`, data)
}

export function deleteSalary(id) {
  return request.delete(`/api/v1/hr/salaries/${id}`)
}

export function getHrContractList(params) {
  return request.get('/api/v1/hr/contracts', { params })
}

export function createHrContract(data) {
  return request.post('/api/v1/hr/contracts', data)
}

export function updateHrContract(id, data) {
  return request.put(`/api/v1/hr/contracts/${id}`, data)
}

export function deleteHrContract(id) {
  return request.delete(`/api/v1/hr/contracts/${id}`)
}

export function getCertificateList(params) {
  return request.get('/api/v1/hr/certificates', { params })
}

export function createCertificate(data) {
  return request.post('/api/v1/hr/certificates', data)
}

export function updateCertificate(id, data) {
  return request.put(`/api/v1/hr/certificates/${id}`, data)
}

export function deleteCertificate(id) {
  return request.delete(`/api/v1/hr/certificates/${id}`)
}

export function getEntryList(params) {
  return request.get('/api/v1/hr/entries', { params })
}

export function createEntry(data) {
  return request.post('/api/v1/hr/entries', data)
}

export function updateEntry(id, data) {
  return request.put(`/api/v1/hr/entries/${id}`, data)
}

export function deleteEntry(id) {
  return request.delete(`/api/v1/hr/entries/${id}`)
}

export function getResignList(params) {
  return request.get('/api/v1/hr/resigns', { params })
}

export function createResign(data) {
  return request.post('/api/v1/hr/resigns', data)
}

export function updateResign(id, data) {
  return request.put(`/api/v1/hr/resigns/${id}`, data)
}

export function deleteResign(id) {
  return request.delete(`/api/v1/hr/resigns/${id}`)
}

// V3.2 新增 - 薪资配置
export function getSalaryConfigList(params) {
  return request.get('/api/v1/hr/salary-config', { params })
}

export function createSalaryConfig(data) {
  return request.post('/api/v1/hr/salary-config', data)
}

export function updateSalaryConfig(id, data) {
  return request.put(`/api/v1/hr/salary-config/${id}`, data)
}

// 社保配置
export function getSocialInsuranceConfig(params) {
  return request.get('/api/v1/hr/social-insurance', { params })
}

export function updateSocialInsuranceConfig(id, data) {
  return request.put(`/api/v1/hr/social-insurance/${id}`, data)
}

// 个税税率表
export function getTaxRateTable() {
  return request.get('/api/v1/hr/tax-rate')
}

export function updateTaxRateTable(id, data) {
  return request.put(`/api/v1/hr/tax-rate/${id}`, data)
}

// 提交操作
export function submitSalary(id) {
  return request.patch(`/api/v1/hr/salaries/${id}/status`, { status: 'submitted' })
}

export function submitEntry(id) {
  return request.patch(`/api/v1/hr/entries/${id}/status`, { status: 'submitted' })
}

export function submitResign(id) {
  return request.patch(`/api/v1/hr/resigns/${id}/status`, { status: 'submitted' })
}

// 资产移交
export function getAssetTransferList(params) {
  return request.get('/api/v1/hr/asset-transfer', { params })
}

export function createAssetTransfer(data) {
  return request.post('/api/v1/hr/asset-transfer', data)
}

export function submitAssetTransfer(id) {
  return request.patch(`/api/v1/hr/asset-transfer/${id}/status`, { status: 'submitted' })
}

export function submitHrContract(id) {
  return request.patch(`/api/v1/hr/contracts/${id}/status`, { status: 'submitted' })
}

export function submitCertificate(id) {
  return request.patch(`/api/v1/hr/certificates/${id}/status`, { status: 'submitted' })
}

export function submitSalaryConfig(id) {
  return request.patch(`/api/v1/hr/salary-config/${id}/status`, { status: 'submitted' })
}

export function submitSocialInsurance(id) {
  return request.patch(`/api/v1/hr/social-insurance/${id}/status`, { status: 'submitted' })
}
