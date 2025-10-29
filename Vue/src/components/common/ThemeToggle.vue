<template>
  <div class="theme-toggle" :class="{ compact }">
    <div v-if="!compact" class="theme-label">
      <el-icon><Moon v-if="!themeStore.isDark" /><Sunny v-else /></el-icon>
      <span>主题模式</span>
    </div>
    <div class="theme-options">
      <el-button-group :size="compact ? 'small' : 'default'">
        <el-button 
          :type="themeStore.userPreference === 'light' ? 'primary' : ''"
          @click="themeStore.setTheme('light')"
          :title="'浅色模式'"
        >
          <el-icon><Sunny /></el-icon>
          <span v-if="!compact">浅色</span>
        </el-button>
        <el-button 
          :type="themeStore.userPreference === 'system' ? 'primary' : ''"
          @click="themeStore.setTheme('system')"
          :title="'跟随系统'"
        >
          <el-icon><Monitor /></el-icon>
          <span v-if="!compact">系统</span>
        </el-button>
        <el-button 
          :type="themeStore.userPreference === 'dark' ? 'primary' : ''"
          @click="themeStore.setTheme('dark')"
          :title="'深色模式'"
        >
          <el-icon><Moon /></el-icon>
          <span v-if="!compact">深色</span>
        </el-button>
      </el-button-group>
    </div>
  </div>
</template>

<script setup>
import { useThemeStore } from '@/stores/themeStore'
import { Moon, Sunny, Monitor } from '@element-plus/icons-vue'

defineProps({
  compact: {
    type: Boolean,
    default: false
  }
})

const themeStore = useThemeStore()
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle.compact {
  gap: 8px;
}

.theme-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--apple-text-primary);
  font-size: 14px;
}

.theme-options .el-button-group {
  --el-button-size: 32px;
}

.theme-toggle.compact .theme-options .el-button-group {
  --el-button-size: 24px;
}

.theme-options .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.theme-toggle.compact .theme-options .el-button {
  padding: 4px 8px;
  min-width: 32px;
}

.theme-options .el-button .el-icon {
  font-size: 14px;
}

.theme-toggle.compact .theme-options .el-button .el-icon {
  font-size: 12px;
}

.theme-options .el-button span {
  font-size: 12px;
}
</style>