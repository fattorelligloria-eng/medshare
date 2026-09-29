import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ProvedorDeAutenticacao } from './contexto/Autenticacao'
import { App } from './App'
import './estilos.css'

createRoot(document.getElementById('raiz')!).render(
  <StrictMode>
    <BrowserRouter>
      <ProvedorDeAutenticacao>
        <App />
      </ProvedorDeAutenticacao>
    </BrowserRouter>
  </StrictMode>,
)
