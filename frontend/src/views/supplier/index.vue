<template>
  <div class="supplier-page">
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="供应商名称">
          <el-input v-model="queryForm.supplierName" placeholder="请输入" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div style="margin-bottom: 12px">
        <el-button type="primary" v-permission="'purchase:supplier-manage'" @click="handleAdd">新增供应商</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="supplier_name" label="供应商名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="contact_person" label="联系人" width="110" />
        <el-table-column prop="contact_phone" label="联系电话" width="130" />
        <el-table-column prop="tax_no" label="纳税人识别号" width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" v-permission="'purchase:supplier-manage'" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" v-permission="'purchase:supplier-manage'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top: 16px; justify-content: flex-end" background
        layout="total, sizes, prev, pager, next, jumper" :total="total" :page-sizes="[10, 20, 50]"
        v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
        @size-change="fetchData" @current-change="fetchData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑供应商' : '新增供应商'" width="650px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开户行"><el-input v-model="form.bankName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行账号"><el-input v-model="form.bankAccount" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="纳税人识别号"><el-input v-model="form.taxNo" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="active">启用</el-radio>
                <el-radio value="inactive">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSupplierList, createSupplier, updateSupplier, deleteSupplier } from '@/api/supplier'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)

const queryForm = reactive({ supplierName: '', status: '', page: 1, size: 20 })

const form = reactive({
  supplierName: '', contactPerson: '', contactPhone: '', address: '',
  bankName: '', bankAccount: '', taxNo: '', status: 'active', remark: ''
})

const rules = { supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }] }

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSupplierList(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => { queryForm.supplierName = ''; queryForm.status = ''; queryForm.page = 1; fetchData() }

const handleAdd = () => { isEdit.value = false; editId.value = null; dialogVisible.value = true }

const handleEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, {
    supplierName: row.supplier_name, contactPerson: row.contact_person || '',
    contactPhone: row.contact_phone || '', address: row.address || '',
    bankName: row.bank_name || '', bankAccount: row.bank_account || '',
    taxNo: row.tax_no || '', status: row.status, remark: row.remark || ''
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    supplierName: '', contactPerson: '', contactPhone: '', address: '',
    bankName: '', bankAccount: '', taxNo: '', status: 'active', remark: ''
  })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) { await updateSupplier(editId.value, form); ElMessage.success('更新成功') }
    else { await createSupplier(form); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除供应商"${row.supplier_name}"？`, '提示', { type: 'warning' })
  await deleteSupplier(row.id); ElMessage.success('删除成功'); fetchData()
}

onMounted(() => { fetchData() })
</script>

<style scoped>.search-card { margin-bottom: 0; }</style>
