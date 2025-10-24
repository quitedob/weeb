import { createApp } from 'vue'
import { createPinia } from 'pinia'; // Added
import { createPersistedState } from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import ElementPlus from 'element-plus' // 导入 Element Plus
import 'element-plus/dist/index.css' // 导入 Element Plus 样式

// 导入统一的样式系统
import './assets/main.css'

// 导入Apple风格的工具类
import appleMessage from './utils/appleMessage'
import appleConfirm from './utils/appleConfirm'
import Icons from './utils/appleIcons'

const app = createApp(App)

app.use(ElementPlus) // 全局注册 Element Plus

const pinia = createPinia(); // Added
pinia.use(createPersistedState({
  auto: true, // 自动为所有 store 启用持久化
  storage: localStorage,
}))
app.use(pinia);             // Added

// 全局注册Apple风格的工具和图标
app.config.globalProperties.$message = appleMessage
app.config.globalProperties.$confirm = appleConfirm
app.config.globalProperties.$icons = Icons

app.use(router)
app.config.globalProperties.$axios = axios  // 如果需要全局使用 axios

app.mount('#app')
