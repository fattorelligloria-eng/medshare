import { lazy, Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api/cliente'
import type { PontoDeColeta } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { EsqueletoDeLista } from '../componentes/Esqueleto'
import { TelaVazia, DesenhoDeBusca } from '../componentes/TelaVazia'
import { Lugar, Relogio } from '../componentes/Icones'
import { linkDoMapa } from '../formatos'

/**
 * O Leaflet sozinho é metade do peso do aplicativo. Carregado assim, só quem
 * abre esta tela paga por ele — quem só doa e acompanha nunca baixa o mapa.
 */
const MapaDeFarmacias = lazy(() =>
  import('../componentes/MapaDeFarmacias').then((m) => ({ default: m.MapaDeFarmacias })),
)

/**
 * As farmácias parceiras, no mapa e na lista.
 *
 * A ordem padrão vem do servidor, que mede a distância a partir do endereço
 * cadastrado da pessoa. Por isso a tela diz de onde está medindo: alguém que
 * se mudou e não atualizou o cadastro precisa entender por que a lista parece
 * errada.
 *
 * O filtro "com vaga" é o que esta tela ganhou de mais útil. Antes ela
 * oferecia cinco farmácias iguais, e só depois de escolher uma e abrir o
 * agendamento é que a pessoa descobria que não havia horário nenhum. Agora a
 * farmácia sem vaga diz isso na própria linha, antes do clique.
 */

type Ordem = 'distancia' | 'nome'
type Filtro = 'todas' | 'abertas' | 'com-vaga'

const FILTROS: { chave: Filtro; nome: string }[] = [
  { chave: 'todas', nome: 'Todas' },
  { chave: 'abertas', nome: 'Abertas agora' },
  { chave: 'com-vaga', nome: 'Com vaga' },
]

export function Farmacias() {
  const [pontos, definirPontos] = useState<PontoDeColeta[]>([])
  const [carregando, definirCarregando] = useState(true)
  const [erro, definirErro] = useState<unknown>(null)
  const [ordem, definirOrdem] = useState<Ordem>('distancia')
  const [filtro, definirFiltro] = useState<Filtro>('todas')
  const [emFoco, definirEmFoco] = useState<number | null>(null)

  const cartoes = useRef<Record<number, HTMLElement | null>>({})

  const carregar = useCallback(() => {
    definirCarregando(true)
    definirErro(null)
    api.get<PontoDeColeta[]>('/pontos-de-coleta/proximos')
      .then(definirPontos)
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  const mostradas = useMemo(() => {
    const cabe = (p: PontoDeColeta) =>
      filtro === 'todas' ? true
        : filtro === 'abertas' ? p.abertaAgora
          : p.vagasProximosDias === null || p.vagasProximosDias > 0

    // A ordem por distância é a que o servidor já devolveu; não reordenar
    // mantém o cálculo num lugar só.
    const lista = pontos.filter(cabe)
    return ordem === 'nome'
      ? [...lista].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      : lista
  }, [pontos, filtro, ordem])

  const contar = useCallback((f: Filtro) => {
    if (f === 'todas') return pontos.length
    if (f === 'abertas') return pontos.filter((p) => p.abertaAgora).length
    return pontos.filter((p) => p.vagasProximosDias === null || p.vagasProximosDias > 0).length
  }, [pontos])

  /** Clicar no alfinete leva o olho até o cartão correspondente. */
  const irParaOCartao = useCallback((id: number) => {
    definirEmFoco(id)
    cartoes.current[id]?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [])

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 28 }}>
        <h1 className="humano" style={{ fontSize: '1.625rem' }}>Farmácias parceiras</h1>
        <p style={{ margin: '8px 0 20px', fontSize: '0.9062rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
          Da mais perto para a mais longe, a partir do endereço do seu cadastro.
          É numa delas que você entrega a caixa — e é lá que um farmacêutico
          confere o lacre antes de ela seguir.
        </p>

        <AvisoDeErro erro={erro} aoTentarDeNovo={carregar} />

        {carregando && (
          <div role="status" aria-live="polite">
            <span className="so-leitor-de-tela">Carregando as farmácias</span>
            <EsqueletoDeLista itens={4} />
          </div>
        )}

        {!carregando && !erro && pontos.length === 0 && (
          <TelaVazia desenho={DesenhoDeBusca} titulo="Nenhuma farmácia por perto">
            Ainda não há farmácia parceira perto do seu endereço. Confira o
            endereço na sua conta — ou aguarde: a rede está crescendo.
          </TelaVazia>
        )}

        {!carregando && !erro && pontos.length > 0 && (
          <>
            <Suspense fallback={<div className="mapa-farmacias"><div className="tela-do-mapa carregando-mapa" /></div>}>
              <MapaDeFarmacias pontos={mostradas} aoEscolher={irParaOCartao} />
            </Suspense>

            <div className="filtro" role="group" aria-label="Filtrar farmácias">
              {FILTROS.map(({ chave, nome }) => (
                <button
                  key={chave}
                  type="button"
                  className={filtro === chave ? 'ativo' : ''}
                  aria-pressed={filtro === chave}
                  onClick={() => definirFiltro(chave)}
                >
                  {nome} <span className="conta">{contar(chave)}</span>
                </button>
              ))}
            </div>

            <div className="ordenacao">
              <label className="campo-em-linha">
                <span>Ordenar por</span>
                <select value={ordem} onChange={(e) => definirOrdem(e.target.value as Ordem)}>
                  <option value="distancia">Mais perto de mim</option>
                  <option value="nome">Nome</option>
                </select>
              </label>
            </div>

            <p className="so-leitor-de-tela" role="status">
              {mostradas.length} farmácia(s) nesta lista.
            </p>

            {mostradas.length === 0 && (
              <p className="nenhuma-com-filtro">
                Nenhuma farmácia neste filtro agora. Veja em <strong>Todas</strong>.
              </p>
            )}

            {mostradas.map((p) => {
              const semVaga = p.vagasProximosDias === 0
              return (
                <article
                  className={emFoco === p.id ? 'item em-foco' : 'item'}
                  key={p.id}
                  ref={(el) => { cartoes.current[p.id] = el }}
                >
                  <div className="linha-titulo-farmacia">
                    <h2 className="humano" style={{ fontSize: '1.1875rem' }}>{p.nome}</h2>
                    {p.abertaAgora
                      ? <span className="etiqueta aberta">Aberta agora</span>
                      : <span className="etiqueta fechada">Fechada agora</span>}
                  </div>

                  <p className="detalhe">{p.endereco}</p>
                  <p className="explicacao">{p.bairro} · {p.municipio}</p>

                  {p.horario && (
                    <div className="linha-icone">
                      <Relogio cor="var(--verde-escuro)" />
                      <span>{p.horario}</span>
                    </div>
                  )}

                  {semVaga && (
                    <p className="sem-vaga">
                      Sem horário livre nas próximas duas semanas. Escolher esta
                      farmácia não vai adiantar agora.
                    </p>
                  )}

                  <div className="linha-icone">
                    <Lugar cor="var(--verde-escuro)" />
                    <a href={linkDoMapa(`${p.endereco}, ${p.bairro}, ${p.municipio}`)}
                       target="_blank" rel="noreferrer">
                      Como chegar
                    </a>
                  </div>
                </article>
              )
            })}

            <p className="credito-do-mapa">Mapa © colaboradores do OpenStreetMap</p>
          </>
        )}
      </div>
    </>
  )
}
