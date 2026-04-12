<template>
  <div class="dept-page">
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd(0)" v-permission="'system:dept-manage'">新增顶级部门</el-button>
      </div>
      <el-table :data="treeData" v-loading="loading" row-key="id" default-expand-all>
        <el-table-column prop="name" label="部门名称" width="250" />
        <el-table-column prop="leader_name" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAdd(row.id)" v-permission="'system:dept-manage'">新增子部门</el-button>
            <el-button link type="primary" @click="handleEdit(row)" v-permission="'system:dept-manage'">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)" v-permission="'system:dept-manage'">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-permission="'system:dept-manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            placeholder="顶级部门"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leaderId" placeholder="负责人ID" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptTree, createDept, updateDept, updateDeptStatus, deleteDept } from '@/api/dept'

const loading = ref(false)
const submitLoading = ref(false)
const treeData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)

const form = reactive({ id: null, name: '', parentId: 0, leaderId: null, phone: '', sort: 0, remark: '' })

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDeptTree()
    treeData.value = res.data
  } finally {
    loading.value = false
  }
}

const handleAdd = (parentId) => {
  Object.assign(form, { id: null, name: '', parentId: parentId, leaderId: null, phone: '', sort: 0, remark: '' })
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, { id: row.id, name: row.name, parentId: row.parent_id, leaderId: row.leader_id, phone: row.phone, sort: row.sort, remark: row.remark })
  dialogTitle.value = '编辑部门'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateDept(form.id, form)
    } else {
      await createDept(form)
    }
    ElMessage.success(form.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await updateDeptStatus(row.id, newStatus)
  ElMessage.success('状态已更新')
  fetchData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除部门 "${row.name}" 吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteDept(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
</style>
