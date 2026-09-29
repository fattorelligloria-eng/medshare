import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // O painel chama /api e o Vite repassa para o Spring Boot. Assim o codigo
    // do front nao precisa saber o endereco da API nem lidar com CORS em
    // desenvolvimento.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
