import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Doacao, Pagina } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Status, explicar } from '../componentes/Status'
import { Marca } from '../componentes/Logo'
import { Confere, Lugar, Mais, Sino } from '../componentes/Icones'
import { data, primeiroNome } from '../formatos'

/**
 * A tela inicial de quem doa.
 *
 * O número grande no topo existe por um motivo: a pessoa doou e não vê mais a
 * caixa. Sem uma prova de que aquilo virou alguma coisa, doar parece jogar no
 * vazio. O número e a faixa verde abaixo dele são essa prova.
 */
export function Doacoes() {
  const { sessao } = useAutenticacao()
  const [doacoes, definirDoacoes] = useState<Doacao[]>([])
  const [carregando, definirCarregando] = useState(true)
  const [erro, definirErro] = useState<unknown>(null)

  useEffect(() => {
    api.get<Pagina<Doacao>>('/doacoes/minhas?size=50')
      .then((p) => definirDoacoes(p.content))
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  const entregues = doacoes.filter((d) => d.status === 'ENTREGUE')
  const emAndamento = doacoes.filter(
    (d) => !['ENTREGUE', 'RECUSADA', 'CANCELADA', 'REJEITADA', 'DESCARTADA'].includes(d.status),
  )
  const ultimaEntregue = entregues[0]

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
        <p className="humano" style={{ fontSize: 26, margin: '0 0 12px' }}>
          Oi, {primeiroNome(sessao?.nome ?? '')}
        </p>

        {doacoes.length > 0 ? (
          <>
            <div className="impacto">
              <span className="numero">{doacoes.length}</span>
              <p>
                {doacoes.length === 1 ? 'caixa sua já' : 'caixas suas já'}
                <br />
                {doacoes.length === 1 ? 'saiu de casa' : 'saíram de casa'}
              </p>
            </div>

            {entregues.length > 0 && (
              <p style={{ margin: '14px 0 0', fontSize: 14, color: 'var(--tinta-media)' }}>
                {entregues.length === 1
                  ? 'Uma delas já chegou a alguém.'
                  : `${entregues.length} delas já chegaram a alguém.`}
              </p>
            )}
          </>
        ) : (
          <p style={{ margin: 0, fontSize: 15.5, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
            Sobrou caixa lacrada de um tratamento? Ela pode ser o tratamento de
            outra pessoa.
          </p>
        )}
      </div>

      {ultimaEntregue && (
        <div className="conteudo-app" style={{ paddingTop: 20 }}>
          <div className="faixa-boa">
            <Confere cor="var(--verde-escuro)" />
            <p>
              <strong>{ultimaEntregue.medicamento} entregue.</strong>{' '}
              Alguém começou o tratamento.
            </p>
          </div>
        </div>
      )}

      <div className="conteudo-app" style={{ paddingTop: 26, flex: 1 }}>
        <AvisoDeErro erro={erro} />

        {carregando && <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Carregando…</p>}

        {!carregando && doacoes.length === 0 && (
          <div className="vazio">
            <h3>Nenhuma doação ainda</h3>
            <p>Toque em doar, fotografe a caixa e escolha uma farmácia perto de você.</p>
          </div>
        )}

        {emAndamento.length > 0 && (
          <>
            <p className="rotulo" style={{ marginBottom: 4 }}>Em andamento</p>
            {emAndamento.map((d) => (
              <Link
                key={d.codigo}
                to={`/app/doacoes/${d.codigo}`}
                className="item"
                style={{ display: 'block', textDecoration: 'none', color: 'inherit' }}
              >
                <Status status={d.status} />
                <h3 className="humano">{d.medicamento}</h3>
                <p className="detalhe">{d.principioAtivo} · lote {d.lote}</p>
                <p className="explicacao">{explicar(d.status)}</p>
                {d.pontoDeColeta && (
                  <div className="linha-icone">
                    <Lugar cor="var(--verde)" />
                    <span>{d.pontoDeColeta}</span>
                  </div>
                )}
              </Link>
            ))}
          </>
        )}

        {entregues.length > 0 && (
          <>
            <p className="rotulo" style={{ margin: '24px 0 4px' }}>Concluídas</p>
            {entregues.map((d) => (
              <Link
                key={d.codigo}
                to={`/app/doacoes/${d.codigo}`}
                className="item"
                style={{ display: 'block', textDecoration: 'none', color: 'inherit' }}
              >
                <Status status={d.status} />
                <h3 className="humano">{d.medicamento}</h3>
                <p className="detalhe">Validade {data(d.validade)}</p>
              </Link>
            ))}
          </>
        )}
      </div>

      <div className="rodape-acao">
        <Link to="/app/doacoes/nova" className="botao principal">
          <Mais cor="#fff" /> Doar um medicamento
        </Link>
      </div>
    </>
  )
}
