import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { CasoDaCentral, Pagina } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { data } from '../formatos'

/**
 * RN10 — a mesa da central.
 *
 * Chega aqui tudo que a leitura automática não pôde resolver sozinha. A tela
 * põe lado a lado o que o doador declarou e o que a IA leu, com as diferenças
 * em destaque, para o analista decidir olhando os dois — e nunca só o palpite
 * do modelo.
 *
 * A justificativa é obrigatória porque fica gravada no histórico da doação
 * para sempre. Decisão sem motivo registrado não é decisão auditável.
 */
export function FilaDaCentral() {
  const [casos, definirCasos] = useState<CasoDaCentral[]>([])
  const [justificativas, definirJustificativas] = useState<Record<string, string>>({})
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)
  const [carregando, definirCarregando] = useState(true)

  const carregar = useCallback(async () => {
    definirCarregando(true)
    try {
      const pagina = await api.get<Pagina<CasoDaCentral>>('/central/fila?size=50')
      definirCasos(pagina.content)
      definirErro(null)
    } catch (e) {
      definirErro(e)
    } finally {
      definirCarregando(false)
    }
  }, [])

  useEffect(() => { void carregar() }, [carregar])

  async function decidir(codigo: string, decisao: 'APROVADA' | 'RECUSADA') {
    definirErro(null)
    definirRecado(null)
    try {
      await api.post(`/central/casos/${codigo}/decisao`, {
        decisao,
        justificativa: justificativas[codigo] ?? '',
      })
      definirRecado(
        `${codigo} ${decisao === 'APROVADA' ? 'aprovada' : 'recusada'}. O doador foi avisado.`,
      )
      await carregar()
    } catch (e) {
      definirErro(e)
    }
  }

  /** Os dois rótulos vêm prontos para não errar a concordância em português. */
  function comparar(
    rotuloDeclarado: string,
    rotuloLido: string,
    declarado: string | null,
    lido: string | null,
  ) {
    const divergente = Boolean(declarado && lido && declarado !== lido)
    return (
      <>
        <div className="dado">
          <p className="rotulo">{rotuloDeclarado}</p>
          <p className="valor">{declarado ?? '—'}</p>
        </div>
        <div className="dado">
          <p className="rotulo">{rotuloLido}</p>
          <p className={divergente ? 'valor divergente' : 'valor'}>
            {lido ?? 'não foi possível ler'}
          </p>
        </div>
      </>
    )
  }

  return (
    <>
      <div className="titulo-area">
        <h1>Central de análise</h1>
        <p>
          Casos que a leitura automática não pôde decidir sozinha. Toda decisão
          aqui é humana e fica registrada com justificativa.
        </p>
      </div>

      <AvisoDeErro erro={erro} />
      {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

      {carregando && <p style={{ color: 'var(--tinta-fraca)' }}>Carregando…</p>}

      {!carregando && casos.length === 0 && (
        <div className="bloco">
          <div className="vazio">
            <h3>Nenhum caso aguardando</h3>
            <p>Quando a leitura automática tiver dúvida, o caso aparece aqui.</p>
          </div>
        </div>
      )}

      {casos.map((caso) => (
        <article className="bloco" key={caso.codigo}>
          <div className="bloco-topo">
            <div>
              <h2>{caso.medicamento}</h2>
              <p className="codigo">{caso.codigo}</p>
            </div>
            {caso.certeza && (
              <div className={`status ${caso.certeza === 'ALTA' ? 'andamento' : 'atencao'}`}>
                <span className="ponto" />
                <span className="texto">certeza {caso.certeza.toLowerCase()}</span>
              </div>
            )}
          </div>

          {caso.motivo && (
            <p style={{ margin: '0 0 18px', fontSize: 14, color: 'var(--tinta-media)', lineHeight: 1.5 }}>
              <strong>Leitura ({caso.avaliador}):</strong> {caso.motivo}
            </p>
          )}

          <div className="grade">
            {comparar('Lote declarado', 'Lote lido pela IA', caso.loteDeclarado, caso.loteLido)}
            {comparar(
              'Validade declarada', 'Validade lida pela IA',
              data(caso.validadeDeclarada), caso.validadeLida ? data(caso.validadeLida) : null,
            )}
            {comparar('Código de barras esperado', 'Código de barras lido', caso.eanEsperado, caso.eanLido)}
            <div className="dado">
              <p className="rotulo">Embalagem</p>
              <p className="valor">{caso.classeEmbalagem ?? '—'}</p>
            </div>
          </div>

          {caso.divergencias.length > 0 && (
            <div className="aviso erro" style={{ marginTop: 20, marginBottom: 0 }}>
              <strong>Divergências encontradas</strong>
              <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
                {caso.divergencias.map((d) => <li key={d}>{d}</li>)}
              </ul>
            </div>
          )}

          <div style={{ marginTop: 20 }}>
            <label className="campo">
              <span>Justificativa da decisão — fica no histórico</span>
              <textarea
                rows={2}
                value={justificativas[caso.codigo] ?? ''}
                onChange={(e) =>
                  definirJustificativas({ ...justificativas, [caso.codigo]: e.target.value })}
                placeholder="Ex.: foto nítida, lacre de fábrica intacto; o lote confere com a caixa."
              />
            </label>
          </div>

          <div className="acoes">
            <button
              className="principal"
              disabled={!(justificativas[caso.codigo] ?? '').trim()}
              onClick={() => decidir(caso.codigo, 'APROVADA')}
            >
              Aprovar
            </button>
            <button
              className="perigo"
              disabled={!(justificativas[caso.codigo] ?? '').trim()}
              onClick={() => decidir(caso.codigo, 'RECUSADA')}
            >
              Recusar
            </button>
          </div>
        </article>
      ))}
    </>
  )
}
