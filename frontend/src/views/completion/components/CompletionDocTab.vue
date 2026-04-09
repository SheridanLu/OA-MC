<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button type="primary" v-permission="'completion:doc-manage'" @click="openCreate">上传竣工资料</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="doc_no" label="资料编号" width="140" />
      <el-table-column prop="project_name" label="项目名称" min-width="130" show-overflow-tooltip />
      <el-table-column prop="doc_name" label="资料名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="doc_type" label="资料类型" width="110" />
      <el-table-column label="状态" width="100">
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
    <el-dialog v-model="formVisible" title="上传竣工资料" width="650px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <project-select v-model="form.projectId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资料名称" prop="docName">
              <el-input v-model="form.docName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="资料类型" prop="docType">
          <el-input v-model="form.docType" />
        </el-form-item>
        <el-form-item label="文件地址" prop="fileUrl">
          <el-input v-model="form.fileUrl" placeholder="上传后自动填入" />
        </el-form-item>
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
import { getCompletionDocList, uploadCompletionDoc } from '@/api/completion'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getCompletionDocList)

const defaultForm = {
  projectId: null,
  docName: '',
  docType: '',
  fileUrl: '',
  remark: ''
}

const { formRef, submitting, formVisible, form, openCreate, handleSubmit } =
  useForm(uploadCompletionDoc, null, defaultForm)

const rules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  docName: [{ required: true, message: '请输入资料名称', trigger: 'blur' }],
  docType: [{ required: true, message: '请输入资料类型', trigger: 'blur' }],
  fileUrl: [{ required: true, message: '请上传文件', trigger: 'blur' }]
}

onMounted(() => fetchData())
</script>
