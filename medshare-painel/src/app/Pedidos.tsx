import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Necessidade, Reserva } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { Mais, Sino } from '../componentes/Icones'
import { data, primeiroNome } from '../formatos'

/** O que a pessoa pediu, e o que falta para conseguir retirar. */
export function Pedidos() {
  const { sessao } = useAutenticacao()
  const navegar = useNavigate()
  const [pedidos, definirPedidos] = useState<Necessidade[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [carregando, definirCarregando] = useState(true)
  const [reservando, definirReservando] = useState<number | null>(null)

  const carregar = useCallback(async () => {
    definirCarregando(true)
    try {
      definirPedidos(await api.get<Necessidade[]>('/necessidades'))
      definirErro(null)
    } catch (e) {
      definirErro(e)
    } finally {
      definirCarregando(false)
    }
  }, [])

  useEffect(() => { void carregar() }, [carregar])

  async function procurar(pedido: Necessidade) {
    definirErro(null)
    definirReservando(pedido.id)
    try {
      const reserva = await api.post<Reserva>('/reservas', { necessidadeId: pedido.id })
      navegar(`/app/reservas?novo=${reserva.codigoRetirada}`)
    } catch (e) {
      definirErro(e)
    } finally {
      definirReservando(null)
    }
  }

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
          <button type="button" className="icone-botao" aria-label="Notificações">
            <Sino cor="var(--tinta-media)" />
          </button>
        </div>
      </div>

      <div className="conteudo-app" style={{ paddingTop: 28 }}>
        <p className="humano" style={{ fontSize: 26, margin: '0 0 10px' }}>
          Oi, {primeiroNome(sessao?.nome ?? '')}
        </p>
        <p style={{ margin: 0, fontSize: 15, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
          Assim que uma caixa do que você precisa aparecer na rede, a gente avisa.
        </p>
      </div>

      <div className="conteudo-app" style={{ paddingTop: 26, flex: 1 }}>
        <AvisoDeErro erro={erro} />

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

            {p.temReceitaValida ? (
              <>
                <p className="explicacao">
                  Receita válida até {data(p.validadeDaReceita)}. Você está na fila.
                </p>
                <button
                  className="secundario"
                  style={{ marginTop: 12 }}
                  disabled={reservando === p.id}
                  onClick={() => procurar(p)}
                >
                  {reservando === p.id ? 'Procurando…' : 'Ver se já tem disponível'}
                </button>
              </>
            ) : (
              <>
                <p className="explicacao">
                  Falta enviar a receita médica. Sem ela não é possível reservar.
                </p>
                <Link
                  to={`/app/pedidos/${p.id}/receita`}
                  className="botao secundario"
                  style={{ marginTop: 12 }}
                >
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
