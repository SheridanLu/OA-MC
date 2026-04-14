<template>
  <el-tag :type="tagType" :effect="effect" size="small" disable-transitions>{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const STATUS_MAP = {
  draft:     { label: '草稿',   type: 'info' },
  pending:   { label: '待审批', type: 'warning' },
  approved:  { label: '已审批', type: 'success' },
  rejected:  { label: '已驳回', type: 'danger' },
  cancelled: { label: '已取消', type: 'info' },
  confirmed: { label: '已确认', type: 'success' },
  collected: { label: '已领取', type: 'success' },
  returned:  { label: '已退回', type: 'warning' },
  closed:    { label: '已关闭', type: 'info' },
  active:    { label: '进行中', type: '' },
  tracking:  { label: '跟踪中', type: 'info' },
  suspended: { label: '已暂停', type: 'warning' },
  terminated:{ label: '已终止', type: 'danger' },
  completion_accepted: { label: '已完工验收', type: 'success' },
  final_accepted:      { label: '已竣工验收', type: 'success' },
  audit_done:          { label: '已完成审计', type: 'success' },
  completed: { label: '已完成', type: 'success' },
  converted: { label: '已转实体', type: 'success' },
  virtual:   { label: '虚拟',   type: 'info' },
  entity:    { label: '实体',   type: '' },
  overdue:   { label: '逾期',   type: 'danger' },
  normal:    { label: '正常',   type: 'success' },
  paid:      { label: '已付款', type: 'success' },
  unpaid:    { label: '未付款', type: 'warning' }
}

const props = defineProps({
  status: { type: String, default: '' },
  effect: { type: String, default: 'light' }
})

const tagType = computed(() => STATUS_MAP[props.status]?.type ?? 'info')
const label = computed(() => STATUS_MAP[props.status]?.label ?? props.status)
</script>
