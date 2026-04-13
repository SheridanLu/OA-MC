<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button type="primary" v-permission="'completion:labor-manage'" @click="openCreate">新建劳务结算</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="project_id" label="项目ID" width="90" />
      <el-table-column prop="settlement_no" label="结算编号" width="140" />
      <el-table-column prop="settlement_amount" label="结算金额" width="130" align="right">
        <template #default="{ row }"><money-text :value="row.settlement_amount" /></template>
      </el-table-column>
      <el-table-column prop="paid_amount" label="已付金额" width="130" align="right">
        <template #default="{ row }"><money-text :value="row.paid_amount" /></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" type="warning" link size="small" @click="handleSubmitApproval(row, 'labor_settlement')">提交审批</el-button>
          <el-button type="primary" link size="small" v-permission="'completion:labor-manage'" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" v-permission="'completion:labor-manage'" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
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
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑劳务结算' : '新建劳务结算'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <project-select v-model="form.projectId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="劳务合同ID" prop="contractId">
              <el-input-number v-model="form.contractId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="结算金额" prop="settlementAmount">
              <money-input v-model="form.settlementAmount" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="已付金额">
              <money-input v-model="form.paidAmount" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="申请付款" prop="applyPayAmount">
              <money-input v-model="form.applyPayAmount" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { ElMessageBox, ElMessage } from 'element-plus'
import { getLaborList, createLabor, updateLabor, deleteLabor } from '@/api/completion'
import { submitApproval } from '@/api/approval'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getLaborList)

const defaultForm = {
  projectId: null,
  contractId: null,
  settlementAmount: null,
  paidAmount: null,
  applyPayAmount: null
}

const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } =
  useForm(createLabor, updateLabor, defaultForm)

const rules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  contractId: [{ required: true, message: '请输入劳务合同ID', trigger: 'blur' }],
  settlementAmount: [{ required: true, message: '请输入结算金额', trigger: 'blur' }],
  applyPayAmount: [{ required: true, message: '请输入申请付款金额', trigger: 'blur' }]
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await deleteLabor(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleSubmitApproval(row, bizType) {
  await ElMessageBox.confirm('确定提交审批？提交后将进入审批流程。', '提交审批', { type: 'info' })
  await submitApproval({ bizType, bizId: row.id, action: 'submit' })
  ElMessage.success('已提交审批')
  fetchData()
}

onMounted(() => fetchData())
</script>
