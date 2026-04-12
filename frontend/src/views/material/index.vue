<template>
  <div class="material-page">
    <!-- 搜索区 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="材料名称">
          <el-input v-model="queryForm.materialName" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 数据表格 -->
    <el-card shadow="never" style="margin-top: 12px">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="handleAdd" v-permission="'material:edit'">新增材料</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="material_code" label="编码" width="140" />
        <el-table-column prop="material_name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="spec_model" label="规格型号" width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="base_price_with_tax" label="含税基准价" width="120" align="right">
          <template #default="{ row }">{{ row.base_price_with_tax != null ? Number(row.base_price_with_tax).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="tax_rate" label="税率" width="80" align="center">
          <template #default="{ row }">{{ row.tax_rate != null ? row.tax_rate + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)" v-permission="'material:edit'">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)" v-permission="'material:edit'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top: 16px; justify-content: flex-end" background
        layout="total, sizes, prev, pager, next, jumper" :total="total" :page-sizes="[10, 20, 50]"
        v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
        @size-change="fetchData" @current-change="fetchData" />
    </el-card>

    <!-- 批量新增弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量新增材料" width="1100px"
      :close-on-click-modal="false" @close="handleBatchClose">
      <div class="batch-table-wrapper">
        <el-table :data="batchRows" border size="small" :row-class-name="batchRowClassName">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column label="名称 *" min-width="160">
            <template #default="{ row, $index }">
              <el-input v-model="row.materialName" placeholder="材料名称" :maxlength="100"
                :class="{ 'is-error': hasRowError($index, 'material_name') }" />
            </template>
          </el-table-column>
          <el-table-column label="规格型号" width="140">
            <template #default="{ row }">
              <el-input v-model="row.specModel" placeholder="选填" :maxlength="200" />
            </template>
          </el-table-column>
          <el-table-column label="分类 *" width="110">
            <template #default="{ row, $index }">
              <el-select v-model="row.category" placeholder="选择"
                :class="{ 'is-error': hasRowError($index, 'category') }">
                <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="单位 *" width="100">
            <template #default="{ row, $index }">
              <el-select v-model="row.unit" placeholder="选择"
                :class="{ 'is-error': hasRowError($index, 'unit') }">
                <el-option v-for="u in UNITS" :key="u" :label="u" :value="u" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="含税基准价 *" width="140">
            <template #default="{ row, $index }">
              <el-input-number v-model="row.basePriceWithTax" :precision="2" :min="0"
                controls-position="right" style="width: 100%"
                :class="{ 'is-error': hasRowError($index, 'base_price_with_tax') }" />
            </template>
          </el-table-column>
          <el-table-column label="税率 *" width="100">
            <template #default="{ row, $index }">
              <el-select v-model="row.taxRate" placeholder="选择"
                :class="{ 'is-error': hasRowError($index, 'tax_rate') }">
                <el-option v-for="r in TAX_RATES" :key="r" :label="r + '%'" :value="r" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="" width="60" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link size="small" @click="removeBatchRow($index)"
                :disabled="batchRows.length <= 1">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 行级错误提示 -->
      <div v-if="batchErrors.length > 0" class="batch-errors">
        <el-alert type="error" :closable="false">
          <template #title>
            <div v-for="err in batchErrors" :key="err.row + err.field">
              第{{ err.row }}行 [{{ err.field }}]: {{ err.message }}
            </div>
          </template>
        </el-alert>
      </div>

      <div style="margin-top: 12px">
        <el-button @click="addBatchRow">+ 添加行</el-button>
      </div>

      <template #footer>
        <el-button @click="handleBatchCancel">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="handleBatchSubmit">
          批量保存({{ validBatchCount }}条)
        </el-button>
      </template>
    </el-dialog>

    <!-- 单条编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑材料" width="650px" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="材料名称" prop="materialName">
          <el-input v-model="editForm.materialName" :maxlength="100" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="editForm.specModel" :maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="editForm.category" style="width: 100%">
                <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="editForm.unit" style="width: 100%">
                <el-option v-for="u in UNITS" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="含税基准价" prop="basePriceWithTax">
              <el-input-number v-model="editForm.basePriceWithTax" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="税率" prop="taxRate">
              <el-select v-model="editForm.taxRate" style="width: 100%">
                <el-option v-for="r in TAX_RATES" :key="r" :label="r + '%'" :value="r" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option label="启用" value="active" />
                <el-option label="停用" value="inactive" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getMaterialList, batchCreateMaterial, updateMaterial, deleteMaterial } from '@/api/material'

// ===== 枚举常量（前端硬编码，与后端一致）=====
const CATEGORIES = ['设备', '材料', '人工']
const UNITS = ['米', '吨', '卷', '箱', '台', '项', '只', '套']
const TAX_RATES = [0, 1, 3, 6, 9, 13]

// ===== 列表 =====
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryForm = reactive({ materialName: '', category: '', page: 1, size: 20 })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMaterialList(queryForm)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => {
  queryForm.materialName = ''; queryForm.category = ''; queryForm.page = 1; fetchData()
}

// ===== 批量新增 =====
const batchDialogVisible = ref(false)
const batchSubmitting = ref(false)
const batchErrors = ref([])
const batchRows = ref([])

const createEmptyRow = () => ({
  materialName: '',
  specModel: '',
  category: '材料',
  unit: '项',
  basePriceWithTax: null,
  taxRate: 13
})

const handleAdd = () => {
  batchRows.value = [createEmptyRow()]
  batchErrors.value = []
  batchDialogVisible.value = true
}

const addBatchRow = () => {
  batchRows.value.push(createEmptyRow())
}

const removeBatchRow = (index) => {
  if (batchRows.value.length > 1) {
    batchRows.value.splice(index, 1)
    // 清除该行错误
    batchErrors.value = batchErrors.value.filter(e => e.row !== index + 1)
  }
}

const validBatchCount = computed(() => {
  return batchRows.value.filter(r => r.materialName && r.materialName.trim()).length
})

const hasRowError = (index, field) => {
  return batchErrors.value.some(e => e.row === index + 1 && e.field === field)
}

const batchRowClassName = ({ rowIndex }) => {
  return batchErrors.value.some(e => e.row === rowIndex + 1) ? 'error-row' : ''
}

// 前端校验
const validateBatchRows = () => {
  const errors = []
  const seen = new Map()

  batchRows.value.forEach((row, i) => {
    const rowNum = i + 1
    // 跳过全空行
    if (!row.materialName && !row.specModel && row.basePriceWithTax == null) return

    if (!row.materialName || row.materialName.trim().length < 2) {
      errors.push({ row: rowNum, field: 'material_name', message: '请输入材料名称(2~100字)' })
    }
    if (!row.category) {
      errors.push({ row: rowNum, field: 'category', message: '请选择材料分类' })
    }
    if (!row.unit) {
      errors.push({ row: rowNum, field: 'unit', message: '请选择计量单位' })
    }
    if (row.basePriceWithTax == null || row.basePriceWithTax < 0) {
      errors.push({ row: rowNum, field: 'base_price_with_tax', message: '请输入含税基准价' })
    }
    if (row.taxRate == null) {
      errors.push({ row: rowNum, field: 'tax_rate', message: '请选择税率' })
    }

    // 行内去重
    const key = `${row.materialName}|${row.specModel || ''}|${row.unit}`
    if (seen.has(key)) {
      errors.push({ row: rowNum, field: 'material_name', message: `与第${seen.get(key)}行重复(同名+同规格+同单位)` })
    } else {
      seen.set(key, rowNum)
    }
  })

  return errors
}

const handleBatchSubmit = async () => {
  // 过滤全空行
  const validRows = batchRows.value.filter(r => r.materialName && r.materialName.trim())
  if (validRows.length === 0) {
    ElMessage.warning('请至少填写一行有效数据')
    return
  }

  // 前端校验
  const frontErrors = validateBatchRows()
  if (frontErrors.length > 0) {
    batchErrors.value = frontErrors
    ElMessage.warning('请检查标红行')
    return
  }

  batchSubmitting.value = true
  batchErrors.value = []
  try {
    const res = await batchCreateMaterial({ items: validRows })
    const data = res.data

    if (data.fail_count === 0) {
      ElMessage.success(`成功新增${data.success_count}条材料`)
      batchDialogVisible.value = false
      fetchData()
    } else if (data.success_count > 0) {
      ElMessage.warning(`成功${data.success_count}条，失败${data.fail_count}条，请检查标红行`)
      batchErrors.value = data.errors || []
      // 移除成功行，保留失败行
      const errorRowSet = new Set((data.errors || []).map(e => e.row))
      batchRows.value = batchRows.value.filter((_, i) => errorRowSet.has(i + 1))
      // 重新映射行号
      batchErrors.value = batchErrors.value.map((e, idx) => ({ ...e, row: idx + 1 }))
      fetchData()
    } else {
      ElMessage.error('提交失败，请检查标红行')
      batchErrors.value = data.errors || []
    }
  } catch (e) {
    ElMessage.error('网络异常，请重试')
  } finally {
    batchSubmitting.value = false
  }
}

const handleBatchCancel = () => {
  const hasData = batchRows.value.some(r => r.materialName && r.materialName.trim())
  if (hasData) {
    ElMessageBox.confirm('已填写的数据将丢失，确定取消？', '提示', { type: 'warning' })
      .then(() => { batchDialogVisible.value = false })
      .catch(() => {})
  } else {
    batchDialogVisible.value = false
  }
}

const handleBatchClose = () => {
  batchRows.value = [createEmptyRow()]
  batchErrors.value = []
}

// ===== 单条编辑 =====
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const editId = ref(null)

const editForm = reactive({
  materialName: '', specModel: '', category: '材料', unit: '项',
  basePriceWithTax: null, taxRate: 13, status: 'active'
})

const editRules = {
  materialName: [{ required: true, message: '请输入材料名称', trigger: 'blur' },
                  { min: 2, max: 100, message: '长度2~100字', trigger: 'blur' }],
  category: [{ required: true, message: '请选择材料分类', trigger: 'change' }],
  unit: [{ required: true, message: '请选择计量单位', trigger: 'change' }],
  basePriceWithTax: [{ required: true, message: '请输入含税基准价', trigger: 'blur' }],
  taxRate: [{ required: true, message: '请选择税率', trigger: 'change' }]
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(editForm, {
    materialName: row.material_name,
    specModel: row.spec_model || '',
    category: row.category || '材料',
    unit: row.unit || '项',
    basePriceWithTax: row.base_price_with_tax,
    taxRate: row.tax_rate != null ? row.tax_rate : 13,
    status: row.status || 'active'
  })
  editDialogVisible.value = true
}

const resetEditForm = () => {
  Object.assign(editForm, {
    materialName: '', specModel: '', category: '材料', unit: '项',
    basePriceWithTax: null, taxRate: 13, status: 'active'
  })
  editFormRef.value?.resetFields()
}

const handleEditSubmit = async () => {
  await editFormRef.value.validate()
  editSubmitting.value = true
  try {
    await updateMaterial(editId.value, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchData()
  } finally { editSubmitting.value = false }
}

// ===== 删除 =====
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除材料"${row.material_name}"？`, '提示', { type: 'warning' })
  await deleteMaterial(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.search-card { margin-bottom: 0; }
.batch-table-wrapper { max-height: 400px; overflow-y: auto; }
.batch-errors { margin-top: 10px; }
:deep(.error-row) { background-color: #fef0f0 !important; }
:deep(.is-error .el-input__wrapper),
:deep(.is-error .el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
}
</style>
