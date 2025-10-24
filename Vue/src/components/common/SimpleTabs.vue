<template>
  <div class="simple-tabs">
    <div class="tab-nav">
      <div
        v-for="tab in tabs"
        :key="tab.name"
        :class="['tab-item', { active: activeTab === tab.name }]"
        @click="activeTab = tab.name"
      >
        {{ tab.label }}
      </div>
    </div>
    <div class="tab-content">
      <slot :activeTab="activeTab"></slot>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  tabs: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const activeTab = ref(props.modelValue || 'profile')
const tabs = ref([])

// 获取所有tab pane
const collectTabs = () => {
  const tabPanes = []
  // 根据使用场景动态设置tab
  if (props.tabs && props.tabs.length > 0) {
    tabPanes.push(...props.tabs)
  } else {
    // 默认设置页面的tabs
    tabPanes.push({ name: 'profile', label: '基本信息' })
    tabPanes.push({ name: 'privacy', label: '隐私设置' })
    tabPanes.push({ name: 'notifications', label: '通知设置' })
    tabPanes.push({ name: 'security', label: '安全设置' })
  }
  tabs.value = tabPanes
}

watch(activeTab, (newValue) => {
  emit('update:modelValue', newValue)
})

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    activeTab.value = newValue
  }
})

onMounted(() => {
  collectTabs()
})
</script>

<style scoped>
.simple-tabs {
  width: 100%;
}

.tab-nav {
  display: flex;
  border-bottom: 1px solid #e5e5e7;
  margin-bottom: 20px;
}

.tab-item {
  padding: 12px 20px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
  font-weight: 500;
  color: #666;
}

.tab-item:hover {
  color: #007aff;
}

.tab-item.active {
  color: #007aff;
  border-bottom-color: #007aff;
}

.tab-content {
  width: 100%;
}
</style>