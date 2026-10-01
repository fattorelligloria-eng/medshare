import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'



function enderecoDaApi(): string {
  if (process.env.MEDSHARE_API) return process.env.MEDSHARE_API

  const envDaApi = fileURLToPath(new URL('../medshare-api/.env', import.meta.url))
  if (existsSync(envDaApi)) {
    const porta = readFileSync(envDaApi, 'utf8').match(/^\s*MEDSHARE_PORTA\s*=\s*["']?(\d+)/m)?.[1]
    if (porta) return `http://localhost:${porta}`
  }
  return 'http://localhost:8080'
}

const api = enderecoDaApi()

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,

    proxy: {
      '/api': { target: api, changeOrigin: true },
    },
  },
  preview: {
    proxy: {
      '/api': { target: api, changeOrigin: true },
    },
  },
})
