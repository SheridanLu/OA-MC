<template>
  <div>
    <div class="toolbar">
      <el-button v-permission="'progress:gantt-manage'" type="primary" @click="handleAdd">新建任务</el-button>
    </div>

    <!-- 甘特图可视化 -->
    <div v-if="sortedData.length > 0" class="gantt-chart-wrapper">
      <table class="gantt-chart" border="0" cellspacing="0">
        <thead>
          <tr>
            <th style="min-width: 200px; text-align: left; padding: 6px 8px">任务名称</th>
            <th style="width: 80px">进度</th>
            <th style="width: 90px">状态</th>
            <th style="min-width: 400px; text-align: left; padding: 6px 8px">时间线</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in sortedData" :key="row.id" :class="{ 'gantt-milestone-row': row.task_type === 1 }">
            <td style="padding: 6px 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">
              <span v-if="row.task_type === 1" style="font-weight: bold">&#9670; {{ row.task_name }}</span>
              <span v-else style="padding-left: 16px">&#9500; {{ row.task_name }}</span>
            </td>
            <td style="text-align: center">{{ row.progress_pct }}%</td>
            <td style="text-align: center"><status-tag :status="row.status" /></td>
            <td style="padding: 6px 8px">
              <div class="gantt-bar-container">
                <div class="gantt-bar"
                  :style="ganttBarStyle(row)"
                  :title="`${row.plan_start_date || '?'} ~ ${row.plan_end_date || '?'}`">
                  <div class="gantt-bar-fill" :style="{ width: (row.progress_pct || 0) + '%' }"></div>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 详细列表 -->
    <el-table :data="sortedData" v-loading="loading" stripe border style="margin-top: 12px">
      <el-table-column label="任务名称" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.task_type === 1" style="font-weight: bold">&#9670; {{ row.task_name }}</span>
          <span v-else style="padding-left: 16px">{{ row.task_name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所属里程碑" width="140">
        <template #default="{ row }">{{ row.parent_id ? milestoneNameMap[row.parent_id] || row.parent_id : '-' }}</template>
      </el-table-column>
      <el-table-column prop="plan_start_date" label="计划开始" width="110" />
      <el-table-column prop="plan_end_date" label="计划结束" width="110" />
      <el-table-column prop="actual_start_date" label="实际开始" width="110" />
      <el-table-column prop="actual_end_date" label="实际结束" width="110" />
      <el-table-column prop="progress_pct" label="进度(%)" width="85" align="right" />
      <el-table-column label="依赖" width="100">
        <template #default="{ row }">
          <span v-if="row.dependency_task_id">{{ row.dependency_type || 'FS' }} #{{ row.dependency_task_id }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-permission="'progress:gantt-manage'" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-dropdown v-permission="'progress:gantt-manage'" @command="(cmd) => handleStatusChange(row, cmd)" style="margin-left: 4px">
            <el-button type="warning" link size="small">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pending">提交审批</el-dropdown-item>
                <el-dropdown-item command="approved">审批通过</el-dropdown-item>
                <el-dropdown-item command="locked">锁定</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-permission="'progress:gantt-manage'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10,20,50,200]" layout="total,sizes,prev,pager,next"
        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <!-- 甘特任务对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="750px" @closed="resetForm">
      <el-form ref="ganttFormRef" :model="ganttForm" :rules="ganttRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId"><project-select v-model="ganttForm.projectId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务类型" prop="taskType">
              <el-select v-model="ganttForm.taskType" style="width: 100%">
                <el-option label="里程碑" :value="1" />
                <el-option label="任务" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="ganttForm.taskName" />
        </el-form-item>
        <el-row :gutter="16" v-if="ganttForm.taskType === 2">
          <el-col :span="12">
            <el-form-item label="所属里程碑">
              <el-select v-model="ganttForm.parentId" filterable clearable placeholder="选择里程碑" style="width: 100%">
                <el-option v-for="m in allMilestones" :key="m.id" :label="m.task_name" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="依赖类型">
              <el-select v-model="ganttForm.dependencyType" clearable placeholder="类型" style="width: 100%">
                <el-option label="FS" value="FS" />
                <el-option label="SS" value="SS" />
                <el-option label="FF" value="FF" />
                <el-option label="SF" value="SF" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="依赖任务ID">
              <el-input-number v-model="ganttForm.dependencyTaskId" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划开始">
              <el-date-picker v-model="ganttForm.planStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束">
              <el-date-picker v-model="ganttForm.planEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="实际开始">
              <el-date-picker v-model="ganttForm.actualStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际结束">
              <el-date-picker v-model="ganttForm.actualEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="进度(%)">
              <el-slider v-model="ganttForm.progressPct" :max="100" :step="1" show-input input-size="small" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="ganttForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitGantt">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import {
  getGanttTaskList, createGanttTask, updateGanttTask, updateGanttTaskStatus, deleteGanttTask,
  getAllMilestones
} from '@/api/progress'
import { submitApproval } from '@/api/approval'

const props = defineProps({
  projectId: { type: [Number, String], default: null }
})

// ====== Table ======
const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(getGanttTaskList, { projectId: null, size: 200 })

// Sync project filter from parent
watch(() => props.projectId, (val) => {
  query.projectId = val
  query.page = 1
  fetchData()
})

// ====== All milestones for parent selector ======
const allMilestones = ref([])
const loadAllMilestones = async () => {
  try {
    const params = {}
    if (props.projectId) params.projectId = props.projectId
    const res = await getAllMilestones(params)
    allMilestones.value = res.data || []
  } catch { /* ignore */ }
}

// Milestone name map
const milestoneNameMap = computed(() => {
  const map = {}
  allMilestones.value.forEach(m => { map[m.id] = m.task_name })
  return map
})

// ====== Tree-sorted data (milestones first, then their child tasks) ======
const sortedData = computed(() => {
  const allRecords = tableData.value
  const milestones = allRecords.filter(t => t.task_type === 1).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
  const tasks = allRecords.filter(t => t.task_type === 2)
  const sorted = []
  milestones.forEach(m => {
    sorted.push(m)
    tasks.filter(t => t.parent_id === m.id).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0)).forEach(t => sorted.push(t))
  })
  // Orphan tasks (no parent milestone)
  tasks.filter(t => !t.parent_id || !milestones.find(m => m.id === t.parent_id)).forEach(t => sorted.push(t))
  return sorted
})

// ====== Gantt bar positioning ======
const ganttTimeRange = computed(() => {
  const allTasks = sortedData.value
  if (!allTasks.length) return { min: null, max: null, days: 1 }
  let min = null, max = null
  allTasks.forEach(t => {
    const s = t.plan_start_date ? new Date(t.plan_start_date) : null
    const e = t.plan_end_date ? new Date(t.plan_end_date) : null
    if (s && (!min || s < min)) min = s
    if (e && (!max || e > max)) max = e
  })
  if (!min || !max) return { min: null, max: null, days: 1 }
  const days = Math.max(1, Math.ceil((max - min) / 86400000) + 1)
  return { min, max, days }
})

const ganttBarStyle = (row) => {
  const { min, days } = ganttTimeRange.value
  if (!min || !row.plan_start_date || !row.plan_end_date) return { display: 'none' }
  const s = new Date(row.plan_start_date)
  const e = new Date(row.plan_end_date)
  const startOffset = Math.max(0, (s - min) / 86400000)
  const duration = Math.max(1, (e - s) / 86400000 + 1)
  const left = (startOffset / days * 100).toFixed(2) + '%'
  const width = (duration / days * 100).toFixed(2) + '%'
  return { left, width, position: 'absolute', top: '4px', height: '20px' }
}

// ====== Form state ======
const ganttFormRef = ref(null)
const dialogVisible = ref(false)
const dialogTitle = ref('新建任务')
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const ganttForm = reactive({
  projectId: null, parentId: null, taskName: '', taskType: 1,
  planStartDate: null, planEndDate: null, actualStartDate: null, actualEndDate: null,
  progressPct: 0, sortOrder: 0, dependencyType: null, dependencyTaskId: null
})

const ganttRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }]
}

// ====== CRUD ======
const handleAdd = () => {
  isEdit.value = false; editId.value = null
  Object.assign(ganttForm, {
    projectId: props.projectId, parentId: null, taskName: '', taskType: 2,
    planStartDate: null, planEndDate: null, actualStartDate: null, actualEndDate: null,
    progressPct: 0, sortOrder: 0, dependencyType: null, dependencyTaskId: null
  })
  dialogTitle.value = '新建任务'
  loadAllMilestones()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  dialogTitle.value = row.task_type === 1 ? '编辑里程碑' : '编辑任务'
  Object.assign(ganttForm, {
    projectId: row.project_id, parentId: row.parent_id || null, taskName: row.task_name || '',
    taskType: row.task_type, planStartDate: row.plan_start_date || null, planEndDate: row.plan_end_date || null,
    actualStartDate: row.actual_start_date || null, actualEndDate: row.actual_end_date || null,
    progressPct: row.progress_pct || 0, sortOrder: row.sort_order || 0,
    dependencyType: row.dependency_type || null, dependencyTaskId: row.dependency_task_id || null
  })
  loadAllMilestones()
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(ganttForm, {
    projectId: null, parentId: null, taskName: '', taskType: 1,
    planStartDate: null, planEndDate: null, actualStartDate: null, actualEndDate: null,
    progressPct: 0, sortOrder: 0, dependencyType: null, dependencyTaskId: null
  })
  ganttFormRef.value?.resetFields()
}

const submitGantt = async () => {
  await ganttFormRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateGanttTask(editId.value, ganttForm)
      ElMessage.success('更新成功')
    } else {
      await createGanttTask(ganttForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData(); loadAllMilestones()
  } finally { submitting.value = false }
}

const statusLabel = { draft: '草稿', pending: '审批中', approved: '已审批', locked: '已锁定' }

const handleStatusChange = async (row, status) => {
  if (status === 'pending') {
    // 走审批流程
    await ElMessageBox.confirm('确定提交审批？提交后将进入审批流程。', '提交审批', { type: 'info' })
    await submitApproval({ bizType: 'gantt_task', bizId: row.id, action: 'submit' })
    ElMessage.success('已提交审批')
  } else {
    await ElMessageBox.confirm(`确定将状态改为"${statusLabel[status]}"？`, '提示', { type: 'warning' })
    await updateGanttTaskStatus(row.id, status)
    ElMessage.success('状态已更新')
  }
  fetchData()
}

const handleDelete = async (row) => {
  const label = row.task_type === 1 ? '里程碑' : '任务'
  await ElMessageBox.confirm(`确定删除该${label}？`, '提示', { type: 'warning' })
  await deleteGanttTask(row.id)
  ElMessage.success('删除成功')
  fetchData(); loadAllMilestones()
}

onMounted(() => {
  query.projectId = props.projectId
  fetchData()
  loadAllMilestones()
})
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.gantt-chart-wrapper {
  overflow-x: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}
.gantt-chart {
  width: 100%;
  border-collapse: collapse;
}
.gantt-chart th {
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
  font-size: 13px;
  font-weight: 500;
}
.gantt-chart td {
  border-bottom: 1px solid var(--el-border-color-extra-light);
  font-size: 13px;
}
.gantt-milestone-row {
  background: var(--el-color-primary-light-9);
}
.gantt-bar-container {
  position: relative;
  height: 28px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}
.gantt-bar {
  background: var(--el-color-primary-light-5);
  border-radius: 3px;
  overflow: hidden;
  cursor: pointer;
}
.gantt-bar-fill {
  height: 100%;
  background: var(--el-color-primary);
  border-radius: 3px;
  transition: width 0.3s;
}
</style>
