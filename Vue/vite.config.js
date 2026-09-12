// vite.config.js
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { proxyOrigin } from './src/utils/serviceUrls.js';

export default defineConfig(async ({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), 'VITE_');

  // 动态导入插件以支持条件加载
  const plugins = [vue()];

  // 开发环境下添加 bundle 分析器
  if (mode === 'development') {
    const { visualizer } = await import('rollup-plugin-visualizer');
    plugins.push(
      visualizer({
        open: true,
        filename: 'dist/stats.html',
        gzipSize: true,
        brotliSize: true,
      })
    );
  }

  return {
    plugins,
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'), // 使用 path.resolve 确保路径正确
        '@constant': path.resolve(__dirname, 'src/constant'),
      },
    },
    server: {
     // host: '0.0.0.0', // 允许外部访问
      port: 5173, // 确保端口号正确
      proxy: {
        '/api': { // 代理所有/api开头的请求
          target: proxyOrigin(env.VITE_API_BASE_URL),
          changeOrigin: true,
          secure: false,
        },
        '/uploads': {
          target: proxyOrigin(env.VITE_API_BASE_URL),
          changeOrigin: true,
          secure: false,
        },
        '/ws': { // 代理WebSocket连接
          target: proxyOrigin(env.VITE_WS_URL, proxyOrigin(env.VITE_API_BASE_URL)),
          changeOrigin: true,
          secure: false,
          ws: true, // 启用WebSocket代理
        },
      },
    },
    // 确保环境变量在客户端代码中可用
    define: {
      global: 'globalThis', // SockJS needs the browser's XMLHttpRequest and WebSocket globals.
    },
    // 构建优化配置
    build: {
      target: 'es2015',
      // SockJS has conditional/cyclic CommonJS imports. Explicit wrapping avoids
      // plugin auto-detection races producing different bundles from identical sources.
      commonjsOptions: { strictRequires: true },
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: mode === 'production', // 生产环境移除 console
          drop_debugger: true,
        },
      },
      rollupOptions: {
        output: {
          // 手动代码分割配置
          manualChunks: {
            // 核心库
            'vendor': ['vue', 'vue-router', 'pinia'],
            // 编辑器相关
            'editor': ['quill'],
            // 工具库
            'utils': ['lodash-es', 'dayjs'],
            // WebSocket 相关
            'websocket': ['@stomp/stompjs', 'sockjs-client', 'socket.io-client'],
          },
          // 按入口点分割
          chunkFileNames: 'assets/js/[name]-[hash].js',
          entryFileNames: 'assets/js/[name]-[hash].js',
          assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
        },
      },
      // 大文件警告限制
      chunkSizeWarningLimit: 1000,
      // 小资源内联限制（4KB）
      assetsInlineLimit: 4096,
    },
    // 预加载优化
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'quill',
        'lodash-es',
        'dayjs',
        '@stomp/stompjs',
        'sockjs-client',
        'socket.io-client'
      ],
    },
  };
});

