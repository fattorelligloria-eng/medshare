import type { ReactNode } from 'react'
import { ErroDaApi } from '../api/cliente'

/**
 * O erro chega da API com a regra separada da mensagem — e é assim que aparece:
 * a sigla da regra em destaque, a explicação em texto corrido. Quem está no
 * balcão lê "RN01" e já sabe do que se trata; a frase ao lado explica a quem
 * não sabe.
 */
export function AvisoDeErro(
  { erro, aoTentarDeNovo }: { erro: unknown; aoTentarDeNovo?: () => void },
) {
  if (!erro) return null
  const regra = erro instanceof ErroDaApi ? erro.regra : undefined
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
