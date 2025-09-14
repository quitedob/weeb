<template>
  <div id="video1">
    <!-- 页头区域 -->
    <header>
      <h1>神神又人人</h1>
      <!-- 导航栏 -->
      <nav>
        <ul>
<!--          <li><a href="#">首页</a></li>-->
<!--          <li><a href="#">课程</a></li>-->
<!--          <li><a href="#">直播</a></li>-->
<!--          <li><a href="#">活动</a></li>-->
          <!-- 根据用户登录状态显示登录链接或用户名和登出链接 -->
          <li v-if="!isLoggedIn">
            <router-link to="/login">登录</router-link>
          </li>
          <li v-else class="user-info">
      <span>
        <router-link to="/usermain" class = "user-name">{{ username }}</router-link>
      </span>
            <router-link to="/logout">登出</router-link>
            <a @click="goBack" class="nav-link">返回</a>
          </li>
        </ul>
      </nav>

    </header>


    <!-- 主内容区域 -->
    <div class="container">
      <main>
        <!-- 视频播放器区域 -->
        <section class="video-player">
          <video controls>
            <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
            您的浏览器不支持视频播放。
          </video>
        </section>

        <!-- 视频详情信息 -->
        <section class="video-info">
          <h2>示例展示视频</h2>
          <p><strong>UP猪：xxx</strong></p>
          <p>本视频只供展示</p>
        </section>

        <!-- 评论区域 -->
        <section class="comments">
          <h3>讨论交流</h3>
          <!-- 模拟评论列表 -->
          <div class="comment" v-for="comment in comments" :key="comment.id">
            <p><strong>{{ comment.user }}：</strong>{{ comment.text }}</p>
            <time :datetime="comment.datetime">{{ comment.timeAgo }}</time>
          </div>

          <!-- 用户输入模块 -->
          <div class="add-comment">
            <textarea
                v-model="newComment"
                :disabled="!isLoggedIn"
                placeholder="请输入您的评论"
            ></textarea>
            <button @click="addComment" :disabled="!isLoggedIn">添加评论</button>
          </div>
        </section>
      </main>

      <!-- 侧边栏区域 -->
      <aside>
        <h3>相关视频推荐</h3>
        <ul>
          <li><a href="#">后端入门 - 存储与数据库</a></li>
          <li><a href="#">对象存储使用方法</a></li>
          <li><a href="#">分布式系统设计</a></li>
          <li><a href="#">云计算基础</a></li>
        </ul>
      </aside>
    </div>

    <!-- 页脚区域 -->
    <footer>
      <p>© 2025 神人网站. 版权所有.</p>
    </footer>
  </div>
</template>

<script>
import { instance } from '../api/axiosInstance';

export default {
  name: "video1", // 组件名称
  data() {
    return {
      isLoggedIn: false, // 用户登录状态
      username: '',      // 用户名
      comments: [        // 初始化的评论列表
        { id: 1, user: '用户001', text: '打卡', datetime: '2024-11-25T19:52:27+08:00', timeAgo: '1月前' },
        { id: 2, user: '用户002', text: '打卡', datetime: '2024-11-24T20:11:04+08:00', timeAgo: '1月前' },
      ],
      newComment: ''     // 新评论内容
    };
  },
  methods: {
    // 添加新评论的方法
    addComment() {
      if (!this.isLoggedIn) {
        alert('请先登录才能发表评论');
        return;
      }
      if (this.newComment.trim() === '') {
        alert('评论内容不能为空');
        return;
      }

      const newId = this.comments.length + 1;
      const currentDate = new Date();
      const isoString = currentDate.toISOString();
      const timeAgo = '刚刚';

      this.comments.push({
        id: newId,
        user: this.username || `用户${String(100 + newId).slice(-4)}`,
        text: this.newComment,
        datetime: isoString,
        timeAgo: timeAgo
      });

      this.newComment = '';
    },
    /**
     * 返回上一页
     */
    goBack() {
      window.history.back();
    },
    // 检查登录状态的方法
    checkLoginStatus() {
      const token = localStorage.getItem('token');
      if (!token) {
        alert('尚未登录, 将跳转到登录页面');
        this.$router.push('/login');
        return;
      }

      // 验证令牌是否有效
      instance.get('/user')
          .then(response => {
            this.isLoggedIn = true;
            this.username = response.data.username || '已登录用户';
          })
          .catch(error => {
            console.error('用户身份验证失败', error);
            alert('身份验证失败，将跳转到登录页面');
            localStorage.removeItem('token'); // 移除失效令牌
            this.$router.push('/login');
          });
    }
  },

  created() {
    this.checkLoginStatus(); // 组件创建时检查登录状态
  }
};
</script>




<style scoped>
/* 样式与之前相同，可根据需要调整 */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Arial', sans-serif;
  background-color: #f0f4f8;
  color: #333;
  line-height: 1.6;
}

header {
  background-color: #1e90ff;
  color: #fff;
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

header h1 {
  font-size: 24px;
}

nav ul {
  list-style: none;
  display: flex;
}

nav li {
  margin-left: 20px;
}

nav a {
  color: #fff;
  text-decoration: none;
  font-weight: bold;
  transition: color 0.3s;
}

nav a:hover {
  color: #ffd700;
}

.container {
  display: flex;
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 20px;
}

main {
  flex: 3;
  margin-right: 20px;
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

aside {
  flex: 1;
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.video-player {
  position: relative;
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
  border-radius: 8px;
}

.video-player video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

.video-info {
  margin-top: 20px;
}

.video-info h2 {
  color: #1e90ff;
  margin-bottom: 10px;
}

.video-info p {
  margin-bottom: 8px;
}

.comments {
  margin-top: 40px;
}

.comments h3 {
  margin-bottom: 20px;
  color: #ff4500;
}

.comment {
  background-color: #fafafa;
  padding: 10px 15px;
  border-radius: 5px;
  margin-bottom: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.comment p {
  margin-bottom: 5px;
}

.comment time {
  font-size: 12px;
  color: #888;
}
nav ul {
  list-style: none; /* 去掉列表样式 */
  padding: 0;
  margin: 0;
}
nav ul li {
  display: inline-block; /* 将<li>设置为行内块级元素 */
  margin-right: 10px; /* 设置<li>之间的间距 */
}
router-link {
  display: inline; /* 将<router-link>设置为行内元素 */
}
.user-info {
  display: flex; /* 将用户名和登出链接的容器设置为 flex 布局 */
  align-items: center; /* 垂直居中对齐 */
  gap: 20px; /* 设置子元素之间的间距 */
}

.user-name{
  color:yellow;
}
.user-info span {
  margin-right: 10px; /* 设置用户名与登出链接之间的间距 */
}

.add-comment {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
}

.add-comment textarea {
  resize: vertical;
  min-height: 60px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 14px;
  margin-bottom: 10px;
}

.add-comment button {
  align-self: flex-end;
  padding: 8px 16px;
  background-color: #1e90ff;
  color: #fff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.add-comment button:hover {
  background-color: #104e8b;
}

aside h3 {
  margin-bottom: 20px;
  color: #32cd32;
}

aside ul {
  list-style: none;
}

aside li {
  margin-bottom: 15px;
}

aside a {
  text-decoration: none;
  color: #1e90ff;
  transition: color 0.3s;
}

aside a:hover {
  color: #32cd32;
}

footer {
  background-color: #1e90ff;
  color: #fff;
  text-align: center;
  padding: 15px 0;
  box-shadow: 0 -2px 4px rgba(0,0,0,0.1);
}

@media (max-width: 768px) {
  .container {
    flex-direction: column;
  }

  main {
    margin-right: 0;
    margin-bottom: 20px;
  }
}
</style>
