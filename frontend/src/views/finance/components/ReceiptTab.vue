<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button type="primary" v-permission="'finance:receipt-create'" @click="openCreate">新建收款</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="receipt_no" label="收款编号" width="140" />
      <el-table-column prop="project_name" label="项目名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="contract_name" label="合同名称" min-width="120" show-overflow-tooltip />
      <el-table-column label="收款金额" width="130" align="right">
        <template #default="{ row }"><money-text :value="row.amount" /></template>
      </el-table-column>
      <el-table-column prop="receipt_date" label="收款日期" width="120" />
      <el-table-column prop="payer" label="付款方" width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
    </el-table>

    <el-pagination
      v-if="total > 0"
      style="margin-top: 16px; justify-content: flex-end"
      background
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="query.page"
      @current-change="handleCurrentChange"
    />

    <!-- Dialog -->
    <el-dialog v-model="formVisible" title="新建收款" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <project-select v-model="form.projectId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联合同" prop="contractId">
              <contract-select v-model="form.contractId" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收款金额" prop="amount">
              <money-input v-model="form.amount" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款日期" prop="receiptDate">
              <el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="付款方" prop="payer">
              <el-input v-model="form.payer" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款方式">
              <el-input v-model="form.receiptMethod" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit(fetchData)">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { getReceiptList, createReceipt } from '@/api/finance'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getReceiptList)

const defaultForm = {
  projectId: null,
  contractId: null,
  amount: null,
  receiptDate: null,
  payer: '',
  receiptMethod: '',
  remark: ''
}

const { formRef, submitting, formVisible, form, openCreate, handleSubmit } =
  useForm(createReceipt, null, defaultForm)

const rules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  contractId: [{ required: true, message: '请选择关联合同', trigger: 'change' }],
  amount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }],
  payer: [{ required: true, message: '请输入付款方', trigger: 'blur' }]
}

onMounted(() => fetchData())
</script>
