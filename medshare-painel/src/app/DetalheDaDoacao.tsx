import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { DoacaoDetalhada, FotoEnviada } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { CodigoCopiavel } from '../componentes/CodigoCopiavel'
import { Camera, Compartilhar, Imprimir } from '../componentes/Icones'
import { QrCode } from '../componentes/QrCode'
import { Status, explicar } from '../componentes/Status'
import { NOME_DO_EVENTO } from '../componentes/Eventos'
import { data, dataComHora } from '../formatos'

/** RN06 — o percurso completo da caixa, do cadastro até onde ela está agora. */
export function DetalheDaDoacao() {
  const { codigo = '' } = useParams()
  const [detalhe, definirDetalhe] = useState<DoacaoDetalhada | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [carregando, definirCarregando] = useState(true)

  const [enviandoFoto, definirEnviandoFoto] = useState(false)

  const carregar = useCallback(() => {
    definirCarregando(true)
    api.get<DoacaoDetalhada>(`/doacoes/${codigo}`)
      .then(definirDetalhe)
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [codigo])

  useEffect(carregar, [carregar])

  /** UC10 A2 — a central pediu outra foto: envia e a pré-validação roda de novo. */
  async function enviarNovaFoto(evento: React.ChangeEvent<HTMLInputElement>) {
    const arquivo = evento.target.files?.[0]
    if (!arquivo) return
    definirErro(null)
    definirEnviandoFoto(true)
    try {
      const foto = await api.enviarArquivo<FotoEnviada>('/fotos', arquivo)
      await api.post(`/doacoes/${codigo}/foto`, { fotoUrl: foto.url })
      carregar()
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviandoFoto(false)
    }
  }

  async function cancelarAgendamento() {
    if (!window.confirm('Cancelar o agendamento? Você poderá reagendar uma única vez.')) return
    definirErro(null)
    try {
      await api.delete(`/doacoes/${codigo}/agendamento`)
      carregar()
    } catch (e) {
      definirErro(e)
    }
  }

  /**
   * Compartilhar usa a folha nativa do sistema (Web Share API), que so existe
   * em contexto seguro e em geral so no celular. Quando nao existe, o botao
   * nem aparece — melhor do que um botao que nao faz nada.
   */
  const podeCompartilhar = typeof navigator !== 'undefined' && typeof navigator.share === 'function'

  async function compartilhar() {
    if (!detalhe?.agendamento) return
    const a = detalhe.agendamento
    try {
      await navigator.share({
        title: 'Entrega MedShare',
        text: `Código de entrega ${a.codigoEntrega}\n${a.pontoDeColeta}\n${a.endereco}\n`
          + `${dataComHora(a.dataHora)}`,
      })
    } catch {
      // A pessoa fechou a folha de compartilhamento. Nao e erro.
    }
  }

  const ultimoEvento = detalhe?.historico[detalhe.historico.length - 1]
  const centralPediuFoto = detalhe?.doacao.status === 'CADASTRADA' && ultimoEvento?.tipo === 'NOVA_FOTO_SOLICITADA'

  return (
    <>
      <CabecalhoInterno titulo={codigo} para="/app/doacoes" />

      <div className="conteudo-app cresce" style={{ paddingTop: 22 }}>
        <AvisoDeErro erro={erro} />
        {carregando && <p style={{ color: 'var(--tinta-fraca)', fontSize: '0.875rem' }}>Carregando…</p>}

        {detalhe && (
          <>
            <Status status={detalhe.doacao.status} />
            <h1 className="humano" style={{ fontSize: '1.6875rem' }}>{detalhe.doacao.medicamento}</h1>
            <p style={{ margin: '10px 0 0', fontSize: '0.9062rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
              {explicar(detalhe.doacao.status)}
            </p>

            <div style={{ margin: '22px 0', paddingTop: 20, borderTop: '1px solid var(--linha)' }}>
              <div className="grade">
                <div className="dado">
                  <p className="rotulo">Princípio ativo</p>
                  <p className="valor">{detalhe.doacao.principioAtivo}</p>
                </div>
                <div className="dado">
                  <p className="rotulo">Lote</p>
                  <p className="valor">{detalhe.doacao.lote}</p>
                </div>
                <div className="dado">
                  <p className="rotulo">Validade</p>
                  <p className="valor">{data(detalhe.doacao.validade)}</p>
                </div>
                {detalhe.doacao.pontoDeColeta && (
                  <div className="dado">
                    <p className="rotulo">Farmácia</p>
                    <p className="valor">{detalhe.doacao.pontoDeColeta}</p>
                  </div>
                )}
              </div>
            </div>

            {detalhe.doacao.status === 'PRE_VALIDADA' && (
              <Link to={`/app/doacoes/${codigo}/agendar`} className="botao principal" style={{ marginBottom: 24 }}>
                Escolher farmácia e horário
              </Link>
            )}

            {detalhe.doacao.status === 'AGENDADA' && detalhe.agendamento && (
              <div className="comprovante" style={{ marginBottom: 24 }}>
                <CodigoCopiavel codigo={detalhe.agendamento.codigoEntrega} />

                <div className="area-do-qr">
                  <QrCode valor={detalhe.agendamento.codigoEntrega} tamanho={168} />
                  <p className="legenda-do-qr">
                    O balcão pode ler o código pelo leitor, sem digitar.
                  </p>
                </div>

                <p style={{ margin: '12px 0 0', fontSize: '0.875rem', fontWeight: 600 }}>
                  {dataComHora(detalhe.agendamento.dataHora)} — {detalhe.agendamento.pontoDeColeta}
                </p>
                <p style={{ margin: '4px 0 14px', fontSize: '0.8438rem', color: 'var(--tinta-media)' }}>
                  {detalhe.agendamento.endereco}
                </p>

                <div className="acoes-do-comprovante">
                  {podeCompartilhar && (
                    <button type="button" className="botao secundario" onClick={compartilhar}>
                      <Compartilhar /> Compartilhar
                    </button>
                  )}
                  <button type="button" className="botao secundario" onClick={() => window.print()}>
                    <Imprimir /> Imprimir
                  </button>
                </div>

                <button
                  type="button"
                  className="texto-botao nao-imprimir"
                  style={{ marginTop: 14 }}
                  onClick={cancelarAgendamento}
                >
                  Cancelar agendamento
                </button>
              </div>
            )}

            {detalhe.podeReagendar && (
              <Link to={`/app/doacoes/${codigo}/agendar`} className="botao principal" style={{ marginBottom: 24 }}>
                Reagendar entrega (uma vez)
              </Link>
            )}

            {centralPediuFoto && (
              <div className="aviso erro" style={{ marginBottom: 24 }}>
                <strong>A equipe pediu uma nova foto.</strong> {ultimoEvento?.descricao}
                <label className="botao secundario" style={{ marginTop: 12 }}>
                  <Camera /> {enviandoFoto ? 'Enviando…' : 'Tirar nova foto'}
                  <input type="file" accept="image/*" capture="environment" onChange={enviarNovaFoto} hidden disabled={enviandoFoto} />
                </label>
              </div>
            )}

            <p className="rotulo" style={{ marginBottom: 4 }}>Por onde passou</p>
            <p style={{ margin: '0 0 16px', fontSize: '0.8125rem', color: 'var(--tinta-media)' }}>
              Registro completo e imutável desta caixa.
            </p>

            <ul className="tempo com-responsavel">
              {detalhe.historico.map((evento, i) => (
                <li key={`${evento.quando}-${i}`}>
                  <div className="passo">
                    <p className="quando">{dataComHora(evento.quando)}</p>
                    <p className="titulo">{NOME_DO_EVENTO[evento.tipo] ?? evento.tipo}</p>
                    <p className="descricao">{evento.descricao}</p>
                  </div>
                  <p className="responsavel">{evento.responsavel ?? 'Sistema'}</p>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
      <div style={{ height: 26, flexShrink: 0 }} />
    </>
  )
}
