<template>
  <div>
    <div class="toolbar">
      <el-button v-permission="'finance:reimburse-manage'" type="primary" @click="openCreate">新建报销</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="reimburse_no" label="报销编号" width="140" />
      <el-table-column label="报销类型" width="120">
        <template #default="{ row }">{{ getReimburseTypeLabel(row.reimburse_type) }}</template>
      </el-table-column>
      <el-table-column prop="dept_name" label="部门" width="120" />
      <el-table-column label="金额" width="130" align="right">
        <template #default="{ row }"><money-text :value="row.amount" /></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" type="warning" link size="small" @click="handleSubmitApproval(row, 'reimburse')">提交审批</el-button>
          <el-button v-permission="'finance:reimburse-manage'" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'finance:reimburse-manage'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next"
        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑报销' : '新建报销'" width="550px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="报销类型" prop="reimburseType">
          <el-select v-model="form.reimburseType" style="width: 100%">
            <el-option v-for="opt in reimburseTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报销金额" prop="amount"><money-input v-model="form.amount" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门ID" prop="deptId">
              <el-input-number v-model="form.deptId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="关联项目">
          <project-select v-model="form.projectId" clearable />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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
import { getReimburseList, createReimburse, updateReimburse, deleteReimburse } from '@/api/finance'
import { submitApproval } from '@/api/approval'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'
import { useDict } from '@/composables/useDict'

const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(getReimburseList)
const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } = useForm(createReimburse, updateReimburse, {
  reimburseType: '', amount: null, deptId: null, projectId: null, description: ''
})

const { options: reimburseTypeOptions, getLabel: getReimburseTypeLabel } = useDict('reimburse_type')

const rules = {
  reimburseType: [{ required: true, message: '请选择报销类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入报销金额', trigger: 'blur' }],
  deptId: [{ required: true, message: '请输入部门ID', trigger: 'blur' }]
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
    await deleteReimburse(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

fetchData()
</script>
