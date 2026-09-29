import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { Doacao, Pagina } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { Status } from '../componentes/Status'
import { data } from '../formatos'

/** Enquanto não há tela de cadastro de farmácia, o balcão trabalha no ponto 1. */
const PONTO_DE_COLETA = 1

/**
 * O balcão da farmácia.
 *
 * As duas caixas de marcar não são enfeite: elas registram o que o
 * farmacêutico conferiu de fato, e ficam gravadas junto com o CRF dele. O
 * botão de aprovar só libera com as duas marcadas — porque o servidor recusa
 * sem elas (RN01), e é melhor a tela deixar isso claro antes da tentativa.
 */
export function FilaDoBalcao() {
  const [doacoes, definirDoacoes] = useState<Doacao[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)
  const [carregando, definirCarregando] = useState(true)
  const [lacre, definirLacre] = useState<Record<string, boolean>>({})
  const [dados, definirDados] = useState<Record<string, boolean>>({})
  const [rejeitando, definirRejeitando] = useState<string | null>(null)
  const [motivo, definirMotivo] = useState('')

  const carregar = useCallback(async () => {
    definirCarregando(true)
    try {
      const pagina = await api.get<Pagina<Doacao>>(
        `/doacoes/fila?pontoDeColetaId=${PONTO_DE_COLETA}&size=50`,
      )
      definirDoacoes(pagina.content)
      definirErro(null)
    } catch (e) {
      definirErro(e)
    } finally {
      definirCarregando(false)
    }
  }, [])

  useEffect(() => { void carregar() }, [carregar])

  async function agir(acao: () => Promise<unknown>, mensagem: string) {
    definirErro(null)
    definirRecado(null)
    try {
      await acao()
      definirRecado(mensagem)
      await carregar()
    } catch (e) {
      definirErro(e)
    }
  }

  return (
    <>
      <div className="titulo-area">
        <h1>Balcão</h1>
        <p>Doações agendadas e recebidas neste ponto de coleta.</p>
      </div>

      <AvisoDeErro erro={erro} />
      {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

      {carregando && <p style={{ color: 'var(--tinta-fraca)' }}>Carregando…</p>}

      {!carregando && doacoes.length === 0 && (
        <div className="bloco">
          <div className="vazio">
            <h3>Nada na fila agora</h3>
            <p>As doações agendadas para esta farmácia aparecem aqui.</p>
          </div>
        </div>
      )}

      {doacoes.map((d) => (
        <article className="bloco" key={d.codigo}>
          <div className="bloco-topo">
            <div>
              <h2>{d.medicamento}</h2>
              <p className="codigo">{d.codigo}</p>
            </div>
            <Status status={d.status} />
          </div>

          <div className="grade">
            <div className="dado">
              <p className="rotulo">Princípio ativo</p>
              <p className="valor">{d.principioAtivo}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Lote declarado</p>
              <p className="valor">{d.lote}</p>
            </div>
            <div className="dado">
              <p className="rotulo">Validade declarada</p>
              <p className="valor">{data(d.validade)}</p>
            </div>
          </div>

          {d.status === 'AGENDADA' && (
            <div className="acoes">
              <button
                className="principal"
                onClick={() => agir(
                  () => api.post(`/doacoes/${d.codigo}/recebimento`),
                  `Recebimento de ${d.codigo} registrado.`,
                )}
              >
                Registrar recebimento
              </button>
            </div>
          )}

          {d.status === 'RECEBIDA' && rejeitando !== d.codigo && (
            <div style={{ marginTop: 20, paddingTop: 20, borderTop: '1px solid var(--linha)' }}>
              <p className="rotulo" style={{ marginBottom: 12 }}>Conferência presencial — RN01</p>

              <label className="marcar">
                <input
                  type="checkbox"
                  checked={lacre[d.codigo] ?? false}
                  onChange={(e) => definirLacre({ ...lacre, [d.codigo]: e.target.checked })}
                />
                <span>Lacre de fábrica intacto, abas coladas, sem sinal de violação</span>
              </label>

              <label className="marcar">
                <input
                  type="checkbox"
                  checked={dados[d.codigo] ?? false}
                  onChange={(e) => definirDados({ ...dados, [d.codigo]: e.target.checked })}
                />
                <span>Lote e validade da caixa conferem com o que foi declarado</span>
              </label>

              <div className="acoes">
                <button
                  className="principal"
                  disabled={!lacre[d.codigo] || !dados[d.codigo]}
                  onClick={() => agir(async () => {
                    await api.post(`/doacoes/${d.codigo}/validacao`)
                    definirLacre({ ...lacre, [d.codigo]: false })
                    definirDados({ ...dados, [d.codigo]: false })
                  }, `${d.codigo} aprovada e já disponível no estoque.`)}
                >
                  Aprovar e disponibilizar
                </button>
                <button className="perigo" onClick={() => definirRejeitando(d.codigo)}>
                  Rejeitar
                </button>
              </div>

              {(!lacre[d.codigo] || !dados[d.codigo]) && (
                <p style={{ marginTop: 10, fontSize: 13.5, color: 'var(--tinta-fraca)' }}>
                  Marque as duas conferências para poder aprovar.
                </p>
              )}
            </div>
          )}

          {rejeitando === d.codigo && (
            <div style={{ marginTop: 20, paddingTop: 20, borderTop: '1px solid var(--linha)' }}>
              <label className="campo">
                <span>O que impediu a aprovação?</span>
                <textarea
                  rows={3}
                  value={motivo}
                  onChange={(e) => definirMotivo(e.target.value)}
                  placeholder="Ex.: selo de segurança rompido; caixa reaberta e colada com fita."
                />
              </label>
              <div className="acoes">
                <button
                  className="perigo"
                  disabled={motivo.trim() === ''}
                  onClick={() => agir(async () => {
                    await api.post(`/doacoes/${d.codigo}/rejeicao`, {
                      lacreIntegro: lacre[d.codigo] ?? false,
                      dadosConferem: dados[d.codigo] ?? false,
                      motivo,
                    })
                    definirRejeitando(null)
                    definirMotivo('')
                  }, `${d.codigo} rejeitada. O doador foi avisado.`)}
                >
                  Confirmar rejeição
                </button>
                <button className="secundario" onClick={() => { definirRejeitando(null); definirMotivo('') }}>
                  Voltar
                </button>
              </div>
            </div>
          )}
        </article>
      ))}
    </>
  )
}
