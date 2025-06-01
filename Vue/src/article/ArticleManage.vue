<template>
  <!--
    顶层容器，包含整个页面的布局
  -->
  <div class="content">

    <!-- 顶部导航栏 -->
    <header class="header">
      <nav class="nav">
        <!-- 平台LOGO区域 -->
        <div class="logo">小蓝盒平台</div>
        <div class = "logo-center">编辑中心</div>
        <!-- 右侧的导航按钮 -->
        <ul class="nav-links">
          <li>
            <!-- 跳转至用户页面 -->
            <button @click="goToUser" class="nav-link">用户</button>
          </li>
          <li>
            <!-- 返回上一页 -->
            <button @click="goBack" class="nav-link">返回</button>
          </li>
        </ul>
      </nav>
    </header>

    <!-- 用于包裹所有 Tabs 的容器，并使用 ref="tabs" 来让我们可以定位到它 -->
    <div class="tabs" ref="tabs" style="margin-top:80px;">
      <!--
        v-for 遍历 this.tabs 中的每个 tab 对象
        :ref="`tab-${tab.id}`" 可以让我们在 JS 中根据 tab.id 获取每个 tab 元素，更新“浮动条”位置
        :class 用于给当前激活的标签添加 active 样式
        @click 切换标签，并触发排序逻辑
      -->
      <div
          v-for="tab in tabs"
          :key="tab.id"
          :ref="`tab-${tab.id}`"
          :class="['tab', { active: tab.id === activeTab }]"
          @click="changeTab(tab.id)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!--
      这个 div 是用来显示在选项卡下方“浮动条”的元素
      :style="tabBarStyle" 表示根据 JS 中计算得到的样式来移动、拉伸此条
    -->
    <div class="tab-bar" :style="tabBarStyle"></div>

    <!-- 文章列表的容器: 当切换不同的标签时，将会对 articles 进行不同的排序并展示 -->
    <div class="articles-container">
      <!--
        统一使用 sortedArticles 根据 activeTab 来排序
        也可以写两个 v-if 分开显示，但写在一起 v-for 就够了
        （若想分开展示，请参考注释部分的示例写法）
      -->
      <div class="articles">
        <!--
          v-for 遍历 computed 属性 sortedArticles，根据 activeTab 不同自动完成不同排序
          :key 保证每篇文章有唯一的标识
        -->
        <div v-for="article in sortedArticles" :key="article.id" class="article">
          <!-- 文章标题，点击可跳转到阅读页面 -->
          <div class="article-title">
            <a
                @click="readArticle(article.id)"
                class="title"
                :title="article.articleTitle"
            >
              {{ article.articleTitle }}
            </a>
          </div>
          <!-- 文章信息：创建时间、阅读数、点赞数等 -->
          <div class="article-info">
            <span>{{ article.createdAt }}</span>
            <span class="dot">·</span>
            <span>{{ article.exposureCount }} 阅读</span>
            <span class="dot">·</span>
            <span>{{ article.likesCount }} 点赞</span>
          </div>
          <!-- 文章操作按钮：编辑、阅读、删除等 -->
          <div class="article-actions">
            <button @click="editArticle(article.id)">编辑</button>
            <button @click="readArticle(article.id)">阅读</button>
            <button @click="deleteArticle(article.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/*
  引入 axios 用于发送 HTTP 请求
*/
import axios from "axios";

export default {
  // 组件名
  name: "ArticleManage",
  data() {
    return {
      // 用户登录状态相关
      isLoggedIn: false, // 是否已登录
      username: "",      // 当前用户的用户名
      token: "",         // token 存储

      // 文章列表数据
      articles: [],      // 存储获取到的文章列表

      // 当前选中的标签 ID，默认为 0("推荐")
      activeTab: 0,

      // 标签列表，这里有两个：推荐（id=0）、最新（id=1）
      // 你可以扩展更多标签
      tabs: [
        { id: 0, label: "推荐", content: "这里是创作等级权益的内容" },
        { id: 1, label: "最新", content: "这里是内容曝光的内容" },
      ],

      // “浮动条”的样式，会在方法 updateTabBar 中被更新
      tabBarStyle: {
        width: "0px",
        transform: "translateX(0px)",
      },
    };
  },
  computed: {
    /*
      根据当前 activeTab 不同，对文章进行不同的排序后再返回
      推荐(id=0) -> 按照文章 id 升序排序
      最新(id=1) -> 按照创建时间 createdAt 降序排序
    */
    sortedArticles() {
      // 如果文章列表还没取到，直接返回空数组
      if (!this.articles || !this.articles.length) {
        return [];
      }

      // activeTab 为 0 表示要“推荐”：我们约定按文章 id 升序
      if (this.activeTab === 0) {
        // 复制一份 articles 然后 sort
        return [...this.articles].sort((a, b) => a.id - b.id);
      }
      // activeTab 为 1 表示要“最新”：按时间 createdAt 降序
      else if (this.activeTab === 1) {
        return [...this.articles].sort(
            (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
        );
      }
      // 其他情况(假如有更多 tab) 就原样返回
      else {
        return this.articles;
      }
    },
  },
  methods: {
    /*
      切换标签并更新“浮动条”的位置
    */
    changeTab(tabId) {
      // 将激活标签设置为点击的 tabId
      this.activeTab = tabId;
      // 调整浮动条位置
      this.updateTabBar();
    },
    /*
      计算并更新浮动条样式：宽度、水平偏移
    */
    updateTabBar() {
      this.$nextTick(() => {
        // 通过 ref="tab-xxx" 获取当前激活标签对应的 DOM 元素
        const activeTabEl = this.$refs[`tab-${this.activeTab}`][0];

        // 如果拿到了元素，则计算它的宽度与相对于容器左侧的 offset
        if (activeTabEl) {
          const { width, left } = activeTabEl.getBoundingClientRect();
          // 获取“外层 tabs 容器”左侧的坐标，用于计算相对偏移量
          const containerLeft = this.$refs.tabs.getBoundingClientRect().left;

          // 更新浮动条的宽度与位置
          this.tabBarStyle.width = `${width}px`;
          this.tabBarStyle.transform = `translateX(${left - containerLeft}px)`;
        }
      });
    },
    /*
      删除指定 ID 的文章
    */
    async deleteArticle(articleId) {
      try {
        // 检查是否有 token
        if (!this.token) {
          alert("请先登录！");
          this.$router.push("/login");
          return;
        }

        // 调用后端删除文章接口，使用 POST + params 的形式
        const response = await axios.post(
            "http://localhost:8080/articles/deletearticle",
            null,
            {
              params: { id: articleId },
              headers: {
                Authorization: `Bearer ${this.token}`,
                "Content-Type": "application/json",
              },
            }
        );

        if (response.status === 200) {
          // 成功删除后，从前端 articles 列表中移除此文章
          this.articles = this.articles.filter(
              (article) => article.id !== articleId
          );
          alert("文章删除成功！");
        } else {
          console.error("删除文章失败，状态码：", response.status);
          alert("删除文章失败，请稍后再试！");
        }
      } catch (error) {
        console.error("删除文章时发生错误：", error);
        if (error.response?.status === 403) {
          alert("权限不足，无法删除文章！");
        } else {
          alert("删除文章失败，请稍后再试！");
        }
      }
    },
    /*
      阅读文章：跳转到阅读页面，并通过 query 参数传递文章 ID
    */
    readArticle(articleId) {
      this.$router.push({
        path: "/articleread",
        query: { id: articleId },
      });
    },
    /*
      检查用户登录状态：如果 token 无效或不存在，就跳转登录页面
      如果有效，就获取用户名并设置已登录
    */
    async checkLoginStatus() {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("尚未登录或令牌无效，将跳转到登录页面");
        this.$router.push("/login");
        return;
      }
      this.token = token;

      try {
        // 调用后端 /user 接口验证 token，并获取用户信息
        const response = await axios.get("http://localhost:8080/user", {
          headers: { Authorization: `Bearer ${token}` },
        });
        this.isLoggedIn = true;
        // 从后端数据中获取用户名
        this.username = response.data.username || "";
      } catch (error) {
        console.error("用户身份验证失败", error);
        alert("身份验证失败，将跳转到登录页面");
        // 验证失败，移除 token 并跳转
        localStorage.removeItem("token");
        this.$router.push("/login");
      }
    },
    /*
      根据当前用户名，从后端获取用户 ID
      @returns number | null
    */
    async getUserId() {
      try {
        // 调用后端，用 username 查找用户 ID
        const response = await axios.get("http://localhost:8080/findByUsername", {
          params: { username: this.username },
          headers: {
            Authorization: `Bearer ${this.token}`,
            "Content-Type": "application/json",
          },
        });

        // 若成功返回用户数据，则获取 userData.id
        const userData = response.data;
        if (userData && userData.id) {
          return userData.id;
        } else {
          console.error("未能获取有效的用户ID，返回数据：", userData);
          return null;
        }
      } catch (error) {
        console.error("获取用户ID失败:", error);
        return null;
      }
    },
    /*
      获取文章列表：查询当前登录用户的所有文章
    */
    async fetchArticles() {
      try {
        // 先拿到用户 ID
        const userId = await this.getUserId();
        if (!userId) {
          alert("用户未登录，请先登录！");
          return;
        }

        // 调用后端接口，获取此用户的文章
        const response = await axios.post(
            "http://localhost:8080/articles/myarticles",
            null,
            {
              headers: {
                Authorization: `Bearer ${this.token}`,
                "Content-Type": "application/json",
              },
              params: { userId },
            }
        );

        // 若成功拿到数组，就赋值给前端的 articles
        if (Array.isArray(response.data)) {
          this.articles = response.data;
        } else {
          console.error("返回数据格式不正确：", response.data);
        }
      } catch (error) {
        if (error.response?.status === 403) {
          alert("权限不足或登录已过期，请重新登录！");
        } else {
          console.error("获取文章列表失败：", error);
        }
      }
    },
    /*
      跳转到文章编辑页面，并通过 query 传递文章 ID
    */
    editArticle(articleId) {
      this.$router.push({
        path: "/articleedit",
        query: { id: articleId },
      });
    },
    /*
      返回上一页
    */
    goBack() {
      window.history.back();
    },
    /*
      跳转到用户页面
    */
    goToUser() {
      this.$router.push("/usermain");
    },
  },

  /*
    组件创建时，会先检查登录状态并获取文章列表
    如果已登录且有用户名，则拉取文章列表
    初始化后，更新一下浮动条位置
  */
  async created() {
    // 检查登录
    await this.checkLoginStatus();
    // 如果登录成功，则获取文章并更新浮动条
    if (this.isLoggedIn && this.username) {
      await this.fetchArticles();
      // 初始化时更新一下浮动条位置
      this.updateTabBar();
    }
  },
};
</script>

<style scoped>
/* 容器整体样式 */
.content {
  background-color: white;
  font-family: Arial, sans-serif;
  padding: 20px;
  max-width: 700px; /* 限制页面宽度 */
  margin: 0 auto;   /* 居中显示 */
}

/* 顶部导航栏整体样式 */
.header {
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  position: fixed;     /* 固定在页面顶部 */
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
  overflow: hidden;
  background-color: #007bff; /* 蓝色背景 */
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  height: 80px; /* 拉长顶部导航栏的高度 */
}

/* 顶部导航内部容器 */
.nav {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 20px;
  box-sizing: border-box;
}

/* LOGO 文字 */
.logo {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

/* LOGO 中间内容居中显示 */
.logo-center {
  font-size: 24px;
  font-weight: bold;
  color: white;
  flex: 1; /* 占据剩余空间 */
  text-align: center; /* 文本居中 */
  margin-left: 50px; /* 向右偏移 40px，可以根据需要调整 */
}

/* 导航右侧按钮的列表样式 */
.nav-links {
  list-style: none; /* 去掉默认的 li 小圆点 */
  display: flex;
  margin-left: auto; /* 左侧自动填充，右侧顶边 */
  padding: 0;
}

/* 每个按钮的间距 */
.nav-links li {
  margin: 0 15px;
}

/* 导航按钮样式 */
.nav-links .nav-link {
  color: white;
  text-decoration: none;
  font-size: 16px;
  background: none;
  border: none;
  cursor: pointer;
  transition: opacity 0.3s ease;
  font-family: 'Arial', sans-serif;
  padding: 8px 12px;
  border-radius: 4px;
}

.nav-links .nav-link:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

/* Tabs 容器 */
.tabs {
  background-color: white;
  display: flex;
  border-bottom: 1px solid #ddd;
  justify-content: center; /* 内容水平居中 */
  padding: 0 20px;
  box-sizing: border-box;
}

/* 单个 Tab 的样式 */
.tab {
  padding: 10px 20px;
  cursor: pointer;
  text-align: center; /* 文字居中 */
}

/* 浮动条，用于标识当前选中的 Tab */
.tab-bar {
  height: 2px;
  background-color: #007bff;
  transition: transform 0.3s, width 0.3s;
}

/* 当 Tab 处于激活态时，文字高亮 */
.tab.active {
  color: #007bff;
  font-weight: bold;
}

/* 文章容器区 */
.articles-container {
  background-color: white;
  margin-top: 10px;
  min-height: 600px;
  max-height: calc(100vh - 160px); /* 限制最大高度以便可滚动 */
  overflow-y: auto; /* 启用垂直滚动 */
  scrollbar-width: thin; /* Firefox 滚动条宽度 */
  scrollbar-color: gray #1a1a1a; /* Firefox 滚动条颜色 */
}

/* 文章列表外层样式 */
.articles {
  padding: 20px;
}

/* 单篇文章的卡片样式 */
.article {
  margin-bottom: 20px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
}

/* 文章标题 */
.article-title {
  font-size: 16px;
  font-weight: bold;
}

/* 文章的一些基本信息 */
.article-info {
  font-size: 14px;
  color: #666;
}

/* 用来分隔的圆点 */
.dot {
  margin: 0 5px;
}
</style>
