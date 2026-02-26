import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [
    react(), 
    tailwindcss()
  ],

  // 1. Nginx의 하위 경로(/app/)와 맞추기 위한 설정
  base: '/app/',

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    host: '0.0.0.0', // 2. 외부 네트워크에서 접근할 수 있도록 허용
    port: 5173,

    // 3. 보안 에러 해결: 접근을 허용할 도메인 목록 추가
    allowedHosts: [
      'www.hsjwavestlop.monster',
      'hsjwavestlop.monster'
    ],

    // 기존 백엔드 프록시 설정 유지
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
