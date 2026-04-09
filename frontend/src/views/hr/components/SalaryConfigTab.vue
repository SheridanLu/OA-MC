<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button v-permission="'hr:salary-config'" type="primary" @click="openCreate">新建薪资配置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="grade" label="等级" width="80" />
      <el-table-column prop="grade_name" label="等级名称" width="140" />
      <el-table-column label="基本工资" width="140" align="right">
        <template #default="{ row }"><money-text :value="row.base_salary" /></template>
      </el-table-column>
      <el-table-column label="津贴" width="140" align="right">
        <template #default="{ row }"><money-text :value="row.allowance" /></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-permission="'hr:salary-config'" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-permission="'hr:salary-config'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑薪资配置' : '新建薪资配置'" width="600px" @closed="onDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="等级" prop="grade">
              <el-input-number v-model="form.grade" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="等级名称" prop="gradeName">
              <el-input v-model="form.gradeName" placeholder="请输入等级名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="基本工资">
              <money-input v-model="form.baseSalary" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="津贴">
              <money-input v-model="form.allowance" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
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
import { getSalaryConfigList, createSalaryConfig, updateSalaryConfig } from '@/api/hr'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const defaultForm = {
  grade: null,
  gradeName: '',
  baseSalary: null,
  allowance: null,
  remark: ''
}

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getSalaryConfigList)
const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } = useForm(createSalaryConfig, updateSalaryConfig, defaultForm)

const rules = {
  grade: [{ required: true, message: '请输入等级', trigger: 'blur' }],
  gradeName: [{ required: true, message: '请输入等级名称', trigger: 'blur' }]
}

function handleEdit(row) {
  openEdit({
    id: row.id,
    grade: row.grade,
    gradeName: row.grade_name || '',
    baseSalary: row.base_salary,
    allowance: row.allowance,
    remark: row.remark || ''
  })
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该薪资配置？', '提示', { type: 'warning' })
  await updateSalaryConfig(row.id, { deleted: true })
  ElMessage.success('删除成功')
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
