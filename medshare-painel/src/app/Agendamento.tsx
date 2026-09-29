import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { PontoDeColeta } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'

const FUSO = 'America/Sao_Paulo'
const DIA = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, weekday: 'short', day: '2-digit', month: '2-digit' })
const HORA = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, hour: '2-digit', minute: '2-digit' })
const CHAVE_DO_DIA = new Intl.DateTimeFormat('en-CA', { timeZone: FUSO, year: 'numeric', month: '2-digit', day: '2-digit' })

/**
 * UC02 — escolha da farmácia e do horário para entregar a caixa.
 *
 * A lista de farmácias vem ordenada por distância do endereço cadastrado, e
 * os horários são só os que a farmácia atende e ainda têm vaga: a pessoa não
 * consegue marcar para um domingo em que a farmácia está fechada. A mesma
 * tela serve para o reagendamento único (A2).
 */
export function Agendamento() {
  const { codigo = '' } = useParams()
  const navegar = useNavigate()
  const [pontos, definirPontos] = useState<PontoDeColeta[]>([])
  const [escolhido, definirEscolhido] = useState<number | null>(null)
  const [horarios, definirHorarios] = useState<string[]>([])
  const [carregandoHorarios, definirCarregandoHorarios] = useState(false)
  const [dia, definirDia] = useState<string | null>(null)
  const [quando, definirQuando] = useState<string | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  useEffect(() => {
    api.get<PontoDeColeta[]>('/pontos-de-coleta/proximos')
      .then(definirPontos)
      .catch(definirErro)
  }, [])

  useEffect(() => {
    if (escolhido === null) return
    definirCarregandoHorarios(true)
    definirQuando(null)
    api.get<string[]>(`/pontos-de-coleta/${escolhido}/horarios`)
      .then((lista) => {
        definirHorarios(lista)
        definirDia(lista.length > 0 ? CHAVE_DO_DIA.format(new Date(lista[0])) : null)
      })
      .catch(definirErro)
      .finally(() => definirCarregandoHorarios(false))
  }, [escolhido])

  const porDia = useMemo(() => {
    const grupos = new Map<string, string[]>()
    for (const h of horarios) {
      const chave = CHAVE_DO_DIA.format(new Date(h))
      grupos.set(chave, [...(grupos.get(chave) ?? []), h])
    }
    return grupos
  }, [horarios])

  async function confirmar() {
    if (escolhido === null || !quando) return
    definirErro(null)
    definirEnviando(true)
    try {
      await api.post(`/doacoes/${codigo}/agendamento`, { pontoDeColetaId: escolhido, dataHora: quando })
      navegar(`/app/doacoes/${codigo}`, { replace: true })
    } catch (e) {
      definirErro(e)
      definirEnviando(false)
    }
  }

  return (
    <>
      <CabecalhoInterno titulo="Agendar entrega" />

      <div className="conteudo-app" style={{ paddingTop: 22, flex: 1 }}>
        <AvisoDeErro erro={erro} />

        <h1 className="humano" style={{ fontSize: 25 }}>Onde você vai entregar?</h1>
        <p style={{ margin: '9px 0 20px', fontSize: 14, color: 'var(--tinta-media)', lineHeight: 1.5 }}>
          As mais próximas do seu endereço aparecem primeiro.
        </p>

        {pontos.map((p) => (
          <label key={p.id} className={`escolha ${escolhido === p.id ? 'marcada' : ''}`}>
            <input
              type="radio"
              name="ponto"
              checked={escolhido === p.id}
              onChange={() => definirEscolhido(p.id)}
            />
            <span className="titulo">{p.nome}</span>
            <p className="sub">{p.endereco} — {p.bairro}, {p.municipio}</p>
            <p className="sub" style={{ marginTop: 4 }}>{p.horario}</p>
          </label>
        ))}

        {escolhido !== null && (
          <div style={{ marginTop: 22 }}>
            <p className="rotulo">Quando você vai levar?</p>
            {carregandoHorarios && <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Carregando horários…</p>}
            {!carregandoHorarios && horarios.length === 0 && (
              <p style={{ fontSize: 14, color: 'var(--tinta-media)' }}>
                Esta farmácia não tem horário livre nas próximas duas semanas. Escolha outra.
              </p>
            )}
            {porDia.size > 0 && (
              <>
                <div className="dias" style={{ marginTop: 8 }}>
                  {[...porDia.keys()].map((chave) => (
                    <button
                      key={chave}
                      type="button"
                      className={dia === chave ? 'marcado' : ''}
                      onClick={() => { definirDia(chave); definirQuando(null) }}
                    >
                      {DIA.format(new Date(porDia.get(chave)![0]))}
                    </button>
                  ))}
                </div>
                <div className="horarios">
                  {(dia ? porDia.get(dia) ?? [] : []).map((h) => (
                    <button
                      key={h}
                      type="button"
                      className={quando === h ? 'marcado' : ''}
                      onClick={() => definirQuando(h)}
                    >
                      {HORA.format(new Date(h))}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 26 }}>
        <button className="principal" disabled={escolhido === null || !quando || enviando} onClick={confirmar}>
          {enviando ? 'Confirmando…' : 'Confirmar agendamento'}
        </button>
      </div>
    </>
  )
}
