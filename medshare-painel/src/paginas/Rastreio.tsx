import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/cliente'
import type { DoacaoDetalhada } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { Status, explicar } from '../componentes/Status'
import { data, dataComHora } from '../formatos'

const NOME_DO_EVENTO: Record<string, string> = {
  CADASTRO: 'Cadastro',
  PRE_VALIDACAO: 'Pré-validação',
  ENVIO_PARA_CENTRAL: 'Enviada para a central',
  DECISAO_DA_CENTRAL: 'Decisão da central',
  AGENDAMENTO: 'Agendamento',
  RECEBIMENTO: 'Recebimento',
  VALIDACAO: 'Conferência do farmacêutico',
  DISPONIBILIZACAO: 'Disponibilizada',
  RESERVA: 'Reservada',
  EXPIRACAO_DE_RESERVA: 'Reserva expirada',
  CANCELAMENTO_DE_RESERVA: 'Reserva cancelada',
  ENTREGA: 'Entrega',
  RECUSA: 'Recusa',
  CANCELAMENTO: 'Cancelamento',
  REJEICAO: 'Rejeição',
  DESCARTE: 'Descarte',
}

/**
 * RN06 — o percurso completo de uma unidade doada.
 *
 * Nenhum evento pode ser alterado nem apagado, nem por quem tem acesso direto
 * ao banco: um gatilho no PostgreSQL bloqueia UPDATE e DELETE nesta tabela.
 * Esta tela é a leitura desse registro.
 */
export function Rastreio() {
  const [codigo, definirCodigo] = useState('')
  const [resultado, definirResultado] = useState<DoacaoDetalhada | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [buscando, definirBuscando] = useState(false)

  async function buscar(evento: FormEvent) {
    evento.preventDefault()
    definirErro(null)
    definirBuscando(true)
    try {
      definirResultado(await api.get<DoacaoDetalhada>(`/doacoes/${codigo.trim().toUpperCase()}`))
    } catch (e) {
      definirResultado(null)
      definirErro(e)
    } finally {
      definirBuscando(false)
    }
  }

  return (
    <>
      <div className="titulo-area">
        <h1>Rastreio</h1>
        <p>Histórico completo e imutável de uma unidade doada.</p>
      </div>

      <form className="bloco" onSubmit={buscar} style={{ maxWidth: 480 }}>
        <label className="campo">
          <span>Código da doação</span>
          <input
            type="text"
            value={codigo}
            onChange={(e) => definirCodigo(e.target.value.toUpperCase())}
            placeholder="Ex.: MS-QQQNE9"
            style={{ fontFamily: 'ui-monospace, Menlo, monospace', letterSpacing: '0.06em' }}
            required
          />
        </label>
        <div className="acoes">
          <button className="principal" disabled={buscando}>
            {buscando ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
      </form>

      <AvisoDeErro erro={erro} />

      {resultado && (
        <article className="bloco">
          <div className="bloco-topo">
            <div>
              <h2>{resultado.doacao.medicamento}</h2>
              <p className="codigo">{resultado.doacao.codigo}</p>
            </div>
            <Status status={resultado.doacao.status} />
          </div>

          <p style={{ margin: '0 0 20px', color: 'var(--tinta-media)', fontSize: 14.5 }}>
            {explicar(resultado.doacao.status)}
          </p>

          <div className="grade" style={{ marginBottom: 28 }}>
            <div className="dado">
              <p className="rotulo">Princípio ativo</p>
              <p className="valor">{resultado.doacao.principioAtivo}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Lote</p>
              <p className="valor">{resultado.doacao.lote}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Validade</p>
              <p className="valor">{data(resultado.doacao.validade)}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Ponto de coleta</p>
              <p className="valor">{resultado.doacao.pontoDeColeta ?? '—'}</p>
            </div>
          </div>

          <p className="rotulo" style={{ marginBottom: 16 }}>Por onde passou</p>

          <ul className="tempo">
            {resultado.historico.map((evento, i) => (
              <li key={`${evento.quando}-${i}`}>
                <p className="quando">{dataComHora(evento.quando)}</p>
                <p className="titulo">{NOME_DO_EVENTO[evento.tipo] ?? evento.tipo}</p>
                <p className="descricao">{evento.descricao}</p>
              </li>
            ))}
          </ul>
        </article>
      )}
    </>
  )
}
