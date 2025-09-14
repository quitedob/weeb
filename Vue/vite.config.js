// vite.config.js
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '');

  return {
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
          target: env.VITE_API_BASE_URL || 'http://localhost:8080', // 使用环境变量
          changeOrigin: true,
          secure: false,
        },
        '/test': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080', // 使用环境变量
          changeOrigin: true,
          secure: false,
        },
      },
    },
    // 确保环境变量在客户端代码中可用
    define: {
      __VITE_ENV__: JSON.stringify(env),
    },
  };
});

