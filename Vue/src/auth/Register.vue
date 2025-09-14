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

    // 辅助函数：判断字符串是否为严格的顺序或逆序（支持数字和字母）
    const isSequential = (str) => {
      if (str.length < 2) return false;
      let ascending = true;
      let descending = true;
      for (let i = 1; i < str.length; i++) {
        if (str.charCodeAt(i) !== str.charCodeAt(i - 1) + 1) {
          ascending = false;
        }
        if (str.charCodeAt(i) !== str.charCodeAt(i - 1) - 1) {
          descending = false;
        }
      }
      return ascending || descending;
    };

    // 验证密码复杂性
    const validatePassword = () => {
      if (!password.value) {
        passwordError.value = "密码不能为空";
        return;
      }
      if (password.value.length < 8) {
        passwordError.value = "密码长度至少8位";
        return;
      }
      // 检查是否全为同一字符
      if (password.value.split("").every((ch) => ch === password.value[0])) {
        passwordError.value = "密码不能全为相同字符";
        return;
      }
      // 检查是否为简单顺序或逆序（数字或字母）
      if (isSequential(password.value)) {
        passwordError.value = "密码不能为简单的顺序或逆序排列";
        return;
      }
      passwordError.value = "";
    };

    // 验证电话号码
    const validatePhone = () => {
      if (!phone.value) {
        phoneError.value = "电话号码不能为空";
        return;
      }
      if (!/^\d+$/.test(phone.value)) {
        phoneError.value = "电话号码只能包含数字";
        return;
      }
      // 根据国家代码限制数字位数
      let requiredLength = 0;
      if (countryCode.value === "+86") {
        requiredLength = 11;
      } else if (countryCode.value === "+1") {
        requiredLength = 10;
      } else if (countryCode.value === "+44") {
        requiredLength = 10;
      } else if (countryCode.value === "+91") {
        requiredLength = 10;
      }
      if (phone.value.length !== requiredLength) {
        phoneError.value = `对于${countryCode.value}的号码，必须是${requiredLength}位数字`;
        return;
      }
      // 检查是否全为相同数字
      if (phone.value.split("").every((ch) => ch === phone.value[0])) {
        phoneError.value = "电话号码不能全为相同数字";
        return;
      }
      // 检查是否为简单顺序或逆序排列
      if (isSequential(phone.value)) {
        phoneError.value = "电话号码不能为简单的顺序或逆序排列";
        return;
      }
      phoneError.value = "";
    };

    // 验证邮箱格式
    const validateEmail = () => {
      if (email.value) {
        // 必须包含 @ 和 .
        if (!email.value.includes("@") || !email.value.includes(".")) {
          emailError.value = "请输入有效的邮箱地址，必须包含 @ 和 .";
          return;
        }
        // 不能只由 @ 和 . 组成
        if (email.value.replace(/[@.]/g, "").length === 0) {
          emailError.value = "邮箱地址不能仅包含 @ 和 .";
          return;
        }
      }
      emailError.value = "";
    };

    // 验证性别（虽然默认有值，但仍做检查）
    const validateGender = () => {
      if (!gender.value) {
        genderError.value = "请选择性别";
      } else {
        genderError.value = "";
      }
    };

    const handleRegister = async () => {
      // 基本非空验证
      if (!username.value) {
        usernameError.value = "用户名不能为空";
      } else {
        usernameError.value = "";
      }
      if (!password.value) {
        passwordError.value = "密码不能为空";
      }
      if (!phone.value) {
        phoneError.value = "电话号码不能为空";
      }
      validateGender();
      // 调用各项验证函数
      validatePassword();
      validatePhone();
      validateEmail();

      // 若有任一错误，不提交
      if (
          usernameError.value ||
          passwordError.value ||
          phoneError.value ||
          emailError.value ||
          genderError.value
      ) {
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
        alert("注册失败，请重试");
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
