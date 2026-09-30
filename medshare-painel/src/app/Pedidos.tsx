import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Necessidade, Oferta, Reserva } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { BotaoDeNotificacoes } from '../componentes/BotaoDeNotificacoes'
import { Lugar, Mais } from '../componentes/Icones'
import { data, dataComHora, primeiroNome } from '../formatos'

/**
 * O que a pessoa pediu, as ofertas que chegaram e o que falta para retirar.
 *
 * UC05/UC06 — ninguém precisa ficar procurando: quando uma caixa aparece, o
 * sistema oferece a UMA pessoa da fila, com 24 h para aceitar. A oferta
 * aparece no topo desta tela e no sino.
 */
export function Pedidos() {
  const { sessao } = useAutenticacao()
  const navegar = useNavigate()
  const [pedidos, definirPedidos] = useState<Necessidade[]>([])
  const [ofertas, definirOfertas] = useState<Oferta[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [carregando, definirCarregando] = useState(true)
  const [respondendo, definirRespondendo] = useState<number | null>(null)

  const carregar = useCallback(async () => {
    definirCarregando(true)
    try {
      const [listaDePedidos, listaDeOfertas] = await Promise.all([
        api.get<Necessidade[]>('/necessidades'),
        api.get<Oferta[]>('/ofertas/minhas'),
      ])
      definirPedidos(listaDePedidos)
      definirOfertas(listaDeOfertas.filter((o) => o.status === 'PENDENTE'))
      definirErro(null)
    } catch (e) {
      definirErro(e)
    } finally {
      definirCarregando(false)
    }
  }, [])

  useEffect(() => { void carregar() }, [carregar])

  async function aceitar(oferta: Oferta) {
    definirErro(null)
    definirRespondendo(oferta.id)
    try {
      const reserva = await api.post<Reserva>(`/ofertas/${oferta.id}/aceite`)
      navegar(`/app/reservas?novo=${reserva.codigoRetirada}`)
    } catch (e) {
      definirErro(e)
      await carregar()
    } finally {
      definirRespondendo(null)
    }
  }

  async function recusar(oferta: Oferta) {
    if (!window.confirm('Recusar esta caixa? Ela vai para a próxima pessoa da fila e você continua esperando outra.')) return
    definirErro(null)
    definirRespondendo(oferta.id)
    try {
      await api.post(`/ofertas/${oferta.id}/recusa`)
      await carregar()
    } catch (e) {
      definirErro(e)
    } finally {
      definirRespondendo(null)
    }
  }

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
          <BotaoDeNotificacoes />
        </div>
      </div>

      <div className="conteudo-app" style={{ paddingTop: 28 }}>
        <p className="humano" style={{ fontSize: 26, margin: '0 0 10px' }}>
          Oi, {primeiroNome(sessao?.nome ?? '')}
        </p>
        <p style={{ margin: 0, fontSize: 15, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
          Quando uma caixa do que você precisa aparecer, ela é oferecida a você aqui e no sino.
        </p>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 26 }}>
        <AvisoDeErro erro={erro} />

        {ofertas.map((o) => (
          <div key={o.id} className="faixa-boa" style={{ display: 'block', marginBottom: 18, padding: 16 }}>
            <p className="rotulo" style={{ margin: 0 }}>Chegou para você</p>
            <h3 className="humano" style={{ margin: '6px 0 2px', fontSize: 20 }}>{o.medicamento}</h3>
            <p className="detalhe" style={{ margin: 0 }}>{o.apresentacao} · validade {data(o.validade)}</p>
            <div className="linha-icone">
              <Lugar cor="var(--verde)" />
              <span>{o.pontoDeColeta} — {o.enderecoDoPonto}</span>
            </div>
            <p className="detalhe" style={{ margin: '4px 0 0' }}>{o.horarioDoPonto}</p>
            <p style={{ margin: '10px 0 12px', fontSize: 13.5, fontWeight: 600 }}>
              Aceite até {dataComHora(o.expiraEm)}. Depois disso a caixa vai para a próxima pessoa.
            </p>
            <div style={{ display: 'flex', gap: 10 }}>
              <button className="principal" disabled={respondendo === o.id} onClick={() => aceitar(o)}>
                {respondendo === o.id ? 'Reservando…' : 'Aceitar e reservar'}
              </button>
              <button className="secundario" disabled={respondendo === o.id} onClick={() => recusar(o)}>
                Recusar
              </button>
            </div>
          </div>
        ))}

        {carregando && <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Carregando…</p>}

        {!carregando && pedidos.length === 0 && (
          <div className="vazio">
            <h3>Nenhum pedido ainda</h3>
            <p>Diga de qual medicamento você precisa e entre na fila.</p>
          </div>
        )}

        {pedidos.map((p) => (
          <div key={p.id} className="item">
            <h3 className="humano">{p.medicamento}</h3>
            <p className="detalhe">{p.principioAtivo}</p>

            {p.emRevisao ? (
              <>
                <p className="explicacao" style={{ color: 'var(--terracota-texto)' }}>
                  A farmácia não pôde entregar: {p.motivoRevisao}. Envie uma receita atualizada para voltar à fila.
                </p>
                <Link to={`/app/pedidos/${p.id}/receita`} className="botao secundario" style={{ marginTop: 12 }}>
                  Enviar nova receita
                </Link>
              </>
            ) : p.temReceitaValida ? (
              <p className="explicacao">
                Receita válida até {data(p.validadeDaReceita)}.{' '}
                {p.prioridade ? 'Você está na frente da fila.' : 'Você está na fila; avisamos quando chegar.'}
              </p>
            ) : (
              <>
                <p className="explicacao">
                  {p.validadeDaReceita
                    ? `A receita venceu em ${data(p.validadeDaReceita)}. Envie uma nova para continuar na fila.`
                    : 'Falta enviar a receita médica. Sem ela não é possível receber ofertas.'}
                </p>
                <Link to={`/app/pedidos/${p.id}/receita`} className="botao secundario" style={{ marginTop: 12 }}>
                  Enviar receita
                </Link>
              </>
            )}
          </div>
        ))}
      </div>

      <div className="rodape-acao">
        <Link to="/app/pedidos/novo" className="botao principal">
          <Mais cor="#fff" /> Pedir um medicamento
        </Link>
      </div>
    </>
  )
}
