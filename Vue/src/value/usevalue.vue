<template>
  <div class="terms-container">
    <!-- 主体内容 -->
    <main class="terms-main">
      <!-- 语言选择器 -->
      <div class="language-selector">
        <p class="text-black font-bold">语言选择</p>
        <select
            class="language-select"
            name="language"
            v-model="selectedLanguage"
            @change="changeLanguage"
        >
          <option value="zh-cn">简体中文</option>
          <option value="en-US">English/英语</option>
          <option value="ja-JP">Japanese/日语</option>
        </select>
      </div>

      <div class="back-selector">
        <button @click="goBack" class="back-button">返回上一级</button>
      </div>

      <!-- 使用条款标题 -->
      <div class="policy-title">
        <p class="policy-date">{{ lastUpdated }}</p>
        <h1 class="policy-heading">{{ termsTitle }}</h1>
      </div>

      <!-- 条款内容 -->
      <div class="terms-overview">
        <p>{{ introText }}</p>
        <p class="terms-content">{{ termsContent }}</p>
      </div>

      <div class="terms-footer">
        <p>{{ footerText }}</p>
      </div>
    </main>
  </div>
</template>

<script>
export default {
  name: "TermsOfService",
  data() {
    return {
      selectedLanguage: "zh-cn", // 默认语言
      lastUpdated: "更新日期: 2024年11月4日", // 更新日期
      termsTitle: "使用条款", // 使用条款标题
      footerText: "请仔细阅读并遵守使用条款。", // 页脚文本
      introText: "以下是使用我们服务的条款，请仔细阅读。",
      termsContent:
          "通过访问或使用我们的服务，您同意遵守这些条款。如果您不同意，请勿使用我们的服务。",
      translations: {
        "zh-cn": {
          lastUpdated: "更新日期: 2024年11月4日",
          termsTitle: "使用条款",
          footerText: "请仔细阅读并遵守使用条款。",
          introText: "以下是使用我们服务的条款，请仔细阅读。",
          termsContent:
              "通过访问或使用我们的服务，您同意遵守这些条款。如果您不同意，请勿使用我们的服务。",
        },
        "en-US": {
          lastUpdated: "Last Updated: November 4, 2024",
          termsTitle: "Terms of Service",
          footerText: "Please read and adhere to the terms of service carefully.",
          introText: "Below are the terms for using our services. Please read them carefully.",
          termsContent:
              "By accessing or using our services, you agree to abide by these terms. If you do not agree, please do not use our services.",
        },
        "ja-JP": {
          lastUpdated: "更新日: 2024年11月4日",
          termsTitle: "利用規約",
          footerText: "利用規約をよく読み、遵守してください。",
          introText: "以下は、当社サービスを利用するための規約です。よくお読みください。",
          termsContent:
              "当社のサービスにアクセスまたは使用することで、これらの規約を遵守することに同意したものとみなされます。異議がある場合は、当社のサービスを使用しないでください。",
        },
      },
    };
  },
  methods: {
    changeLanguage() {
      const translation = this.translations[this.selectedLanguage];
      if (translation) {
        this.lastUpdated = translation.lastUpdated;
        this.termsTitle = translation.termsTitle;
        this.footerText = translation.footerText;
        this.introText = translation.introText;
        this.termsContent = translation.termsContent;
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
.terms-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f3f4f6;
}

.terms-main {
  background-color: #ffffff;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-radius: 0.5rem;
  padding: 2rem;
  width: 100%;
  max-width: 50rem;
  margin-bottom: 1.5rem;
}

.language-selector {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.language-select {
  border: 1px solid #d1d5db;
  font-size: 0.875rem;
  border-radius: 0.375rem;
  padding: 0.5rem 1rem;
}

.back-selector {
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 10;
}

.back-button {
  color: #3b82f6;
  text-decoration: underline;
  cursor: pointer;
}

.policy-title {
  margin-bottom: 1.5rem;
}

.policy-date {
  color: #a0aec0;
  font-size: 0.875rem;
}

.policy-heading {
  font-size: 2.25rem;
  font-weight: 700;
  margin-top: 1rem;
}

.terms-overview {
  text-align: left;
  color: #4a5568;
  font-size: 1rem;
  line-height: 1.75;
  margin-top: 2rem;
}

.terms-content {
  margin-top: 1rem;
}

.terms-footer {
  margin-top: auto;
  padding-top: 1rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
</style>
