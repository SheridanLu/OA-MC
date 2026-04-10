import { defineStore } from 'pinia'
import { getDictDataByType } from '@/api/dict'

export const useDictStore = defineStore('dict', {
  state: () => ({
    dictMap: {}
  }),

  actions: {
    async loadDict(dictType) {
      if (this.dictMap[dictType]) return
      try {
        const res = await getDictDataByType(dictType)
        const list = (res.data || []).map(item => ({
          value: item.dict_value,
          label: item.dict_label,
          listClass: item.list_class || '',
          colorHex: item.color_hex || '',
          raw: item
        }))
        this.dictMap[dictType] = list
      } catch {
        this.dictMap[dictType] = []
      }
    },

    async loadDicts(dictTypes) {
      await Promise.all(dictTypes.map(t => this.loadDict(t)))
    },

    getDictOptions(dictType) {
      return this.dictMap[dictType] || []
    },

    getDictLabel(dictType, value) {
      const list = this.dictMap[dictType] || []
      const item = list.find(d => d.value === String(value))
      return item?.label || value
    },

    getDictListClass(dictType, value) {
      const list = this.dictMap[dictType] || []
      const item = list.find(d => d.value === String(value))
      return item?.listClass || 'info'
    },

    getDictColorHex(dictType, value) {
      const list = this.dictMap[dictType] || []
      const item = list.find(d => d.value === String(value))
      return item?.colorHex || '#909399'
    },

    invalidate(dictType) {
      delete this.dictMap[dictType]
    },

    invalidateAll() {
      this.dictMap = {}
    }
  }
})
