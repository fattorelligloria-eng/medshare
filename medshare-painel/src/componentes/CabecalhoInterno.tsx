import { useNavigate } from 'react-router-dom'
import { Voltar } from './Icones'

/**
 * Topo das telas internas do app: voltar + título.
 *
 * `aoVoltar` existe para o cadastro em passos, onde voltar significa ir ao
 * passo anterior e não sair da tela. Sem ele, o botão volta no histórico do
 * navegador, que é o comportamento certo em todo o resto do aplicativo.
 */
export function CabecalhoInterno({
  titulo,
  para,
  aoVoltar,
}: {
  titulo: string
  para?: string
  aoVoltar?: () => void
}) {
  const navegar = useNavigate()

  function voltar() {
    if (aoVoltar) aoVoltar()
    else if (para) navegar(para)
    else navegar(-1)
  }

  return (
    <div className="topo-app">
      <div className="cabecalho-app">
        <button type="button" className="icone-botao" aria-label="Voltar" onClick={voltar}>
          <Voltar />
        </button>
        <span style={{ fontSize: 15, fontWeight: 600 }}>{titulo}</span>
        <span style={{ width: 34 }} />
      </div>
    </div>
  )
}
