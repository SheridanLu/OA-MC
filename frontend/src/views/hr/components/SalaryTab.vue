<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button v-permission="'hr:salary-manage'" type="primary" @click="openCreate">新建薪资</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="user_id" label="员工ID" width="90" />
      <el-table-column prop="salary_month" label="月份" width="100" />
      <el-table-column label="基本工资" width="120" align="right">
        <template #default="{ row }"><money-text :value="row.base_salary" /></template>
      </el-table-column>
      <el-table-column label="岗位工资" width="120" align="right">
        <template #default="{ row }"><money-text :value="row.position_salary" /></template>
      </el-table-column>
      <el-table-column label="绩效" width="100" align="right">
        <template #default="{ row }"><money-text :value="row.performance" /></template>
      </el-table-column>
      <el-table-column label="补贴" width="100" align="right">
        <template #default="{ row }"><money-text :value="row.allowance" /></template>
      </el-table-column>
      <el-table-column label="奖金" width="100" align="right">
        <template #default="{ row }"><money-text :value="row.bonus" /></template>
      </el-table-column>
      <el-table-column label="扣款" width="100" align="right">
        <template #default="{ row }"><money-text :value="row.deduction" /></template>
      </el-table-column>
      <el-table-column label="实发工资" width="120" align="right">
        <template #default="{ row }"><money-text :value="row.net_salary" /></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" type="warning" link size="small" @click="handleSubmitApproval(row, 'salary')">提交审批</el-button>
          <el-button v-permission="'hr:salary-manage'" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-permission="'hr:salary-manage'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑薪资' : '新建薪资'" width="700px" @closed="onDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="员工ID" prop="userId">
              <el-input-number v-model="form.userId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工资月份" prop="salaryMonth">
              <el-input v-model="form.salaryMonth" placeholder="如: 2026-03" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="基本工资">
              <money-input v-model="form.baseSalary" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="岗位工资">
              <money-input v-model="form.positionSalary" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="绩效">
              <money-input v-model="form.performance" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="补贴">
              <money-input v-model="form.allowance" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="奖金">
              <money-input v-model="form.bonus" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="扣款">
              <money-input v-model="form.deduction" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="社保">
              <money-input v-model="form.socialInsurance" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="个税">
              <money-input v-model="form.tax" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="实发工资">
              <money-input v-model="form.netSalary" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getSalaryList, createSalary, updateSalary, deleteSalary } from '@/api/hr'
import { submitApproval } from '@/api/approval'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const defaultForm = {
  userId: null,
  salaryMonth: '',
  baseSalary: null,
  positionSalary: null,
  performance: null,
  allowance: null,
  bonus: null,
  deduction: null,
  socialInsurance: null,
  tax: null,
  netSalary: null
}

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getSalaryList)
const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } = useForm(createSalary, updateSalary, defaultForm)

const rules = {
  userId: [{ required: true, message: '请输入员工ID', trigger: 'blur' }],
  salaryMonth: [{ required: true, message: '请输入工资月份', trigger: 'blur' }]
}

function handleEdit(row) {
  openEdit({
    id: row.id,
    userId: row.user_id,
    salaryMonth: row.salary_month || '',
    baseSalary: row.base_salary,
    positionSalary: row.position_salary,
    performance: row.performance,
    allowance: row.allowance,
    bonus: row.bonus,
    deduction: row.deduction,
    socialInsurance: row.social_insurance,
    tax: row.tax,
    netSalary: row.net_salary
  })
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await deleteSalary(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleSubmitApproval(row, bizType) {
  await ElMessageBox.confirm('确定提交审批？提交后将进入审批流程。', '提交审批', { type: 'info' })
  await submitApproval({ bizType, bizId: row.id, action: 'submit' })
  ElMessage.success('已提交审批')
  fetchData()
}

function onSubmit() {
  handleSubmit(fetchData)
}

function onDialogClosed() {
  Object.assign(form, { ...defaultForm })
  formRef.value?.resetFields()
}

onMounted(() => fetchData())
</script>
