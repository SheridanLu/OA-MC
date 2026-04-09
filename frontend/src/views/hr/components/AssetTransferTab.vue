<template>
  <div>
    <div style="margin-bottom: 12px">
      <el-button v-permission="'hr:asset-transfer'" type="primary" @click="openCreate">新建资产交接</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="transfer_no" label="交接单号" width="160" />
      <el-table-column prop="user_name" label="员工姓名" width="110" />
      <el-table-column prop="asset_name" label="资产名称" width="140" />
      <el-table-column prop="asset_code" label="资产编码" width="130" />
      <el-table-column prop="transfer_type" label="交接类型" width="100" />
      <el-table-column prop="transfer_date" label="交接日期" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column prop="created_at" label="创建时间" width="170" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'draft' || row.status === 0"
            v-permission="'hr:asset-transfer'"
            type="success"
            link
            size="small"
            @click="handleSubmit(row)"
          >提交</el-button>
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
    <el-dialog v-model="formVisible" title="新建资产交接" width="600px" @closed="onDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="员工ID" prop="userId">
          <el-input-number v-model="form.userId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName">
              <el-input v-model="form.assetName" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产编码" prop="assetCode">
              <el-input v-model="form.assetCode" placeholder="请输入资产编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="交接类型" prop="transferType">
              <el-select v-model="form.transferType" placeholder="请选择类型" style="width: 100%">
                <el-option label="入职交接" value="entry" />
                <el-option label="离职交接" value="resign" />
                <el-option label="调岗交接" value="transfer" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交接日期" prop="transferDate">
              <el-date-picker v-model="form.transferDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onFormSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getAssetTransferList, createAssetTransfer, submitAssetTransfer } from '@/api/hr'
import { useTable } from '@/composables/useTable'

const defaultForm = {
  userId: null,
  assetName: '',
  assetCode: '',
  transferType: '',
  transferDate: '',
  remark: ''
}

const { loading, tableData, total, query, fetchData, handleCurrentChange } = useTable(getAssetTransferList)

const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ ...defaultForm })

const rules = {
  userId: [{ required: true, message: '请输入员工ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  assetCode: [{ required: true, message: '请输入资产编码', trigger: 'blur' }],
  transferType: [{ required: true, message: '请选择交接类型', trigger: 'change' }],
  transferDate: [{ required: true, message: '请选择交接日期', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, { ...defaultForm })
  formVisible.value = true
}

async function onFormSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await createAssetTransfer(form)
    ElMessage.success('创建成功')
    formVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleSubmit(row) {
  await ElMessageBox.confirm('确定提交该资产交接单？', '提示', { type: 'warning' })
  await submitAssetTransfer(row.id)
  ElMessage.success('提交成功')
  fetchData()
}

function onDialogClosed() {
  Object.assign(form, { ...defaultForm })
  formRef.value?.resetFields()
}

onMounted(() => fetchData())
</script>
