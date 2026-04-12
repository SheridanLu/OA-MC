<template>
  <div class="app-container">
    <search-form>
      <el-form inline>
        <el-form-item label="字典类型">
          <el-tag>{{ dictType }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">刷新</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </search-form>

    <page-header :title="`字典数据 - ${dictType}`">
      <el-button v-permission="'system:dict-manage'" type="primary" @click="openCreate">新增</el-button>
    </page-header>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="dict_label" label="字典标签" min-width="150" />
      <el-table-column prop="dict_value" label="字典值" min-width="120" />
      <el-table-column prop="dict_sort" label="排序" width="80" align="center" />
      <el-table-column prop="list_class" label="Tag类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.list_class" :type="row.list_class" size="small">{{ row.dict_label }}</el-tag>
          <span v-else>{{ row.dict_label }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="color_hex" label="颜色" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.color_hex" :style="{ color: row.color_hex, fontWeight: 'bold' }">&#9632; {{ row.color_hex }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:dict-manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'system:dict-manage'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0" :current-page="query.page" :page-size="query.size"
      :total="total" layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50]" @size-change="handleSizeChange" @current-change="handleCurrentChange"
      style="margin-top: 16px; justify-content: flex-end;"
    />

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑字典数据' : '新增字典数据'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="存储值" />
        </el-form-item>
        <el-form-item label="排序" prop="dictSort">
          <el-input-number v-model="form.dictSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="Tag类型">
          <el-select v-model="form.listClass" clearable placeholder="Element Plus Tag type">
            <el-option label="default" value="" />
            <el-option label="success" value="success" />
            <el-option label="warning" value="warning" />
            <el-option label="danger" value="danger" />
            <el-option label="info" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.colorHex" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictDataList, createDictData, updateDictData, deleteDictData } from '@/api/dict'
import { useTable } from '@/composables/useTable'
import { useDictStore } from '@/stores/dict'

const route = useRoute()
const router = useRouter()
const dictType = route.params.dictType
const dictStore = useDictStore()

const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(
  (params) => getDictDataList({ ...params, dictType }),
  {}
)

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editId = ref(null)
const form = reactive({
  dictLabel: '', dictValue: '', dictSort: 0, listClass: '', colorHex: '', status: 1, remark: ''
})
const rules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { dictLabel: '', dictValue: '', dictSort: 0, listClass: '', colorHex: '', status: 1, remark: '' })
  formVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    dictLabel: row.dict_label, dictValue: row.dict_value, dictSort: row.dict_sort || 0,
    listClass: row.list_class || '', colorHex: row.color_hex || '', status: row.status, remark: row.remark || ''
  })
  formVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form, dictType }
    if (isEdit.value) {
      await updateDictData(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createDictData(payload)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    dictStore.invalidate(dictType)
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.dict_label}」？`, '提示', { type: 'warning' })
  await deleteDictData(row.id)
  ElMessage.success('删除成功')
  dictStore.invalidate(dictType)
  fetchData()
}

onMounted(fetchData)
</script>
