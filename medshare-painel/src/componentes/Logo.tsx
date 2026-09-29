/** A marca de verdade: a cápsula partida ao meio, que é a ideia de dividir. */
export function Logo({ tamanho = 27 }: { tamanho?: number }) {
  return (
    <img
      src="/logo.png"
      alt=""
      width={tamanho}
      height={tamanho}
      style={{ borderRadius: tamanho * 0.3, display: 'block' }}
    />
  )
}

export function Marca({ tamanho = 27 }: { tamanho?: number }) {
  return (
    <div className="marca">
      <Logo tamanho={tamanho} />
      <span>MedShare</span>
    </div>
  )
}
