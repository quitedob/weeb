import { createApp } from 'vue'
import { createPinia } from 'pinia'; // Added
import { createPersistedState } from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import axios from 'axios'

const app = createApp(App)

const pinia = createPinia(); // Added
pinia.use(createPersistedState({
  auto: true, // 自动为所有 store 启用持久化
  storage: localStorage,
}))
app.use(pinia);             // Added
app.use(ElementPlus)        // Added Element Plus

app.use(router)
app.config.globalProperties.$axios = axios  // 如果需要全局使用 axios

app.mount('#app')
