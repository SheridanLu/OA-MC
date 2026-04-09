<template>
  <div class="page-container" v-loading="loading">
    <!-- 项目头部 -->
    <div class="detail-header">
      <el-button link @click="$router.push('/projects')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
      <h2>{{ project.project_name }}</h2>
      <div class="header-actions">
        <status-tag :status="project.status" />
        <el-button v-if="project.status==='virtual'" v-permission="'project:edit'" type="primary" size="small" @click="handleConvert">虚拟转实体</el-button>
        <el-button v-if="project.status==='virtual'" v-permission="'project:delete'" type="danger" size="small" @click="handleTerminate">中止项目</el-button>
        <el-button v-if="project.status==='active'" v-permission="'project:suspend'" type="warning" size="small" @click="handleSuspend">暂停项目</el-button>
        <el-button v-if="project.status==='suspended'" v-permission="'project:suspend'" type="success" size="small" @click="handleResume">恢复项目</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目编号"><numbering-display :value="project.project_no" /></el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ project.project_name }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ project.project_type === 'virtual' ? '虚拟项目' : '实体项目' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><status-tag :status="project.status" /></el-descriptions-item>
          <el-descriptions-item label="项目负责人">{{ project.manager_name }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ project.dept_name }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ project.start_date }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ project.end_date }}</el-descriptions-item>
          <el-descriptions-item label="合同金额"><money-text :value="project.contract_amount" /></el-descriptions-item>
          <el-descriptions-item label="已收款"><money-text :value="project.received_amount" /></el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ project.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- 关联合同 -->
      <el-tab-pane label="关联合同" name="contracts">
        <el-table :data="project.contracts || []" border>
          <el-table-column prop="contract_no" label="合同编号" width="140" />
          <el-table-column prop="contract_name" label="合同名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="contract_type" label="类型" width="100" />
          <el-table-column prop="amount" label="金额" width="130" align="right">
            <template #default="{ row }"><money-text :value="row.amount" /></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }"><status-tag :status="row.status" /></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 物资概况 -->
      <el-tab-pane label="物资概况" name="materials">
        <el-table :data="project.materials || []" border>
          <el-table-column prop="material_name" label="物料名称" min-width="140" />
          <el-table-column prop="spec" label="规格" width="100" />
          <el-table-column prop="unit" label="单位" width="60" />
          <el-table-column prop="inbound_qty" label="入库量" width="100" align="right" />
          <el-table-column prop="outbound_qty" label="出库量" width="100" align="right" />
          <el-table-column prop="stock_qty" label="库存量" width="100" align="right" />
        </el-table>
      </el-tab-pane>

      <!-- 进度概况 -->
      <el-tab-pane label="进度概况" name="progress">
        <el-progress :percentage="project.progress || 0" :stroke-width="20" style="margin-bottom:20px" />
        <el-table :data="project.milestones || []" border>
          <el-table-column prop="name" label="里程碑" min-width="160" />
          <el-table-column prop="plan_date" label="计划完成" width="120" />
          <el-table-column prop="actual_date" label="实际完成" width="120" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }"><status-tag :status="row.status" /></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 财务概况 -->
      <el-tab-pane label="财务概况" name="finance">
        <el-row :gutter="16" style="margin-bottom:20px">
          <el-col :span="6"><el-statistic title="合同金额" :value="project.contract_amount || 0" :precision="2" prefix="¥" /></el-col>
          <el-col :span="6"><el-statistic title="已收款" :value="project.received_amount || 0" :precision="2" prefix="¥" /></el-col>
          <el-col :span="6"><el-statistic title="已付款" :value="project.paid_amount || 0" :precision="2" prefix="¥" /></el-col>
          <el-col :span="6"><el-statistic title="利润" :value="(project.received_amount || 0) - (project.paid_amount || 0)" :precision="2" prefix="¥" /></el-col>
        </el-row>
      </el-tab-pane>

      <!-- 项目成员 -->
      <el-tab-pane label="项目成员" name="members">
        <div style="margin-bottom:12px">
          <el-button type="primary" size="small" @click="memberPickerVisible = true"><el-icon><Plus /></el-icon>添加成员</el-button>
        </div>
        <el-table :data="members" border>
          <el-table-column prop="real_name" label="姓名" width="100" />
          <el-table-column prop="dept_name" label="部门" width="140" />
          <el-table-column prop="role_in_project" label="项目角色" width="120" />
          <el-table-column prop="phone" label="手机" width="130" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="removeMember(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <user-picker v-model:visible="memberPickerVisible" @confirm="addMember" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectById, convertProject, terminateProject, suspendProject, resumeProject, getProjectMembers, addProjectMember, removeProjectMember } from '@/api/project'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('info')
const project = ref({})
const members = ref([])
const memberPickerVisible = ref(false)

const projectId = route.params.id

async function fetchProject() {
  loading.value = true
  try {
    const res = await getProjectById(projectId)
    project.value = res.data || {}
  } finally {
    loading.value = false
  }
}

async function fetchMembers() {
  try {
    const res = await getProjectMembers(projectId)
    members.value = res.data || []
  } catch { /* endpoint may not exist */ }
}

async function handleConvert() {
  await ElMessageBox.confirm('确认将虚拟项目转为实体项目？此操作不可逆。')
  await convertProject(projectId)
  ElMessage.success('已转为实体项目')
  fetchProject()
}

async function handleTerminate() {
  const { value } = await ElMessageBox.prompt('请输入中止原因', '中止项目', { inputType: 'textarea' })
  await terminateProject(projectId, { reason: value })
  ElMessage.success('项目已中止')
  fetchProject()
}

async function handleSuspend() {
  const { value } = await ElMessageBox.prompt('请输入暂停原因', '暂停项目', { inputType: 'textarea' })
  await suspendProject(projectId, { reason: value })
  ElMessage.success('项目已暂停')
  fetchProject()
}

async function handleResume() {
  await ElMessageBox.confirm('确认恢复项目？')
  await resumeProject(projectId)
  ElMessage.success('项目已恢复')
  fetchProject()
}

async function addMember(user) {
  await addProjectMember(projectId, { user_id: user.id })
  ElMessage.success('已添加成员')
  fetchMembers()
}

async function removeMember(row) {
  await ElMessageBox.confirm(`确认移除 ${row.real_name}？`)
  await removeProjectMember(projectId, row.id)
  ElMessage.success('已移除')
  fetchMembers()
}

onMounted(() => {
  fetchProject()
  fetchMembers()
})
</script>

<style scoped lang="scss">
.page-container { background: #fff; border-radius: 4px; padding: 20px; }
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  h2 { margin: 0; flex: 1; }
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
