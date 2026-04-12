<template>
  <div class="app-container">
    <page-header :title="templateName || '动态报表'">
      <el-button @click="$router.back()">返回</el-button>
    </page-header>

    <!-- 内置报表选择 -->
    <el-card v-if="!templateId" shadow="never" style="margin-bottom:16px">
      <template #header>内置报表</template>
      <el-space wrap>
        <el-button @click="loadBuiltin('stock-flow')">进销存流水</el-button>
        <el-button @click="loadBuiltin('stock-aging')">库龄分析</el-button>
        <el-button @click="loadBuiltin('purchase-price')">采购价格对比</el-button>
      </el-space>
    </el-card>

    <!-- 动态参数 -->
    <el-card v-if="paramsDef.length" shadow="never" style="margin-bottom:16px">
      <template #header>查询参数</template>
      <el-form inline>
        <el-form-item v-for="p in paramsDef" :key="p.name" :label="p.label || p.name">
          <el-input-number v-if="p.type === 'number'" v-model="paramValues[p.name]" :placeholder="p.label" />
          <el-date-picker v-else-if="p.type === 'date'" v-model="paramValues[p.name]" type="date" value-format="YYYY-MM-DD" />
          <el-input v-else v-model="paramValues[p.name]" :placeholder="p.label" style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="execute">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" v-loading="loading">
      <!-- 图表区 -->
      <div v-if="chartVisible && chartType !== 'table'" ref="chartRef" style="height:360px;margin-bottom:16px" />

      <!-- 表格区 -->
      <el-table v-if="tableRows.length" :data="tableRows" border stripe size="small" style="margin-top:8px">
        <el-table-column v-for="col in tableColumns" :key="col" :prop="col" :label="col" show-overflow-tooltip />
      </el-table>
      <el-empty v-else-if="!loading" description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getReportTemplate, executeReportTemplate,
  getStockFlowReport, getStockAgingReport, getPurchasePriceComparison
} from '@/api/report'

const route = useRoute()
const templateId = computed(() => route.query.id)
const templateName = ref('')
const chartType = ref('table')
const xField = ref('')
const yFields = ref([])
const paramsDef = ref([])
const paramValues = reactive({})
const loading = ref(false)
const tableRows = ref([])
const tableColumns = ref([])
const chartRef = ref(null)
const chartVisible = ref(false)
let chartInstance = null
let echarts = null

async function ensureEcharts() {
  if (!echarts) echarts = await import('echarts')
  return echarts
}

async function loadTemplate() {
  if (!templateId.value) return
  try {
    const res = await getReportTemplate(templateId.value)
    const t = res.data
  templateName.value = t.report_name
  chartType.value = t.chart_type || 'table'
  xField.value = t.x_field || ''
  yFields.value = t.y_fields ? t.y_fields.split(',').map(s => s.trim()).filter(Boolean) : []
  paramsDef.value = []
  Object.keys(paramValues).forEach(key => delete paramValues[key])
  if (t.params_json) {
    try {
      paramsDef.value = JSON.parse(t.params_json)
      paramsDef.value.forEach(p => { paramValues[p.name] = p.defaultValue ?? null })
    } catch {
      paramsDef.value = []
    }
  }
  execute()
  } catch (e) {
    ElMessage.error('加载报表模板失败')
  }
}

async function execute() {
  loading.value = true
  chartVisible.value = false
  try {
    let rows = []
    if (templateId.value) {
      const res = await executeReportTemplate(templateId.value, { ...paramValues })
      const payload = res.data || {}
      templateName.value = payload.reportName || templateName.value
      chartType.value = payload.chartType || chartType.value
      xField.value = payload.xField || xField.value
      yFields.value = Array.isArray(payload.yFields)
        ? payload.yFields.map(s => String(s).trim()).filter(Boolean)
        : yFields.value
      rows = payload.rows || []
    }
    tableColumns.value = rows.length ? Object.keys(rows[0]) : []
    tableRows.value = rows
    if (rows.length && chartType.value !== 'table' && xField.value && yFields.value.length) {
      chartVisible.value = true
      await nextTick()
      await renderChart(rows)
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '报表执行失败')
    tableRows.value = []
    tableColumns.value = []
  } finally {
    loading.value = false
  }
}

async function loadBuiltin(type) {
  loading.value = true
  chartVisible.value = false
  chartType.value = 'table'
  try {
    let res
    if (type === 'stock-flow') { res = await getStockFlowReport(); templateName.value = '进销存流水' }
    else if (type === 'stock-aging') { res = await getStockAgingReport(); templateName.value = '库龄分析' }
    else if (type === 'purchase-price') { res = await getPurchasePriceComparison(); templateName.value = '采购价格对比' }
    const rows = res?.data || []
    tableColumns.value = rows.length ? Object.keys(rows[0]) : []
    tableRows.value = rows
  } catch (e) {
    ElMessage.error('加载内置报表失败')
    tableRows.value = []
    tableColumns.value = []
  } finally {
    loading.value = false
  }
}

async function renderChart(rows) {
  try {
    const ec = await ensureEcharts()
    if (chartInstance) chartInstance.dispose()
    const el = chartRef.value
    if (!el) return
    chartInstance = ec.init(el)
    const categories = rows.map(r => r[xField.value] ?? '')
    let option
    if (chartType.value === 'pie') {
      option = {
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left' },
        series: yFields.value.slice(0, 1).map(field => ({
          name: field,
          type: 'pie',
          radius: '60%',
          data: rows.map(r => ({ name: r[xField.value], value: r[field] }))
        }))
      }
    } else if (chartType.value === 'radar') {
      const maxValue = Math.max(...yFields.value.flatMap(field => rows.map(r => Number(r[field]) || 0)), 1)
      option = {
        tooltip: {},
        legend: { data: rows.map(r => r[xField.value]) },
        radar: { indicator: yFields.value.map(field => ({ name: field, max: maxValue })) },
        series: [{
          type: 'radar',
          data: rows.map(r => ({ name: r[xField.value], value: yFields.value.map(field => Number(r[field]) || 0) }))
        }]
      }
    } else {
      option = {
        tooltip: { trigger: 'axis' },
        legend: { data: yFields.value },
        xAxis: { type: 'category', data: categories },
        yAxis: { type: 'value' },
        series: yFields.value.map(field => ({
          name: field,
          type: chartType.value,
          data: rows.map(r => r[field])
        }))
      }
    }
    chartInstance.setOption(option)
  } catch {
    ElMessage.warning('图表渲染失败，请检查字段配置')
    chartVisible.value = false
  }
}

onBeforeUnmount(() => { chartInstance?.dispose() })

onMounted(() => {
  if (templateId.value) loadTemplate()
})
</script>
