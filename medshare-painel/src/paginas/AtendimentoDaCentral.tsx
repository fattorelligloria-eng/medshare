import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { MensagemNaCentral, Pagina } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { NOME_DO_ASSUNTO } from '../app/Atendimento'
import { dataComHora } from '../formatos'

/**
 * A mesa de atendimento da central: as mensagens que chegaram pelo aplicativo.
 *
 * Da mais antiga para a mais nova, como a fila de análise. Respondida, a
 * mensagem sai da lista e a pessoa recebe uma notificação dizendo que há
 * resposta — o texto ela lê dentro do aplicativo, não na tela bloqueada.
 */
export function AtendimentoDaCentral() {
  const [mensagens, definirMensagens] = useState<MensagemNaCentral[]>([])
  const [respostas, definirRespostas] = useState<Record<number, string>>({})
  const [carregando, definirCarregando] = useState(true)
  const [enviando, definirEnviando] = useState<number | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)

  const carregar = useCallback(() => {
    definirCarregando(true)
    definirErro(null)
    api.get<Pagina<MensagemNaCentral>>('/central/atendimento?size=50')
      .then((p) => definirMensagens(p.content))
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  async function responder(mensagem: MensagemNaCentral) {
    const texto = (respostas[mensagem.id] ?? '').trim()
    if (!texto) return
    definirErro(null)
    definirRecado(null)
    definirEnviando(mensagem.id)
    try {
      await api.post(`/central/atendimento/${mensagem.id}/resposta`, { texto })
      definirMensagens((atuais) => atuais.filter((m) => m.id !== mensagem.id))
      definirRecado(`Resposta enviada para ${mensagem.nome}.`)
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(null)
    }
  }

  return (
    <>
      <div className="titulo-area">
        <h1>Atendimento</h1>
        <p>
          Mensagens de quem usa o aplicativo, da mais antiga para a mais nova. A pessoa
          é avisada por notificação e lê a resposta dentro do aplicativo.
        </p>
      </div>

      <AvisoDeErro erro={erro} aoTentarDeNovo={carregar} />
      {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

      {carregando && <p style={{ color: 'var(--tinta-fraca)' }}>Carregando…</p>}

      {!carregando && mensagens.length === 0 && (
        <div className="bloco">
          <div className="vazio">
            <h3>Nenhuma mensagem esperando</h3>
            <p>Quando alguém escrever pelo aplicativo, a mensagem aparece aqui.</p>
          </div>
        </div>
      )}

      {mensagens.map((m) => (
        <article className="bloco" key={m.id}>
          <div className="bloco-topo">
            <div>
              <h2>{m.nome}</h2>
              <p className="meta-da-mensagem">{NOME_DO_ASSUNTO[m.assunto]} · {dataComHora(m.criadoEm)}</p>
            </div>
          </div>
          <p className="texto-da-mensagem">{m.texto}</p>
          <label className="campo" style={{ marginTop: 16 }}>
            <span>Resposta — a pessoa lê no aplicativo</span>
            <textarea
              rows={3}
              maxLength={2000}
              value={respostas[m.id] ?? ''}
              onChange={(e) => definirRespostas({ ...respostas, [m.id]: e.target.value })}
            />
          </label>
          <div className="acoes">
            <button
              className="principal"
              disabled={enviando === m.id || !(respostas[m.id] ?? '').trim()}
              onClick={() => responder(m)}
            >
              {enviando === m.id ? 'Enviando…' : 'Responder'}
            </button>
          </div>
        </article>
      ))}
    </>
  )
}
