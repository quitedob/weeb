<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-description">抱歉，您访问的页面不存在或已被移动。</p>
      <router-link to="/" class="home-link">
        返回首页
      </router-link>
    </div>
  </div>
</template>

<script setup>
// NotFound 组件逻辑
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.not-found-content {
  text-align: center;
  padding: 2rem;
}

.error-code {
  font-size: 8rem;
  font-weight: bold;
  color: #409eff;
  margin: 0;
  line-height: 1;
}

.error-title {
  font-size: 2rem;
  color: #666;
  margin: 1rem 0;
}

.error-description {
  font-size: 1.1rem;
  color: #999;
  margin-bottom: 2rem;
}

.home-link {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background-color: #409eff;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.home-link:hover {
  background-color: #337ecc;
}
</style>

