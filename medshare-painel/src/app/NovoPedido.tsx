import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, ErroDaApi } from '../api/cliente'
import type { Medicamento, Necessidade } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { BuscaDeMedicamento } from '../componentes/BuscaDeMedicamento'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'

/** Entra na fila de um medicamento e emenda direto no envio da receita. */
export function NovoPedido() {
  const navegar = useNavigate()
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  async function pedir(medicamento: Medicamento) {
    definirErro(null)
    definirEnviando(true)
    try {
      const pedido = await api.post<Necessidade>('/necessidades', {
        medicamentoId: medicamento.id,
      })
      // Sem receita o pedido não anda, então já vai para ela.
      navegar(`/app/pedidos/${pedido.id}/receita`, { replace: true })
    } catch (e) {
      definirErro(e)
      definirEnviando(false)
    }
  }

  return (
    <>
      <CabecalhoInterno titulo="Do que você precisa?" para="/app/pedidos" />
      <div className="conteudo-app cresce" style={{ paddingTop: 22 }}>
        <AvisoDeErro erro={erro} />
        {erro instanceof ErroDaApi && erro.regra === 'RN08' && (
          // RN08 — sem CadÚnico confirmado não há pedido; o caminho é informar o NIS.
          <Link to="/app/cadunico" className="botao secundario" style={{ marginBottom: 18 }}>
            Informar meu NIS
          </Link>
        )}
        {enviando
          ? <p style={{ color: 'var(--tinta-fraca)', fontSize: 14 }}>Registrando…</p>
          : <BuscaDeMedicamento aoEscolher={pedir} />}
      </div>
    </>
  )
}
