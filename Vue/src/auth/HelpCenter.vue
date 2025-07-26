<script>
export default {
  name: "HelpCenter",
  data() {
    return {
      activeTab: 0, // 当前激活的标签
      tabs: [
        { id: 0, label: "创作等级权益", content: "这里是创作等级权益的内容" },
        { id: 1, label: "内容曝光", content: "这里是内容曝光的内容" },
        { id: 2, label: "内容审核", content: "这里是内容审核的内容" },
      ],
      tabBarStyle: {
        width: "0px",
        transform: "translateX(0px)",
      },
    };
  },
  methods: {
    // 切换标签
    changeTab(tabId) {
      this.activeTab = tabId;
      this.updateTabBar(); // 更新浮动条位置
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

    // 更新浮动条样式
    updateTabBar() {
      this.$nextTick(() => {
        const activeTabEl = this.$refs[`tab-${this.activeTab}`][0]; // 获取激活的标签元素
        if (activeTabEl) {
          const { width, left } = activeTabEl.getBoundingClientRect();
          const containerLeft = this.$refs.tabs.getBoundingClientRect().left;
          this.tabBarStyle.width = `${width}px`;
          this.tabBarStyle.transform = `translateX(${left - containerLeft}px)`;
        }
      });
    },
  },
  mounted() {
    this.updateTabBar(); // 初始化浮动条位置
  },
};
</script>


<template>
  <div class="help-center">
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
<div class ="tabtab">
    <div class="tabs" ref="tabs">
      <div
          v-for="tab in tabs"
          :key="tab.id"
          :ref="`tab-${tab.id}`"
          :class="['tab', { active: tab.id === activeTab }]"
          @click="changeTab(tab.id)"
      >
        {{ tab.label }}
      </div>
      <div class="tab-bar" :style="tabBarStyle"></div>
    </div>
    <div class="tab-content">
      <div v-for="tab in tabs" :key="tab.id" v-show="tab.id === activeTab">
        {{ tab.content }}
      </div>
    </div>
</div>
  </div>
</template>


<style scoped>
.tabtab{
  margin-top: 120px;
  display: flex;
  align-items: center; /* 垂直居中 */
  flex-direction: column; /* 如果需要多个元素垂直排列 */
  height: calc(100vh - 120px); /* 减去顶部导航的高度 */
  box-sizing: border-box; /* 确保 padding 和边框不影响大小 */
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

.help-center {
  font-family: Arial, sans-serif;
  padding: 20px;
}

.tabs {

  display: flex;
  position: relative;
  border-bottom: 2px solid #e0e0e0;
  margin-bottom: 20px;
}

.tab {
  padding: 10px 20px;
  cursor: pointer;
  transition: color 0.3s;
}

.tab.active {
  font-weight: bold;
  color: #409eff;
}

.tab-bar {
  position: absolute;
  bottom: 0;
  height: 2px;
  background-color: #409eff;
  transition: transform 0.3s ease, width 0.3s ease;
}

.tab-content {
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}
</style>
