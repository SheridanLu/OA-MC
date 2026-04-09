<template>
  <div>
    <div class="toolbar">
      <el-button v-permission="'progress:milestone-manage'" type="primary" @click="handleAdd">新建里程碑</el-button>
    </div>

    <!-- 里程碑时间线可视化 -->
    <div v-if="tableData.length > 0" class="milestone-timeline-wrapper">
      <div class="milestone-timeline">
        <svg :width="timelineSvgWidth" :height="timelineSvgHeight" class="milestone-dep-lines">
          <defs><marker id="arrowhead" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="#409eff" /></marker></defs>
          <line v-for="(line, idx) in depLines" :key="idx"
            :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2"
            stroke="#409eff" stroke-width="1.5" stroke-dasharray="4 3" marker-end="url(#arrowhead)" />
        </svg>
        <div v-for="(ms, idx) in tableData" :key="ms.id" class="milestone-node"
          :style="{ left: milestoneNodePos(idx).x + 'px', top: milestoneNodePos(idx).y + 'px' }">
          <div class="milestone-diamond" :class="'ms-' + (ms.status || 'draft')" :title="ms.task_name">
            <span class="milestone-icon">&#9670;</span>
          </div>
          <div class="milestone-label">{{ ms.task_name }}</div>
          <div class="milestone-date">{{ ms.plan_end_date || '未设置' }}</div>
          <status-tag :status="ms.status" />
        </div>
      </div>
    </div>

    <!-- 里程碑列表 -->
    <el-table :data="tableData" v-loading="loading" stripe border style="margin-top: 12px">
      <el-table-column prop="task_name" label="里程碑名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="plan_end_date" label="计划完成时间" width="130" />
      <el-table-column prop="actual_end_date" label="实际完成时间" width="130" />
      <el-table-column label="前置依赖" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.dep_milestone_names && row.dep_milestone_names.length">
            <el-tag v-for="name in row.dep_milestone_names" :key="name" size="small" type="info" style="margin: 0 4px 2px 0">{{ name }}</el-tag>
          </template>
          <span v-else style="color: #999">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><status-tag :status="row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-permission="'progress:milestone-manage'" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-dropdown v-permission="'progress:milestone-manage'" @command="(cmd) => handleStatusChange(row, cmd)" style="margin-left: 4px">
            <el-button type="warning" link size="small">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pending" :disabled="row.status === 'pending'">提交审批</el-dropdown-item>
                <el-dropdown-item command="approved" :disabled="row.status === 'approved'">审批通过</el-dropdown-item>
                <el-dropdown-item command="locked" :disabled="row.status === 'locked'">锁定</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-permission="'progress:milestone-manage'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next"
        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <!-- 里程碑对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑里程碑' : '新建里程碑'" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="msForm" :rules="msRules" label-width="110px">
        <el-form-item label="关联项目" prop="projectId"><project-select v-model="msForm.projectId" /></el-form-item>
        <el-form-item label="里程碑名称" prop="milestoneName">
          <el-input v-model="msForm.milestoneName" placeholder="请输入里程碑名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划完成时间" prop="deadline">
              <el-date-picker v-model="msForm.deadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际完成时间">
              <el-date-picker v-model="msForm.actualEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选择日期" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="前置依赖">
          <el-select v-model="msForm.depMilestoneIds" multiple filterable clearable placeholder="选择前置里程碑" style="width: 100%">
            <el-option v-for="m in availableDepMilestones" :key="m.id" :label="m.task_name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="msForm.sortOrder" :min="0" controls-position="right" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMilestone">确定</el-button>
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
  getMilestoneList, getAllMilestones, createMilestone, updateMilestone, deleteMilestone,
  updateMilestoneStatus
} from '@/api/progress'

const props = defineProps({
  projectId: { type: [Number, String], default: null }
})

// ====== Table ======
const { loading, tableData, total, query, fetchData, handleSizeChange, handleCurrentChange } = useTable(getMilestoneList, { projectId: null })

// Sync project filter from parent
watch(() => props.projectId, (val) => {
  query.projectId = val
  query.page = 1
  fetchData()
})

// ====== All milestones for dependency selector ======
const allMilestones = ref([])
const loadAllMilestones = async () => {
  try {
    const params = {}
    if (props.projectId) params.projectId = props.projectId
    const res = await getAllMilestones(params)
    allMilestones.value = res.data || []
  } catch { /* ignore */ }
}

// ====== Form state ======
const formRef = ref(null)
const formVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const msForm = reactive({
  projectId: null, milestoneName: '', deadline: null, actualEndDate: null,
  sortOrder: 0, depMilestoneIds: []
})

const msRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  milestoneName: [{ required: true, message: '请输入里程碑名称', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择计划完成时间', trigger: 'change' }]
}

const availableDepMilestones = computed(() => allMilestones.value.filter(m => m.id !== editId.value))

// ====== Timeline visualization ======
const MS_NODE_W = 120
const MS_GAP_X = 160
const MS_GAP_Y = 120
const MS_COLS = 5

const milestoneNodePos = (idx) => {
  const col = idx % MS_COLS
  const row = Math.floor(idx / MS_COLS)
  return { x: 40 + col * MS_GAP_X, y: 20 + row * MS_GAP_Y }
}

const timelineSvgWidth = computed(() => {
  const count = tableData.value.length
  const cols = Math.min(count, MS_COLS)
  return Math.max(cols * MS_GAP_X + 80, 400)
})

const timelineSvgHeight = computed(() => {
  const count = tableData.value.length
  const rows = Math.ceil(count / MS_COLS)
  return Math.max(rows * MS_GAP_Y + 40, 160)
})

const depLines = computed(() => {
  const lines = []
  const dataArr = tableData.value
  const idxMap = {}
  dataArr.forEach((ms, idx) => { idxMap[ms.id] = idx })

  dataArr.forEach((ms, idx) => {
    const deps = ms.dep_milestone_ids || []
    deps.forEach(depId => {
      const depIdx = idxMap[depId]
      if (depIdx === undefined) return
      const from = milestoneNodePos(depIdx)
      const to = milestoneNodePos(idx)
      lines.push({
        x1: from.x + MS_NODE_W / 2,
        y1: from.y + 30,
        x2: to.x + MS_NODE_W / 2,
        y2: to.y
      })
    })
  })
  return lines
})

// ====== CRUD ======
const handleAdd = () => {
  isEdit.value = false; editId.value = null
  Object.assign(msForm, { projectId: props.projectId, milestoneName: '', deadline: null, actualEndDate: null, sortOrder: 0, depMilestoneIds: [] })
  loadAllMilestones()
  formVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  Object.assign(msForm, {
    projectId: row.project_id,
    milestoneName: row.task_name,
    deadline: row.plan_end_date,
    actualEndDate: row.actual_end_date || null,
    sortOrder: row.sort_order || 0,
    depMilestoneIds: row.dep_milestone_ids || []
  })
  loadAllMilestones()
  formVisible.value = true
}

const resetForm = () => {
  Object.assign(msForm, { projectId: null, milestoneName: '', deadline: null, actualEndDate: null, sortOrder: 0, depMilestoneIds: [] })
  formRef.value?.resetFields()
}

const submitMilestone = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMilestone(editId.value, msForm)
      ElMessage.success('更新成功')
    } else {
      await createMilestone(msForm)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    fetchData(); loadAllMilestones()
  } finally { submitting.value = false }
}

const statusLabel = { draft: '草稿', pending: '审批中', approved: '已审批', locked: '已锁定' }

const handleStatusChange = async (row, status) => {
  await ElMessageBox.confirm(`确定将状态改为"${statusLabel[status]}"？`, '提示', { type: 'warning' })
  await updateMilestoneStatus(row.id, status)
  ElMessage.success('状态已更新')
  fetchData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该里程碑？', '提示', { type: 'warning' })
  await deleteMilestone(row.id)
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
.milestone-timeline-wrapper {
  overflow-x: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
  padding: 12px;
}
.milestone-timeline {
  position: relative;
  min-height: 160px;
}
.milestone-dep-lines {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}
.milestone-node {
  position: absolute;
  width: 120px;
  text-align: center;
}
.milestone-diamond {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}
.milestone-icon {
  font-size: 28px;
  line-height: 1;
}
.ms-draft .milestone-icon { color: #909399; }
.ms-pending .milestone-icon { color: #e6a23c; }
.ms-approved .milestone-icon { color: #67c23a; }
.ms-locked .milestone-icon { color: #303133; }
.milestone-label {
  font-size: 12px;
  font-weight: 600;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.milestone-date {
  font-size: 11px;
  color: #909399;
  margin-top: 1px;
}
</style>
