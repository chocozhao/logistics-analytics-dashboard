import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    target: 'es2018',
    cssTarget: 'chrome61',
  },
  esbuild: {
    target: 'es2018',
  },
})
