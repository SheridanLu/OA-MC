<!-- src/components/MobileDegradeWrapper.vue -->
<template>
  <component :is="resolvedComponent" v-bind="$attrs" />
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { isMobile, MOBILE_DEGRADE_MAP } from '@/config/mobile-features'

const props = defineProps({
  /** PC 端组件名 */
  componentName: {
    type: String,
    required: true
  }
})

const resolvedComponent = computed(() => {
  if (isMobile() && MOBILE_DEGRADE_MAP[props.componentName]) {
    const mobileName = MOBILE_DEGRADE_MAP[props.componentName]
    return defineAsyncComponent(() =>
      import(`@/views/${mobileName}.vue`))
  }
  return defineAsyncComponent(() =>
    import(`@/views/${props.componentName}.vue`))
})
</script>
