<template>
  <div class="flex justify-center items-center min-h-screen bg-gray-100">
    <!-- 主体内容 -->
    <main class="bg-white shadow-md rounded-lg p-8 w-full max-w-md  justify-center mb-6">
      <!-- 语言选择器 -->
      <div class="language-selector mb-6 fixed top-4 right-4 flex items-center space-x-2">
        <p class="text-black font-bold">语言选择</p>
        <select
            class="border border-gray-300 text-sm rounded-md py-2 px-4"
            name="language"
            v-model="selectedLanguage"
            @change="changeLanguage"
        >
          <option value="zh-cn">简体中文</option>
          <option value="en-US">English/英语</option>
          <option value="ja-JP">Japanese/日语</option>
        </select>
      </div>

      <div class="back-selector mb-6 fixed top-4 right-4 flex items-center space-x-2">
        <button @click="goBack" class="text-blue-500 hover:underline">返回上一级</button>
      </div>

      <!-- 隐私政策标题 -->
      <div class="policy-title mb-6">
        <p class="text-gray-500 text-sm">{{ lastUpdated }}</p>
        <h1 class="text-4xl font-bold mt-4">{{ policyTitle }}</h1>
      </div>

      <!-- 概述内容 -->
      <div class="policy-overview text-left text-gray-700 text-base leading-relaxed">
        <p>{{ introText }}</p>
        <p class="mt-4">{{ policyContent }}</p>
      </div>

      <div class="text-center mt-4 text-sm text-gray-500 self-end">
        <p>{{ sectionTitle }}</p>
      </div>
    </main>
  </div>
</template>

<script>
export default {
  name: "PrivacyPolicy",
  data() {
    return {
      selectedLanguage: "zh-cn", // 默认语言
      lastUpdated: "更新日期: 2024年11月4日", // 更新日期
      policyTitle: "隐私政策", // 隐私政策标题
      sectionTitle: "祝你在使用中体验愉快", // 第一部分标题
      introText: "对于个人，您可以阅读我们的隐私政策版本。",
      policyContent:
          "我们尊重您的隐私，并坚定地致力于妥善保管我们从您那里获得的信息或所获得的有关您的信息。本隐私政策说明了我们会如何处理在您使用我们网站、应用程序和服务（统称“服务”）时我们从您那里收集的个人数据或所收集的有关您的个人数据。",
      translations: {
        "zh-cn": {
          lastUpdated: "更新日期: 2024年11月4日",
          policyTitle: "隐私政策",
          sectionTitle: "我们收集的个人数据",
          introText: "对于个人，您可以阅读我们的隐私政策版本。",
          policyContent:
              "我们尊重您的隐私，并坚定地致力于妥善保管我们从您那里获得的信息或所获得的有关您的信息。本隐私政策说明了我们会如何处理在您使用我们网站、应用程序和服务（统称“服务”）时我们从您那里收集的个人数据或所收集的有关您的个人数据。",
        },
        "en-US": {
          lastUpdated: "Last Updated: November 4, 2024",
          policyTitle: "Privacy Policy",
          sectionTitle: "Wishing you a pleasant experience during use",
          introText: "For individuals, you can read our version of the privacy policy.",
          policyContent:
              "We respect your privacy and are firmly committed to properly safeguarding the information we obtain from you or about you. This privacy policy explains how we handle the personal data we collect from you or about you when you use our website, applications, and services (collectively, 'services').",
        },
        "ja-JP": {
          lastUpdated: "更新日: 2024年11月4日",
          policyTitle: "プライバシーポリシー",
          sectionTitle: "ご利用中に楽しい体験をお過ごしください",
          introText: "個人向けには、プライバシーポリシーのバージョンをお読みいただけます。",
          policyContent:
              "私たちはお客様のプライバシーを尊重し、お客様から取得した情報やお客様に関する情報を適切に保護することに全力を尽くします。本プライバシーポリシーは、当社のウェブサイト、アプリケーション、サービス（以下「サービス」と総称）を利用する際に収集する個人データの取り扱いについて説明しています。",
        },
      },
    };
  },
  methods: {
    changeLanguage() {
      const translation = this.translations[this.selectedLanguage];
      if (translation) {
        this.lastUpdated = translation.lastUpdated;
        this.policyTitle = translation.policyTitle;
        this.sectionTitle = translation.sectionTitle;
        this.introText = translation.introText;
        this.policyContent = translation.policyContent;
      }
    },
    goBack() {
      if (this.$router.history && this.$router.history.state.back) {
        // 有上一页时返回上一页
        this.$router.go(-1);
      } else {
        // 没有上一页时返回登录页面
        this.$router.push("/login");
      }
    },
  },
};
</script>


<style scoped>
.text-center {
  margin-top: auto; /* 将元素推到容器底部 */
  padding-top: 1rem; /* 增加顶部间距，与其他内容分隔 */
  text-align: center; /* 居中对齐文字 */
  font-size: 0.875rem; /* 设置字体大小 */
  color: #6b7280; /* 深灰色文字 */
}
.language-selector {
  position: fixed; /* 固定定位 */
  top: 16px;       /* 距离页面顶部 16px */
  right: 16px;     /* 距离页面右侧 16px */
  z-index: 10;     /* 保证语言选择器在其他元素之上 */
}
.back-selector {
  position: fixed; /* 固定定位 */
  top: 16px;       /* 距离页面顶部 16px */
  left: 16px;     /* 距离页面右侧 16px */
  z-index: 10;     /* 保证语言选择器在其他元素之上 */
}

/* ======= 通用样式 ======= */
body {
  font-family: Arial, sans-serif;
  line-height: 1.6;
  color: #333;
}

.flex {
  display: flex;
}

.justify-center {
  justify-content: center;
}

.items-center {
  align-items: center;
}

.min-h-screen {
  min-height: 100vh;
}

/* ======= 布局相关 ======= */
.bg-gray-100 {
  background-color: #f3f4f6;
}

.bg-white {
  background-color: #ffffff;
}

.shadow-md {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.rounded-lg {
  border-radius: 0.5rem;
}

.p-8 {
  padding: 2rem;
}

.w-full {
  width: 100%;
}

.max-w-md {
  max-width: 50rem;
  height: 1000px;
}

.mb-6 {
  margin-bottom: 1.5rem;
}

/* ======= 文本样式 ======= */
.text-4xl {
  font-size: 2.25rem;
}

.font-bold {
  font-weight: 700;
}

.text-sm {
  font-size: 0.875rem;
}

.text-gray-500 {
  color: #a0aec0;
}

.text-gray-700 {
  color: #4a5568;
}

.text-xl {
  font-size: 1.25rem;
  font-weight: 600;
}

.leading-relaxed {
  line-height: 1.75;
}

/* ======= 特定组件 ======= */
.language-selector select {
  max-width: 200px;
}

.policy-overview {
  margin-top: 2rem;
}

.section h2 {
  border-bottom: 2px solid #333;
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}
</style>
