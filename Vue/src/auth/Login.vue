<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">欢迎回来</h1>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="login-form-group">
          <label for="username" class="login-label">请输入账号/电话/邮箱</label>
          <input
              type="text"
              id="username"
              v-model="username"
              placeholder="请输入账号/电话/邮箱"
              class="login-input"
              required
          />
        </div>
        <div class="login-form-group">
          <label for="password" class="login-label">密码</label>
          <input
              type="password"
              id="password"
              v-model="password"
              placeholder="请输入密码"
              class="login-input"
              required
          />
        </div>
        <button
            type="submit"
            class="login-button"
            :disabled="!username || !password"
        >
          登录
        </button>
      </form>
      <div class="login-footer">
        <p class="login-register-text">
          还没有账号?
          <router-link to="/register" class="login-link">注册</router-link>
        </p>
        <p class="login-divider">或</p>
        <div class="login-social">
          <button class="login-social-button" @click="handleGoogleLogin">继续使用 Google 登录</button>
          <button class="login-social-button" @click="handleMicrosoftLogin">继续使用 Microsoft 帐户登录</button>
          <button class="login-social-button">
            继续使用 Apple 登录
            <router-link to="/chatpage" class="login-link">测试</router-link>
          </button>
        </div>
      </div>
      <div class="login-terms">
        <router-link to="/uservalue" class="login-link">使用条款</router-link>
        <router-link to="/value" class="login-link">隐私政策</router-link>
      </div>
    </div>
  </div>
</template>
<script>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { instance } from "../api/axiosInstance";

export default {
  name: "Login",
  setup() {
    const username = ref("");
    const password = ref("");
    const router = useRouter();

    const handleLogin = async () => {
      try {
        const response = await instance.post("/login", {
          username: username.value,
          password: password.value,
        });
        const token = response.data.token;
        if (token) {
          localStorage.setItem("token", token);
          router.push("/articlemain");
        } else {
          alert("用户名或密码错误");
        }
      } catch (error) {
        console.error("登录请求失败", error);
        if (error.response && error.response.data) {
          alert(error.response.data);
        } else {
          alert("登录失败，请重试");
        }
      }
    };

    // 处理 Google 登录
    const handleGoogleLogin = () => {
      // 重定向到后端OAuth2 Google登录端点
      window.location.href = `${window.location.origin}/oauth2/authorization/google`;
    };

    // 处理 Microsoft 登录
    const handleMicrosoftLogin = () => {
      // 重定向到后端OAuth2 Microsoft登录端点
      window.location.href = `${window.location.origin}/oauth2/authorization/microsoft`;
    };

    return {
      username,
      password,
      handleLogin,
      handleGoogleLogin,      // 确保方法在这里返回
      handleMicrosoftLogin,   // 确保方法在这里返回
    };
  },
};
</script>

<style scoped>
/* 登录页面整体容器 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f3f4f6;
  padding: 0 20px;
}

/* 登录卡片 */
.login-card {
  background-color: #ffffff;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

/* 登录标题 */
.login-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
}

/* 登录表单 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 表单组 */
.login-form-group {
  display: flex;
  flex-direction: column;
}

/* 表单标签 */
.login-label {
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
  margin-bottom: 8px;
}

/* 输入框 */
.login-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  box-sizing: border-box;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  height: 40px;
  background-color: #48bb78;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.login-button:hover:not(:disabled) {
  background-color: #38a169;
}

.login-button:disabled {
  background-color: #d2d6dc;
  cursor: not-allowed;
}

/* 登录页页脚 */
.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

/* 注册提示文字 */
.login-register-text {
  margin-bottom: 8px;
}

/* 分割线 */
.login-divider {
  margin: 10px 0;
  color: #a0aec0;
}

/* 社交登录按钮组 */
.login-social {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 社交登录按钮 */
.login-social-button {
  width: 100%;
  padding: 10px;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  background-color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.login-social-button:hover {
  background-color: #e2e8f0;
}

/* 条款与隐私 */
.login-terms {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #a0aec0;
  display: flex;
  justify-content: center;
  gap: 20px; /* 增加间距 */
}

/* 统一链接样式 */
.login-link {
  color: #3b82f6;
  text-decoration: none;
}
</style>