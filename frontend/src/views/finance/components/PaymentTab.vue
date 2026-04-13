<template>
  <div>
    <div class="toolbar">
      <el-button v-permission="'finance:payment-create'" type="primary" @click="openCreate">新建付款申请</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="payment_no" label="付款编号" width="140" />
      <el-table-column prop="contract_id" label="合同ID" width="90" />
      <el-table-column label="付款类型" width="120">
        <template #default="{ row }">{{ getPaymentTypeLabel(row.payment_type) }}</template>
      </el-table-column>
      <el-table-column prop="payee_name" label="收款方" width="130" />
      <el-table-column label="申请金额" width="130" align="right">
        <template #default="{ row }"><money-text :value="row.amount" /></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" type="warning" link size="small" @click="handleSubmitApproval(row, 'payment')">提交审批</el-button>
          <el-button v-permission="'finance:payment-create'" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'finance:payment-create'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next"
        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑付款申请' : '新建付款申请'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId"><project-select v-model="form.projectId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款类型" prop="paymentType">
              <el-select v-model="form.paymentType" style="width: 100%">
                <el-option v-for="opt in paymentTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="合同" prop="contractId"><contract-select v-model="form.contractId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款金额" prop="amount"><money-input v-model="form.amount" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="收款方" prop="payeeName">
              <el-input v-model="form.payeeName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开户行">
              <el-input v-model="form.payeeBank" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="银行账号">
              <el-input v-model="form.payeeAccount" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit(onSuccess)">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getPaymentList, createPayment, updatePayment, deletePayment } from '@/api/finance'
import { submitApproval } from '@/api/approval'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'
import { useDict } from '@/composables/useDict'

const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(getPaymentList)
const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } = useForm(createPayment, updatePayment, {
  projectId: null, paymentType: '', contractId: null, amount: null,
  payeeName: '', payeeBank: '', payeeAccount: '', remark: ''
})

const { options: paymentTypeOptions, getLabel: getPaymentTypeLabel } = useDict('payment_type')

const rules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  paymentType: [{ required: true, message: '请选择付款类型', trigger: 'change' }],
  contractId: [{ required: true, message: '请选择合同', trigger: 'change' }],
  amount: [{ required: true, message: '请输入付款金额', trigger: 'blur' }],
  payeeName: [{ required: true, message: '请输入收款方名称', trigger: 'blur' }]
}

const onSuccess = () => fetchData()

const handleSubmitApproval = async (row, bizType) => {
  await ElMessageBox.confirm('确定提交审批？提交后将进入审批流程。', '提交审批', { type: 'info' })
  await submitApproval({ bizType, bizId: row.id, action: 'submit' })
  ElMessage.success('已提交审批')
  fetchData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(async () => {
    await deletePayment(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

fetchData()
</script>
