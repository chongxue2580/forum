import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5181,
    historyApiFallback: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // 头像、文章图片等上传文件由后端静态资源 /uploads/** 提供，
      // 开发环境需代理到后端，否则图片会落到 Vite dev server 上 404。
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1100,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/vuex/')) {
            return 'vue-vendor'
          }
          if (id.includes('/element-plus/') || id.includes('/@element-plus/')) {
            return 'element-plus'
          }
          if (id.includes('/@fortawesome/')) {
            return 'fontawesome'
          }
          if (id.includes('/marked/') || id.includes('/highlight.js/')) {
            return 'markdown'
          }
          if (id.includes('/axios/')) {
            return 'http-vendor'
          }
          return 'vendor'
        }
      }
    }
  }
})
