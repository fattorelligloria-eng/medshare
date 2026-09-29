import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/cliente'
import type { Reserva } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'

/**
 * A retirada no balcão — RN03.
 *
 * O botão só libera com as duas conferências marcadas. Não é enfeite de
 * interface: o servidor recusa a entrega sem elas, e a tela deixa isso claro
 * antes de a pessoa tentar.
 */
export function Retirada() {
  const [codigo, definirCodigo] = useState('')
  const [receita, definirReceita] = useState(false)
  const [documento, definirDocumento] = useState(false)
  const [erro, definirErro] = useState<unknown>(null)
  const [concluida, definirConcluida] = useState<Reserva | null>(null)
  const [enviando, definirEnviando] = useState(false)

  async function registrar(evento: FormEvent) {
    evento.preventDefault()
    definirErro(null)
    definirConcluida(null)
    definirEnviando(true)
    try {
      const reserva = await api.post<Reserva>(
        `/reservas/${codigo.trim().toUpperCase()}/retirada`,
        { receitaConferida: receita, documentoConferido: documento },
      )
      definirConcluida(reserva)
      definirCodigo('')
      definirReceita(false)
      definirDocumento(false)
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

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

      <form className="bloco" onSubmit={registrar} style={{ maxWidth: 520 }}>
        <label className="campo">
          <span>Código de retirada</span>
          <input
            type="text"
            value={codigo}
            onChange={(e) => definirCodigo(e.target.value.toUpperCase())}
            placeholder="Ex.: MASV846J"
            style={{ fontFamily: 'ui-monospace, Menlo, monospace', letterSpacing: '0.1em', fontSize: 17 }}
            required
          />
        </label>

        <p className="rotulo" style={{ margin: '20px 0 12px' }}>Conferência obrigatória</p>

        <label className="marcar">
          <input type="checkbox" checked={receita} onChange={(e) => definirReceita(e.target.checked)} />
          <span>Receita válida, dentro do prazo e para o mesmo princípio ativo</span>
        </label>

        <label className="marcar">
          <input type="checkbox" checked={documento} onChange={(e) => definirDocumento(e.target.checked)} />
          <span>Documento com foto confere com o titular da reserva</span>
        </label>

        <div className="acoes">
          <button className="principal" disabled={!codigo.trim() || !receita || !documento || enviando}>
            {enviando ? 'Registrando…' : 'Registrar entrega'}
          </button>
        </div>

        {!(receita && documento) && (
          <p style={{ marginTop: 12, fontSize: 13.5, color: 'var(--tinta-fraca)' }}>
            A entrega só pode ser registrada com as duas conferências feitas.
          </p>
        )}
      </form>
    </>
  )
}
