<template>
  <el-tag v-if="label" :type="tagType" :effect="effect">{{ label }}</el-tag>
  <span v-else>{{ value }}</span>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useDictStore } from '@/stores/dict'

const props = defineProps({
  type: { type: String, required: true },
  value: { type: [String, Number], default: '' },
  effect: { type: String, default: 'light' }
})

const dictStore = useDictStore()

onMounted(() => {
  dictStore.loadDict(props.type)
})

const label = computed(() => dictStore.getDictLabel(props.type, props.value))
const tagType = computed(() => dictStore.getDictListClass(props.type, props.value))
</script>
