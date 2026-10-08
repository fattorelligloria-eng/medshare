import { useState } from 'react'
import type { FormEvent } from 'react'
import { api, ErroDaApi } from '../api/cliente'
import type { ConferenciaDaRetirada, Reserva } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { rotuloDaRegra } from '../componentes/Regras'
import { FotoDoCaso } from '../componentes/FotoDoCaso'
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
  /** '' = o próprio titular; senão, o CPF do procurador (UC07 A3). */
  const [quemRetira, definirQuemRetira] = useState('')
  const [negando, definirNegando] = useState(false)
  const [motivoDaNegativa, definirMotivoDaNegativa] = useState('')
  const [negada, definirNegada] = useState<string | null>(null)

  function recomecar() {
    definirConferencia(null)
    definirReceita(false)
    definirDocumento(false)
    definirQuemRetira('')
    definirNegando(false)
    definirMotivoDaNegativa('')
  }

  /** UC07 A1 — a receita não bate com o princípio ativo: entrega negada, caixa volta ao estoque. */
  async function negar() {
    if (!conferencia) return
    definirErro(null)
    definirEnviando(true)
    try {
      await api.post(`/reservas/${encodeURIComponent(conferencia.codigoRetirada)}/negativa`, { motivo: motivoDaNegativa.trim() })
      definirNegada(conferencia.codigoRetirada)
      definirCodigo('')
      recomecar()
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  async function buscar(evento: FormEvent) {
    evento.preventDefault()
    definirErro(null)
    definirConcluida(null)
    definirNegada(null)
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
        { receitaConferida: receita, documentoConferido: documento, cpfDeQuemRetira: quemRetira },
      )
      definirConcluida(reserva)
      definirCodigo('')
      recomecar()
    } catch (e) {
      definirErro(e)
      // RN02 no balcão: a caixa foi descartada e a reserva caiu; não há mais o que entregar.
      if (e instanceof ErroDaApi && e.regra === 'RN02') recomecar()
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
      {negada && (
        <AvisoDeSucesso>
          Entrega da reserva {negada} negada. A caixa voltou ao estoque e o beneficiário foi avisado
          para enviar uma receita atualizada.
        </AvisoDeSucesso>
      )}

      <form className="bloco form-busca" onSubmit={buscar}>
        <label className="campo">
          <span>Código de retirada</span>
          <input
            type="text"
            value={codigo}
            onChange={(e) => { definirCodigo(e.target.value.toUpperCase()); recomecar() }}
            placeholder="Ex.: MASV846J"
            style={{ fontFamily: 'ui-monospace, Menlo, monospace', letterSpacing: '0.1em', fontSize: '1.0625rem' }}
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
              <p className="rotulo">Apresentação</p>
              <p className="valor">{conferencia.apresentacao}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Validade da caixa</p>
              <p className="valor">{data(conferencia.validadeDaCaixa)}</p>
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
            <div style={{ marginTop: 20 }}>
              <FotoDoCaso url={conferencia.receitaFotoUrl} descricao="Receita anexada pelo beneficiário" />
            </div>
          )}

          {conferencia.status !== 'ATIVA' && (
            <div className="aviso erro" style={{ marginTop: 20 }}>
              Esta reserva está {conferencia.status.toLowerCase()} e não pode ser entregue.
            </div>
          )}
          {conferencia.status === 'ATIVA' && !conferencia.receitaValida && (
            <div className="aviso erro" style={{ marginTop: 20 }}>
              <span className="regra">{rotuloDaRegra('RN03')}</span>
              A receita anexada está vencida. O beneficiário precisa enviar uma nova pelo app.
            </div>
          )}

          <label className="campo" style={{ marginTop: 20 }}>
            <span>Quem está retirando?</span>
            <select value={quemRetira} disabled={!podeEntregar} onChange={(e) => definirQuemRetira(e.target.value)}>
              <option value="">{conferencia.titular} (titular)</option>
              {conferencia.procuradores.map((p) => (
                <option key={p.cpf} value={p.cpf}>{p.nome} — procurador(a), CPF final {p.cpf.slice(-4)}</option>
              ))}
            </select>
            {conferencia.procuradores.length === 0 && (
              <span className="apoio">Nenhum procurador cadastrado: só o titular pode retirar.</span>
            )}
          </label>

          <p className="rotulo" style={{ margin: '8px 0 12px' }}>Conferência obrigatória</p>

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
            <span>
              Documento com foto é de{' '}
              {quemRetira
                ? conferencia.procuradores.find((p) => p.cpf === quemRetira)?.nome
                : conferencia.titular}
            </span>
          </label>

          <div className="acoes">
            <button
              className="principal"
              disabled={!podeEntregar || !receita || !documento || enviando}
              onClick={registrar}
            >
              {enviando ? 'Registrando…' : 'Registrar entrega'}
            </button>
            {conferencia.status === 'ATIVA' && !negando && (
              <button className="perigo" disabled={enviando} onClick={() => definirNegando(true)}>
                Negar entrega
              </button>
            )}
          </div>

          {negando && (
            <div style={{ marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--linha)' }}>
              <label className="campo">
                <span>Por que a entrega foi negada?</span>
                <textarea
                  rows={2}
                  maxLength={280}
                  value={motivoDaNegativa}
                  onChange={(e) => definirMotivoDaNegativa(e.target.value)}
                  placeholder="Ex.: receita é de outro princípio ativo; receita em papel vencida."
                />
              </label>
              <div className="acoes">
                <button className="perigo" disabled={!motivoDaNegativa.trim() || enviando} onClick={negar}>
                  Confirmar: negar entrega
                </button>
                <button className="secundario" onClick={() => { definirNegando(false); definirMotivoDaNegativa('') }}>
                  Voltar
                </button>
              </div>
            </div>
          )}
        </article>
      )}
    </>
  )
}
