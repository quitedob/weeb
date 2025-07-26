// vite.config.js
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
  css: {
    preprocessorOptions: {
      less: {
        // 配置 less 的全局变量或自定义配置
        javascriptEnabled: true,
      },
    },
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'), // 使用 path.resolve 确保路径正确
      '@constant': path.resolve(__dirname, 'Constant'), // 新增别名，指向 src 同级的 Constant 目录
    },
  },
  server: {
   // host: '0.0.0.0', // 允许外部访问
    port: 5173, // 确保端口号正确
    proxy: {
      '/user': {
        target: 'http://localhost:8080', // 后端接口地址
        changeOrigin: true,
        secure: false,
      },
      '/test': {
        target: 'http://localhost:8080', // 如果有其他代理需求
        changeOrigin: true,
        secure: false,
      },
    },
  },
});

