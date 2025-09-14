<script>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { instance } from '../api/axiosInstance';

export default {
  name: "Logout",
  setup() {
    const router = useRouter();

    onMounted(async () => {
      try {
        // 获取存储的令牌
        const token = localStorage.getItem('token');

        // 如果存在令牌，则发送登出请求到后端
        if (token) {
          await instance.post('/logout');
        }
      } catch (error) {
        console.error("登出请求失败", error);
      } finally {
        // 无论成功与否，都清除本地令牌
        localStorage.removeItem('token');
        // 重定向到登录页面
        router.push('/login');
      }
    });

    return {};
  }
}
</script>

<template>
  <div>
    正在登出...
  </div>
</template>

<style scoped>
/* 样式可根据需要调整 */
</style>
