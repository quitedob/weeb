<template>
  <!-- 顶部导航栏 -->
  <header class="header">
    <nav class="nav">
      <div class="logo">小蓝盒平台</div>
      <ul class="nav-links">
        <li>
          <button @click="goToUser" class="nav-link">用户</button>
        </li>
        <li>
          <button @click="goBack" class="nav-link">返回</button>
        </li>
      </ul>
    </nav>
  </header>

  <!-- 最外层容器 -->
  <div class="blog-page">
    <!-- 左侧固定的按钮栏 -->
    <div class="side-panel">
      <!-- 点赞按钮 -->
      <div class="panel-btn" @click="handleLike">
        <img :src="likeImage" alt="Like" class="panel-icon" />
      </div>
      <!-- 返回顶部按钮 -->
      <div class="panel-btn" @click="scrollToTop">
        <img :src="scrollUpImage" alt="Scroll Up" class="panel-icon" />
      </div>
    </div>
    <!-- 中间区域 -->
    <div class="form-container">
      <!-- 标题区域 -->
      <h1 class="article-title">{{ article.title }}</h1>

      <!-- 作者与元信息 -->
      <div class="author-info-block">
        <div class="author-name">
          <span>作者名称:  {{ user.username }}</span>
        </div>
        <div class="meta-box">
          <time class="time">{{ article.createdAt }}</time>
          <span class="views-count">阅读量{{ article.exposureCount }}</span>
          <span class="views-count">点赞量{{ article.likesCount }}</span>
          <span class="read-time">需要约{{ article.readTime || '5分钟' }}</span>
        </div>
      </div>

      <!-- 显示加载的文章数据 -->
      <div v-if="article.title" class="loaded-article">
        <!-- 使用加载的文章链接或内容 -->
        <p><a >{{ article.articleLink }}</a></p>
      </div>
    </div>

    <!-- 底部留白 -->
    <div class="footer">
      <p>没有版权信息和备注，没赞助</p>
    </div>
  </div>
</template>
<script>
// 导入所需库和资源
import axios from "axios";                     // 引入 axios 库用于 HTTP 请求
import likeImage from "@/picture/like.png";    // 点赞图片资源
import scrollUpImage from "@/picture/scrollup.png"; // 返回顶部图片资源

export default {
  name: "BlogPost",
  data() {
    return {
      user: {
        username: "",
        id: null,
      }, // 确保 user 对象已初始化
      likeImage, // 点赞图片
      scrollUpImage, // 返回顶部图片
      likes: 0, // 点赞数
      token: localStorage.getItem("token") || "", // 从本地存储获取用户 token
      isLoggedIn: false, // 登录状态
      article: {
        id: null,
        userId: null,
        title: "",
        articleLink: "",
        likesCount: 0,
        favoritesCount: 0,
        sponsorsCount: 0.0,
        exposureCount: 0,
        createdAt: "",
        updatedAt: "",
        contentBlocks: [], // 用于存储文章内容
      },
    };
  },
  methods: {

    async getUsername() {
      try {
        const response = await axios.get('http://localhost:8080/findByUserID', {
          params: {
            userID: this.article.userId,
          },
        });
        console.log(response.data);  // 打印返回的用户信息
        this.user = response.data;   // 将用户信息赋值给组件的数据属性
      } catch (error) {
        console.error('获取用户信息失败:', error);
      }
    },
    async beenRead() {
      try {
        // 获取文章 ID
        const articleId1 = this.article.id;

        // 发送 POST 请求
        const response = await axios.post(`http://localhost:8080/articles/${articleId1}/read`);

        console.log('阅读成功', response.data); // 打印成功信息
      } catch (error) {
        console.error('阅读失败', error); // 打印错误信息
      }
    },

    /**
     * 返回上一页
     */
    goBack() {
      window.history.back();  // 调用浏览器历史记录返回上一页
    },

    /**
     * 跳转到用户页面
     */
    goToUser() {
      this.$router.push("/usermain");  // 导航到用户主页
    },

    /**
     * 平滑滚动到页面顶部
     */
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth" // 平滑滚动
      });
    },

    /**
     * 点赞处理函数
     * 添加令牌验证，令牌存在则发送点赞请求，否则提示失败
     */
    async handleLike() {
      if (!this.token || this.token.length <= 10) {
        // 如果令牌不存在或长度无效
        alert("点赞失败，请先登录！");
        return;
      }

      try {
        // 获取文章 ID
        const articleId = this.article.id;

        // 调用后端接口发送点赞请求，附带令牌
        const response = await axios.post(
            `http://localhost:8080/articles/${articleId}/like`,
            {},
            {
              headers: {
                Authorization: `Bearer ${this.token}`, // 在请求头中携带 token
              },
            }
        );

        if (response.status === 200) {
          console.log("文章点赞成功！", response.data);
          alert("点赞成功！");
        } else {
          console.error("点赞失败：", response.statusText);
          alert("点赞失败，请稍后重试！");
        }
      } catch (error) {
        console.error("点赞失败：", error);
        alert("点赞失败，请稍后重试！");
      }
    },
    /**
     * 加载文章内容并映射到页面表单
     * 修改点：移除了令牌验证，直接获取测试文章信息
     */
    async loadArticle() {
      const articleId = this.$route.query.id;  // 从 URL query 中获取文章 ID
      if (!articleId) {
        console.log("未提供文章 ID，无法加载文章内容！");
        window.history.back();  // 调用浏览器历史记录返回上一页
        return;
      }
      try {
        // 调用后端接口获取文章内容，不再携带 token
        const response = await axios.get(`http://localhost:8080/articles/${articleId}`);
        // 检查返回的数据是否包含所需字段
        const data = response.data;
        if (data) {
          // 将返回的 JSON 数据映射到页面表单字段，一一对应
          this.article.id = data.id || null;
          this.article.userId = data.userId || null;
          this.article.title = data.articleTitle || "";
          this.article.articleLink = data.articleLink || "";
          this.article.likesCount = data.likesCount || 0;
          this.article.favoritesCount = data.favoritesCount || 0;
          this.article.sponsorsCount = data.sponsorsCount || 0.0;
          this.article.exposureCount = data.exposureCount || 0;
          this.article.createdAt = data.createdAt || "";
          this.article.updatedAt = data.updatedAt || "";
          // 假设 API 返回 contentBlocks 数组
          this.article.contentBlocks = data.contentBlocks || [];
          console.log("成功获取文章信息：", data);
        } else {
          console.error("API 返回的数据为空！");
          alert("无法加载文章内容，请稍后重试！");
        }
      } catch (error) {
        console.error("加载文章内容失败：", error);
        alert("无法加载文章内容，请稍后重试！");
      }
    }
  },
  /**
   * 组件挂载完成时执行的操作
   */
  async mounted() {
    // 在组件挂载时加载并映射文章内容
    await this.loadArticle();
    // 调用正确的方法
    await this.beenRead(); // 注意大小写
  }

};
</script>


<style scoped>
.loaded-article{
  background-color: #ffffff;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-radius: 0.5rem;
  padding: 2rem;
  max-width: 50rem;
  margin-bottom: 1.5rem;
  justify-content: center;
}
/* 顶部导航栏样式 */
.header {
  background-color: #007bff;
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
  z-index: 1000;
}

.nav {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 5px 20px;
  box-sizing: border-box;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: white;
}

.nav-links {
  list-style: none;
  display: flex;
  margin-left: auto;
  padding: 0;
}

.nav-links li {
  margin: 0 15px;
}

.nav-links .nav-link {
  color: white;
  text-decoration: none;
  font-size: 14px;
  background: none;
  border: none;
  cursor: pointer;
  transition: opacity 0.3s ease;
  font-family: 'Arial', sans-serif;
  padding: 5px 10px;
  border-radius: 4px;
}

.nav-links .nav-link:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 左侧按钮栏样式 */
.side-panel {
  position: fixed;
  top: 50%;
  left: 50px;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-btn {
  width: 60px;
  height: 60px;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  cursor: pointer;
}

.panel-btn:hover {
  background-color: #eaeaea;
}

.panel-icon {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

/* 主体内容样式 */
.blog-page {
  width: 800px;
  margin: 0 auto;
  font-family: sans-serif;
  color: #252933;
  line-height: 1.75;
  padding-top: 80px; /* 为顶部留出空间 */
}

.form-container {
  margin-top: 60px;
  background-color: white;
  width: 100%;
  max-width: 800px;
  padding: 20px;
  border-radius: 45px;
  box-sizing: border-box;
  min-height: 600px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: gray #1a1a1a;
}

.form-container::-webkit-scrollbar {
  width: 8px;
}

.form-container::-webkit-scrollbar-track {
  background: #1a1a1a;
}

.form-container::-webkit-scrollbar-thumb {
  background-color: gray;
  border-radius: 10px;
  border: 2px solid #1a1a1a;
}

.article-title {
  font-size: 24px;
  line-height: 32px;
  margin: 20px 0 10px 0;
}

.author-info-block {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.author-name {
  font-weight: bold;
  margin-right: 20px;
  display: flex;
  align-items: center;
}

.meta-box {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
  margin-right: 20px;
}

.time,
.views-count,
.read-time {
  color: #666;
}


.article-block p {
  margin: 0;
}

.article-block h2 {
  font-size: 20px;
  margin: 24px 0 12px 0;
  border-bottom: 1px solid #ececec;
  padding-bottom: 8px;
}

.article-block pre {
  background: #f8f8f8;
  padding: 12px;
  overflow: auto;
}

.footer {
  margin: 20px 0;
  font-size: 14px;
  text-align: center;
  color: #999;
}
</style>
