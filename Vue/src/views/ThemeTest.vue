<template>
  <div class="theme-test-page">
    <div class="test-container">
      <h1>主题切换测试页面</h1>
      <p>这个页面用于测试主题切换功能是否正常工作</p>
      
      <div class="test-section">
        <h2>当前主题信息</h2>
        <p>HTML data-theme: <code>{{ htmlTheme }}</code></p>
        <p>Body classes: <code>{{ bodyClasses }}</code></p>
        <p>localStorage: <code>{{ localStorageTheme }}</code></p>
      </div>

      <div class="test-section">
        <h2>颜色测试</h2>
        <div class="color-samples">
          <div class="color-sample bg-primary">主背景色</div>
          <div class="color-sample bg-secondary">次背景色</div>
          <div class="color-sample bg-tertiary">三级背景色</div>
        </div>
      </div>

      <div class="test-section">
        <h2>组件测试</h2>
        <div class="component-samples">
          <el-button type="primary">主要按钮</el-button>
          <el-button>默认按钮</el-button>
          <el-input v-model="testInput" placeholder="测试输入框" style="width: 200px; margin-left: 10px;" />
        </div>
      </div>

      <div class="test-section">
        <h2>手动切换测试</h2>
        <div class="manual-controls">
          <el-button @click="setTestTheme('light')">浅色模式</el-button>
          <el-button @click="setTestTheme('dark')">深色模式</el-button>
          <el-button @click="setTestTheme('system')">跟随系统</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const htmlTheme = ref('')
const bodyClasses = ref('')
const localStorageTheme = ref('')
const testInput = ref('测试文本')

let observer = null

const updateThemeInfo = () => {
  htmlTheme.value = document.documentElement.getAttribute('data-theme') || 'none'
  bodyClasses.value = document.body.className || 'none'
  localStorageTheme.value = localStorage.getItem('theme-preference') || 'none'
}

const setTestTheme = (theme) => {
  localStorage.setItem('theme-preference', theme)
  
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  let actualTheme = theme
  if (theme === 'system') {
    actualTheme = mediaQuery.matches ? 'dark' : 'light'
  }
  
  document.documentElement.setAttribute('data-theme', actualTheme)
  document.body.classList.toggle('dark-theme', actualTheme === 'dark')
  document.body.classList.toggle('light-theme', actualTheme === 'light')
  
  updateThemeInfo()
}

onMounted(() => {
  updateThemeInfo()
  
  // 监听DOM变化
  observer = new MutationObserver(updateThemeInfo)
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme']
  })
  observer.observe(document.body, {
    attributes: true,
    attributeFilter: ['class']
  })
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style scoped>
.theme-test-page {
  min-height: 100vh;
  background-color: var(--apple-bg-secondary);
  padding: 24px;
}

.test-container {
  max-width: 800px;
  margin: 0 auto;
  background-color: var(--apple-bg-primary);
  border-radius: var(--apple-radius-large);
  padding: 32px;
  box-shadow: var(--apple-shadow-light);
}

h1 {
  color: var(--apple-text-primary);
  font-size: 28px;
  margin-bottom: 16px;
}

h2 {
  color: var(--apple-text-primary);
  font-size: 20px;
  margin-bottom: 16px;
  margin-top: 32px;
}

p {
  color: var(--apple-text-secondary);
  line-height: 1.6;
}

.test-section {
  margin-bottom: 32px;
  padding: 20px;
  background-color: var(--apple-bg-secondary);
  border-radius: var(--apple-radius-medium);
  border: 1px solid var(--apple-border-secondary);
}

code {
  background-color: var(--apple-bg-tertiary);
  color: var(--apple-text-primary);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', monospace;
}

.color-samples {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.color-sample {
  padding: 16px 24px;
  border-radius: var(--apple-radius-medium);
  color: var(--apple-text-primary);
  font-weight: 500;
  border: 1px solid var(--apple-border-primary);
}

.bg-primary {
  background-color: var(--apple-bg-primary);
}

.bg-secondary {
  background-color: var(--apple-bg-secondary);
}

.bg-tertiary {
  background-color: var(--apple-bg-tertiary);
}

.component-samples {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.manual-controls {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>