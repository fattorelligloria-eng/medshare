import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Doacao, Pagina } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Status, explicar } from '../componentes/Status'
import { Marca } from '../componentes/Logo'
import { BotaoDeNotificacoes } from '../componentes/BotaoDeNotificacoes'
import { EsqueletoDeLista } from '../componentes/Esqueleto'
import { TelaVazia, DesenhoDeCaixa } from '../componentes/TelaVazia'
import { Confere, Lugar, Mais, Relogio } from '../componentes/Icones'
import { data, diaEHora, linkDoMapa, primeiroNome, quantoFalta } from '../formatos'

/** Os estados em que a caixa parou de andar. */
const ENCERRADOS = ['ENTREGUE', 'RECUSADA', 'CANCELADA', 'REJEITADA', 'DESCARTADA']

type Aba = 'andamento' | 'concluidas' | 'recusadas'

const ABAS: { chave: Aba; nome: string; cabe: (d: Doacao) => boolean }[] = [
  { chave: 'andamento', nome: 'Em andamento', cabe: (d) => !ENCERRADOS.includes(d.status) },
  { chave: 'concluidas', nome: 'Concluídas', cabe: (d) => d.status === 'ENTREGUE' },
  {
    chave: 'recusadas',
    // "Recusadas" ficou errado quando os estados foram renomeados: a aba junta
    // caixa não aceita, reprovada no balcão, vencida e cancelada — e cancelada
    // foi decisão da própria pessoa, ninguém recusou nada.
    nome: 'Não seguiram',
    cabe: (d) => ['RECUSADA', 'REJEITADA', 'CANCELADA', 'DESCARTADA'].includes(d.status),
  },
]

/**
 * A tela inicial de quem doa.
 *
 * Duas coisas governam esta tela.
 *
 * A primeira é a prova: a pessoa doou e nunca mais viu a caixa. Sem um sinal de
 * que aquilo virou alguma coisa, doar parece jogar no vazio — por isso o número
 * grande e a faixa de quando a caixa chega a alguém.
 *
 * A segunda é a próxima ação. Se existe um agendamento, ele é a coisa mais
 * importante da tela e vem antes de tudo, com o dia, quanto falta e o caminho
 * até a farmácia. Hoje a pessoa tinha que abrir doação por doação para
 * descobrir que precisava sair de casa na quinta.
 */
export function Doacoes() {
  const { sessao } = useAutenticacao()
  const [doacoes, definirDoacoes] = useState<Doacao[]>([])
  const [carregando, definirCarregando] = useState(true)
  const [erro, definirErro] = useState<unknown>(null)
  const [aba, definirAba] = useState<Aba>('andamento')

  const carregar = useCallback(() => {
    definirCarregando(true)
    definirErro(null)
    api.get<Pagina<Doacao>>('/doacoes/minhas?size=50')
      .then((p) => definirDoacoes(p.content))
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  const entregues = doacoes.filter((d) => d.status === 'ENTREGUE')
  const ultimaEntregue = entregues[0]

  // A próxima entrega: a agendada mais perto de hoje que ainda não aconteceu.
  const proxima = doacoes
    .filter((d) => d.agendadaPara && !ENCERRADOS.includes(d.status))
    .sort((a, b) => (a.agendadaPara! < b.agendadaPara! ? -1 : 1))[0]

  const daAba = doacoes.filter(ABAS.find((a) => a.chave === aba)!.cabe)
  const contar = (chave: Aba) => doacoes.filter(ABAS.find((a) => a.chave === chave)!.cabe).length

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
          <BotaoDeNotificacoes />
        </div>
      </div>

      <div className="conteudo-app" style={{ paddingTop: 28 }}>
        <h1 className="humano" style={{ fontSize: '1.625rem', margin: '0 0 12px' }}>
          Oi, {primeiroNome(sessao?.nome ?? '')}
        </h1>

        {doacoes.length > 0 ? (
          <>
            <div className="impacto">
              <span className="numero">{doacoes.length}</span>
              <p>
                {doacoes.length === 1 ? 'caixa sua já' : 'caixas suas já'}
                <br />
                {doacoes.length === 1 ? 'saiu de casa' : 'saíram de casa'}
              </p>
            </div>

            {entregues.length > 0 && (
              <p style={{ margin: '14px 0 0', fontSize: '0.875rem', color: 'var(--tinta-media)' }}>
                {entregues.length === 1
                  ? 'Uma delas já chegou a alguém.'
                  : `${entregues.length} delas já chegaram a alguém.`}
              </p>
            )}
          </>
        ) : !carregando && (
          <p style={{ margin: 0, fontSize: '0.9688rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
            Sobrou caixa lacrada de um tratamento? Ela pode ser o tratamento de
            outra pessoa.
          </p>
        )}
      </div>

      {/* --- o próximo passo: a única coisa que pede ação da pessoa --- */}
      {proxima && (
        <div className="conteudo-app" style={{ paddingTop: 22 }}>
          <section className="proximo-passo" aria-labelledby="titulo-proximo-passo">
            <p className="rotulo">Seu próximo passo</p>

            <h2 id="titulo-proximo-passo" className="humano">
              Leve o {proxima.medicamento} à {proxima.pontoDeColeta}
            </h2>

            <p className="quando">
              <Relogio cor="var(--tinta-media)" />
              {diaEHora(proxima.agendadaPara)}
              {quantoFalta(proxima.agendadaPara) && (
                <>
                  {' · '}
                  <strong>{quantoFalta(proxima.agendadaPara)}</strong>
                </>
              )}
            </p>

            {proxima.enderecoDoPonto && (
              <>
                <div className="linha-icone">
                  <Lugar cor="var(--verde-escuro)" />
                  <span>{proxima.enderecoDoPonto}</span>
                </div>

                <div className="acoes-passo">
                  <a
                    className="botao secundario"
                    href={linkDoMapa(`${proxima.enderecoDoPonto}, ${proxima.pontoDeColeta}`)}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Como chegar
                  </a>
                  <Link className="botao secundario" to={`/app/doacoes/${proxima.codigo}`}>
                    Ver a doação
                  </Link>
                </div>
              </>
            )}
          </section>
        </div>
      )}

      {ultimaEntregue && (
        <div className="conteudo-app" style={{ paddingTop: 20 }}>
          <div className="faixa-boa">
            <Confere cor="var(--verde-escuro)" />
            <p>
              <strong>{ultimaEntregue.medicamento} entregue.</strong>{' '}
              Alguém começou o tratamento.
            </p>
          </div>
        </div>
      )}

      <div className="conteudo-app cresce" style={{ paddingTop: 26 }}>
        <AvisoDeErro erro={erro} aoTentarDeNovo={carregar} />

        {carregando && (
          <div role="status" aria-live="polite">
            <span className="so-leitor-de-tela">Carregando suas doações</span>
            <EsqueletoDeLista itens={3} />
          </div>
        )}

        {!carregando && !erro && doacoes.length === 0 && (
          <TelaVazia
            desenho={DesenhoDeCaixa}
            titulo="Nenhuma doação ainda"
            acao={
              <Link to="/app/doacoes/nova" className="botao principal">
                <Mais cor="#fff" /> Doar um medicamento
              </Link>
            }
          >
            Fotografe a caixa, escolha uma farmácia perto de você e leve lá. O
            resto do caminho você acompanha por aqui.
          </TelaVazia>
        )}

        {!carregando && doacoes.length > 0 && (
          <>
            {/* --- filtro --- */}
            <div className="filtro" role="tablist" aria-label="Filtrar doações">
              {ABAS.map(({ chave, nome }) => (
                <button
                  key={chave}
                  type="button"
                  role="tab"
                  aria-selected={aba === chave}
                  className={aba === chave ? 'marcado' : ''}
                  onClick={() => definirAba(chave)}
                >
                  {nome} <span className="conta">{contar(chave)}</span>
                </button>
              ))}
            </div>

            {daAba.length === 0 && (
              <p className="nada-no-filtro">
                Nenhuma doação {aba === 'andamento' ? 'em andamento' : aba === 'concluidas' ? 'concluída' : 'recusada'} por aqui.
              </p>
            )}

            {daAba.map((d) => (
              <Link
                key={d.codigo}
                to={`/app/doacoes/${d.codigo}`}
                className="item"
                style={{ display: 'block', textDecoration: 'none', color: 'inherit' }}
              >
                <Status status={d.status} />
                <h3 className="humano">{d.medicamento}</h3>
                <p className="detalhe">
                  {aba === 'concluidas'
                    ? `Validade ${data(d.validade)}`
                    : `${d.principioAtivo} · lote ${d.lote}`}
                </p>
                <p className="explicacao">{explicar(d.status)}</p>

                {d.pontoDeColeta && (
                  <div className="linha-icone">
                    <Lugar cor="var(--verde-escuro)" />
                    <span>{d.pontoDeColeta}</span>
                  </div>
                )}

                {/* Relógio para hora, alfinete para lugar. Trocar os dois faz a
                    pessoa ler o ícone errado antes de ler o texto. */}
                {d.agendadaPara && (
                  <div className="linha-icone">
                    <Relogio cor="var(--verde-escuro)" />
                    <span>{diaEHora(d.agendadaPara)}</span>
                  </div>
                )}
              </Link>
            ))}
          </>
        )}
      </div>

      {doacoes.length > 0 && (
        <div className="rodape-acao">
          <Link to="/app/doacoes/nova" className="botao principal">
            <Mais cor="#fff" /> Doar um medicamento
          </Link>
        </div>
      )}
    </>
  )
}
