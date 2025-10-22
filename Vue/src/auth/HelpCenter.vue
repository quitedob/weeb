<script>
export default {
  name: "HelpCenter",
  data() {
    return {
      activeTab: 0, // 当前激活的标签
      tabs: [
        {
          id: 0,
          label: "创作等级权益",
          content: `创作等级权益说明

🌟 等级体系
我们采用完善的创作者等级体系，鼓励优质内容创作。等级越高，获得的权益和曝光机会越多。

📊 等级划分
• LV1 新手创作者：完成实名认证即可
• LV2 进阶创作者：发布5篇优质文章
• LV3 资深创作者：发布20篇文章，累计阅读量超过1万
• LV4 专家创作者：发布50篇文章，累计阅读量超过5万
• LV5 大师创作者：发布100篇文章，累计阅读量超过20万

🎁 等级权益
LV1：基础创作权限
- 每日可发布1篇文章
- 基础编辑功能使用
- 参与社区讨论

LV2：进阶权益
- 每日可发布3篇文章
- 使用高级编辑工具
- 文章进入推荐池
- 获得创作积分奖励

LV3：资深特权
- 每日可发布5篇文章
- 优先审核通道
- 专属创作标识
- 月度创作奖金

LV4：专家权限
- 无发文数量限制
- 人工审核加急
- 专题创作邀请
- 创作数据分析报告

LV5：大师特权
- 平台最高优先级
- 专属客服支持
- 平台决策参与权
- 年度创作者大奖

📈 升级规则
• 基于发布文章数量和质量
• 考虑阅读量、点赞、评论等互动数据
• 内容质量和原创性权重最高
• 违规内容将影响等级提升`
        },
        {
          id: 1,
          label: "内容曝光",
          content: `内容曝光机制说明

🔍 推荐算法
我们的智能推荐系统会根据多维度因素为您的内容匹配合适的读者：

内容质量评分
• 原创性检测
• 文章结构完整性
• 语言表达流畅度
• 信息价值和准确性

用户匹配度
• 用户历史阅读偏好
• 搜索关键词匹配
• 社交关系网络
• 地理位置和语言

时效性因素
• 发布时间新鲜度
• 热点话题相关性
• 季节性和时效性内容

📢 曝光渠道
首页推荐
• 精选内容展示
• 个性化推荐流
• 编辑精选专题

搜索排名
• 关键词搜索优化
• 相关性排序
• 权威性加分

社交传播
• 好友分享推荐
• 社区热门话题
• 互动传播效应

📈 提升曝光技巧
1. 标题优化
- 使用吸引人且准确的标题
- 包含关键词但避免堆砌
- 控制长度在20-30字最佳

2. 内容质量
- 确保内容原创且有价值
- 结构清晰，段落分明
- 配图和排版要美观

3. 发布时机
- 选择用户活跃时间段发布
- 关注热点话题和时效性
- 保持稳定更新频率

4. 互动促进
- 及时回复读者评论
- 引导用户点赞分享
- 参与相关话题讨论

🎯 曝光数据分析
在创作者后台可以查看：
• 文章阅读量趋势
• 用户来源分析
• 互动数据统计
• 曝光转化率报告

通过持续优化内容和发布策略，提升您的内容曝光效果！`
        },
        {
          id: 2,
          label: "内容审核",
          content: `内容审核标准与流程

⚖️ 审核原则
我们坚持公平、公正、透明的审核原则，确保平台内容质量，维护良好的创作环境。

✅ 允许的内容类型
原创优质内容
• 有独特观点和见解
• 信息准确可靠
• 结构完整清晰

知识分享类
• 教程和指南
• 经验总结
• 行业分析

生活记录类
• 真实的生活分享
• 有意义的人生感悟
• 正能量的生活态度

🚫 禁止的内容
违法违规内容
• 违反国家法律法规
• 涉及敏感政治话题
• 传播虚假信息

低质量内容
• 抄袭剽窃他人作品
• 无意义的文字堆砌
• 重复或相似内容过多

不当行为
• 恶意营销推广
• 人身攻击和诽谤
• 色情低俗内容

🔍 审核流程
机器初审（1-5分钟）
• 基础规则检测
• 关键词过滤
• 抄袭检测

人工审核（5-30分钟）
• 内容质量评估
• 上下文理解
• 复杂情况判断

审核结果
• 通过：正常发布
• 需修改：指出具体问题
• 拒绝：说明拒绝原因

📋 审核标准详解
内容质量（40%权重）
• 原创性和独特性
• 信息价值和实用性
• 逻辑结构和表达

规范合规（30%权重）
• 符合法律法规
• 遵守平台规则
• 尊重他人权益

用户体验（20%权重）
• 排版美观易读
• 图文搭配合理
• 无误导性信息

社会责任（10%权重）
• 传播正能量
• 促进知识分享
• 维护社区和谐

⚠️ 常见问题及处理
1. 审核时间过长
- 高峰期审核可能延迟
- 可在创作者后台查看进度
- 紧急情况可联系客服

2. 对审核结果有异议
- 可在7天内申诉
- 提供详细的说明材料
- 平台会重新审核

3. 如何提高通过率
- 仔细阅读平台规则
- 确保内容原创优质
- 避免敏感话题

📞 申诉渠道
如对审核结果有异议，请通过以下方式申诉：
• 创作者后台申诉入口
• 客服邮箱：support@xiaolanh.com
• 客服电话：400-123-4567（工作日9:00-18:00）

我们会认真对待每一次申诉，确保审核公平公正。`
        },
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

    // 格式化内容，处理换行和样式
    formatContent(content) {
      return content
        .replace(/\n\n/g, '</p><p>')
        .replace(/\n/g, '<br>')
        .replace(/^/, '<p>')
        .replace(/$/, '</p>')
        .replace(/• (.*?)(<br>|$)/g, '<li>$1</li>')
        .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
        .replace(/🌟|📊|🎁|📈|🔍|📢|📈|✅|🚫|⚖️|🔍|📋|⚠️|📞/g, '<span class="emoji">$&</span>');
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
      <div v-for="tab in tabs" :key="tab.id" v-show="tab.id === activeTab" class="content-section">
        <pre v-html="formatContent(tab.content)"></pre>
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
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  max-height: calc(100vh - 250px);
  overflow-y: auto;
}

.content-section {
  line-height: 1.6;
  color: #333;
}

.content-section pre {
  font-family: 'Arial', sans-serif;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  padding: 0;
  font-size: 14px;
}

.content-section p {
  margin: 10px 0;
  font-size: 14px;
}

.content-section ul {
  margin: 8px 0;
  padding-left: 20px;
}

.content-section li {
  margin: 4px 0;
  list-style-type: none;
  position: relative;
}

.content-section li::before {
  content: "•";
  color: #409eff;
  font-weight: bold;
  position: absolute;
  left: -15px;
}

.emoji {
  font-size: 16px;
  margin-right: 4px;
}
</style>
