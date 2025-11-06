import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 导入统一的样式系统
import './assets/main.css'

// 导入Apple风格的工具类
import appleMessage from './utils/appleMessage'
import appleConfirm from './utils/appleConfirm'
import Icons from './utils/appleIcons'

// 导入Store配置
import { setupPinia } from './stores/setup'
import { initializeStores } from './stores'

const app = createApp(App)

// 配置Element Plus
app.use(ElementPlus)

// 配置Pinia（使用我们的增强配置）
const pinia = setupPinia({
  enablePersist: true,
  enableMonitor: import.meta.env.DEV,
  enableDevTools: import.meta.env.DEV,
  enableHistory: import.meta.env.DEV,
})
app.use(pinia)

// 全局注册Apple风格的工具和图标
app.config.globalProperties.$message = appleMessage
app.config.globalProperties.$confirm = appleConfirm
app.config.globalProperties.$icons = Icons
app.config.globalProperties.$axios = axios

// 配置路由
app.use(router)

// 挂载应用
app.mount('#app')

// 初始化所有Store（在应用挂载后）
initializeStores()
