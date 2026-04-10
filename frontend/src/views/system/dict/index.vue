<template>
  <div class="app-container">
    <search-form>
      <el-form :model="query" inline>
        <el-form-item label="字典名称">
          <el-input v-model="query.dictName" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="query.dictType" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </search-form>

    <page-header title="字典管理">
      <el-button v-permission="'system:dict-manage'" type="primary" @click="openCreate">新增</el-button>
    </page-header>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="dict_name" label="字典名称" min-width="150" />
      <el-table-column prop="dict_type" label="字典类型" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="goData(row.dict_type)">{{ row.dict_type }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
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

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑字典类型' : '新增字典类型'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="form.dictType" :disabled="isEdit" placeholder="如 biz_status" />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="form.dictName" placeholder="如 业务状态" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictTypeList, createDictType, updateDictType, deleteDictType } from '@/api/dict'
import { useTable } from '@/composables/useTable'

const router = useRouter()
const { loading, tableData, total, query, fetchData, handleSearch, handleReset, handleSizeChange, handleCurrentChange } = useTable(getDictTypeList, { dictName: '', dictType: '' })

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const editId = ref(null)
const form = reactive({ dictType: '', dictName: '', status: 1, remark: '' })
const rules = {
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }]
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { dictType: '', dictName: '', status: 1, remark: '' })
  formVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { dictType: row.dict_type, dictName: row.dict_name, status: row.status, remark: row.remark || '' })
  formVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDictType(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await createDictType(form)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除字典类型「${row.dict_name}」？关联的字典数据也会被删除。`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

function goData(dictType) {
  router.push(`/system/dict/${dictType}/data`)
}
</script>
