import type { ReactNode } from 'react'
import { ErroDaApi } from '../api/cliente'
import { rotuloDaRegra } from './Regras'

/**
 * O erro chega da API com a regra separada da mensagem, e é assim que aparece:
 * o nome da regra em destaque, a explicação em texto corrido.
 *
 * A etiqueta mostrava a sigla crua — "RN01", "RN08". A sigla é do nosso
 * documento de modelagem, não do vocabulário de quem usa: ninguém no balcão
 * decorou os números das nossas regras. Agora passa pelo mapa de Regras.ts e
 * sai em português.
 */
export function AvisoDeErro(
  { erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo?: () => void },
) {
  if (!erro) return null
  const regra = rotuloDaRegra(erro instanceof ErroDaApi ? erro.regra : undefined)
  const mensagem = erro instanceof Error ? erro.message : String(erro)

  return (
    <div className="aviso erro" role="alert">
      {regra && <span className="regra">{regra}</span>}
      {mensagem}
      {/* Erro de rede sem saída é beco: a pessoa lê, entende, e não tem o que
          fazer. O botão só aparece quando quem chamou sabe como repetir. */}
      {aoTentarDeNovo && (
        <div style={{ marginTop: 12 }}>
          <button type="button" className="secundario" onClick={aoTentarDeNovo}>
            Tentar de novo
          </button>
        </div>
      )}
    </div>
  )
}

export function AvisoDeSucesso({ children }: { children: ReactNode }) {
  return <div className="aviso ok" role="status">{children}</div>
}
