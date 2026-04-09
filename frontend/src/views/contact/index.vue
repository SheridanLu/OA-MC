<template>
  <div class="page-container">
    <page-header title="通讯录">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="internal">企业通讯录</el-radio-button>
        <el-radio-button value="external">外部联系人</el-radio-button>
      </el-radio-group>
    </page-header>

    <!-- 企业通讯录 -->
    <div v-if="viewMode === 'internal'" class="contact-layout">
      <div class="dept-tree-panel">
        <el-input v-model="deptKeyword" placeholder="搜索部门" clearable size="small" style="margin-bottom:8px" />
        <el-tree
          ref="deptTreeRef"
          :data="deptTree"
          :props="{ label: 'dept_name', children: 'children' }"
          :filter-node-method="filterDeptNode"
          node-key="id"
          default-expand-all
          highlight-current
          @node-click="handleDeptClick"
        />
      </div>
      <div class="user-list-panel">
        <el-input v-model="userKeyword" placeholder="搜索姓名/手机" clearable style="margin-bottom:12px" @keyup.enter="fetchUserList" />
        <el-table v-loading="userLoading" :data="userList" border stripe>
          <el-table-column prop="real_name" label="姓名" width="100" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="dept_name" label="部门" width="140" />
          <el-table-column prop="position" label="职位" width="120" />
          <el-table-column prop="phone" label="手机" width="130" />
          <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>
    </div>

    <!-- 外部联系人 -->
    <div v-else>
      <el-form :model="extQuery" inline class="search-wrapper">
        <el-form-item label="关键字"><el-input v-model="extQuery.keyword" placeholder="姓名/公司/手机" clearable @keyup.enter="fetchExtContacts" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="extQuery.type" placeholder="全部" clearable>
            <el-option label="客户" value="customer" />
            <el-option label="供应商" value="supplier" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchExtContacts">查询</el-button>
          <el-button @click="extQuery.keyword = ''; extQuery.type = ''; fetchExtContacts()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="extLoading" :data="extContacts" border stripe>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="company" label="公司" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.type==='customer'" type="success" size="small">客户</el-tag>
            <el-tag v-else-if="row.type==='supplier'" type="warning" size="small">供应商</el-tag>
            <el-tag v-else size="small">其他</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getDeptTree } from '@/api/dept'
import { getUserList } from '@/api/user'
import { getExternalContacts } from '@/api/contact'

const viewMode = ref('internal')

// ===== 企业通讯录 =====
const deptTreeRef = ref(null)
const deptKeyword = ref('')
const deptTree = ref([])
const selectedDeptId = ref(null)
const userKeyword = ref('')
const userLoading = ref(false)
const userList = ref([])

function filterDeptNode(value, data) {
  if (!value) return true
  return data.dept_name?.includes(value)
}

watch(deptKeyword, (val) => { deptTreeRef.value?.filter(val) })

function handleDeptClick(node) {
  selectedDeptId.value = node.id
  fetchUserList()
}

async function fetchDeptTree() {
  const res = await getDeptTree()
  deptTree.value = res.data || []
}

async function fetchUserList() {
  userLoading.value = true
  try {
    const params = { page: 1, size: 200, keyword: userKeyword.value }
    if (selectedDeptId.value) params.dept_id = selectedDeptId.value
    const res = await getUserList(params)
    userList.value = res.data?.records || []
  } finally {
    userLoading.value = false
  }
}

// ===== 外部联系人 =====
const extQuery = ref({ keyword: '', type: '' })
const extLoading = ref(false)
const extContacts = ref([])

async function fetchExtContacts() {
  extLoading.value = true
  try {
    const res = await getExternalContacts(extQuery.value)
    extContacts.value = res.data?.records || []
  } finally {
    extLoading.value = false
  }
}

onMounted(() => {
  fetchDeptTree()
  fetchUserList()
})
</script>

<style scoped lang="scss">
.page-container { background: #fff; border-radius: 4px; padding: 20px; }
.contact-layout { display: flex; gap: 16px; }
.dept-tree-panel { width: 240px; flex-shrink: 0; border-right: 1px solid #ebeef5; padding-right: 16px; }
.user-list-panel { flex: 1; min-width: 0; }
</style>
