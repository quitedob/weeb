<template>
  <div class="theme-demo-page">
    <div class="demo-container">
      <div class="demo-header">
        <h1>主题系统演示</h1>
        <p>展示白天模式和暗夜模式的切换效果</p>
      </div>

      <div class="demo-content">
        <!-- 主题切换器 -->
        <div class="demo-section">
          <h2>主题切换</h2>
          <div class="theme-controls">
            <ThemeToggle />
          </div>
          <p class="demo-description">
            当前主题: <strong>{{ themeStore.currentTheme }}</strong>
            (用户偏好: {{ themeStore.userPreference }})
          </p>
        </div>

        <!-- 颜色展示 -->
        <div class="demo-section">
          <h2>颜色系统</h2>
          <div class="color-grid">
            <div class="color-item">
              <div class="color-swatch bg-primary"></div>
              <span>主背景色</span>
            </div>
            <div class="color-item">
              <div class="color-swatch bg-secondary"></div>
              <span>次背景色</span>
            </div>
            <div class="color-item">
              <div class="color-swatch bg-tertiary"></div>
              <span>三级背景色</span>
            </div>
            <div class="color-item">
              <div class="color-swatch text-primary"></div>
              <span>主文字色</span>
            </div>
            <div class="color-item">
              <div class="color-swatch text-secondary"></div>
              <span>次文字色</span>
            </div>
            <div class="color-item">
              <div class="color-swatch text-tertiary"></div>
              <span>三级文字色</span>
            </div>
          </div>
        </div>

        <!-- 组件展示 -->
        <div class="demo-section">
          <h2>组件展示</h2>
          <div class="component-grid">
            <!-- 按钮 -->
            <div class="component-demo">
              <h3>按钮</h3>
              <div class="button-group">
                <el-button type="primary">主要按钮</el-button>
                <el-button>默认按钮</el-button>
                <el-button type="success">成功按钮</el-button>
                <el-button type="danger">危险按钮</el-button>
              </div>
            </div>

            <!-- 卡片 -->
            <div class="component-demo">
              <h3>卡片</h3>
              <el-card class="demo-card">
                <template #header>
                  <span>卡片标题</span>
                </template>
                <p>这是一个演示卡片，展示在不同主题下的效果。</p>
                <p>卡片内容会根据当前主题自动调整颜色。</p>
              </el-card>
            </div>

            <!-- 输入框 -->
            <div class="component-demo">
              <h3>输入框</h3>
              <el-input v-model="demoInput" placeholder="请输入内容" />
            </div>

            <!-- 下拉菜单 -->
            <div class="component-demo">
              <h3>下拉菜单</h3>
              <el-dropdown>
                <el-button>
                  下拉菜单
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>选项1</el-dropdown-item>
                    <el-dropdown-item>选项2</el-dropdown-item>
                    <el-dropdown-item divided>选项3</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <!-- 文字展示 -->
        <div class="demo-section">
          <h2>文字层级</h2>
          <div class="text-demo">
            <h1 class="demo-title">一级标题 (H1)</h1>
            <h2 class="demo-subtitle">二级标题 (H2)</h2>
            <h3 class="demo-heading">三级标题 (H3)</h3>
            <p class="demo-text-primary">主要文字：这是主要文字颜色的展示</p>
            <p class="demo-text-secondary">次要文字：这是次要文字颜色的展示</p>
            <p class="demo-text-tertiary">辅助文字：这是辅助文字颜色的展示</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useThemeStore } from '@/stores/themeStore'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { ArrowDown } from '@element-plus/icons-vue'

const themeStore = useThemeStore()
const demoInput = ref('演示文本')
</script>

<style scoped>
.theme-demo-page {
  min-height: 100vh;
  background-color: var(--apple-bg-secondary);
  padding: 24px;
}

.demo-container {
  max-width: 1200px;
  margin: 0 auto;
}

.demo-header {
  text-align: center;
  margin-bottom: 48px;
}

.demo-header h1 {
  font-size: 36px;
  font-weight: 700;
  color: var(--apple-text-primary);
  margin-bottom: 16px;
}

.demo-header p {
  font-size: 18px;
  color: var(--apple-text-secondary);
}

.demo-content {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

.demo-section {
  background-color: var(--apple-bg-primary);
  border-radius: var(--apple-radius-large);
  padding: 32px;
  box-shadow: var(--apple-shadow-light);
  border: 1px solid var(--apple-border-secondary);
}

.demo-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--apple-text-primary);
  margin-bottom: 24px;
}

.theme-controls {
  margin-bottom: 16px;
}

.demo-description {
  color: var(--apple-text-secondary);
  font-size: 14px;
}

.color-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.color-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.color-swatch {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  border: 2px solid var(--apple-border-primary);
}

.bg-primary { background-color: var(--apple-bg-primary); }
.bg-secondary { background-color: var(--apple-bg-secondary); }
.bg-tertiary { background-color: var(--apple-bg-tertiary); }
.text-primary { background-color: var(--apple-text-primary); }
.text-secondary { background-color: var(--apple-text-secondary); }
.text-tertiary { background-color: var(--apple-text-tertiary); }

.color-item span {
  font-size: 12px;
  color: var(--apple-text-secondary);
  text-align: center;
}

.component-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.component-demo h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text-primary);
  margin-bottom: 16px;
}

.button-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.demo-card {
  margin-top: 8px;
}

.text-demo {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.demo-title {
  color: var(--apple-text-primary);
  font-size: 32px;
  font-weight: 700;
  margin: 0;
}

.demo-subtitle {
  color: var(--apple-text-primary);
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.demo-heading {
  color: var(--apple-text-primary);
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.demo-text-primary {
  color: var(--apple-text-primary);
  font-size: 16px;
  margin: 0;
}

.demo-text-secondary {
  color: var(--apple-text-secondary);
  font-size: 16px;
  margin: 0;
}

.demo-text-tertiary {
  color: var(--apple-text-tertiary);
  font-size: 16px;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .theme-demo-page {
    padding: 16px;
  }
  
  .demo-section {
    padding: 24px 16px;
  }
  
  .color-grid {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
  
  .component-grid {
    grid-template-columns: 1fr;
  }
  
  .button-group {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>