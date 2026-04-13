<template>
  <div>
    <div class="toolbar">
      <el-button v-permission="'finance:statement-manage'" type="primary" @click="openCreate">新建结算单</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="statement_no" label="结算编号" width="140" />
      <el-table-column prop="contract_id" label="合同ID" width="90" />
      <el-table-column prop="period" label="期间" width="90" />
      <el-table-column label="本期产值" width="130" align="right">
        <template #default="{ row }"><money-text :value="row.current_output" /></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" type="warning" link size="small" @click="handleSubmitApproval(row, 'statement')">提交审批</el-button>
          <el-button v-permission="'finance:statement-manage'" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'finance:statement-manage'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next"
        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑结算单' : '新建结算单'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId"><project-select v-model="form.projectId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联合同" prop="contractId"><contract-select v-model="form.contractId" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="期间" prop="period">
              <el-input v-model="form.period" placeholder="如: 2026-03" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合同含税金额" prop="contractAmount">
              <money-input v-model="form.contractAmount" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="形象进度(%)">
              <el-input-number v-model="form.progressRatio" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="本期产值">
              <money-input v-model="form.currentOutput" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="累计产值">
              <money-input v-model="form.cumulativeOutput" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="本期收款">
              <money-input v-model="form.currentCollection" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="累计收款">
              <money-input v-model="form.cumulativeCollection" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit(onSuccess)">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getStatementList, createStatement, updateStatement, deleteStatement } from '@/api/finance'
import { submitApproval } from '@/api/approval'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(getStatementList)
const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } = useForm(createStatement, updateStatement, {
  projectId: null, contractId: null, period: '', contractAmount: null,
  progressRatio: null, currentOutput: null, cumulativeOutput: null,
  currentCollection: null, cumulativeCollection: null
})

const rules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  contractId: [{ required: true, message: '请选择关联合同', trigger: 'change' }],
  period: [{ required: true, message: '请输入期间', trigger: 'blur' }],
  contractAmount: [{ required: true, message: '请输入合同含税金额', trigger: 'blur' }]
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
    await deleteStatement(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

fetchData()
</script>
