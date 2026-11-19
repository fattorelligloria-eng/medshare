import type { ReactNode } from 'react'
import { Seta } from './Icones'

/**
 * Uma linha que abre: dados do cadastro, senha, acessibilidade.
 *
 * É um <details> de verdade, e não um div que esconde coisas: abrir com Enter,
 * o leitor de tela anunciar "recolhido / expandido" e funcionar sem JavaScript
 * vêm de graça. A seta gira quando abre; quem prefere menos movimento não vê
 * a animação (regra no tema.css).
 */
export function SecaoRecolhivel({
  titulo,
  icone,
  className = '',
  children,
}: {
  titulo: string
  icone: ReactNode
  className?: string
  children: ReactNode
}) {
  return (
    <details className={`secao-recolhivel ${className}`.trim()}>
      <summary>
        {icone}
        {titulo}
        <span className="seta"><Seta /></span>
      </summary>
      <div className="dentro">{children}</div>
    </details>
  )
}
