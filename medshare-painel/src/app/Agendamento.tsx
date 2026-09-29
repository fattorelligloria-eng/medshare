import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { PontoDeColeta } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'

/**
 * Escolha da farmácia e do horário para entregar a caixa.
 *
 * A lista vem ordenada por distância do endereço cadastrado — quem depende de
 * transporte público não deveria atravessar a cidade para doar.
 */
export function Agendamento() {
  const { codigo = '' } = useParams()
  const navegar = useNavigate()
  const [pontos, definirPontos] = useState<PontoDeColeta[]>([])
  const [escolhido, definirEscolhido] = useState<number | null>(null)
  const [quando, definirQuando] = useState('')
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  useEffect(() => {
    api.get<PontoDeColeta[]>('/pontos-de-coleta/proximos')
      .then(definirPontos)
      .catch(definirErro)
  }, [])

  async function confirmar() {
    if (escolhido === null || !quando) return
    definirErro(null)
    definirEnviando(true)
    try {
      await api.post(`/doacoes/${codigo}/agendamento`, {
        pontoDeColetaId: escolhido,
        dataHora: new Date(quando).toISOString(),
      })
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

        <label className="campo" style={{ marginTop: 20 }}>
          <span>Quando você vai levar?</span>
          <input
            type="datetime-local"
            value={quando}
            onChange={(e) => definirQuando(e.target.value)}
          />
          <span className="apoio">Respeite o horário de funcionamento da farmácia.</span>
        </label>
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 26 }}>
        <button
          className="principal"
          disabled={escolhido === null || !quando || enviando}
          onClick={confirmar}
        >
          {enviando ? 'Confirmando…' : 'Confirmar agendamento'}
        </button>
      </div>
    </>
  )
}
