import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 主题状态
  const isDark = ref(false)
  const systemPreference = ref('light')
  const userPreference = ref('system') // 'light', 'dark', 'system'

  // 计算当前主题
  const currentTheme = computed(() => {
    if (userPreference.value === 'system') {
      return systemPreference.value
    }
    return userPreference.value
  })

  // 初始化主题
  const initTheme = () => {
    // 从localStorage读取用户偏好
    const saved = localStorage.getItem('theme-preference')
    if (saved && ['light', 'dark', 'system'].includes(saved)) {
      userPreference.value = saved
    }

    // 检测系统主题偏好
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemPreference.value = mediaQuery.matches ? 'dark' : 'light'
    
    // 监听系统主题变化
    mediaQuery.addEventListener('change', (e) => {
      systemPreference.value = e.matches ? 'dark' : 'light'
      applyTheme()
    })

    applyTheme()
  }

  // 应用主题
  const applyTheme = () => {
    const theme = currentTheme.value
    isDark.value = theme === 'dark'
    
    // 更新HTML根元素的data-theme属性
    document.documentElement.setAttribute('data-theme', theme)
    
    // 更新body类名
    document.body.classList.toggle('dark-theme', isDark.value)
    document.body.classList.toggle('light-theme', !isDark.value)
  }

  // 设置主题
  const setTheme = (theme) => {
    if (!['light', 'dark', 'system'].includes(theme)) return
    
    userPreference.value = theme
    localStorage.setItem('theme-preference', theme)
    applyTheme()
  }

  // 切换主题
  const toggleTheme = () => {
    const newTheme = isDark.value ? 'light' : 'dark'
    setTheme(newTheme)
  }

  return {
    isDark,
    currentTheme,
    userPreference,
    initTheme,
    setTheme,
    toggleTheme
  }
})