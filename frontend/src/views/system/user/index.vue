<template>
  <div class="user-page">
    <!-- 搜索区 -->
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="用户名">
          <el-input v-model="queryForm.username" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.realName" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryForm.phone" placeholder="请输入" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd" v-permission="'system:user-manage'">新增用户</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="real_name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="dept_name" label="部门" width="120" />
        <el-table-column prop="position" label="职位" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)" v-permission="'system:user-manage'">编辑</el-button>
            <el-button link type="primary" @click="handleRoles(row)" v-permission="'system:user-manage'">角色</el-button>
            <el-button link type="warning" @click="handleResetPwd(row)" v-permission="'system:user-manage'">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-permission="'system:user-manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!form.id">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            placeholder="请选择部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="职位">
          <el-select v-model="form.position" filterable clearable placeholder="请选择职位" style="width: 100%">
            <el-option v-for="p in positionOptions" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="r in roleList" :key="r.id" :value="r.id" :label="r.role_name" />
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRoles" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, updateUserStatus, assignUserRoles, resetUserPassword } from '@/api/user'
import { getRoleList } from '@/api/role'
import { getDeptTree } from '@/api/dept'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const roleDialogVisible = ref(false)
const selectedRoleIds = ref([])
const currentUserId = ref(null)
const deptTree = ref([])
const roleList = ref([])
const formRef = ref(null)

const positionOptions = [
  '总经理', '副总经理', '总工程师',
  '项目经理', '项目副经理', '技术负责人',
  '施工员', '安全员', '质量员', '测量员',
  '预算员', '材料员', '资料员', '采购员',
  '财务主管', '会计', '出纳',
  '法务人员', '人事专员', '行政专员',
  '劳务队长', '班组长'
]

const queryForm = reactive({ username: '', realName: '', phone: '', status: null, page: 1, size: 20 })

const form = reactive({ id: null, username: '', realName: '', phone: '', email: '', deptId: null, position: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryForm)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { queryForm.page = 1; fetchData() }
const handleReset = () => {
  Object.assign(queryForm, { username: '', realName: '', phone: '', status: null, page: 1, size: 20 })
  fetchData()
}

const handleAdd = () => {
  Object.assign(form, { id: null, username: '', realName: '', phone: '', email: '', deptId: null, position: '', password: '' })
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, { id: row.id, username: row.username, realName: row.real_name, phone: row.phone, email: row.email, deptId: row.dept_id, position: row.position, password: '' })
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await updateUser(form.id, form)
    } else {
      await createUser(form)
    }
    ElMessage.success(form.id ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？`, '提示', { type: 'warning' }).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const handleStatusChange = async (row, val) => {
  await updateUserStatus(row.id, val ? 1 : 0)
  ElMessage.success('状态已更新')
  fetchData()
}

const handleRoles = async (row) => {
  currentUserId.value = row.id
  selectedRoleIds.value = row.role_ids || []
  roleDialogVisible.value = true
}

const handleSubmitRoles = async () => {
  submitLoading.value = true
  try {
    await assignUserRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

const handleResetPwd = (row) => {
  ElMessageBox.prompt('请输入新密码', `重置 ${row.username} 的密码`, {
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码至少6位'
  }).then(async ({ value }) => {
    await resetUserPassword(row.id, value)
    ElMessage.success('密码已重置')
  }).catch(() => {})
}

const loadDepts = async () => {
  try {
    const res = await getDeptTree()
    deptTree.value = res.data
  } catch (e) {}
}

const loadRoles = async () => {
  try {
    const res = await getRoleList({ page: 1, size: 100 })
    roleList.value = res.data.records
  } catch (e) {}
}

onMounted(() => { fetchData(); loadDepts(); loadRoles() })
</script>

<style scoped>
.search-card { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
