<template>
  <div class="app-container">
    <page-header :title="`编辑代码生成配置 - ${tableInfo.table_name}`">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </page-header>

    <!-- 基础配置 -->
    <el-card style="margin-bottom: 16px;">
      <template #header>基础配置</template>
      <el-form :model="tableForm" label-width="100px" inline>
        <el-form-item label="所属模块">
          <el-input v-model="tableForm.moduleName" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="业务名">
          <el-input v-model="tableForm.bizName" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="类名">
          <el-input v-model="tableForm.className" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="tableForm.author" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select v-model="tableForm.templateType" style="width: 120px;">
            <el-option :value="1" label="单表" />
            <el-option :value="2" label="主子表" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列配置 -->
    <el-card>
      <template #header>字段配置</template>
      <el-table :data="columns" border stripe max-height="500">
        <el-table-column prop="column_name" label="字段名" width="160" />
        <el-table-column label="注释" width="140">
          <template #default="{ row }">
            <el-input v-model="row.columnComment" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="Java类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.javaType" size="small">
              <el-option value="String" label="String" />
              <el-option value="Integer" label="Integer" />
              <el-option value="Long" label="Long" />
              <el-option value="Double" label="Double" />
              <el-option value="java.math.BigDecimal" label="BigDecimal" />
              <el-option value="java.time.LocalDateTime" label="LocalDateTime" />
              <el-option value="java.time.LocalDate" label="LocalDate" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Java字段" width="130">
          <template #default="{ row }">
            <el-input v-model="row.javaField" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="前端组件" width="110">
          <template #default="{ row }">
            <el-select v-model="row.htmlType" size="small">
              <el-option value="input" label="输入框" />
              <el-option value="select" label="下拉选" />
              <el-option value="datetime" label="日期时间" />
              <el-option value="textarea" label="文本域" />
              <el-option value="upload" label="上传" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="字典类型" width="130">
          <template #default="{ row }">
            <el-input v-model="row.dictType" size="small" placeholder="如 biz_status" />
          </template>
        </el-table-column>
        <el-table-column label="新增" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.createOperation" :true-value="1" :false-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="编辑" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.updateOperation" :true-value="1" :false-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="列表" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.listOperation" :true-value="1" :false-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="查询" width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.queryOperation" :true-value="1" :false-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="查询方式" width="110">
          <template #default="{ row }">
            <el-select v-if="row.queryOperation" v-model="row.queryCondition" size="small">
              <el-option value="EQ" label="等于" />
              <el-option value="LIKE" label="模糊" />
              <el-option value="BETWEEN" label="区间" />
              <el-option value="GT" label="大于" />
              <el-option value="LT" label="小于" />
            </el-select>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCodegenTableDetail, updateCodegenTable, getCodegenColumnList, updateCodegenColumns } from '@/api/codegen'

const route = useRoute()
const router = useRouter()
const tableId = parseInt(route.params.id)

const tableInfo = ref({})
const tableForm = reactive({ moduleName: '', bizName: '', className: '', author: '', templateType: 1 })
const columns = ref([])
const saving = ref(false)

onMounted(async () => {
  const res = await getCodegenTableDetail(tableId)
  tableInfo.value = res.data
  Object.assign(tableForm, {
    moduleName: res.data.module_name,
    bizName: res.data.biz_name,
    className: res.data.class_name,
    author: res.data.author,
    templateType: res.data.template_type
  })
  const colRes = await getCodegenColumnList(tableId)
  columns.value = colRes.data || []
})

async function handleSave() {
  saving.value = true
  try {
    await updateCodegenTable(tableId, tableForm)
    await updateCodegenColumns(tableId, columns.value)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}
</script>
