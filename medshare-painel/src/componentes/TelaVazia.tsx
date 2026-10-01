import type { ReactNode } from 'react'

/**
 * A tela de quem ainda não tem nada.
 *
 * É a primeira coisa que uma pessoa vê depois de criar a conta, e o momento em
 * que o aplicativo está mais vazio do que jamais estará de novo. Texto cinza
 * solto no meio do branco faz a pessoa achar que algo quebrou.
 *
 * Então: um desenho, um título, uma frase do que acontece aqui, e a ação logo
 * abaixo — tudo junto, para a pessoa não ter que procurar o que fazer.
 */
export function TelaVazia(
  { desenho, titulo, children, acao }:
  { desenho: ReactNode; titulo: string; children: ReactNode; acao?: ReactNode },
) {
  return (
    <div className="tela-vazia">
      <div className="desenho" aria-hidden="true">{desenho}</div>
      <h3 className="humano">{titulo}</h3>
      <p>{children}</p>
      {acao && <div className="acao">{acao}</div>}
    </div>
  )
}

/* ---------------------------------------------------------------------------
   Os desenhos.

   Geométricos de propósito, no mesmo traço dos ícones: linha de 1,5, cantos
   arredondados, o verde da marca só no detalhe que importa. Ilustração
   desenhada à mão ficaria bonita e destoaria de todo o resto.
   ------------------------------------------------------------------------- */

export const DesenhoDeCaixa = (
  <svg width="104" height="104" viewBox="0 0 104 104" fill="none">
    <rect x="22" y="38" width="60" height="44" rx="7"
          stroke="var(--verde)" strokeWidth="2.5" />
    <path d="M22 52h60" stroke="var(--verde)" strokeWidth="2.5" />
    <rect x="44" y="38" width="16" height="14" rx="3" fill="var(--verde-claro)" />
    {/* a cápsula saindo da caixa: é disto que o aplicativo trata */}
    <rect x="58" y="16" width="24" height="13" rx="6.5"
          transform="rotate(-24 58 16)" stroke="var(--verde)" strokeWidth="2.5" />
    <path d="M64.5 24.5l10-4.5" stroke="var(--verde)" strokeWidth="2.5" />
  </svg>
)

export const DesenhoDeReceita = (
  <svg width="104" height="104" viewBox="0 0 104 104" fill="none">
    <rect x="28" y="18" width="48" height="68" rx="7"
          stroke="var(--verde)" strokeWidth="2.5" />
    <path d="M40 38h24M40 50h24M40 62h14" stroke="var(--verde)" strokeWidth="2.5"
          strokeLinecap="round" />
    <circle cx="70" cy="70" r="15" fill="var(--branco)" />
    <circle cx="70" cy="70" r="13.5" stroke="var(--verde)" strokeWidth="2.5" />
    <path d="M64 70l4.5 4.5L77 66" stroke="var(--verde)" strokeWidth="2.5"
          strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export const DesenhoDeBusca = (
  <svg width="104" height="104" viewBox="0 0 104 104" fill="none">
    <circle cx="46" cy="46" r="22" stroke="var(--verde)" strokeWidth="2.5" />
    <path d="M62 62l18 18" stroke="var(--verde)" strokeWidth="2.5" strokeLinecap="round" />
    <path d="M38 46h16M46 38v16" stroke="var(--verde-claro)" strokeWidth="3"
          strokeLinecap="round" />
  </svg>
)
