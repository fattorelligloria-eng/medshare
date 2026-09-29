import { useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { Notificacao, Pagina } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { dataComHora } from '../formatos'

/**
 * Os avisos da pessoa: oferta recebida, lembrete de entrega, reserva, CadÚnico.
 * Abrir a tela marca como lido o que estava novo — o ponto verde continua
 * nesta visita, para a pessoa ver o que chegou.
 */
export function Notificacoes({ noPainel = false }: { noPainel?: boolean }) {
  const [itens, definirItens] = useState<Notificacao[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [carregando, definirCarregando] = useState(true)

  useEffect(() => {
    api.get<Pagina<Notificacao>>('/notificacoes?size=50')
      .then((pagina) => {
        definirItens(pagina.content)
        pagina.content
          .filter((n) => !n.lida)
          .forEach((n) => { void api.post(`/notificacoes/${n.id}/leitura`).catch(() => undefined) })
      })
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  const lista = (
    <>
      <AvisoDeErro erro={erro} />
      {carregando && <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Carregando…</p>}
      {!carregando && itens.length === 0 && (
        <div className="vazio">
          <h3>Nenhuma notificação</h3>
          <p>Ofertas, lembretes e respostas da equipe aparecem aqui.</p>
        </div>
      )}
      {itens.map((n) => (
        <div key={n.id} className={`notificacao ${n.lida ? '' : 'nova'}`}>
          <p className="titulo">{n.titulo}</p>
          <p className="corpo">{n.corpo}</p>
          <p className="quando">{dataComHora(n.quando)}</p>
        </div>
      ))}
    </>
  )

  if (noPainel) {
    return (
      <>
        <div className="titulo-area">
          <h1>Notificações</h1>
          <p>Alertas da operação, como casos parados há mais de 48 horas na central.</p>
        </div>
        <div className="bloco">{lista}</div>
      </>
    )
  }

  return (
    <>
      <CabecalhoInterno titulo="Notificações" />
      <div className="conteudo-app" style={{ paddingTop: 12, flex: 1 }}>{lista}</div>
    </>
  )
}
