<template>
  <div class="contract-tpl-page">
    <el-card shadow="never">
      <div style="display: flex; justify-content: space-between; margin-bottom: 12px">
        <el-form :model="queryForm" inline>
          <el-form-item label="合同类型">
            <el-select v-model="queryForm.contractType" placeholder="全部" clearable style="width: 180px">
              <el-option v-for="t in contractTypes" :key="t.code" :label="t.label" :value="t.code" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchData">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="handleAdd" v-permission="'system:tpl-manage'">新建模板</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="contract_type" label="合同类型" width="160">
          <template #default="{ row }">{{ typeLabel(row.contract_type) }}</template>
        </el-table-column>
        <el-table-column prop="tpl_name" label="模板名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleUploadVersion(row)" v-permission="'system:tpl-manage'">上传版本</el-button>
            <el-button type="success" link size="small" @click="handleViewVersions(row)">版本列表</el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)" v-permission="'system:tpl-manage'">编辑</el-button>
            <el-button type="info" link size="small" @click="handleAuditLog(row)">日志</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)" v-permission="'system:tpl-manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top: 16px; justify-content: flex-end" background
        layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10, 20, 50]"
        v-model:current-page="queryForm.page" v-model:page-size="queryForm.size"
        @size-change="fetchData" @current-change="fetchData" />
    </el-card>

    <!-- 新建/编辑模板 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑模板' : '新建模板'" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="合同类型" prop="contractType">
          <el-select v-model="form.contractType" style="width: 100%" :disabled="isEdit">
            <el-option v-for="t in contractTypes" :key="t.code" :label="t.label" :value="t.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称" prop="tplName">
          <el-input v-model="form.tplName" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="模板文件" v-if="!isEdit">
          <el-upload ref="createUploadRef" drag :auto-upload="false" :limit="1" accept=".docx"
            :on-change="(f) => createFile = f.raw" :on-remove="() => createFile = null">
            <el-icon style="font-size: 40px; color: #909399"><Upload /></el-icon>
            <div>将 .docx 模板文件拖到此处，或点击上传</div>
            <template #tip><div class="el-upload__tip">仅支持 .docx 格式，文件中使用 {{字段名}} 标记可编辑字段</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传版本 -->
    <el-dialog v-model="uploadVisible" title="上传模板版本" width="500px">
      <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" accept=".docx"
        :on-change="(f) => uploadFile = f.raw">
        <el-icon style="font-size: 40px; color: #909399"><Upload /></el-icon>
        <div>将 .docx 模板文件拖到此处，或点击上传</div>
        <template #tip><div class="el-upload__tip">仅支持 .docx 格式，文件中使用 {{字段名}} 标记可编辑字段</div></template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUploadVersion">上传</el-button>
      </template>
    </el-dialog>

    <!-- 版本列表 -->
    <el-dialog v-model="versionsVisible" title="版本列表" width="750px">
      <el-table :data="versions" stripe border>
        <el-table-column prop="version_no" label="版本" width="70" />
        <el-table-column prop="file_name" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="file_md5" label="MD5" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="上传时间" width="170" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" type="success" link size="small" @click="toggleVersion(row, 1)" v-permission="'system:tpl-manage'">启用</el-button>
            <el-button v-else type="warning" link size="small" @click="toggleVersion(row, 0)" v-permission="'system:tpl-manage'">停用</el-button>
            <el-button v-if="row.status !== 1" type="primary" link size="small" @click="handleSubmitApproval(row)" v-permission="'system:tpl-manage'">提交审批</el-button>
            <el-button type="primary" link size="small" @click="handlePreview(row)">预览</el-button>
            <el-button type="info" link size="small" @click="handleViewFields(row)">字段</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 字段定义 -->
    <el-dialog v-model="fieldsVisible" title="模板字段定义" width="800px">
      <el-table :data="fieldDefs" stripe border>
        <el-table-column prop="field_key" label="字段标识" width="130" />
        <el-table-column prop="field_name" label="中文名" width="130">
          <template #default="{ row }">
            <el-input v-model="row.field_name" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="field_type" label="类型" width="110">
          <template #default="{ row }">
            <el-select v-model="row.field_type" size="small">
              <el-option label="文本" value="text" />
              <el-option label="数字" value="number" />
              <el-option label="日期" value="date" />
              <el-option label="下拉" value="select" />
              <el-option label="多行文本" value="textarea" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="required" label="必填" width="70">
          <template #default="{ row }">
            <el-switch v-model="row.required" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column prop="max_length" label="最大长度" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.max_length" size="small" :min="0" controls-position="right" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column prop="placeholder" label="占位提示" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.placeholder" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="fieldsVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingFields" @click="handleSaveFields">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="previewVisible" title="模板预览" width="80%">
      <div class="tpl-preview" v-html="previewHtml"></div>
    </el-dialog>

    <!-- 审计日志 -->
    <el-dialog v-model="auditVisible" title="模板审计日志" width="700px">
      <el-table :data="auditLogs" stripe border>
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="detail" label="详情" min-width="250" show-overflow-tooltip />
        <el-table-column prop="operator_id" label="操作人" width="90" />
        <el-table-column prop="operated_at" label="操作时间" width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getContractTypes, getTplList, createTpl, createTplWithFile, updateTpl, deleteTpl,
         uploadTplVersion, getTplVersions, updateVersionStatus, submitVersionApproval,
         previewVersion, getVersionFields, updateVersionFields, getTplAuditLogs } from '@/api/contractTpl'

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const savingFields = ref(false)
const tableData = ref([])
const total = ref(0)
const contractTypes = ref([])
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)
const createUploadRef = ref(null)
const createFile = ref(null)

const uploadVisible = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)
const uploadTplId = ref(null)

const versionsVisible = ref(false)
const versions = ref([])
const currentTplId = ref(null)

const fieldsVisible = ref(false)
const fieldDefs = ref([])
const currentVersionId = ref(null)

const previewVisible = ref(false)
const previewHtml = ref('')

const auditVisible = ref(false)
const auditLogs = ref([])

const queryForm = reactive({ contractType: '', page: 1, size: 20 })

const form = reactive({ contractType: '', tplName: '', description: '' })
const formRules = {
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  tplName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const typeLabel = (code) => {
  const t = contractTypes.value.find(i => i.code === code)
  return t ? t.label : code
}

const fetchTypes = async () => {
  const res = await getContractTypes()
  contractTypes.value = res.data || []
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTplList(queryForm)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const handleAdd = () => { isEdit.value = false; editId.value = null; formVisible.value = true }
const handleEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  form.contractType = row.contract_type; form.tplName = row.tpl_name; form.description = row.description || ''
  formVisible.value = true
}
const resetForm = () => { Object.assign(form, { contractType: '', tplName: '', description: '' }); formRef.value?.resetFields(); createFile.value = null; createUploadRef.value?.clearFiles() }

const handleSubmitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTpl(editId.value, form)
      ElMessage.success('更新成功')
    } else if (createFile.value) {
      await createTplWithFile(form.contractType, form.tplName, form.description, createFile.value)
      ElMessage.success('创建成功，模板文件已上传')
    } else {
      await createTpl(form)
      ElMessage.success('创建成功')
    }
    formVisible.value = false; fetchData()
  } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除模板"${row.tpl_name}"？`, '提示', { type: 'warning' })
  await deleteTpl(row.id); ElMessage.success('删除成功'); fetchData()
}

const handleUploadVersion = (row) => { uploadTplId.value = row.id; uploadFile.value = null; uploadVisible.value = true }
const doUploadVersion = async () => {
  if (!uploadFile.value) { ElMessage.warning('请选择文件'); return }
  uploading.value = true
  try {
    await uploadTplVersion(uploadTplId.value, uploadFile.value)
    ElMessage.success('版本上传成功')
    uploadVisible.value = false; fetchData()
  } finally { uploading.value = false }
}

const handleViewVersions = async (row) => {
  currentTplId.value = row.id
  const res = await getTplVersions(row.id)
  versions.value = res.data || []
  versionsVisible.value = true
}

const toggleVersion = async (row, status) => {
  await updateVersionStatus(row.id, status)
  ElMessage.success(status === 1 ? '已启用' : '已停用')
  const res = await getTplVersions(currentTplId.value)
  versions.value = res.data || []
}

const handleSubmitApproval = async (row) => {
  await ElMessageBox.confirm(`确定提交版本V${row.version_no}的启用审批？`, '提交审批', { type: 'info' })
  try {
    await submitVersionApproval(row.id)
    ElMessage.success('审批已提交')
    const res = await getTplVersions(currentTplId.value)
    versions.value = res.data || []
  } catch (e) {}
}

const handlePreview = async (row) => {
  const res = await previewVersion(row.id)
  previewHtml.value = res.data || '<p>无内容</p>'
  previewVisible.value = true
}

const handleViewFields = async (row) => {
  currentVersionId.value = row.id
  const res = await getVersionFields(row.id)
  fieldDefs.value = res.data || []
  fieldsVisible.value = true
}

const handleSaveFields = async () => {
  savingFields.value = true
  try {
    await updateVersionFields(currentVersionId.value, fieldDefs.value)
    ElMessage.success('字段定义已保存')
    fieldsVisible.value = false
  } finally { savingFields.value = false }
}

const handleAuditLog = async (row) => {
  const res = await getTplAuditLogs(row.id, { page: 1, size: 50 })
  auditLogs.value = res.data?.records || []
  auditVisible.value = true
}

onMounted(() => { fetchTypes(); fetchData() })
</script>

<style scoped>
.tpl-preview {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
  border: 1px solid #ebeef5;
  background: #fff;
  line-height: 1.8;
}
</style>
