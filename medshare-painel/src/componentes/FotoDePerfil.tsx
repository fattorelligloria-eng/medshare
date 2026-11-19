/**
 * A foto da pessoa, ou o bonequinho enquanto ela não coloca uma.
 *
 * Sem iniciais de propósito: foi decisão da Glória. É decorativa para o leitor
 * de tela — o nome está sempre escrito ao lado.
 */
export function FotoDePerfil({ url, tamanho = 48 }: { url?: string | null; tamanho?: number }) {
  return (
    <span className="foto-de-perfil" style={{ width: tamanho, height: tamanho }} aria-hidden="true">
      {url ? (
        <img src={url} alt="" width={tamanho} height={tamanho} />
      ) : (
        <svg viewBox="0 0 64 64" width={tamanho} height={tamanho}>
          <circle cx="32" cy="25" r="11" fill="currentColor" />
          <path d="M10 64c0-13 10-22 22-22s22 9 22 22Z" fill="currentColor" />
        </svg>
      )}
    </span>
  )
}
