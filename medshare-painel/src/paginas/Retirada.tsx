import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/cliente'
import type { ConferenciaDaRetirada, Reserva } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { data, dataComHora } from '../formatos'

/**
 * A retirada no balcão — RN03.
 *
 * Em dois passos: primeiro o código traz o titular e a receita anexada, para
 * o farmacêutico ter com o que comparar o documento e a receita em papel.
 * Só depois as duas conferências liberam o registro da entrega — o servidor
 * recusa a entrega sem elas.
 */
export function Retirada() {
  const [codigo, definirCodigo] = useState('')
  const [conferencia, definirConferencia] = useState<ConferenciaDaRetirada | null>(null)
  const [receita, definirReceita] = useState(false)
  const [documento, definirDocumento] = useState(false)
  const [erro, definirErro] = useState<unknown>(null)
  const [concluida, definirConcluida] = useState<Reserva | null>(null)
  const [enviando, definirEnviando] = useState(false)

  function recomecar() {
    definirConferencia(null)
    definirReceita(false)
    definirDocumento(false)
  }

  async function buscar(evento: FormEvent) {
    evento.preventDefault()
    definirErro(null)
    definirConcluida(null)
    recomecar()
    definirEnviando(true)
    try {
      definirConferencia(await api.get<ConferenciaDaRetirada>(
        `/reservas/${encodeURIComponent(codigo.trim().toUpperCase())}/conferencia`,
      ))
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  async function registrar() {
    if (!conferencia) return
    definirErro(null)
    definirEnviando(true)
    try {
      const reserva = await api.post<Reserva>(
        `/reservas/${encodeURIComponent(conferencia.codigoRetirada)}/retirada`,
        { receitaConferida: receita, documentoConferido: documento },
      )
      definirConcluida(reserva)
      definirCodigo('')
      recomecar()
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  const podeEntregar = conferencia?.status === 'ATIVA' && conferencia.receitaValida

  return (
    <>
      <div className="titulo-area">
        <h1>Retirada</h1>
        <p>O beneficiário apresenta o código, a receita e um documento com foto.</p>
      </div>

      <AvisoDeErro erro={erro} />
      {concluida && (
        <AvisoDeSucesso>
          Entrega de <strong>{concluida.medicamento}</strong> registrada. A reserva{' '}
          {concluida.codigoRetirada} foi concluída.
        </AvisoDeSucesso>
      )}

      <form className="bloco" onSubmit={buscar} style={{ maxWidth: 520 }}>
        <label className="campo">
          <span>Código de retirada</span>
          <input
            type="text"
            value={codigo}
            onChange={(e) => { definirCodigo(e.target.value.toUpperCase()); recomecar() }}
            placeholder="Ex.: MASV846J"
            style={{ fontFamily: 'ui-monospace, Menlo, monospace', letterSpacing: '0.1em', fontSize: 17 }}
            required
          />
        </label>
        <div className="acoes">
          <button className="principal" disabled={!codigo.trim() || enviando}>
            {enviando && !conferencia ? 'Buscando…' : 'Buscar reserva'}
          </button>
        </div>
      </form>

      {conferencia && (
        <article className="bloco" style={{ maxWidth: 720 }}>
          <div className="bloco-topo">
            <div>
              <h2>{conferencia.medicamento}</h2>
              <p className="codigo">{conferencia.codigoRetirada}</p>
            </div>
          </div>

          <div className="grade">
            <div className="dado">
              <p className="rotulo">Titular</p>
              <p className="valor">{conferencia.titular}</p>
            </div>
            <div className="dado">
              <p className="rotulo">CPF</p>
              <p className="valor">{conferencia.cpfDoTitular}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Princípio ativo</p>
              <p className="valor">{conferencia.principioAtivo}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Retirar até</p>
              <p className="valor">{dataComHora(conferencia.expiraEm)}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Médico</p>
              <p className="valor">{conferencia.receitaCrm ?? '—'}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Receita válida até</p>
              <p className={conferencia.receitaValida ? 'valor' : 'valor divergente'}>
                {data(conferencia.receitaValidade)}
              </p>
            </div>
          </div>

          {conferencia.receitaFotoUrl && (
            <a href={conferencia.receitaFotoUrl} target="_blank" rel="noreferrer">
              <img
                src={conferencia.receitaFotoUrl}
                alt="Receita anexada pelo beneficiário"
                style={{ display: 'block', width: '100%', maxHeight: 360, objectFit: 'contain', marginTop: 20, borderRadius: 'var(--raio)' }}
              />
            </a>
          )}

          {conferencia.status !== 'ATIVA' && (
            <div className="aviso erro" style={{ marginTop: 20 }}>
              Esta reserva está {conferencia.status.toLowerCase()} e não pode ser entregue.
            </div>
          )}
          {conferencia.status === 'ATIVA' && !conferencia.receitaValida && (
            <div className="aviso erro" style={{ marginTop: 20 }}>
              <span className="regra">RN03</span>
              A receita anexada está vencida. O beneficiário precisa enviar uma nova pelo app.
            </div>
          )}

          <p className="rotulo" style={{ margin: '20px 0 12px' }}>Conferência obrigatória</p>

          <label className="marcar">
            <input
              type="checkbox"
              checked={receita}
              disabled={!podeEntregar}
              onChange={(e) => definirReceita(e.target.checked)}
            />
            <span>Receita em papel confere com a anexada, está no prazo e é do mesmo princípio ativo</span>
          </label>

          <label className="marcar">
            <input
              type="checkbox"
              checked={documento}
              disabled={!podeEntregar}
              onChange={(e) => definirDocumento(e.target.checked)}
            />
            <span>Documento com foto é de {conferencia.titular}</span>
          </label>

          <div className="acoes">
            <button
              className="principal"
              disabled={!podeEntregar || !receita || !documento || enviando}
              onClick={registrar}
            >
              {enviando ? 'Registrando…' : 'Registrar entrega'}
            </button>
          </div>
        </article>
      )}
    </>
  )
}
