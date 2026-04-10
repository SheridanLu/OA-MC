import { ref, computed, onMounted } from 'vue'
import { useDictStore } from '@/stores/dict'

/**
 * 字典 composable — 从后端 API 获取字典数据，Pinia 缓存
 * 返回签名与旧版一致：{ options, getLabel }
 */

// 旧版硬编码兜底（API 未返回时使用）
const FALLBACK = {
  status: [
    { value: 'draft', label: '草稿' }, { value: 'pending', label: '待审批' },
    { value: 'approved', label: '已审批' }, { value: 'rejected', label: '已驳回' },
    { value: 'cancelled', label: '已取消' }, { value: 'confirmed', label: '已确认' },
    { value: 'collected', label: '已领取' }, { value: 'returned', label: '已退回' },
    { value: 'closed', label: '已关闭' }, { value: 'active', label: '进行中' },
    { value: 'suspended', label: '已暂停' }, { value: 'terminated', label: '已终止' },
    { value: 'completed', label: '已完成' }, { value: 'virtual', label: '虚拟' },
    { value: 'entity', label: '实体' }, { value: 'overdue', label: '逾期' },
    { value: 'normal', label: '正常' }, { value: 'paid', label: '已付款' },
    { value: 'unpaid', label: '未付款' }
  ]
}

// 旧 dictType → 新 dictType 映射（兼容旧调用）
const TYPE_ALIAS = {
  status: 'biz_status'
}

export function useDict(dictType) {
  const dictStore = useDictStore()
  const resolvedType = TYPE_ALIAS[dictType] || dictType

  onMounted(() => {
    dictStore.loadDict(resolvedType)
  })

  const options = computed(() => {
    const storeOpts = dictStore.getDictOptions(resolvedType)
    if (storeOpts.length > 0) return storeOpts
    return FALLBACK[dictType] || []
  })

  const getLabel = (val) => {
    const storeLabel = dictStore.getDictLabel(resolvedType, val)
    if (storeLabel !== val) return storeLabel
    const fb = (FALLBACK[dictType] || []).find(o => o.value === String(val))
    return fb?.label || val
  }

  return { options, getLabel }
}
