<template>
  <div class="purchase-page">
    <el-card shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="pending" />
            <el-option label="已审批" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <el-tabs v-model="activeTab">
        <!-- ==================== 采购清单 ==================== -->
        <el-tab-pane label="采购清单" name="list">
          <div style="margin-bottom: 12px">
            <el-button type="primary" @click="handleAddPurchase" v-permission="'purchase:list-manage'">新建采购清单</el-button>
          </div>
          <el-table :data="purchaseData" v-loading="loading" stripe border>
            <el-table-column prop="list_no" label="清单编号" width="140" />
            <el-table-column prop="project_id" label="项目ID" width="90" />
            <el-table-column prop="total_amount" label="总金额" width="130" align="right">
              <template #default="{ row }">{{ row.total_amount ? Number(row.total_amount).toLocaleString() : '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusType[row.status]" size="small">
                  {{ statusText[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="创建时间" width="170" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEditPurchase(row)" v-permission="'purchase:list-manage'">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeletePurchase(row)" v-permission="'purchase:list-manage'">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-if="purchaseTotal > 0" style="margin-top: 16px; justify-content: flex-end" background
            layout="total, prev, pager, next" :total="purchaseTotal"
            v-model:current-page="queryForm.page" @current-change="fetchPurchases" />
        </el-tab-pane>

        <!-- ==================== 零星采购 ==================== -->
        <el-tab-pane label="零星采购" name="spot">
          <div style="margin-bottom: 12px">
            <el-button type="primary" @click="handleAddSpot" v-permission="'purchase:spot-manage'">新建零星采购</el-button>
          </div>
          <el-table :data="spotData" v-loading="spotLoading" stripe border>
            <el-table-column prop="purchase_no" label="采购编号" width="140" />
            <el-table-column prop="item_name" label="物品名称" min-width="150" />
            <el-table-column prop="quantity" label="数量" width="100" align="right" />
            <el-table-column prop="unit_price" label="单价" width="110" align="right" />
            <el-table-column prop="amount" label="金额" width="130" align="right">
              <template #default="{ row }">{{ Number(row.amount).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusType[row.status]" size="small">
                  {{ statusText[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="创建时间" width="170" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEditSpot(row)" v-permission="'purchase:spot-manage'">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteSpot(row)" v-permission="'purchase:spot-manage'">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-if="spotTotal > 0" style="margin-top: 16px; justify-content: flex-end" background
            layout="total, prev, pager, next" :total="spotTotal"
            v-model:current-page="spotPage" @current-change="fetchSpots" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ==================== 采购清单对话框 ==================== -->
    <el-dialog v-model="purchaseDialogVisible" :title="isEditPurchase ? '编辑采购清单' : '新建采购清单'" width="850px" @closed="resetPurchaseForm">
      <el-form ref="purchaseFormRef" :model="purchaseForm" :rules="purchaseRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <el-select v-model="purchaseForm.projectId" filterable placeholder="选择项目" style="width: 100%">
                <el-option v-for="p in projects" :key="p.id" :label="p.project_name || p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="purchaseForm.remark" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">采购明细</el-divider>
        <el-button type="primary" size="small" style="margin-bottom: 12px" @click="addItem">添加明细行</el-button>
        <el-table :data="purchaseForm.items" border size="small">
          <el-table-column label="物资名称" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.materialName" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="规格型号" width="120">
            <template #default="{ row }">
              <el-input v-model="row.specModel" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="80">
            <template #default="{ row }">
              <el-input v-model="row.unit" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="预估单价" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.estimatedPrice" :min="0" :precision="2" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">{{ ((row.quantity || 0) * (row.estimatedPrice || 0)).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{ $index }">
              <el-button type="danger" link size="small" @click="purchaseForm.items.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitPurchase">确定</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 零星采购对话框 ==================== -->
    <el-dialog v-model="spotDialogVisible" :title="isEditSpot ? '编辑零星采购' : '新建零星采购'" width="650px" @closed="resetSpotForm">
      <el-form ref="spotFormRef" :model="spotForm" :rules="spotRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联项目" prop="projectId">
              <el-select v-model="spotForm.projectId" filterable placeholder="选择项目" style="width: 100%">
                <el-option v-for="p in projects" :key="p.id" :label="p.project_name || p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物品名称" prop="itemName">
              <el-input v-model="spotForm.itemName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="规格型号">
              <el-input v-model="spotForm.specModel" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="spotForm.quantity" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="spotForm.unitPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-input v-model="spotForm.supplierName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额">
              <el-input :model-value="((spotForm.quantity || 0) * (spotForm.unitPrice || 0)).toFixed(2)" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="spotForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitSpot">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPurchaseList, createPurchase, updatePurchase, deletePurchase, getPurchaseItems,
  getSpotPurchaseList, createSpotPurchase, updateSpotPurchase, deleteSpotPurchase
} from '@/api/purchase'
import { getAllProjects } from '@/api/project'

const loading = ref(false)
const spotLoading = ref(false)
const submitting = ref(false)
const purchaseData = ref([])
const spotData = ref([])
const purchaseTotal = ref(0)
const spotTotal = ref(0)
const activeTab = ref('list')
const spotPage = ref(1)
const projects = ref([])

const statusType = { draft: 'info', pending: 'warning', approved: 'success', rejected: 'danger' }
const statusText = { draft: '草稿', pending: '待审批', approved: '已审批', rejected: '已驳回' }

const queryForm = reactive({ status: '', page: 1, size: 20 })

// ====== 采购清单 ======
const purchaseDialogVisible = ref(false)
const isEditPurchase = ref(false)
const purchaseFormRef = ref(null)
const editPurchaseId = ref(null)

const createEmptyItem = () => ({ materialName: '', specModel: '', unit: '', quantity: null, estimatedPrice: null, remark: '' })

const purchaseForm = reactive({
  projectId: null, remark: '',
  items: [createEmptyItem()]
})

const purchaseRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }]
}

const addItem = () => { purchaseForm.items.push(createEmptyItem()) }

// ====== 零星采购 ======
const spotDialogVisible = ref(false)
const isEditSpot = ref(false)
const spotFormRef = ref(null)
const editSpotId = ref(null)

const spotForm = reactive({
  projectId: null, itemName: '', specModel: '', quantity: null, unitPrice: null, supplierName: '', remark: ''
})

const spotRules = {
  projectId: [{ required: true, message: '请选择关联项目', trigger: 'change' }],
  itemName: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

// ====== 数据加载 ======
const fetchPurchases = async () => {
  loading.value = true
  try {
    const res = await getPurchaseList(queryForm)
    purchaseData.value = res.data.records || []
    purchaseTotal.value = res.data.total || 0
  } finally { loading.value = false }
}

const fetchSpots = async () => {
  spotLoading.value = true
  try {
    const res = await getSpotPurchaseList({ page: spotPage.value, size: 20 })
    spotData.value = res.data.records || []
    spotTotal.value = res.data.total || 0
  } finally { spotLoading.value = false }
}

const loadProjects = async () => {
  try { const res = await getAllProjects(); projects.value = res.data || [] } catch { /* ignore */ }
}

const handleSearch = () => { queryForm.page = 1; fetchPurchases() }
const handleReset = () => { queryForm.status = ''; queryForm.page = 1; fetchPurchases() }

// ====== 采购清单 CRUD ======
const handleAddPurchase = () => {
  isEditPurchase.value = false; editPurchaseId.value = null; loadProjects()
  purchaseDialogVisible.value = true
}

const handleEditPurchase = async (row) => {
  isEditPurchase.value = true; editPurchaseId.value = row.id; loadProjects()
  let existingItems = [createEmptyItem()]
  try {
    const res = await getPurchaseItems(row.id)
    const items = res.data || []
    if (items.length > 0) {
      existingItems = items.map(i => ({
        materialName: i.material_name || '', specModel: i.spec_model || '', unit: i.unit || '',
        quantity: i.quantity, estimatedPrice: i.estimated_price, remark: i.remark || ''
      }))
    }
  } catch { /* ignore */ }
  Object.assign(purchaseForm, {
    projectId: row.project_id, remark: row.remark || '',
    items: existingItems
  })
  purchaseDialogVisible.value = true
}

const resetPurchaseForm = () => {
  Object.assign(purchaseForm, { projectId: null, remark: '', items: [createEmptyItem()] })
  purchaseFormRef.value?.resetFields()
}

const handleSubmitPurchase = async () => {
  await purchaseFormRef.value.validate()
  const items = purchaseForm.items.filter(i => i.materialName)
  const totalAmount = items.reduce((s, i) => s + (i.quantity || 0) * (i.estimatedPrice || 0), 0)
  const payload = { projectId: purchaseForm.projectId, totalAmount, remark: purchaseForm.remark, items }
  submitting.value = true
  try {
    if (isEditPurchase.value) { await updatePurchase(editPurchaseId.value, payload); ElMessage.success('更新成功') }
    else { await createPurchase(payload); ElMessage.success('创建成功') }
    purchaseDialogVisible.value = false; fetchPurchases()
  } finally { submitting.value = false }
}

const handleDeletePurchase = async (row) => {
  await ElMessageBox.confirm('确定删除该采购清单？', '提示', { type: 'warning' })
  await deletePurchase(row.id); ElMessage.success('删除成功'); fetchPurchases()
}

// ====== 零星采购 CRUD ======
const handleAddSpot = () => {
  isEditSpot.value = false; editSpotId.value = null; loadProjects()
  spotDialogVisible.value = true
}

const handleEditSpot = (row) => {
  isEditSpot.value = true; editSpotId.value = row.id; loadProjects()
  Object.assign(spotForm, {
    projectId: row.project_id, itemName: row.item_name || '', specModel: row.spec_model || '',
    quantity: row.quantity, unitPrice: row.unit_price, supplierName: row.supplier_name || '', remark: row.remark || ''
  })
  spotDialogVisible.value = true
}

const resetSpotForm = () => {
  Object.assign(spotForm, { projectId: null, itemName: '', specModel: '', quantity: null, unitPrice: null, supplierName: '', remark: '' })
  spotFormRef.value?.resetFields()
}

const handleSubmitSpot = async () => {
  await spotFormRef.value.validate()
  const payload = { ...spotForm, amount: (spotForm.quantity || 0) * (spotForm.unitPrice || 0) }
  submitting.value = true
  try {
    if (isEditSpot.value) { await updateSpotPurchase(editSpotId.value, payload); ElMessage.success('更新成功') }
    else { await createSpotPurchase(payload); ElMessage.success('创建成功') }
    spotDialogVisible.value = false; fetchSpots()
  } finally { submitting.value = false }
}

const handleDeleteSpot = async (row) => {
  await ElMessageBox.confirm('确定删除该零星采购？', '提示', { type: 'warning' })
  await deleteSpotPurchase(row.id); ElMessage.success('删除成功'); fetchSpots()
}

onMounted(() => { fetchPurchases(); fetchSpots() })
</script>
