import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0', // Expose to docker host
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://host.docker.internal:8080',
        changeOrigin: true,
        // The backend context path is /api, so we don't need to rewrite
        // /api/ai/chat... -> http://host.docker.internal:8080/api/ai/chat...
      }
    }
  }
})
