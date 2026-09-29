import { useNavigate } from 'react-router-dom'
import { Voltar } from './Icones'

/** Topo das telas internas do app: voltar + título. */
export function CabecalhoInterno({ titulo, para }: { titulo: string; para?: string }) {
  const navegar = useNavigate()
  return (
    <div className="topo-app">
      <div className="cabecalho-app">
        <button
          type="button"
          className="icone-botao"
          aria-label="Voltar"
          onClick={() => (para ? navegar(para) : navegar(-1))}
        >
          <Voltar />
        </button>
        <span style={{ fontSize: 15, fontWeight: 600 }}>{titulo}</span>
        <span style={{ width: 34 }} />
      </div>
    </div>
  )
}
