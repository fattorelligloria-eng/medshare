import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import { Sino } from './Icones'

/** Confere de minuto em minuto: aviso de oferta tem prazo de 24 h (UC06). */
const INTERVALO_MS = 60_000

/**
 * A contagem do que não foi lido.
 *
 * Virou hook porque agora dois lugares precisam dela: o sino do topo e o item
 * de Notificações no menu lateral.
 */
export function useNaoLidas(): number {
  const [naoLidas, definirNaoLidas] = useState(0)

  useEffect(() => {
    let ativo = true
    const atualizar = () =>
      api.get<{ quantidade: number }>('/notificacoes/nao-lidas')
        .then((r) => { if (ativo) definirNaoLidas(r.quantidade) })
        .catch(() => { /* o sino não pode derrubar a tela */ })
    void atualizar()
    const relogio = setInterval(atualizar, INTERVALO_MS)
    return () => { ativo = false; clearInterval(relogio) }
  }, [])

  return naoLidas
}

/**
 * O sino do topo, com a contagem do que ainda não foi lido.
 *
 * Enquanto o push (Firebase) não está configurado, é por aqui que a pessoa fica
 * sabendo de oferta, lembrete de entrega e reserva expirando.
 */
export function BotaoDeNotificacoes({ para = '/app/notificacoes' }: { para?: string }) {
  const naoLidas = useNaoLidas()

  return (
    <Link
      to={para}
      className="icone-botao"
      aria-label={naoLidas > 0 ? `Notificações: ${naoLidas} não lidas` : 'Notificações'}
    >
      <Sino cor="var(--tinta-media)" />
      {naoLidas > 0 && <span className="contador">{naoLidas > 9 ? '9+' : naoLidas}</span>}
    </Link>
  )
}
