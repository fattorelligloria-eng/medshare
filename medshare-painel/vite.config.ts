import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

/**
 * Para onde o painel manda as chamadas /api.
 *
 * Em ordem: a variável MEDSHARE_API, se definida; senão a MEDSHARE_PORTA do
 * .env da API (o mesmo arquivo que a API lê); senão a 8080. Ler o .env evita o
 * erro de a API subir na 8081 — porque a 8080 está ocupada na máquina — e o
 * painel continuar chamando a 8080: as telas carregavam vazias, sem aviso,
 * como a lista de municípios do cadastro.
 */
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
    // O painel chama /api e o Vite repassa para o Spring Boot. Assim o codigo
    // do front nao precisa saber o endereco da API nem lidar com CORS em
    // desenvolvimento.
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
