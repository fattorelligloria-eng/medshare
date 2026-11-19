import { useId } from 'react'

/**
 * A marca: a cápsula partida ao meio, que é a ideia de dividir.
 *
 * Desenhada em SVG, e não mais o PNG: o "vidro" (aro claro, reflexo no canto)
 * precisa ficar nítido do ícone do menu ao logo grande da entrada, e o PNG
 * borrava nos tamanhos pequenos.
 *
 * Cada logo leva seus próprios degradês. Com dois logos na mesma tela (menu
 * lateral e topo), ids repetidos fariam o segundo pintar com o degradê do
 * primeiro — e, se o primeiro estiver escondido, com nada. O useId resolve.
 */
export function Logo({ tamanho = 40 }: { tamanho?: number }) {
  const id = useId().replace(/:/g, '')
  const aro = `${id}-aro`
  const fundo = `${id}-fundo`
  const brilho = `${id}-brilho`
  const menta = `${id}-menta`
  const branca = `${id}-branca`

  return (
    <svg className="logo" viewBox="0 0 120 120" width={tamanho} height={tamanho} aria-hidden="true">
      <defs>
        <linearGradient id={aro} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity="0.95" />
          <stop offset="0.5" stopColor="#9FD9C2" stopOpacity="0.45" />
          <stop offset="1" stopColor="#FFFFFF" stopOpacity="0.8" />
        </linearGradient>
        <linearGradient id={fundo} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#2BD39A" />
          <stop offset="0.55" stopColor="#0A8568" />
          <stop offset="1" stopColor="#05473A" />
        </linearGradient>
        <linearGradient id={brilho} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#FFFFFF" stopOpacity="0.45" />
          <stop offset="0.5" stopColor="#FFFFFF" stopOpacity="0" />
        </linearGradient>
        <linearGradient id={menta} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#2FB386" />
          <stop offset="1" stopColor="#9AF2CE" />
        </linearGradient>
        <linearGradient id={branca} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#CFE6DB" />
          <stop offset="1" stopColor="#FFFFFF" />
        </linearGradient>
      </defs>
      <rect x="2" y="2" width="116" height="116" rx="32" fill={`url(#${aro})`} />
      <rect x="10" y="10" width="100" height="100" rx="25" fill={`url(#${fundo})`} />
      <path d="M10 35a25 25 0 0 1 25-25h50L10 85Z" fill={`url(#${brilho})`} />
      <g transform="rotate(-45 60 60)">
        <path d="M24 60a16 16 0 0 1 16-16h18v32H40a16 16 0 0 1-16-16Z" fill={`url(#${branca})`} />
        <path d="M62 44h18a16 16 0 0 1 0 32H62Z" fill={`url(#${menta})`} />
      </g>
    </svg>
  )
}

/** Logo e nome: "Med" reto, "Share" em itálico — como no desenho aprovado. */
export function Marca({ tamanho = 40 }: { tamanho?: number }) {
  return (
    <div className="marca">
      <Logo tamanho={tamanho} />
      <span>Med<em>Share</em></span>
    </div>
  )
}
