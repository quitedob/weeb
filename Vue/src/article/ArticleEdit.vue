<template>
  <div class="edit-article">
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
                v-model="article.articleLink"
                id="content"
                placeholder="请输入文章内容"
                rows="15"
                required
            ></textarea>
          </div>

          <!-- 提交按钮 -->
          <div class="buttons">
            <button type="submit">发布</button>
          </div>
        </form>
      </div>
    </main>


  </div>
</template>

<script>
import { instance } from "../api/axiosInstance";

export default {
  data() {
    return {
      isLoggedIn: false,      // 用户登录状态
      token: "",              // 身份验证令牌
      article: {              // 文章对象，用于绑定标题和内容
        title: "",
        articleLink: "",
      },
    };
  },
  methods: {
    /**
     * 检查用户登录状态
     */
    checkLoginStatus() {
      const token = localStorage.getItem("token");  // 从本地存储获取 token
      if (!token || token.length <= 10) {           // 如果 token 不存在或长度不合法
        alert("尚未登录或令牌无效, 将跳转到登录页面");
        this.$router.push("/login");              // 跳转到登录页面
        return;
      }
      this.token = token;                           // 设置组件内的 token
      this.isLoggedIn = true;                       // 更新登录状态
    },

    /**
     * 加载文章内容并映射到页面表单
     */
    async loadArticle() {
      const articleId = this.$route.query.id;  // 从 URL query 中获取文章 ID
      if (!articleId) {
        alert("未提供文章 ID，无法加载文章内容！");
        this.$router.push("/");  // 若没有提供 ID，则跳转到主页
        return;
      }
      try {
        // 调用后端接口获取文章内容
        const response = await instance.get(`/articles/${articleId}`);
        // 将返回的 JSON 数据映射到页面表单字段
        this.article.title = response.data.articleTitle || "";
        this.article.articleLink = response.data.articleLink || "";
      } catch (error) {
        console.error("加载文章内容失败：", error);
        alert("无法加载文章内容，请稍后重试！");
      }
    },

    /**
     * 发布（保存）文章修改，模拟测试代码的编辑文章功能
     */
    async publishArticle() {
      const articleId = this.$route.query.id;  // 从 URL query 中获取文章 ID
      if (!articleId) {
        alert("文章 ID 丢失，无法保存修改！");
        return;
      }
      try {
        // 构造与测试代码类似的文章更新数据
        const newArticleData = {
          articleTitle: this.article.title,             // 使用表单中的标题
          articleContent: this.article.articleLink    // 使用articleContent字段
        };
        // 调用后端接口提交修改后的文章内容
        await instance.put(
            `/articles/${articleId}`,
            newArticleData  // 提交构造的文章数据
        );
        alert("文章修改成功！");
      } catch (error) {
        console.error("保存文章失败：", error);
        alert("保存文章失败，请稍后重试！");
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
  },

  /**
   * 组件创建时的生命周期钩子
   */
  async created() {
    this.checkLoginStatus();  // 检查是否已登录
    await this.loadArticle(); // 加载并映射文章内容
  },
};
</script>


<style scoped>
.edit-article {
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
