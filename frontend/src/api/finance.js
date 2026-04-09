import request from '@/utils/request'

export function getStatementList(params) {
  return request.get('/api/v1/finance/statements', { params })
}

export function createStatement(data) {
  return request.post('/api/v1/finance/statements', data)
}

export function updateStatement(id, data) {
  return request.put(`/api/v1/finance/statements/${id}`, data)
}

export function deleteStatement(id) {
  return request.delete(`/api/v1/finance/statements/${id}`)
}

export function getPaymentList(params) {
  return request.get('/api/v1/finance/payments', { params })
}

export function createPayment(data) {
  return request.post('/api/v1/finance/payments', data)
}

export function updatePayment(id, data) {
  return request.put(`/api/v1/finance/payments/${id}`, data)
}

export function deletePayment(id) {
  return request.delete(`/api/v1/finance/payments/${id}`)
}

export function getInvoiceList(params) {
  return request.get('/api/v1/finance/invoices', { params })
}

export function createInvoice(data) {
  return request.post('/api/v1/finance/invoices', data)
}

export function updateInvoice(id, data) {
  return request.put(`/api/v1/finance/invoices/${id}`, data)
}

export function deleteInvoice(id) {
  return request.delete(`/api/v1/finance/invoices/${id}`)
}

export function getReimburseList(params) {
  return request.get('/api/v1/finance/reimburses', { params })
}

export function createReimburse(data) {
  return request.post('/api/v1/finance/reimburses', data)
}

export function updateReimburse(id, data) {
  return request.put(`/api/v1/finance/reimburses/${id}`, data)
}

export function deleteReimburse(id) {
  return request.delete(`/api/v1/finance/reimburses/${id}`)
}

export function getCostLedgerList(params) {
  return request.get('/api/v1/finance/cost-ledger', { params })
}

// V3.2 新增
export function submitPayment(id) {
  return request.patch(`/api/v1/finance/payments/${id}/status`, { status: 'submitted' })
}

export function confirmPayment(id) {
  return request.patch(`/api/v1/finance/payments/${id}/status`, { status: 'confirmed' })
}

export function submitReimburse(id) {
  return request.patch(`/api/v1/finance/reimburses/${id}/status`, { status: 'submitted' })
}

export function submitStatement(id) {
  return request.patch(`/api/v1/finance/statements/${id}/status`, { status: 'submitted' })
}

// 收款管理
export function getReceiptList(params) {
  return request.get('/api/v1/finance/receipts', { params })
}

export function createReceipt(data) {
  return request.post('/api/v1/finance/receipts', data)
}

export function submitReceipt(id) {
  return request.patch(`/api/v1/finance/receipts/${id}/status`, { status: 'submitted' })
}

export function submitInvoice(id) {
  return request.patch(`/api/v1/finance/invoices/${id}/status`, { status: 'submitted' })
}

// 费用汇总
export function getCostSummary(params) {
  return request.get('/api/v1/finance/cost-summary', { params })
}
