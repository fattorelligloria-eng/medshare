import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api/cliente'
import type { DoacaoNoBalcao, FotoEnviada, Pagina } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { FotoDoCaso } from '../componentes/FotoDoCaso'
import { Status } from '../componentes/Status'
import { data, dataComHora } from '../formatos'

/**
 * O balcão da farmácia (UC03).
 *
 * O doador chega e mostra o código de entrega; sem ele, dá para achar pelo
 * CPF ou telefone (A3). A tela mostra a foto que ele enviou e o que declarou.
 * O farmacêutico confere a caixa de verdade: se lote ou validade forem outros,
 * corrige antes de aprovar (A1) — a correção fica no histórico. A foto tirada
 * aqui é opcional e vira exemplo para o modelo da fase 2.
 *
 * As duas caixas de marcar registram o que foi conferido de fato; aprovar só
 * libera com as duas, porque o servidor recusa sem elas (RN01).
 */
export function FilaDoBalcao() {
  const [doacoes, definirDoacoes] = useState<DoacaoNoBalcao[]>([])
  const [busca, definirBusca] = useState('')
  const [resultadoDaBusca, definirResultadoDaBusca] = useState<DoacaoNoBalcao[] | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)
  const [carregando, definirCarregando] = useState(true)

  const carregar = useCallback(async () => {
    definirCarregando(true)
    try {
      // O servidor sabe em qual farmácia o farmacêutico logado atua.
      const pagina = await api.get<Pagina<DoacaoNoBalcao>>('/doacoes/fila?size=50')
      definirDoacoes(pagina.content)
      definirErro(null)
    } catch (e) {
      definirErro(e)
    } finally {
      definirCarregando(false)
    }
  }, [])

  useEffect(() => { void carregar() }, [carregar])

  async function buscar(evento: FormEvent) {
    evento.preventDefault()
    const termo = busca.trim().toUpperCase()
    if (!termo) return
    definirErro(null)
    try {
      const digitos = termo.replace(/\D/g, '')
      // Código de entrega tem letras; CPF (11) e telefone (10-11) são só números.
      if (/[A-Z]/.test(termo)) {
        definirResultadoDaBusca([await api.get<DoacaoNoBalcao>(`/doacoes/balcao/entrega/${encodeURIComponent(termo)}`)])
      } else {
        definirResultadoDaBusca(await api.get<DoacaoNoBalcao[]>(`/doacoes/balcao/busca?documento=${digitos}`))
      }
    } catch (e) {
      definirResultadoDaBusca(null)
      definirErro(e)
    }
  }

  async function aposAcao(mensagem: string) {
    definirRecado(mensagem)
    definirResultadoDaBusca(null)
    definirBusca('')
    await carregar()
  }

  const lista = resultadoDaBusca ?? doacoes

  return (
    <>
      <div className="titulo-area">
        <h1>Balcão</h1>
        <p>Doações agendadas e recebidas neste ponto de coleta.</p>
      </div>

      <form className="bloco form-busca" onSubmit={buscar}>
        <label className="campo">
          <span>Código de entrega — ou CPF / telefone do doador</span>
          <input
            type="text"
            value={busca}
            onChange={(e) => definirBusca(e.target.value)}
            placeholder="Ex.: 95B3PVUM ou 11122233344"
            style={{ fontFamily: 'ui-monospace, Menlo, monospace', letterSpacing: '0.06em' }}
          />
        </label>
        <div className="acoes">
          <button className="principal" disabled={!busca.trim()}>Buscar</button>
          {resultadoDaBusca && (
            <button type="button" className="secundario" onClick={() => { definirResultadoDaBusca(null); definirBusca('') }}>
              Ver a fila toda
            </button>
          )}
        </div>
      </form>

      <AvisoDeErro erro={erro} />
      {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

      {carregando && <p style={{ color: 'var(--tinta-fraca)' }}>Carregando…</p>}

      {!carregando && lista.length === 0 && (
        <div className="bloco">
          <div className="vazio">
            <h3>{resultadoDaBusca ? 'Nada encontrado' : 'Nada na fila agora'}</h3>
            <p>As doações agendadas para esta farmácia aparecem aqui.</p>
          </div>
        </div>
      )}

      {lista.map((d) => (
        <CartaoDoBalcao key={d.codigo} doacao={d} aoErrar={definirErro} aoConcluir={aposAcao} />
      ))}
    </>
  )
}

function CartaoDoBalcao({ doacao: d, aoErrar, aoConcluir }: {
  doacao: DoacaoNoBalcao
  aoErrar: (e: unknown) => void
  aoConcluir: (mensagem: string) => Promise<void>
}) {
  const [lacre, definirLacre] = useState(false)
  const [dados, definirDados] = useState(false)
  const [corrigindo, definirCorrigindo] = useState(false)
  const [lote, definirLote] = useState(d.lote)
  const [validade, definirValidade] = useState(d.validade)
  const [foto, definirFoto] = useState<File | null>(null)
  const [rejeitando, definirRejeitando] = useState(false)
  const [motivo, definirMotivo] = useState('')
  const [enviando, definirEnviando] = useState(false)

  async function agir(acao: () => Promise<unknown>, mensagem: string) {
    aoErrar(null)
    definirEnviando(true)
    try {
      await acao()
      await aoConcluir(mensagem)
    } catch (e) {
      aoErrar(e)
    } finally {
      definirEnviando(false)
    }
  }

  async function fotoDaConferencia(): Promise<string | null> {
    return foto ? (await api.enviarArquivo<FotoEnviada>('/fotos', foto)).url : null
  }

  return (
    <article className="bloco">
      <div className="bloco-topo">
        <div>
          <h2>{d.medicamento}</h2>
          <p className="codigo">{d.codigo}{d.codigoEntrega ? ` · entrega ${d.codigoEntrega}` : ''}</p>
        </div>
        <Status status={d.status} />
      </div>

      {d.fotoUrl && <FotoDoCaso url={d.fotoUrl} descricao={`Foto enviada pelo doador de ${d.medicamento}`} />}

      <div className="grade">
        <div className="dado"><p className="rotulo">Doador</p><p className="valor">{d.doador}</p></div>
        <div className="dado"><p className="rotulo">Apresentação</p><p className="valor">{d.apresentacao}</p></div>
        <div className="dado"><p className="rotulo">Lote declarado</p><p className="valor">{d.lote}</p></div>
        <div className="dado"><p className="rotulo">Validade declarada</p><p className="valor">{data(d.validade)}</p></div>
        {d.agendadaPara && (
          <div className="dado"><p className="rotulo">Agendada para</p><p className="valor">{dataComHora(d.agendadaPara)}</p></div>
        )}
      </div>

      {d.status === 'AGENDADA' && (
        <div className="acoes">
          <button
            className="principal"
            disabled={enviando}
            onClick={() => agir(() => api.post(`/doacoes/${d.codigo}/recebimento`), `Recebimento de ${d.codigo} registrado.`)}
          >
            Registrar recebimento
          </button>
        </div>
      )}

      {d.status === 'RECEBIDA' && !rejeitando && (
        <div style={{ marginTop: 20, paddingTop: 20, borderTop: '1px solid var(--linha)' }}>
          <p className="rotulo" style={{ marginBottom: 12 }}>Conferência presencial — RN01</p>

          <label className="marcar">
            <input type="checkbox" checked={lacre} onChange={(e) => definirLacre(e.target.checked)} />
            <span>Lacre de fábrica intacto, abas coladas, sem sinal de violação</span>
          </label>
          <label className="marcar">
            <input type="checkbox" checked={dados} onChange={(e) => definirDados(e.target.checked)} />
            <span>Lote e validade conferidos na caixa{corrigindo ? ' (com a correção abaixo)' : ''}</span>
          </label>

          {!corrigindo ? (
            <button type="button" className="texto-botao" onClick={() => definirCorrigindo(true)}>
              Lote ou validade da caixa estão diferentes? Corrigir
            </button>
          ) : (
            <div className="linha-form" style={{ marginTop: 8 }}>
              <label className="campo">
                <span>Lote na caixa</span>
                <input type="text" maxLength={30} value={lote} onChange={(e) => definirLote(e.target.value.toUpperCase())} />
              </label>
              <label className="campo">
                <span>Validade na caixa</span>
                <input type="date" value={validade} onChange={(e) => definirValidade(e.target.value)} />
              </label>
            </div>
          )}

          <label className="campo" style={{ marginTop: 10 }}>
            <span>Foto da caixa no balcão (opcional)</span>
            <input type="file" accept="image/*" capture="environment" onChange={(e) => definirFoto(e.target.files?.[0] ?? null)} />
          </label>

          <div className="acoes">
            <button
              className="principal"
              disabled={!lacre || !dados || enviando}
              onClick={() => agir(async () => {
                await api.post(`/doacoes/${d.codigo}/validacao`, {
                  fotoUrl: await fotoDaConferencia(),
                  lote: corrigindo ? lote : null,
                  validade: corrigindo ? validade : null,
                })
              }, `${d.codigo} aprovada, no estoque e oferecida à fila.`)}
            >
              {enviando ? 'Salvando…' : 'Aprovar e disponibilizar'}
            </button>
            <button className="perigo" disabled={enviando} onClick={() => definirRejeitando(true)}>
              Rejeitar
            </button>
          </div>
        </div>
      )}

      {rejeitando && (
        <div style={{ marginTop: 20, paddingTop: 20, borderTop: '1px solid var(--linha)' }}>
          <label className="campo">
            <span>O que impediu a aprovação?</span>
            <textarea
              rows={3}
              maxLength={400}
              value={motivo}
              onChange={(e) => definirMotivo(e.target.value)}
              placeholder="Ex.: selo de segurança rompido; caixa reaberta e colada com fita."
            />
          </label>
          <label className="campo">
            <span>Foto da caixa (opcional)</span>
            <input type="file" accept="image/*" capture="environment" onChange={(e) => definirFoto(e.target.files?.[0] ?? null)} />
          </label>
          <div className="acoes">
            <button
              className="perigo"
              disabled={motivo.trim() === '' || enviando}
              onClick={() => agir(async () => {
                await api.post(`/doacoes/${d.codigo}/rejeicao`, {
                  lacreIntegro: lacre,
                  dadosConferem: dados,
                  motivo,
                  fotoUrl: await fotoDaConferencia(),
                })
              }, `${d.codigo} rejeitada. O doador foi avisado.`)}
            >
              Confirmar rejeição
            </button>
            <button className="secundario" onClick={() => { definirRejeitando(false); definirMotivo('') }}>
              Voltar
            </button>
          </div>
        </div>
      )}
    </article>
  )
}
