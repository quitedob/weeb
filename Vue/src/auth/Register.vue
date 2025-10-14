<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="register-card__title">注册</h1>
      <form @submit.prevent="handleRegister" class="register-form">
        <!-- 用户名 -->
        <div class="register-form__group">
          <label class="register-form__label" for="username">用户名</label>
          <input
              id="username"
              type="text"
              v-model="username"
              placeholder="请输入用户名"
              class="register-form__input"
              required
          />
          <p v-if="usernameError" class="register-form__error">{{ usernameError }}</p>
        </div>
        <!-- 密码 -->
        <div class="register-form__group">
          <label class="register-form__label" for="password">密码</label>
          <input
              id="password"
              type="password"
              v-model="password"
              placeholder="请输入密码"
              class="register-form__input"
              @input="validatePassword"
              required
          />
          <p v-if="passwordError" class="register-form__error">{{ passwordError }}</p>
        </div>
        <!-- 性别选择 -->
        <div class="register-form__group">
          <label class="register-form__label">性别</label>
          <div class="gender-selection">
            <button
                type="button"
                :class="{ 'active': gender === 'male' }"
                @click="gender = 'male'"
            >男</button>
            <button
                type="button"
                :class="{ 'active': gender === 'female' }"
                @click="gender = 'female'"
            >女</button>
          </div>
          <p v-if="genderError" class="register-form__error">{{ genderError }}</p>
        </div>
        <!-- 电话号码 -->
        <div class="register-form__group">
          <label class="register-form__label" for="phone">电话号码</label>
          <div class="register-form__phone">
            <select v-model="countryCode" class="register-form__phone-select" @change="validatePhone">
              <option value="+86">中国 (+86)</option>
              <option value="+1">美国 (+1)</option>
              <option value="+44">英国 (+44)</option>
              <option value="+91">印度 (+91)</option>
            </select>
            <input
                id="phone"
                type="text"
                v-model="phone"
                placeholder="请输入电话号码"
                class="register-form__phone-input"
                @input="validatePhone"
                required
            />
          </div>
          <p v-if="phoneError" class="register-form__error">{{ phoneError }}</p>
        </div>
        <!-- 邮箱 (选填) -->
        <div class="register-form__group">
          <label class="register-form__label" for="email">邮箱 (选填)</label>
          <input
              id="email"
              type="email"
              v-model="email"
              placeholder="请输入邮箱"
              class="register-form__input"
              @blur="validateEmail"
          />
          <p v-if="emailError" class="register-form__error">{{ emailError }}</p>
        </div>
        <button
            type="submit"
            class="register-form__button"
            :disabled="
            usernameError ||
            passwordError ||
            phoneError ||
            !username ||
            !password ||
            !phone ||
            !gender
          "
        >
          注册
        </button>
      </form>
      <div class="register-card__footer">
        <p>
          已经有账号?
          <router-link to="/login" class="register-card__link">登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { instance } from "../api/axiosInstance";

export default {
  name: "Register",
  setup() {
    const router = useRouter();
    const username = ref("");
    const password = ref("");
    const phone = ref("");
    const email = ref("");
    const countryCode = ref("+86");
    const gender = ref("male");

    const usernameError = ref("");
    const passwordError = ref("");
    const phoneError = ref("");
    const emailError = ref("");
    const genderError = ref("");

    // 简化的注册处理 - 依赖后端进行详细验证

    // 简化的即时验证 - 仅保留基本反馈
    const validatePassword = () => {
      if (!password.value) {
        passwordError.value = "密码不能为空";
      } else if (password.value.length < 6) {
        passwordError.value = "密码至少需要6位";
      } else {
        passwordError.value = "";
      }
    };

    // 简化的电话号码验证 - 仅检查基本格式
    const validatePhone = () => {
      if (!phone.value) {
        phoneError.value = "电话号码不能为空";
      } else if (!/^\d+$/.test(phone.value)) {
        phoneError.value = "电话号码只能包含数字";
      } else {
        phoneError.value = "";
      }
    };

    // 简化的邮箱验证 - 仅检查基本格式
    const validateEmail = () => {
      if (email.value && !email.value.includes("@")) {
        emailError.value = "请输入有效的邮箱地址";
      } else {
        emailError.value = "";
      }
    };

    const handleRegister = async () => {
      // 清除之前的错误
      usernameError.value = "";
      passwordError.value = "";
      phoneError.value = "";
      emailError.value = "";
      genderError.value = "";

      // 基本前端检查 - 主要依赖后端验证
      if (!username.value || !password.value || !phone.value || !gender.value) {
        if (!username.value) usernameError.value = "请输入用户名";
        if (!password.value) passwordError.value = "请输入密码";
        if (!phone.value) phoneError.value = "请输入电话号码";
        if (!gender.value) genderError.value = "请选择性别";
        return;
      }

      const fullPhone = `${countryCode.value}${phone.value}`;

      try {
        // 注意：后端期望的字段名为 phone、email、gender
        await instance.post("/register", {
          username: username.value,
          password: password.value,
          phone: fullPhone,
          email: email.value,
          gender: gender.value, // "male" 或 "female"
        });
        alert("注册成功");
        router.push("/login");
      } catch (error) {
        console.error("注册请求失败", error);
        // 处理后端验证错误
        if (error.response && error.response.data && error.response.data.message) {
          // 显示后端返回的具体错误信息
          const errorMsg = error.response.data.message;
          if (errorMsg.includes("用户名")) {
            usernameError.value = errorMsg;
          } else if (errorMsg.includes("密码")) {
            passwordError.value = errorMsg;
          } else if (errorMsg.includes("电话") || errorMsg.includes("手机")) {
            phoneError.value = errorMsg;
          } else if (errorMsg.includes("邮箱")) {
            emailError.value = errorMsg;
          } else {
            alert(errorMsg); // 其他错误用alert显示
          }
        } else {
          alert("注册失败，请检查网络连接或稍后重试");
        }
      }
    };

    return {
      username,
      password,
      phone,
      email,
      countryCode,
      gender,
      usernameError,
      passwordError,
      phoneError,
      emailError,
      genderError,
      validatePassword,
      validatePhone,
      validateEmail,
      handleRegister,
    };
  },
};
</script>

<style scoped>
/* 整体容器：居中显示，固定宽度，左右间距相等 */
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f3f4f6;
  padding: 0 20px;
}

/* 卡片主体 */
.register-card {
  background-color: #ffffff;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

/* 标题 */
.register-card__title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
}

/* 表单 */
.register-form {
  display: flex;
  flex-direction: column;
}

/* 表单每一项 */
.register-form__group {
  margin-bottom: 20px;
}

/* 标签 */
.register-form__label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
  margin-bottom: 8px;
}

/* 通用输入框 */
.register-form__input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  box-sizing: border-box;
}

/* 电话号码组合 */
.register-form__phone {
  display: flex;
  width: 100%;
}

/* 区号选择 */
.register-form__phone-select {
  width: 30%;
  height: 40px;
  border: 1px solid #d2d6dc;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  padding: 0 8px;
  background-color: #fff;
  box-sizing: border-box;
}

/* 电话号码输入 */
.register-form__phone-input {
  width: 70%;
  height: 40px;
  border: 1px solid #d2d6dc;
  border-left: none;
  border-radius: 0 4px 4px 0;
  padding: 0 12px;
  font-size: 14px;
  box-sizing: border-box;
}

/* 错误提示 */
.register-form__error {
  color: #f56565;
  font-size: 12px;
  margin-top: 4px;
}

/* 提交按钮 */
.register-form__button {
  width: 100%;
  height: 40px;
  background-color: #48bb78;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.register-form__button:hover:not(:disabled) {
  background-color: #38a169;
}

.register-form__button:disabled {
  background-color: #d2d6dc;
  cursor: not-allowed;
}

/* 底部链接 */
.register-card__footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.register-card__link {
  color: #3b82f6;
  text-decoration: none;
}

/* 性别选择样式 */
.gender-selection {
  display: flex;
  gap: 10px;
}

.gender-selection button {
  flex: 1;
  height: 36px;
  border: 1px solid #d2d6dc;
  background-color: #fff;
  color: #4a5568;
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.gender-selection button.active,
.gender-selection button:hover {
  background-color: #e2e8f0;
}
</style>
