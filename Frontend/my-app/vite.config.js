import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/loantool': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/inventory': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/loan': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/user': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/tool': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/auth': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/kardex': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
      '/images': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
