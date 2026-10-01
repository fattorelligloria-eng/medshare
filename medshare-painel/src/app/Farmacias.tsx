import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { PontoDeColeta } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { EsqueletoDeLista } from '../componentes/Esqueleto'
import { TelaVazia, DesenhoDeBusca } from '../componentes/TelaVazia'
import { Lugar, Relogio } from '../componentes/Icones'
import { linkDoMapa } from '../formatos'

/**
 * As farmácias parceiras, da mais perto para a mais longe.
 *
 * A ordem vem do servidor, que mede a distância a partir do endereço cadastrado
 * da pessoa. Por isso a tela diz de onde está medindo: alguém que se mudou e
 * não atualizou o cadastro precisa entender por que a lista parece errada.
 */
export function Farmacias() {
  const [pontos, definirPontos] = useState<PontoDeColeta[]>([])
  const [carregando, definirCarregando] = useState(true)
  const [erro, definirErro] = useState<unknown>(null)

  const carregar = useCallback(() => {
    definirCarregando(true)
    definirErro(null)
    api.get<PontoDeColeta[]>('/pontos-de-coleta/proximos')
      .then(definirPontos)
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 28 }}>
        <h1 className="humano" style={{ fontSize: 26 }}>Farmácias parceiras</h1>
        <p style={{ margin: '8px 0 24px', fontSize: 14.5, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
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

        {pontos.map((p) => (
          <article className="item" key={p.id}>
            <h2 className="humano" style={{ fontSize: 19 }}>{p.nome}</h2>
            <p className="detalhe">{p.endereco}</p>
            <p className="explicacao">{p.bairro} · {p.municipio}</p>

            {p.horario && (
              <div className="linha-icone">
                <Relogio cor="var(--verde-escuro)" />
                <span>{p.horario}</span>
              </div>
            )}

            <div className="linha-icone">
              <Lugar cor="var(--verde-escuro)" />
              <a href={linkDoMapa(`${p.endereco}, ${p.bairro}, ${p.municipio}`)}
                 target="_blank" rel="noreferrer">
                Como chegar
              </a>
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
