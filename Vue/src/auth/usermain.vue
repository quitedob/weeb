<template>
  <div id="usermain" class="total-white"> <!-- 与 name 保持一致 -->
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="logo">小蓝盒网站 创作者中心</div>
      <nav class="nav">
        <router-link class="nav1" to="/userinform">{{ username }}</router-link>

        <router-link to="/logout" >登出</router-link>
      </nav>
    </header>

    <!-- 主体布局 -->
    <div class="main-container">
      <!-- 左侧固定浮窗 -->
      <aside class="sidebar">
        <button @click="write"class="write-btn">写文章</button>
        <ul>
          <router-link to="/articlemain" >首页</router-link>
          <router-link to="/articlemanage" >文章管理</router-link>
          <router-link to="/video" >视频demo</router-link>
          <router-link to="/helpcenter" >帮助中心</router-link>
          <router-link to="/userinform" >用户中心</router-link>
        </ul>
      </aside>

      <!-- 主内容区 -->
      <main class="content">
        <div class="profile">
          <div class="user-info">
            <h2>{{ username }}</h2>
            <p>{{ userInformation.fans_count }} 粉丝 | 在创作的第 {{ registrationDays !== null ? registrationDays + ' 天' : '加载中...' }}</p>
          </div>
          <div class="data-overview">
            <h3>数据概览</h3>
            <div class="data-grid">
              <div>文章阅读量<br /><span>{{ userInformation.total_article_exposure }}</span></div>
              <div>用户金币数<br /><span>{{ userInformation.website_coins }}</span></div>
              <div>文章点赞数<br /><span>{{ userInformation.total_likes }}</span></div>
              <div>文章收藏数<br /><span>{{ userInformation.total_favorites }}</span></div>

            </div>
          </div>
        </div>
        <div class="tasks">
          <h3>创作任务</h3>
          <ul>
            <li>
              首次成功发布文章：奖励石 5000 <button>去完成</button>
            </li>
            <li>
              每天完成 3 次点赞：奖励金币 500 <button>去完成</button>
            </li>
          </ul>
        </div>
      </main>

      <!-- 右侧固定浮窗 -->
      <aside class="right-panel">
        <div class="activities">
          <h3>创作活动</h3>
          <p>如何使用小蓝盒</p>
        </div>
        <div class="topics">
          <h3>创作话题</h3>
          <ul>
            <li># 2024年终总结</li>
            <li># 小蓝盒网站活动</li>
            <li># 每天一个知识点</li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import { getUserByUsername, getCurrentUser } from '@/api/modules/user';

export default {
  name: "usermain",
  data() {
    return {
      isLoggedIn: false,
      username: '',
      userInformation: {}, // 存储用户的其他信息
      registrationDate: '',     // 注册日期
      registrationDays: null,   // 注册天数
    };
  },
  methods: {
    // 检查用户登录状态
    async checkLoginStatus() {
      const token = localStorage.getItem('token');
      if (!token) {
        alert('尚未登录, 将跳转到登录页面');
        this.$router.push('/login');
        return;
      }

      try {
        // ✅ 使用新的API获取当前用户信息
        const response = await getCurrentUser();
        this.isLoggedIn = true;
        this.username = response.data.user?.username || '已登录用户';
        
        // 获取用户的其他信息
        await this.fetchUserInformation();
      } catch (error) {
        console.error('用户身份验证失败', error);
        alert('身份验证失败，将跳转到登录页面');
        localStorage.removeItem('token'); // 移除失效令牌
        this.$router.push('/login');
      }
    },
    
    write(){
      this.$router.push('/articlewrite');
    },
    
    // ✅ 获取用户其他信息 - 使用新的API
    async fetchUserInformation() {
      try {
        // 使用新的getUserByUsername API
        const response = await getUserByUsername(this.username);
        console.log('用户信息获取成功:', response.data);

        // 解构响应数据
        const { user, stats } = response.data;

        // 将获取到的用户信息保存到 Vue 状态中
        this.userInformation = {
          fans_count: stats?.followersCount || 0,
          total_article_exposure: stats?.totalViews || 0,
          website_coins: stats?.totalCoins || 0,
          total_likes: stats?.likesCount || 0,
          total_favorites: stats?.totalFavorites || 0,
          registrationDate: user?.createdAt || user?.registrationDate
        };

        // 如果返回数据中包含 registrationDate
        if (this.userInformation.registrationDate) {
          // 注册日期字符串
          const registrationDateStr = this.userInformation.registrationDate;

          // 将 ISO 8601 格式的日期字符串解析为 JavaScript Date 对象
          const registrationDate = new Date(registrationDateStr);

          // 获取当前时间并计算本地时间差
          const now = new Date();
          const timeDifference = now.getTime() - registrationDate.getTime();
          const calculatedDays = Math.floor(timeDifference / (1000 * 60 * 60 * 24));

          // 保存本地格式化后的日期和重新计算的天数到状态中
          this.registrationDate = registrationDate.toLocaleString(); // 格式化为本地时间字符串
          this.registrationDays = calculatedDays; // 本地重新计算的天数
        }
      } catch (error) {
        console.error('获取用户信息失败:', error.response?.data || error.message);
        alert(`获取用户信息失败: ${error.response?.status || 'Unknown'} ${error.response?.statusText || error.message}`);
      }
    }
  },
  created() {
    this.checkLoginStatus(); // 组件创建时检查登录状态
  },
};
</script>



<style scoped>
.total-white{
  background-color: white;
}
/* 全局样式 */
body {
  margin: 0;
  font-family: Arial, sans-serif;
  background-color: #f5f6fa;
  color: #333;
}

/* 顶部导航栏 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #1e80ff;
  color: white;
  padding: 1rem;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%; /* 保证 header 不超出页面宽度 */
  z-index: 1000;
  box-sizing: border-box; /* 确保 padding 不影响宽度 */
  overflow: hidden; /* 防止子元素溢出 */
}
.header .logo {
  font-size: 1.5rem;
  font-weight: bold;
}
.header .nav {
  display: flex; /* 使用 flexbox 布局 */
  flex-wrap: wrap; /* 如果内容过多，则自动换行 */
}
.header .nav1 {
  color: white;
  margin-top: 0.1rem; /* 调整垂直位置 */
  font-size: 1.2rem; /* 控制字体大小 */
  white-space: nowrap; /* 防止换行 */
}
.header .nav a {
  color: white; /* 确保字体颜色为白色 */
  margin-left: 1rem; /* 设置链接间的间隔 */
  text-decoration: none; /* 去掉下划线 */
  white-space: nowrap; /* 防止文字换行 */
}

.header .nav a:hover {
  text-decoration: underline; /* 增加悬停效果，提升用户体验 */
}

/* 页面主体 */
.main-container {
  display: flex;
  background-color: white;
  margin: 4rem 1rem 1rem 1rem; /* 给顶部导航栏留出空间 */
}

/* 左侧固定浮窗 */
.sidebar {

  width: 200px;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  position: fixed;
  top: 70px;
  left: 0;
  height: auto; /* 长度适中 */
  overflow-y: auto; /* 滚动条 */
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
}
.sidebar .write-btn {
  display: block;
  width: 100%;
  padding: 0.5rem;
  margin-bottom: 1rem;
  background-color: #1e80ff;
  color: white;
  border: none;
  border-radius: 5px;
  text-align: center;
}
.sidebar ul {
  list-style: none;
  padding: 0;
}
.sidebar li {
  margin: 0.5rem 0;
}
.sidebar a {
  text-decoration: none;
  color: #333;
  padding: 0.5rem;
  display: block;
  border-radius: 4px;
}
.sidebar a:hover {
  background-color: #f0f0f0;
}

/* 主内容 */
.content {
  flex: 1;
  margin: 0 1rem;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  margin-left: 220px; /* 给左侧浮窗留出空间 */
  margin-right: 320px; /* 给右侧浮窗留出空间 */
}
.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  margin: 1rem 0;
}
.data-grid div {
  background: #f9f9f9;
  padding: 1rem;
  text-align: center;
  border-radius: 8px;
}
.right-panel {
  width: 300px; /* 固定宽度 */
  background: white; /* 背景颜色 */
  padding: 1rem; /* 内边距 */
  border-radius: 8px; /* 圆角边框 */
  position: fixed; /* 固定定位 */
  top: 70px; /* 距离顶部70px */
  right: 30px; /* 距离右侧10px，确保有适当间距 */
  height: auto; /* 高度根据内容自动调整 */
  overflow-y: auto; /* 垂直方向出现滚动条时自动显示 */
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1); /* 阴影效果 */
  box-sizing: border-box; /* 包括内边距和边框在内计算元素的总宽度 */
}

.activities,
.topics {
  margin-bottom: 1rem;
}
</style>
