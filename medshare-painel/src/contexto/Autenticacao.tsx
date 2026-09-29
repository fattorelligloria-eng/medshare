import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, EVENTO_SESSAO_ENCERRADA, guardarSessao, lerSessao } from '../api/cliente'
import type { Papel, Sessao } from '../api/tipos'

interface Autenticacao {
  sessao: Sessao | null
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => void
  temPapel: (papel: Papel) => boolean
  /** Usada pelo cadastro, que já recebe a sessão pronta da API. */
  definirSessaoManualmente: (sessao: Sessao) => void
}

const Contexto = createContext<Autenticacao | null>(null)

export function ProvedorDeAutenticacao({ children }: { children: ReactNode }) {
  const [sessao, definirSessao] = useState<Sessao | null>(() => lerSessao())

  // Quando o cliente HTTP não consegue renovar o token, a tela volta ao login
  // em vez de continuar mostrando telas que só vão dar erro.
  useEffect(() => {
    const aoEncerrar = () => definirSessao(null)
    window.addEventListener(EVENTO_SESSAO_ENCERRADA, aoEncerrar)
    return () => window.removeEventListener(EVENTO_SESSAO_ENCERRADA, aoEncerrar)
  }, [])

  const entrar = useCallback(async (email: string, senha: string) => {
    const nova = await api.post<Sessao>('/autenticacao/login', { email, senha })
    guardarSessao(nova)
    definirSessao(nova)
  }, [])

  const sair = useCallback(() => {
    guardarSessao(null)
    definirSessao(null)
  }, [])

  const temPapel = useCallback(
    (papel: Papel) => sessao?.papeis.includes(papel) ?? false,
    [sessao],
  )

  const definirSessaoManualmente = useCallback((nova: Sessao) => {
    guardarSessao(nova)
    definirSessao(nova)
  }, [])

  const valor = useMemo(
    () => ({ sessao, entrar, sair, temPapel, definirSessaoManualmente }),
    [sessao, entrar, sair, temPapel, definirSessaoManualmente],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useAutenticacao() {
  const contexto = useContext(Contexto)
  if (!contexto) {
    throw new Error('useAutenticacao precisa estar dentro de ProvedorDeAutenticacao')
  }
  return contexto
}
