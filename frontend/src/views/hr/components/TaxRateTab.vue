<template>
  <div v-permission="'hr:salary-config'" v-loading="loading">
    <div style="margin-bottom: 12px">
      <el-button type="primary" :loading="submitting" @click="onSave">保存税率表</el-button>
    </div>

    <el-table :data="tableData" stripe border>
      <el-table-column prop="level" label="级数" width="80" />
      <el-table-column label="最低收入" width="160" align="right">
        <template #default="{ row }"><money-text :value="row.min_income" /></template>
      </el-table-column>
      <el-table-column label="最高收入" width="160" align="right">
        <template #default="{ row }"><money-text :value="row.max_income" /></template>
      </el-table-column>
      <el-table-column label="税率(%)" width="120" align="right">
        <template #default="{ row }">{{ row.rate }}%</template>
      </el-table-column>
      <el-table-column label="速算扣除数" width="160" align="right">
        <template #default="{ row }"><money-text :value="row.deduction" /></template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row, $index }">
          <el-button type="primary" link size="small" @click="handleEdit(row, $index)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Edit Dialog -->
    <el-dialog v-model="editVisible" title="编辑税率" width="500px" @closed="onDialogClosed">
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="级数">
          <el-input-number v-model="editForm.level" :min="1" disabled controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最低收入" prop="minIncome">
          <money-input v-model="editForm.minIncome" />
        </el-form-item>
        <el-form-item label="最高收入" prop="maxIncome">
          <money-input v-model="editForm.maxIncome" />
        </el-form-item>
        <el-form-item label="税率(%)" prop="rate">
          <el-input-number v-model="editForm.rate" :min="0" :max="100" :precision="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="速算扣除数" prop="deduction">
          <money-input v-model="editForm.deduction" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="onConfirmEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaxRateTable, updateTaxRateTable } from '@/api/hr'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])

const editVisible = ref(false)
const editIndex = ref(-1)
const formRef = ref(null)

const editForm = reactive({
  level: null,
  minIncome: null,
  maxIncome: null,
  rate: null,
  deduction: null
})

const rules = {
  minIncome: [{ required: true, message: '请输入最低收入', trigger: 'blur' }],
  maxIncome: [{ required: true, message: '请输入最高收入', trigger: 'blur' }],
  rate: [{ required: true, message: '请输入税率', trigger: 'blur' }],
  deduction: [{ required: true, message: '请输入速算扣除数', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTaxRateTable()
    tableData.value = (res.data && res.data.records) ? res.data.records : []
  } finally {
    loading.value = false
  }
}

function handleEdit(row, index) {
  editIndex.value = index
  Object.assign(editForm, {
    level: row.level,
    minIncome: row.min_income,
    maxIncome: row.max_income,
    rate: row.rate,
    deduction: row.deduction
  })
  editVisible.value = true
}

function onConfirmEdit() {
  formRef.value.validate((valid) => {
    if (!valid) return
    const row = tableData.value[editIndex.value]
    row.level = editForm.level
    row.min_income = editForm.minIncome
    row.max_income = editForm.maxIncome
    row.rate = editForm.rate
    row.deduction = editForm.deduction
    editVisible.value = false
  })
}

async function onSave() {
  submitting.value = true
  try {
    await updateTaxRateTable(tableData.value)
    ElMessage.success('保存成功')
    loadData()
  } finally {
    submitting.value = false
  }
}

function onDialogClosed() {
  editIndex.value = -1
}

onMounted(() => loadData())
</script>
