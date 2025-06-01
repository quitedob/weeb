<template>
  <div class="write-article">
    <!-- 顶部导航栏 -->
    <header class="header">
      <nav class="nav">
        <div class="logo">小蓝盒平台</div>
        <ul class="nav-links">
          <!-- 设置导航链接 -->
          <li><button @click="goToUser" class="nav-link">用户</button></li>
          <li><button @click="goBack" class="nav-link">返回</button></li>
        </ul>
      </nav>
    </header>

    <!-- 主内容区域 -->
    <main class="centered">
      <div class="form-container">
        <form @submit.prevent="publishArticle">
          <!-- 标题输入 -->
          <div class="form-group">
            <label for="title">文章标题：</label>
            <input
                v-model="article.title"
                type="text"
                id="title"
                placeholder="输入文章标题"
                maxlength="80"
                required
            />
          </div>

          <!-- 内容输入 -->
          <div class="form-group">
            <label for="content">文章内容：</label>
            <textarea
                v-model="article.content"
                id="content"
                placeholder="请输入文章内容"
                rows="15"
                required
            ></textarea>
          </div>

          <!-- 按钮 -->
          <div class="buttons">
            <button type="submit">发布</button>
          </div>
        </form>
      </div>
    </main>

    <!-- 底部 -->
    <footer>
      <p>© 2025 神人平台  | <a href="#">请不要联系我们</a></p>
    </footer>
  </div>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      isLoggedIn: false,
      username: "",
      token: "", // 添加 token 字段
      article: {
        title: "",
        content: "",
      },
    };
  },
  methods: {
    // 检查用户登录状态
    checkLoginStatus() {
      const token = localStorage.getItem("token");
      if (!token || token.length <= 10) { // 判断令牌是否存在且长度大于 10
        alert("尚未登录或令牌无效, 将跳转到登录页面");
        this.$router.push("/login");
        return;
      }

      this.token = token; // 将令牌存储到组件数据中
      this.isLoggedIn = true; // 假设用户已登录
      this.username = "已登录用户"; // 设置默认用户名
    },

    // 发布文章方法
    async publishArticle() {
      if (!this.token) {
        alert("令牌无效，请重新登录！");
        this.$router.push("/login");
        return;
      }

      if (!this.article.title || !this.article.content) {
        alert("标题和内容不能为空！");
        return;
      }

      // 打包文章数据
      const articleData = {
        userId: this.article.userId || 1, // 假设有一个默认的用户 ID
        articleTitle: this.article.title,
        articleLink: this.article.content, // 假设内容是文章链接
      };

      try {
        // 发送请求到后端 API
        const response = await axios.post(
            "http://localhost:8080/articles/new", // 替换为你的 API 路径
            articleData,
            {
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${this.token}`,
              },
            }
        );

        // 处理响应结果
        if (response.status === 200 || response.status === 201) {
          alert("文章已发布！");
        } else {
          alert("文章发布失败，请稍后再试！");
        }
      } catch (error) {
        console.error("文章发布失败：", error);
        alert("文章发布失败，请检查网络或重试！");
      }
    },

    // 返回上一页方法
    goBack() {
      window.history.back();
    },

    // 跳转到用户页面方法
    goToUser() {
      this.$router.push("/usermain");
    },
  },
  created() {
    // 在组件创建时检查登录状态
    this.checkLoginStatus();
  },
};
</script>



<style scoped>
.write-article {
  background-color: white;
}

/* 顶部导航栏样式 */
.header {
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
  overflow: hidden;
  background-color: #007bff;
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  height: 60px; /* 缩短顶部导航栏的高度 */
}

.nav {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 5px 20px; /* 减小内边距 */
  box-sizing: border-box;
}

.logo {
  font-size: 20px; /* 减小字体大小 */
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
  margin: 0 15px; /* 减小链接间距 */
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

/* 主内容区域居中样式 */
.centered {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 80px;
  margin-bottom: 100px;
  padding: 0 10px;
}

/* 设置滚动条样式 */
.form-container {
  background-color: white;
  width: 100%;
  max-width: 800px;
  padding: 20px;
  border-radius: 45px;
  box-sizing: border-box;
  min-height: 600px;
  max-height: calc(100vh - 160px); /* 限制容器最大高度 */
  overflow-y: auto; /* 启用垂直滚动条 */
  scrollbar-width: thin; /* Firefox 滚动条宽度 */
  scrollbar-color: gray #1a1a1a; /* Firefox 滚动条颜色 */
}

/* Webkit 浏览器滚动条样式 */
.form-container::-webkit-scrollbar {
  width: 8px; /* 滚动条宽度 */
}

.form-container::-webkit-scrollbar-track {
  background: #1a1a1a; /* 滚动条轨道背景色 */
}

.form-container::-webkit-scrollbar-thumb {
  background-color: gray; /* 滚动条颜色 */
  border-radius: 10px; /* 圆角 */
  border: 2px solid #1a1a1a; /* 间隙 */
}


/* 表单样式 */
.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  font-size: 18px;
  margin-bottom: 8px;
  font-weight: bold;
}

input,
textarea {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
}

textarea {
  resize: vertical;
}

/* 按钮样式 */
.buttons {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

button {
  padding: 12px 24px;
  font-size: 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

button:hover {
  background-color: #0056b3;
}

/* 底部样式 */
footer {
  text-align: center;
  padding: 20px;
  background-color: #007bff;
  color: white;
  font-size: 14px;
  margin: 0;
  width: 100%;
  box-sizing: border-box;
  position: fixed;
  bottom: 0;
  left: 0;
  height: 60px;
}

footer a {
  color: white;
  text-decoration: none;
  margin: 0 5px;
}

footer a:hover {
  text-decoration: underline;
}
</style>
