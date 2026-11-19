import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { AssuntoDaMensagem, MensagemDeAtendimento } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { BotaoDeNotificacoes } from '../componentes/BotaoDeNotificacoes'
import { SecaoRecolhivel } from '../componentes/SecaoRecolhivel'
import { dataComHora } from '../formatos'
import { PERGUNTAS } from './Ajuda'

export const NOME_DO_ASSUNTO: Record<AssuntoDaMensagem, string> = {
  DUVIDA: 'Dúvida',
  PROBLEMA_COM_CAIXA: 'Problema com uma caixa',
  SUGESTAO_OU_RECLAMACAO: 'Sugestão ou reclamação',
  OUTRO: 'Outro assunto',
}

/**
 * Atendimento: onde a pessoa pede ajuda.
 *
 * "Fale com a central" vem primeiro e é o único bloco com cor: é a saída para
 * quem não achou a resposta. A mensagem vai para a mesa da central, no painel,
 * e a resposta volta como notificação — e fica guardada aqui, embaixo da
 * pergunta. Telefone não tem, e a tela não finge que tem.
 *
 * As perguntas frequentes, que eram uma tela à parte (Ajuda), moram aqui
 * dentro agora.
 */
export function Atendimento() {
  const { temPapel } = useAutenticacao()
  const [escrevendo, definirEscrevendo] = useState<AssuntoDaMensagem | null>(null)
  const [mensagens, definirMensagens] = useState<MensagemDeAtendimento[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)
  const [pergunta, definirPergunta] = useState<number | null>(null)
  const ancora = useRef<HTMLDivElement>(null)

  const carregar = useCallback(() => {
    definirErro(null)
    api.get<MensagemDeAtendimento[]>('/atendimento/mensagens')
      .then(definirMensagens)
      .catch(definirErro)
  }, [])

  useEffect(() => { carregar() }, [carregar])

  function escrever(assunto: AssuntoDaMensagem) {
    definirRecado(null)
    definirEscrevendo(assunto)
    // Quem clicou lá embaixo, na sanfona, precisa ver o formulário abrir.
    requestAnimationFrame(() => ancora.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
  }

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
          <BotaoDeNotificacoes />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 22 }}>
        <h1 className="humano" style={{ fontSize: '1.75rem' }}>Atendimento</h1>
        <p className="subtitulo-de-tela">Estamos aqui para ajudar você.</p>

        <div className="atendimento-grade">
          <div className="coluna-da-central" ref={ancora}>
            <section className="fale-com-a-central manchas-verdes" aria-labelledby="t-central">
              <h2 id="t-central">Fale com a central</h2>
              <p>Escreva para a equipe MedShare. A resposta chega nas suas notificações.</p>
              {escrevendo === null && (
                <button type="button" className="botao principal" onClick={() => escrever('DUVIDA')}>
                  Mandar mensagem
                </button>
              )}
            </section>

            {escrevendo !== null && (
              <FormularioDeMensagem
                assuntoInicial={escrevendo}
                aoCancelar={() => definirEscrevendo(null)}
                aoEnviar={(nova) => {
                  definirMensagens((atuais) => [nova, ...atuais])
                  definirEscrevendo(null)
                  definirRecado('Mensagem enviada. A central responde por aqui, nas notificações.')
                }}
              />
            )}

            {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}
            <AvisoDeErro erro={erro} aoTentarDeNovo={carregar} />

            {mensagens.length > 0 && (
              <section className="suas-mensagens" aria-labelledby="t-mensagens">
                <h2 id="t-mensagens" className="titulo-de-secao">Suas mensagens</h2>
                {mensagens.map((m) => (
                  <article className="mensagem" key={m.id}>
                    <p className="mensagem-cabeca">
                      <strong>{NOME_DO_ASSUNTO[m.assunto]}</strong>
                      <span>{dataComHora(m.criadoEm)}</span>
                    </p>
                    <p className="mensagem-texto">{m.texto}</p>
                    {m.resposta ? (
                      <div className="resposta-da-central">
                        <p className="rotulo">Resposta da central · {dataComHora(m.respondidaEm)}</p>
                        <p>{m.resposta}</p>
                      </div>
                    ) : (
                      <p className="esperando-resposta">Esperando resposta</p>
                    )}
                  </article>
                ))}
              </section>
            )}

            <p className="aviso-de-emergencia">
              Em uma emergência médica, ligue <strong>192</strong> (SAMU). O MedShare não faz atendimento de saúde.
            </p>
          </div>

          <div className="coluna-da-sanfona">
            <div className="lista-da-conta">
              <SecaoRecolhivel titulo="Dúvidas frequentes">
                <div className="perguntas">
                  {PERGUNTAS.map(({ p, r }, i) => (
                    <div className={pergunta === i ? 'pergunta aberta' : 'pergunta'} key={p}>
                      <h3>
                        <button
                          type="button"
                          aria-expanded={pergunta === i}
                          onClick={() => definirPergunta(pergunta === i ? null : i)}
                        >
                          <span>{p}</span>
                          <span className="seta" aria-hidden="true">{pergunta === i ? '−' : '+'}</span>
                        </button>
                      </h3>
                      {pergunta === i && <p>{r.replace(/\s+/g, ' ').trim()}</p>}
                    </div>
                  ))}
                </div>
              </SecaoRecolhivel>

              <SecaoRecolhivel titulo="Problema com uma caixa">
                <p className="explicacao-secao" style={{ marginTop: 0 }}>
                  Código que não funcionou, caixa recusada ou retirada que não aconteceu. Conte o que
                  houve e diga o código da doação ou do pedido.
                </p>
                <div className="botoes-em-linha">
                  <button type="button" className="botao secundario" onClick={() => escrever('PROBLEMA_COM_CAIXA')}>
                    Escrever para a central
                  </button>
                  {temPapel('DOADOR') && <Link to="/app/doacoes" className="texto-botao">Ver minhas doações</Link>}
                  {temPapel('BENEFICIARIO') && <Link to="/app/pedidos" className="texto-botao">Ver meus pedidos</Link>}
                </div>
              </SecaoRecolhivel>

              <SecaoRecolhivel titulo="Atendimento nas farmácias">
                <p className="explicacao-secao" style={{ marginTop: 0 }}>No dia de entregar ou de retirar, leve:</p>
                <ul className="lista-simples">
                  <li>Documento com foto</li>
                  <li>O código que aparece no aplicativo</li>
                  <li>A receita, para quem vai receber</li>
                </ul>
                <Link to="/app/farmacias" className="texto-botao">Ver as farmácias parceiras</Link>
              </SecaoRecolhivel>

              <SecaoRecolhivel titulo="Sugestões e reclamações">
                <p className="explicacao-secao" style={{ marginTop: 0 }}>
                  Conte o que pode melhorar. A central lê todas.
                </p>
                <button type="button" className="botao secundario" onClick={() => escrever('SUGESTAO_OU_RECLAMACAO')}>
                  Escrever para a central
                </button>
              </SecaoRecolhivel>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

function FormularioDeMensagem({
  assuntoInicial,
  aoEnviar,
  aoCancelar,
}: {
  assuntoInicial: AssuntoDaMensagem
  aoEnviar: (mensagem: MensagemDeAtendimento) => void
  aoCancelar: () => void
}) {
  const [assunto, definirAssunto] = useState<AssuntoDaMensagem>(assuntoInicial)
  const [texto, definirTexto] = useState('')
  const [enviando, definirEnviando] = useState(false)
  const [erro, definirErro] = useState<unknown>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!texto.trim()) return
    definirErro(null)
    definirEnviando(true)
    try {
      aoEnviar(await api.post<MensagemDeAtendimento>('/atendimento/mensagens', { assunto, texto: texto.trim() }))
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <form className="formulario-de-mensagem" onSubmit={enviar}>
      <label className="campo">
        <span>Assunto</span>
        <select value={assunto} onChange={(e) => definirAssunto(e.target.value as AssuntoDaMensagem)}>
          {(Object.keys(NOME_DO_ASSUNTO) as AssuntoDaMensagem[]).map((chave) => (
            <option key={chave} value={chave}>{NOME_DO_ASSUNTO[chave]}</option>
          ))}
        </select>
      </label>
      <label className="campo">
        <span>Mensagem</span>
        <textarea
          rows={5}
          maxLength={2000}
          value={texto}
          onChange={(e) => definirTexto(e.target.value)}
          placeholder="Conte o que aconteceu ou o que você quer saber."
          autoFocus
        />
        <span className="apoio">Não escreva CPF, NIS nem o que está na receita: a central não precisa disso para responder.</span>
      </label>
      <AvisoDeErro erro={erro} />
      <div className="botoes-em-linha">
        <button type="submit" className="botao principal" disabled={enviando || !texto.trim()}>
          {enviando ? 'Enviando…' : 'Enviar'}
        </button>
        <button type="button" className="texto-botao" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  )
}
