<template>
  <!-- 整体聊天容器 -->
  <div class="chat-container">
    <!-- 返回按钮 -->
    <button class="back-button" @click="goBack">返回</button>
    <div class="chat-bg">
      <div class="mask"></div>
      <div class="chat-box">
        <!-- 左侧导航栏 -->
        <div class="box-left">
          <div class="chat-list-title">个人中心</div>
          <!-- 循环渲染导航项 -->
          <div
              v-for="(item, index) in navItems"
              :key="index"
              class="nav-item"
              :class="{ active: currentView === item.view }"
              @click="changeView(item.view)"
          >
            <i :class="item.icon"></i>
            <span>{{ item.name }}</span>
          </div>
        </div>
        <!-- 中间内容区域 -->
        <div class="box-middle">
          <div class="middle-top">
            {{ currentView === 'profile' ? '个人资料' : currentView + ' 页面' }}
          </div>
          <div class="middle-content">
            <!-- 当前视图为个人资料时显示 -->
            <div v-if="currentView === 'profile'" class="profile-container">
              <!-- 左侧表单区域 -->
              <div class="profile-left">
                <form @submit.prevent="saveProfile" class="profile-form">
                  <!-- 用户名 -->
                  <div class="form-group">
                    <label>用户名</label>
                    <input
                        type="text"
                        v-model="profile.username"
                        placeholder="请输入用户名"
                        maxlength="20"
                    />
                    <span>{{ profile.username.length }}/20</span>
                  </div>
                  <!-- 邮箱 -->
                  <div class="form-group">
                    <label>邮箱</label>
                    <input
                        type="email"
                        v-model="email"
                        placeholder="请输入邮箱"
                    />
                  </div>
                  <!-- 电话号码 -->
                  <div class="register-form__group">
                    <label class="register-form__label" for="phone">电话号码</label>
                    <div class="register-form__phone">
                      <select
                          v-model="countryCode"
                          class="register-form__phone-select"
                          @change="validatePhone"
                      >
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
                  <!-- 性别选择 -->
                  <div class="register-form__group">
                    <label class="register-form__label">性别</label>
                    <div class="gender-selection">
                      <button
                          type="button"
                          :class="{ active: gender === 'male' }"
                          @click="setGender('male')"
                      >男</button>
                      <button
                          type="button"
                          :class="{ active: gender === 'female' }"
                          @click="setGender('female')"
                      >女</button>
                    </div>
                    <p v-if="genderError" class="register-form__error">{{ genderError }}</p>
                  </div>
                  <!-- 保存按钮 -->
                  <button type="submit" class="save-btn">保存修改</button>
                </form>
              </div>
              <!-- 右侧头像上传区域（不涉及更新信息，本部分保持不变） -->
              <div class="profile-right">
                <div class="avatar-upload">
                  <h3>上传头像</h3>
                  <div class="avatar-wrapper" @click="triggerUpload">
                    <img :src="profile.avatar" alt="avatar" class="avatar" />
                    <div class="upload-overlay">点击上传头像</div>
                  </div>
                  <input type="file" ref="fileInput" @change="uploadAvatar" hidden />
                  <button @click="clearAvatar">清除头像</button>
                </div>
              </div>
            </div>
            <!-- 非个人资料页面显示默认内容 -->
            <div v-else>
              <h2>{{ currentView }} 页面</h2>
              <p>这里是 {{ currentView }} 页面的内容。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "App",
  data() {
    return {
      // 导航栏选项
      navItems: [
        { name: "个人资料", view: "profile", icon: "fas fa-user" },
        { name: "账号设置", view: "account", icon: "fas fa-cog" },
        { name: "通用设置", view: "common", icon: "fas fa-sliders-h" },
        { name: "消息设置", view: "message", icon: "fas fa-envelope" },
        { name: "屏蔽管理", view: "block", icon: "fas fa-ban" },
        { name: "标签管理", view: "tags", icon: "fas fa-tags" }
      ],
      // 当前视图
      currentView: "profile",
      // 个人资料数据
      profile: {
        username: "",
        avatar: "https://via.placeholder.com/100"
      },
      // 单独定义的字段（与后端对应的更新字段）
      email: "",
      phone: "",
      gender: "",
      // 用于错误提示
      genderError: "",
      phoneError: "",
      emailError: "",
      countryCode: "+86"
    };
  },
  mounted() {
    this.checkLoginStatus();
    this.fetchProfile();

  },
  methods: {
    // 检查用户登录状态
    checkLoginStatus() {
      const token = localStorage.getItem('token');
      if (!token) {
        alert('尚未登录, 将跳转到登录页面');
        this.$router.push('/login');
        return;
      }},
    // 返回上一页
    goBack() {
      window.history.back();
    },
    // 切换视图
    changeView(view) {
      this.currentView = view;
    },
    // 通过后端接口获取当前用户信息，并填充到表单中
    fetchProfile() {
      // 假设 token 存在 localStorage 中
      const token = localStorage.getItem("token");
      if (!token) {
        console.error("未登录，无法获取用户信息");
        return;
      }
      fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/user/info`, {
        method: "GET",
        headers: {
          "Authorization": "Bearer " + token,
          "Content-Type": "application/json"
        }
      })
          .then(res => res.json())
          .then(data => {
            if (data.code === 0 || data.code === 1 && data.msg === "获取用户信息成功") {
              // 假设返回 data.data 包含 username, userEmail, phoneNumber, sex
              const info = data.data;
              this.profile.username = info.username || "";
              this.email = info.userEmail || "";
              this.phone = info.phoneNumber || "";
              this.gender = info.sex === 1 ? "male" : "female";
            } else {
              console.error("获取用户信息失败：", data.msg);
            }
          })
          .catch(err => {
            console.error("请求用户信息出错：", err);
          });
    },
    // 保存个人资料，只提交 username, userEmail, phoneNumber, sex
    saveProfile() {
      // 构造只包含更新字段的 payload
      const payload = {
        username: this.profile.username,
        userEmail: this.email,
        phoneNumber: this.phone,
        sex: this.gender === "male" ? 1 : 0
      };
      // 假设 token 存储在 localStorage 中
      const token = localStorage.getItem("token");
      fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/user/info`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + token
        },
        body: JSON.stringify(payload)
      })
          .then(res => res.json())
          .then(data => {
            if (data.code === 0) {
              alert("更新成功！");
            } else {
              alert("更新失败：" + data.msg);
            }
          })
          .catch(err => {
            console.error("更新用户信息请求失败：", err);
            alert("更新请求失败");
          });
    },
    // 以下方法与头像上传、标签操作等保持不变
    triggerUpload() {
      this.$refs.fileInput.click();
    },
    uploadAvatar(event) {
      const file = event.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.profile.avatar = e.target.result;
      };
      reader.readAsDataURL(file);
    },
    clearAvatar() {
      this.profile.avatar = "https://via.placeholder.com/100";
    },
    setGender(value) {
      this.gender = value;
      this.genderError = "";
    },
    validatePhone() {
      const phoneReg = /^\d{6,}$/;
      if (!phoneReg.test(this.phone)) {
        this.phoneError = "请输入正确的电话号码。";
      } else {
        this.phoneError = "";
      }
    },
    validateEmail() {
      if (this.email === "") {
        this.emailError = "";
        return;
      }
      const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailReg.test(this.email)) {
        this.emailError = "请输入正确的邮箱格式。";
      } else {
        this.emailError = "";
      }
    }
  }
};
</script>

<style scoped>
/* 整体聊天容器样式 */
.chat-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f0f0, #d0e0f0);
}

/* 聊天盒子样式 */
.chat-box {
  width: 90%;
  height: 90vh;
  display: flex;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

/* 左侧导航栏样式 */
.box-left {
  flex: 2;
  background: #f5f5f5;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

/* 中间内容区域样式（仅允许竖向滚动） */
.box-middle {
  flex: 12;
  background: white;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
  overflow-x: hidden; /* 禁止横向滚动 */
  scrollbar-width: thin;
  scrollbar-color: gray #1a1a1a;
}

/* 页面标题 */
.middle-top {
  font-size: 1.5em;
  font-weight: bold;
}

/* 导航项样式 */
.nav-item {
  margin: 10px 0;
  padding: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background 0.3s;
}

.nav-item.active,
.nav-item:hover {
  background: #007bff;
  color: white;
}

/* 全局表单、输入框样式 */
input,
textarea,
select {
  width: 100%;
  padding: 10px;
  margin-top: 5px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

/* 保存按钮样式 */
.save-btn {
  background: #007bff;
  color: white;
  padding: 10px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

/* 标签样式 */
.tag {
  padding: 5px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin: 5px;
  display: inline-block;
  cursor: pointer;
  background: #f0f0f0;
  transition: background 0.3s;
}

.tag.selected {
  background: #007bff;
  color: white;
}

.remove-tag {
  margin-left: 5px;
  cursor: pointer;
}

/* 头像上传区域样式 */
.avatar-upload {
  margin-bottom: 20px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  cursor: pointer;
  margin-top: 10px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 2px solid #ccc;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100px; /* 固定宽度 */
  height: 100px; /* 高度与宽度相同 */
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .upload-overlay {
  opacity: 1;
}

/* 个人资料左右布局 */
.profile-container {
  display: flex;
  gap: 20px;
}

/* 左侧表单区域 */
.profile-left {
  flex: 2;
}

/* 右侧头像上传区域 */
.profile-right {
  flex: 1;
  margin-left: 30px;
}

/* 表单分组、标签及错误提示样式 */
.register-form__group {
  margin-bottom: 15px;
}

.register-form__label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

/* 修改后的电话号码组合样式 */
.register-form__phone {
  display: flex;
  width: 100%;
}

/* 修改后的区号选择样式 */
.register-form__phone-select {
  width: 25%;
  height: 40px;
  border: 1px solid #d2d6dc;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  padding: 0 8px;
  background-color: #fff;
  box-sizing: border-box;
}

/* 修改后的电话号码输入框样式 */
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

/* 公共输入框样式（用于邮箱等） */
.register-form__input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  box-sizing: border-box;
}

/* 错误提示样式 */
.register-form__error {
  color: #f56565;
  font-size: 12px;
  margin-top: 4px;
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

/* 返回按钮样式 */
.back-button {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
  z-index: 999;
}

.back-button:hover {
  background-color: #0056b3;
  transform: scale(1.05);
}
</style>
