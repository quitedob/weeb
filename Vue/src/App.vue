<template>
  <div id="app">
      <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

// 简单的主题初始化
onMounted(() => {
  // 检测系统主题偏好
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const savedTheme = localStorage.getItem('theme-preference') || 'system'
  
  const applyTheme = () => {
    let theme = savedTheme
    if (savedTheme === 'system') {
      theme = mediaQuery.matches ? 'dark' : 'light'
    }
    
    document.documentElement.setAttribute('data-theme', theme)
    document.body.classList.toggle('dark-theme', theme === 'dark')
    document.body.classList.toggle('light-theme', theme === 'light')
  }
  
  // 监听系统主题变化
  mediaQuery.addEventListener('change', applyTheme)
  
  // 初始化主题
  applyTheme()
})
</script>

<style>
/* Apple设计系统已在main.css中导入，无需重复导入 */

/* 全局样式 */
#app {
  height: 100vh;
  background-color: var(--apple-bg-primary);
  color: var(--apple-text-primary);
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 页面过渡动画 */
.router-view-enter-active,
.router-view-leave-active {
  transition: all 0.3s ease;
}

.router-view-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.router-view-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
