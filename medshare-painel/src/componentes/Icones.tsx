/**
 * Ícones desenhados em traço, no mesmo peso.
 *
 * Emoji foi descartado de propósito: cada sistema desenha o seu de um jeito, o
 * que quebraria a unidade visual justamente na hora da apresentação.
 */
type Props = { tamanho?: number; cor?: string }

const base = (tamanho: number, cor: string) => ({
  width: tamanho,
  height: tamanho,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: cor,
  strokeWidth: 2,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
})

export const Sino = ({ tamanho = 20, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
    <path d="M13.7 21a2 2 0 0 1-3.4 0" />
  </svg>
)

export const Confere = ({ tamanho = 18, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}><path d="M20 6 9 17l-5-5" /></svg>
)

export const Mais = ({ tamanho = 18, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}><path d="M12 5v14M5 12h14" /></svg>
)

export const Lugar = ({ tamanho = 14, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <path d="M21 10c0 7-9 12-9 12s-9-5-9-12a9 9 0 0 1 18 0z" />
    <circle cx="12" cy="10" r="3" />
  </svg>
)

export const Caixa = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <path d="M21 16V8a2 2 0 0 0-1-1.7l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.7l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
  </svg>
)

export const Lupa = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" />
  </svg>
)

export const Pessoa = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <path d="M20 21a8 8 0 1 0-16 0" /><circle cx="12" cy="7" r="4" />
  </svg>
)

export const Receita = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
    <path d="M14 2v6h6M9 13h6M9 17h4" />
  </svg>
)

export const Senha = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <rect x="3" y="11" width="18" height="10" rx="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
)

export const Voltar = ({ tamanho = 20, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}><path d="M19 12H5M12 19l-7-7 7-7" /></svg>
)

export const Camera = ({ tamanho = 20, cor = 'currentColor' }: Props) => (
  <svg {...base(tamanho, cor)}>
    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
    <circle cx="12" cy="13" r="4" />
  </svg>
)

export const Relogio = ({ tamanho = 14, cor = 'currentColor' }: Props) => (
  <svg width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none"
       stroke={cor} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="9" />
    <path d="M12 7v5l3 2" />
  </svg>
)

export const Farmacia = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none"
       stroke={cor} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M4 9h16v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9Z" />
    <path d="M3 9l2-4h14l2 4" />
    <path d="M12 12v6M9 15h6" />
  </svg>
)

export const Duvida = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none"
       stroke={cor} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="9" />
    <path d="M9.5 9.5a2.6 2.6 0 1 1 3.3 2.5c-.6.2-.8.7-.8 1.3v.3" />
    <path d="M12 17h.01" />
  </svg>
)

export const Informacao = ({ tamanho = 21, cor = 'currentColor' }: Props) => (
  <svg width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none"
       stroke={cor} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="9" />
    <path d="M12 11v5" />
    <path d="M12 8h.01" />
  </svg>
)
