<template>
  <div class="approval-page">
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ==================== 待我审批 ==================== -->
        <el-tab-pane label="待我审批" name="pending">
          <el-table :data="pendingData" v-loading="loading" stripe border>
            <el-table-column prop="id" label="实例ID" width="80" />
            <el-table-column prop="biz_type" label="业务类型" width="120">
              <template #default="{ row }">{{ bizTypeLabel[row.biz_type] || row.biz_type }}</template>
            </el-table-column>
            <el-table-column prop="biz_id" label="单据ID" width="80" />
            <el-table-column prop="flow_name" label="流程名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="current_node_name" label="当前节点" width="130" />
            <el-table-column prop="initiator_name" label="发起人" width="100" />
            <el-table-column prop="created_at" label="提交时间" width="170" />
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button type="success" link size="small" @click="handleApprove(row)">通过</el-button>
                <el-button type="danger" link size="small" @click="handleReject(row)">驳回</el-button>
                <el-dropdown trigger="click" @command="(cmd) => handleMoreAction(cmd, row)">
                  <el-button type="primary" link size="small">更多<el-icon class="el-icon--right"><ArrowDown/></el-icon></el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="transfer">转办</el-dropdown-item>
                      <el-dropdown-item command="cosign">加签</el-dropdown-item>
                      <el-dropdown-item command="read_handle">阅办</el-dropdown-item>
                      <el-dropdown-item command="cc">阅知</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== 我发起的 ==================== -->
        <el-tab-pane label="我发起的" name="initiated">
          <el-table :data="initiatedData" v-loading="loading" stripe border>
            <el-table-column prop="id" label="实例ID" width="80" />
            <el-table-column prop="biz_type" label="业务类型" width="120">
              <template #default="{ row }">{{ bizTypeLabel[row.biz_type] || row.biz_type }}</template>
            </el-table-column>
            <el-table-column prop="biz_id" label="单据ID" width="80" />
            <el-table-column prop="flow_name" label="流程名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="current_node_name" label="当前节点" width="130" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType[row.status]" size="small">{{ statusLabel[row.status] || row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="提交时间" width="170" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'pending'" type="warning" link size="small" @click="handleWithdraw(row)">撤回</el-button>
                <el-button type="primary" link size="small" @click="handleViewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ==================== 流程定义 ==================== -->
        <el-tab-pane label="流程定义" name="flows">
          <div style="margin-bottom: 12px">
            <el-button type="primary" v-permission="'approval:flow-manage'" @click="handleAddFlow">新建流程</el-button>
          </div>
          <el-table :data="flowData" v-loading="loading" stripe border>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="biz_type" label="业务类型" width="120">
              <template #default="{ row }">{{ bizTypeLabel[row.biz_type] || row.biz_type }}</template>
            </el-table-column>
            <el-table-column prop="flow_name" label="流程名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="version" label="版本" width="70" align="center" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="创建时间" width="170" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEditFlow(row)" v-permission="'approval:flow-manage'">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteFlow(row)" v-permission="'approval:flow-manage'">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <el-pagination v-if="total > 0" style="margin-top: 16px; justify-content: flex-end" background
        layout="total, prev, pager, next" :total="total"
        v-model:current-page="page" @current-change="fetchData" />
    </el-card>

    <!-- ==================== 审批操作对话框 ==================== -->
    <el-dialog v-model="actionDlg" :title="actionTitle" width="500px" @closed="resetAction">
      <el-form label-width="80px">
        <el-form-item label="业务类型">
          <span>{{ bizTypeLabel[actionRow.biz_type] || actionRow.biz_type }} (ID: {{ actionRow.biz_id }})</span>
        </el-form-item>
        <el-form-item label="流程名称">
          <span>{{ actionRow.flow_name }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="actionOpinion" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDlg = false">取消</el-button>
        <el-button :type="actionType === 'approve' ? 'success' : 'danger'" :loading="submitting" @click="submitAction">
          {{ actionType === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 转办/加签/阅办对话框(选择用户) ==================== -->
    <el-dialog v-model="userPickDlg" :title="userPickTitle" width="500px" @closed="resetUserPick">
      <el-form label-width="80px">
        <el-form-item label="业务信息">
          <span>{{ bizTypeLabel[userPickRow.biz_type] || userPickRow.biz_type }} - {{ userPickRow.flow_name }}</span>
        </el-form-item>
        <el-form-item :label="userPickMode === 'cc' ? '选择用户' : '目标用户'">
          <el-select v-model="userPickTarget" :multiple="userPickMode === 'cc'" filterable remote
            :remote-method="searchUsers" :loading="userSearching" placeholder="请输入姓名搜索" style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.real_name + ' (' + u.username + ')'" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userPickMode !== 'cc' && userPickMode !== 'read_handle'" label="意见">
          <el-input v-model="userPickOpinion" type="textarea" :rows="2" placeholder="请输入意见(可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userPickDlg = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUserPick">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 审批详情对话框 ==================== -->
    <el-dialog v-model="detailDlg" title="审批详情" width="750px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="业务类型">{{ bizTypeLabel[detailData.biz_type] || detailData.biz_type }}</el-descriptions-item>
        <el-descriptions-item label="单据ID">{{ detailData.biz_id }}</el-descriptions-item>
        <el-descriptions-item label="流程名称">{{ detailData.flow_name }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType[detailData.status]" size="small">{{ statusLabel[detailData.status] || detailData.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detailData.initiator_name }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detailData.created_at }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin: 12px 0 8px">审批节点</h4>
      <el-steps :active="detailData.status === 'approved' ? (detailData.nodes || []).length : (detailData.current_node || 1) - 1" align-center finish-status="success" style="margin-bottom: 16px">
        <el-step v-for="n in (detailData.nodes || [])" :key="n.node_order" :title="n.node_name" />
      </el-steps>

      <h4 style="margin: 12px 0 8px">审批记录</h4>
      <el-table :data="detailData.records || []" border size="small">
        <el-table-column prop="node_name" label="节点" width="130" />
        <el-table-column prop="approver_name" label="审批人" width="100" />
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="actionTagType[row.action] || 'info'" size="small">
              {{ actionLabel[row.action] || row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opinion" label="意见" min-width="150" show-overflow-tooltip />
        <el-table-column label="转办来源" width="100">
          <template #default="{ row }">{{ row.delegate_from_name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="created_at" label="时间" width="170" />
      </el-table>

      <!-- 会签记录 -->
      <template v-if="(detailData.cosigns || []).length > 0">
        <h4 style="margin: 12px 0 8px">会签记录</h4>
        <el-table :data="detailData.cosigns" border size="small">
          <el-table-column prop="cosigner_name" label="会签人" width="100" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'approved' ? 'success' : 'warning'" size="small">{{ row.status === 'approved' ? '已签' : '待签' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="opinion" label="意见" min-width="150" show-overflow-tooltip />
          <el-table-column prop="completed_at" label="完成时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'pending'" type="success" link size="small" @click="handleCosignApprove(row)">审批</el-button>
              <span v-else style="color: #909399; font-size: 12px">已完成</span>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 抄送/阅办记录 -->
      <template v-if="(detailData.cc_list || []).length > 0">
        <h4 style="margin: 12px 0 8px">抄送/阅办</h4>
        <el-table :data="detailData.cc_list" border size="small">
          <el-table-column prop="user_name" label="接收人" width="100" />
          <el-table-column prop="cc_type" label="类型" width="80">
            <template #default="{ row }">{{ row.cc_type === 'read_handle' ? '阅办' : '阅知' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.is_handled ? 'success' : (row.is_read ? 'warning' : 'info')" size="small">
                {{ row.is_handled ? '已处理' : (row.is_read ? '已读' : '未读') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handled_at" label="处理时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.cc_type === 'read_handle' && !row.is_handled" type="primary" link size="small" @click="handleMarkCcDone(row)">标记已处理</el-button>
              <span v-else-if="row.is_handled" style="color: #909399; font-size: 12px">已处理</span>
              <span v-else style="color: #909399; font-size: 12px">-</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- ==================== 新建/编辑流程对话框 ==================== -->

    <!-- ==================== 会签审批对话框 ==================== -->
    <el-dialog v-model="cosignDlg" title="会签审批" width="500px" @closed="resetCosign">
      <el-form label-width="80px">
        <el-form-item label="会签人">
          <span>{{ cosignRow.cosigner_name }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="cosignOpinion" type="textarea" :rows="3" placeholder="请输入会签意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cosignDlg = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitCosignApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 新建/编辑流程对话框(原位置) ==================== -->
    <el-dialog v-model="flowDlg" :title="isEditFlow ? '编辑流程' : '新建流程'" width="700px" @closed="resetFlowForm">
      <el-form ref="flowFormRef" :model="flowForm" :rules="flowRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="业务类型" prop="bizType">
              <el-select v-model="flowForm.bizType" placeholder="请选择" style="width: 100%" :disabled="isEditFlow">
                <el-option v-for="(label, key) in bizTypeLabel" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程名称" prop="flowName">
              <el-input v-model="flowForm.flowName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="版本号">
              <el-input-number v-model="flowForm.version" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="flowForm.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">审批节点配置</el-divider>
        <div v-for="(node, idx) in flowForm.nodes" :key="idx" style="margin-bottom: 12px">
          <el-row :gutter="8" align="middle">
            <el-col :span="2"><el-tag size="small">{{ idx + 1 }}</el-tag></el-col>
            <el-col :span="7">
              <el-input v-model="node.nodeName" placeholder="节点名称" />
            </el-col>
            <el-col :span="6">
              <el-select v-model="node.approverType" placeholder="审批人类型" style="width: 100%">
                <el-option label="指定用户" value="user" />
                <el-option label="指定角色" value="role" />
                <el-option label="部门主管" value="dept_leader" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-input-number v-if="node.approverType !== 'dept_leader'" v-model="node.approverId" placeholder="用户/角色ID" :min="1" controls-position="right" style="width: 100%" />
              <span v-else style="color: #909399; font-size: 12px">自动匹配</span>
            </el-col>
            <el-col :span="3">
              <el-button type="danger" link @click="removeNode(idx)">删除</el-button>
            </el-col>
          </el-row>
        </div>
        <el-button type="primary" link @click="addNode">+ 添加审批节点</el-button>
      </el-form>
      <template #footer>
        <el-button @click="flowDlg = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitFlow">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  getFlowDefList, createFlowDef, updateFlowDef, deleteFlowDef,
  getMyPending, getMyInitiated, getInstanceDetail,
  approveInstance, rejectInstance, withdrawInstance,
  transferInstance, addCosigner, sendReadHandle, sendCc,
  approveCosign, markCcHandled
} from '@/api/approval'
import { getUserList } from '@/api/user'

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('pending')
const page = ref(1)
const total = ref(0)

const pendingData = ref([])
const initiatedData = ref([])
const flowData = ref([])

const bizTypeLabel = {
  project: '立项', contract: '合同', purchase: '采购清单', spot_purchase: '零星采购',
  inbound: '入库单', outbound: '出库单', return_order: '退库单',
  inventory_check: '盘点单', change_order: '变更单', gantt_task: '进度计划',
  statement: '对账单', income_split: '收入拆分',
  payment: '付款申请', reimburse: '报销单', salary: '工资单',
  hr_entry: '入职申请', hr_resign: '离职申请',
  completion: '完工验收', labor_settlement: '劳务结算'
}

const statusLabel = { draft: '草稿', pending: '审批中', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
const statusType = { draft: '', pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }

const actionLabel = {
  approve: '通过', reject: '驳回', cancel: '撤回',
  delegate: '转办', auto_transfer: '系统转办',
  cosign_add: '加签', cosign_approve: '会签通过',
  read: '阅办', cc: '阅知'
}
const actionTagType = {
  approve: 'success', reject: 'danger', cancel: 'info',
  delegate: 'warning', auto_transfer: 'warning',
  cosign_add: '', cosign_approve: 'success',
  read: '', cc: ''
}

// ====== 审批操作 ======
const actionDlg = ref(false)
const actionTitle = ref('')
const actionType = ref('')
const actionRow = ref({})
const actionOpinion = ref('')

// ====== 用户选择(转办/加签/阅办/阅知) ======
const userPickDlg = ref(false)
const userPickTitle = ref('')
const userPickMode = ref('') // transfer / cosign / read_handle / cc
const userPickRow = ref({})
const userPickTarget = ref(null)
const userPickOpinion = ref('')
const userOptions = ref([])
const userSearching = ref(false)

// ====== 审批详情 ======
const detailDlg = ref(false)
const detailData = ref({})

// ====== 会签审批 ======
const cosignDlg = ref(false)
const cosignRow = ref({})
const cosignOpinion = ref('')

// ====== 流程定义 ======
const flowDlg = ref(false)
const isEditFlow = ref(false)
const editFlowId = ref(null)
const flowFormRef = ref(null)
const flowForm = reactive({
  bizType: '', flowName: '', version: 1, status: 1,
  nodes: [{ nodeName: '', approverType: 'user', approverId: null }]
})
const flowRules = {
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  flowName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      const res = await getMyPending({ page: page.value, size: 20 })
      pendingData.value = res.data.records || []
      total.value = res.data.total || 0
    } else if (activeTab.value === 'initiated') {
      const res = await getMyInitiated({ page: page.value, size: 20 })
      initiatedData.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      const res = await getFlowDefList({ page: page.value, size: 20 })
      flowData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally { loading.value = false }
}

const handleTabChange = () => { page.value = 1; fetchData() }

// ====== 搜索用户 ======
const searchUsers = async (query) => {
  if (!query || query.length < 1) { userOptions.value = []; return }
  userSearching.value = true
  try {
    const res = await getUserList({ keyword: query, size: 20 })
    userOptions.value = res.data.records || []
  } catch { userOptions.value = [] }
  finally { userSearching.value = false }
}

// ====== 审批操作 ======
const handleApprove = (row) => {
  actionRow.value = row; actionType.value = 'approve'
  actionTitle.value = '审批通过'; actionOpinion.value = ''; actionDlg.value = true
}

const handleReject = (row) => {
  actionRow.value = row; actionType.value = 'reject'
  actionTitle.value = '审批驳回'; actionOpinion.value = ''; actionDlg.value = true
}

const submitAction = async () => {
  submitting.value = true
  try {
    if (actionType.value === 'approve') {
      await approveInstance(actionRow.value.id, actionOpinion.value)
      ElMessage.success('审批通过')
    } else {
      await rejectInstance(actionRow.value.id, actionOpinion.value)
      ElMessage.success('已驳回')
    }
    actionDlg.value = false
    fetchData()
  } finally { submitting.value = false }
}

const resetAction = () => { actionRow.value = {}; actionOpinion.value = '' }

// ====== 撤回 ======
const handleWithdraw = async (row) => {
  await ElMessageBox.confirm('确定撤回该审批？撤回后需重新提交。', '提示', { type: 'warning' })
  await withdrawInstance(row.id)
  ElMessage.success('已撤回')
  fetchData()
}

// ====== 更多操作(转办/加签/阅办/阅知) ======
const handleMoreAction = (command, row) => {
  userPickRow.value = row
  userPickMode.value = command
  userPickTarget.value = command === 'cc' ? [] : null
  userPickOpinion.value = ''
  userOptions.value = []
  const titles = { transfer: '转办', cosign: '加签', read_handle: '阅办', cc: '阅知(抄送)' }
  userPickTitle.value = titles[command] || command
  userPickDlg.value = true
}

const submitUserPick = async () => {
  if (userPickMode.value === 'cc') {
    if (!userPickTarget.value || userPickTarget.value.length === 0) {
      ElMessage.warning('请选择抄送人'); return
    }
  } else {
    if (!userPickTarget.value) {
      ElMessage.warning('请选择目标用户'); return
    }
  }
  submitting.value = true
  try {
    const id = userPickRow.value.id
    if (userPickMode.value === 'transfer') {
      await transferInstance(id, { targetUserId: userPickTarget.value, opinion: userPickOpinion.value })
      ElMessage.success('已转办')
    } else if (userPickMode.value === 'cosign') {
      await addCosigner(id, { cosignerId: userPickTarget.value, opinion: userPickOpinion.value })
      ElMessage.success('已加签')
    } else if (userPickMode.value === 'read_handle') {
      await sendReadHandle(id, { targetUserId: userPickTarget.value })
      ElMessage.success('已发送阅办')
    } else if (userPickMode.value === 'cc') {
      await sendCc(id, { userIds: userPickTarget.value })
      ElMessage.success('已发送阅知')
    }
    userPickDlg.value = false
    fetchData()
  } finally { submitting.value = false }
}

const resetUserPick = () => {
  userPickRow.value = {}; userPickTarget.value = null; userPickOpinion.value = ''
}

// ====== 审批详情 ======
const handleViewDetail = async (row) => {
  try {
    const res = await getInstanceDetail(row.id)
    detailData.value = res.data
    detailDlg.value = true
  } catch { /* ignore */ }
}

// ====== 会签审批 ======
const handleCosignApprove = (row) => {
  cosignRow.value = row
  cosignOpinion.value = ''
  cosignDlg.value = true
}

const submitCosignApprove = async () => {
  submitting.value = true
  try {
    await approveCosign(cosignRow.value.id, cosignOpinion.value)
    ElMessage.success('会签审批通过')
    cosignDlg.value = false
    // 刷新详情数据
    const res = await getInstanceDetail(detailData.value.id)
    detailData.value = res.data
  } finally { submitting.value = false }
}

const resetCosign = () => { cosignRow.value = {}; cosignOpinion.value = '' }

// ====== CC 标记已处理 ======
const handleMarkCcDone = async (row) => {
  try {
    await markCcHandled(row.id)
    ElMessage.success('已标记为已处理')
    // 刷新详情数据
    const res = await getInstanceDetail(detailData.value.id)
    detailData.value = res.data
  } catch { /* ignore */ }
}

// ====== 流程定义 CRUD ======
const handleAddFlow = () => {
  isEditFlow.value = false; editFlowId.value = null
  Object.assign(flowForm, { bizType: '', flowName: '', version: 1, status: 1, nodes: [{ nodeName: '', approverType: 'user', approverId: null }] })
  flowDlg.value = true
}

const handleEditFlow = (row) => {
  isEditFlow.value = true; editFlowId.value = row.id
  let rawNodes = []
  try { rawNodes = JSON.parse(row.nodes_json || '[]') } catch { rawNodes = [] }
  const nodes = rawNodes.map(n => ({
    nodeName: n.node_name || n.nodeName || '',
    approverType: n.approver_type || n.approverType || 'user',
    approverId: n.approver_id || n.approverId || null
  }))
  if (nodes.length === 0) nodes.push({ nodeName: '', approverType: 'user', approverId: null })
  Object.assign(flowForm, { bizType: row.biz_type, flowName: row.flow_name, version: row.version, status: row.status, nodes })
  flowDlg.value = true
}

const handleDeleteFlow = async (row) => {
  await ElMessageBox.confirm('确定删除该流程定义？', '提示', { type: 'warning' })
  await deleteFlowDef(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

const addNode = () => {
  flowForm.nodes.push({ nodeName: '', approverType: 'user', approverId: null })
}

const removeNode = (idx) => {
  if (flowForm.nodes.length <= 1) { ElMessage.warning('至少保留一个审批节点'); return }
  flowForm.nodes.splice(idx, 1)
}

const submitFlow = async () => {
  await flowFormRef.value.validate()
  const validNodes = flowForm.nodes.filter(n => n.nodeName && (n.approverType === 'dept_leader' || n.approverId))
  if (validNodes.length === 0) { ElMessage.warning('请至少配置一个完整的审批节点'); return }
  const nodesWithOrder = validNodes.map((n, i) => ({
    node_order: i + 1, node_name: n.nodeName,
    approver_type: n.approverType, approver_id: n.approverType === 'dept_leader' ? null : n.approverId
  }))
  const payload = {
    bizType: flowForm.bizType,
    flowName: flowForm.flowName,
    version: flowForm.version,
    status: flowForm.status,
    nodesJson: JSON.stringify(nodesWithOrder)
  }
  submitting.value = true
  try {
    if (isEditFlow.value) {
      await updateFlowDef(editFlowId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createFlowDef(payload)
      ElMessage.success('创建成功')
    }
    flowDlg.value = false
    fetchData()
  } finally { submitting.value = false }
}

const resetFlowForm = () => {
  Object.assign(flowForm, { bizType: '', flowName: '', version: 1, status: 1, nodes: [{ nodeName: '', approverType: 'user', approverId: null }] })
  flowFormRef.value?.resetFields()
}

onMounted(() => { fetchData() })
</script>
