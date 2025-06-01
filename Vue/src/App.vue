<template>
  <div id="app" class = "main">

    <!-- 主内容区域，基于路由动态加载页面 -->
    <main class ="main">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: "App",
};
</script>

<style>/* 字体颜色：蓝色 */

.main{
  background-color: white;
}

/* 全局样式 */
#app {
  font-family: Arial, sans-serif;
  color: #333;
  background-color: #f5f5f5;
  height: 100%;
}


</style>
