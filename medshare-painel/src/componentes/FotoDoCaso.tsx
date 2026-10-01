import { useState } from 'react'

/**
 * A foto que o doador enviou, para quem confere a caixa.
 *
 * O link é assinado e vence em 15 minutos; se a tela ficou aberta além disso,
 * ou o arquivo sumiu, mostramos um aviso no lugar do ícone de imagem quebrada.
 * Com foto, abre em tamanho cheio numa aba nova.
 */
export function FotoDoCaso({ url, descricao }: { url: string; descricao: string }) {
  const [falhou, setFalhou] = useState(false)

  if (falhou) {
    return (
      <div className="foto-caso foto-indisponivel" role="img" aria-label={descricao}>
        Foto indisponível — recarregue a página para tentar de novo
      </div>
    )
  }

  return (
    <a href={url} target="_blank" rel="noreferrer">
      <img className="foto-caso" src={url} alt={descricao} onError={() => setFalhou(true)} />
    </a>
  )
}
