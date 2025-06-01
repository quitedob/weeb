<!--
  文件名: ArticleMain.vue
  功能:
    1) 从后端获取文章数据并展示到页面
    2) 根据标签“推荐”（按 id 升序）与“最新”（按 updatedAt 降序）进行排序
    3) 摘要显示 articleLink 的前 20 个字符
    4) 点击标题跳转到 articleread 页面并传递文章 ID
-->

<template>
  <div class="article-main">
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

    <!-- 内容区域置于页面正中间 -->
    <div class="content">
      <!--
        这里是切换标签的区域
        ref="tabs" 用来获得“标签容器”的 DOM，用于后续计算“浮动条”位置
      -->
      <div class="tabs" ref="tabs">
        <!--
          遍历 tabs 数组，每个 tab 包含 id、label
          :ref="`tab-${tab.id}`" 用于获取对应 tab DOM，计算浮动条位置
          通过 :class 控制当前激活 tab 的样式
          点击时触发 changeTab 方法
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

      <!-- 用来显示在选项卡下方 “浮动条” 的元素 -->
      <div class="tab-bar" :style="tabBarStyle"></div>

      <!--
        文章列表区域
        使用 v-for="entry in sortedEntries"，在 computed 属性中根据 activeTab 对 entries 排序后返回
      -->
      <div class="entry-list-wrap">
        <div v-for="entry in sortedEntries" :key="entry.id" class="entry">
          <div class="title-row">
            <!-- 修改为跳转到 articleread 页面并携带文章 ID 参数 -->
            <a @click="readArticle(entry.id)" class="title" :title="entry.articleTitle">
              {{ entry.articleTitle }}
            </a>
          </div>

          <!--
            摘要部分，显示 articleLink 的前二十个字符
            如果不足二十个字符，则显示全部
          -->
          <div class="abstract">
            {{ entry.articleLink.substring(0, 20) }}
          </div>

          <div class="entry-footer">
            <span class="views">浏览量：{{ entry.exposureCount }}</span>
            <span class="likes">点赞：{{ entry.likesCount }}</span>
            <div class="tags">
              <span class="tag">最后更新时间：{{ entry.updatedAt }}</span>
            </div>
            <button @click="readArticle(entry.id)" >阅读</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 侧边栏固定在右侧 -->
    <div class="sidebar">
      <div class="signin-tip sidebar-block signin">
        <div class="first-line">
          <div class="icon-text">
            <span class="title">{{ greeting }}</span>
            <div class="second-line">点亮在社区的每一天</div>
          </div>
          <button class="btn signin-btn" @click="handleSignin">
            <span class="btn-text">去签到</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 最外层容器，用于承载左侧固定按钮（回到顶部等） -->
  <div class="blog-page">
    <!-- 左侧固定的按钮栏 -->
    <div class="side-panel">
      <!-- 返回顶部按钮 -->
      <div class="panel-btn" @click="scrollToTop">
        <img :src="scrollUpImage" alt="Scroll Up" class="panel-icon" />
      </div>
    </div>
  </div>
</template>

<script>
// 引入所需资源
import scrollUpImage from "@/picture/scrollup.png"; // 回到顶部按钮的图片
import axios from "axios";

export default {
  name: 'ArticleMain',

  data() {
    return {
      // 回到顶部按钮图片
      scrollUpImage,

      // 从后端获取的所有文章列表
      entries: [],

      // 标签数组，包含“推荐”和“最新”
      tabs: [
        { id: 0, label: "推荐" },
        { id: 1, label: "最新" },
      ],

      // 当前激活的标签 id
      activeTab: 0,

      // 用于存储“浮动条”的样式：宽度、位移
      tabBarStyle: {
        width: "0px",
        transform: "translateX(0px)",
      },
    };
  },

  computed: {
    /**
     * 根据 activeTab 的值，对 articles（此处叫 entries）进行不同排序
     * - 推荐(id=0): 按 id 升序
     * - 最新(id=1): 按 updatedAt 降序
     */
    sortedEntries() {
      // 若还没拿到数据，直接返回空数组
      if (!this.entries || !this.entries.length) {
        return [];
      }
      // 当 activeTab=0 -> 推荐
      if (this.activeTab === 0) {
        // 按 id 升序
        return [...this.entries].sort((a, b) => a.id - b.id);
      }
      // 当 activeTab=1 -> 最新
      else if (this.activeTab === 1) {
        // 按 updatedAt 降序
        return [...this.entries].sort(
            (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)
        );
      }
      // 若还有其他选项卡逻辑，可在此补充
      else {
        return this.entries;
      }
    },

    /**
     * 根据当前时间计算问候语
     */
    greeting() {
      const currentHour = new Date().getHours();
      if (currentHour >= 5 && currentHour < 12) {
        return '早上好！';
      } else if (currentHour >= 12 && currentHour < 18) {
        return '下午好！';
      } else if (currentHour >= 18 && currentHour < 22) {
        return '晚上好！';
      } else {
        return '夜深了，注意休息！';
      }
    },
  },

  methods: {
    /**
     * 切换标签
     * @param {number} tabId
     */
    changeTab(tabId) {
      // 设置当前激活标签
      this.activeTab = tabId;
      // 更新浮动条位置
      this.updateTabBar();
    },

    /**
     * 计算并更新浮动条样式
     * 在切换 tab 后，需要计算当前激活标签在页面上的宽度和位置，移动浮动条
     */
    updateTabBar() {
      this.$nextTick(() => {
        // 通过 ref="tab-xxx" 找到当前激活的 tab DOM
        const activeTabEl = this.$refs[`tab-${this.activeTab}`][0];
        if (activeTabEl) {
          // 获取元素的宽度和相对于视口左边的位移
          const { width, left } = activeTabEl.getBoundingClientRect();
          // 获取“tabs 容器”的左边距离，用于计算相对位移
          const containerLeft = this.$refs.tabs.getBoundingClientRect().left;
          // 设置浮动条的宽度和 transform
          this.tabBarStyle.width = `${width}px`;
          this.tabBarStyle.transform = `translateX(${left - containerLeft}px)`;
        }
      });
    },

    /**
     * 平滑滚动到页面顶部
     */
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth", // 平滑滚动
      });
    },

    /**
     * 跳转到阅读页面并携带文章 ID
     * @param {number} articleId - 文章的ID
     */
    readArticle(articleId) {
      this.$router.push({
        path: `/articleread`,
        query: { id: articleId }, // 使用 query 传递文章 ID
      });
    },

    /**
     * 从后端获取所有文章数据
     */
    async fetchArticles() {
      try {
        // 发起后端请求，获取所有文章
        const response = await axios.post("http://localhost:8080/articles/getall");
        // 将返回数据赋值给 entries
        this.entries = response.data;
      } catch (error) {
        console.error("获取所有文章失败:", error);
      }
    },

    /**
     * 返回上一页
     */
    goBack() {
      window.history.back();
    },

    /**
     * 跳转到用户页面
     */
    goToUser() {
      this.$router.push("/usermain");
    },

    /**
     * 示例：签到按钮点击处理
     * 可自行修改或完善
     */
    handleSignin() {
      alert("签到成功");
    },
  },

  // 组件挂载后，获取文章数据并初始化浮动条位置
  mounted() {
    this.fetchArticles();
    // 等数据加载完成后，再更新一次浮动条位置
    //（也可放在 nextTick 中，但这里先获取，然后再 nextTick）
    this.$nextTick(() => {
      this.updateTabBar();
    });
  },
};
</script>

<style scoped>
/*
  ============ 1. 基础布局及面板样式 ============
*/

/* 回到顶部图标按钮 */
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

/* 左侧按钮栏样式 */
.side-panel {
  position: fixed;
  top: 90%;
  right: 50px;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
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

/* 顶部导航栏整体样式 */
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
  height: 80px;
}

/* 导航条 */
.nav {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 20px;
  box-sizing: border-box;
}

/* logo 文字 */
.logo {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

/* 导航链接列表 */
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

/*
  使 article-main 相对定位，以便后续绝对定位 sidebar
*/
.article-main {
  position: relative;
  background-color: white;
  padding-top: 80px; /* 顶部留出导航栏的高度 */
  min-height: 100vh; /* 确保占满整个视口高度 */
}

/*
  内容区域居中
  宽度 60%，可按需调整
*/
.content {
  width: 60%;
  margin: 60px auto 0 auto;
  background-color: white;
  border-bottom: none;
  padding: 0 20px;
  box-sizing: border-box;
}

/*
  侧边栏固定在右侧 50px
  top: 135px 使其与内容区域在同一水平
*/
.sidebar {
  position: absolute;
  right: 50px;
  top: 135px;
  width: 250px;
}

/*
  ============ 2. 标签切换区域(新增) ============
*/
/* 整个 tabs 容器，放在 content 内 */
.tabs {
  display: flex;
  border-bottom: 1px solid #ddd;
  justify-content: start; /* 左对齐，也可改 center 居中 */
  margin-top: 10px;
  position: relative; /* 让 tab-bar 绝对定位相对于 tabs */
}

/* 单个 Tab 的样式 */
.tab {
  padding: 10px 20px;
  cursor: pointer;
  text-align: center;
  font-weight: normal;
  color: #333;
  font-size: 16px;
}

/* 激活状态 */
.tab.active {
  color: #007bff;
  font-weight: bold;
}

/* 底部浮动条，用于指示当前选中的 Tab */
.tab-bar {
  height: 2px;
  background-color: #007bff;
  transition: transform 0.3s, width 0.3s;
}

/*
  ============ 3. 文章列表区 ============
*/
/* 文章列表容器样式 */
.entry-list-wrap {
  padding: 10px;
  background-color: #fafafa;
}

/* 单个文章条目样式 */
.entry {
  border-bottom: none;
  padding: 15px 0;
}

/* 文章标题样式 */
.title-row .title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  text-decoration: none;
}

/* 摘要文本样式 */
.abstract {
  margin: 10px 0;
  color: #555;
  line-height: 1.5;
}

/* 底部信息区域样式 */
.entry-footer {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #888;
}

/* 浏览量和点赞数样式 */
.views,
.likes {
  margin-right: 15px;
}

/* 标签区域样式 */
.tags {
  display: flex;
}

/* 单个标签样式 */
.tag {
  background-color: #f0f0f0;
  color: #555;
  padding: 2px 6px;
  margin-right: 5px;
  border-radius: 3px;
  font-size: 12px;
}

/*
  ============ 4. 侧边栏签到区域 ============
*/
.signin-tip {
  border: 1px solid #e0e0e0;
  padding: 15px;
  background-color: #fff;
  border-radius: 5px;
  text-align: center;
}

.icon-text .title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.icon-text .second-line {
  margin-top: 5px;
  color: #666;
  font-size: 14px;
}

/* 按钮样式 */
.btn {
  margin-top: 10px;
  padding: 10px 20px;
  background-color: #42b983;
  color: #fff;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}
.btn:hover {
  background-color: #369870;
}
.btn-text {
  font-size: 16px;
}
</style>
