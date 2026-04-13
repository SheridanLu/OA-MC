<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button type="primary" v-permission="'completion:finish-manage'" @click="openCreate">新建竣工验收</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="project_id" label="项目ID" width="90" />
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="plan_finish_date" label="计划竣工日期" width="130" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" type="warning" link size="small" @click="handleSubmitApproval(row, 'completion')">提交审批</el-button>
          <el-button type="primary" link size="small" v-permission="'completion:finish-manage'" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" v-permission="'completion:finish-manage'" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑竣工验收' : '新建竣工验收'" width="650px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <project-select v-model="form.projectId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划竣工日期">
              <el-date-picker v-model="form.planFinishDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="验收标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="完工内容">
          <el-input v-model="form.finishContent" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="自检结果">
          <el-input v-model="form.selfCheckResult" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="遗留问题">
          <el-input v-model="form.remainingIssues" type="textarea" :rows="2" />
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
import { ElMessageBox, ElMessage } from 'element-plus'
import { getCompletionList, createCompletion, updateCompletion, deleteCompletion } from '@/api/completion'
import { submitApproval } from '@/api/approval'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getCompletionList)

const defaultForm = {
  projectId: null,
  title: '',
  planFinishDate: null,
  finishContent: '',
  selfCheckResult: '',
  remainingIssues: ''
}

const { formRef, submitting, formVisible, isEdit, form, openCreate, openEdit, handleSubmit } =
  useForm(createCompletion, updateCompletion, defaultForm)

const rules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  title: [{ required: true, message: '请输入验收标题', trigger: 'blur' }]
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await deleteCompletion(row.id)
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
