import { useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { MeusDados } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'

/**
 * Os dados da própria conta, compartilhados entre as telas.
 *
 * O menu lateral, o Início, a Conta e a carteirinha mostram a mesma foto e o
 * mesmo número. Cada um buscando sozinho seriam quatro chamadas iguais a cada
 * troca de tela; aqui é uma, e quem troca a foto avisa todo mundo de uma vez.
 *
 * O link da foto vence em 15 minutos (é assinado, como o das fotos de receita),
 * então o que foi buscado há mais de 10 é buscado de novo.
 */
const VALIDADE_MS = 10 * 60_000

let guardado: { dono: number; dados: MeusDados; quando: number } | null = null
let buscando: Promise<MeusDados> | null = null
const ouvintes = new Set<(dados: MeusDados) => void>()

function avisar(dados: MeusDados) {
  ouvintes.forEach((ouvinte) => ouvinte(dados))
}

/** Depois de salvar o cadastro ou trocar a foto: todas as telas veem o novo na hora. */
export function guardarMeusDados(dono: number, dados: MeusDados) {
  guardado = { dono, dados, quando: Date.now() }
  avisar(dados)
}

export function useMeusDados(): { dados: MeusDados | null; erro: unknown; recarregar: () => void } {
  const { sessao } = useAutenticacao()
  const dono = sessao?.usuarioId ?? 0
  const valido = guardado && guardado.dono === dono && Date.now() - guardado.quando < VALIDADE_MS
  const [dados, definirDados] = useState<MeusDados | null>(valido ? guardado!.dados : null)
  const [erro, definirErro] = useState<unknown>(null)
  const [versao, definirVersao] = useState(0)

  useEffect(() => {
    ouvintes.add(definirDados)
    return () => { ouvintes.delete(definirDados) }
  }, [])

  useEffect(() => {
    if (!dono) return
    if (versao === 0 && guardado && guardado.dono === dono && Date.now() - guardado.quando < VALIDADE_MS) {
      definirDados(guardado.dados)
      return
    }
    definirErro(null)
    buscando ??= api.get<MeusDados>('/usuarios/eu').finally(() => { buscando = null })
    buscando
      .then((novos) => guardarMeusDados(dono, novos))
      .catch(definirErro)
  }, [dono, versao])

  return { dados, erro, recarregar: () => definirVersao((v) => v + 1) }
}
