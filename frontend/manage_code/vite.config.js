import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    host: '0.0.0.0',
    proxy: {
      // 开发环境保留和生产一致的网关前缀，避免前端请求落到 Vite 自身导致 404。
      '/cl584734139': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
});
