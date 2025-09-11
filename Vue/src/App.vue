<template>
  <div id="app">
      <router-view />
  </div>
</template>

<script>
export default {
  name: "App",
};
</script>

<style>
@import './assets/apple-style.css';

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
