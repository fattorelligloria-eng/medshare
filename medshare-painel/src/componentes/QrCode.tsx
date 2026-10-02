import { useMemo } from 'react'
import { gerarQr } from './qr'

/**
 * O codigo de retirada em QR, desenhado em SVG.
 *
 * SVG e nao canvas por tres motivos: imprime sem serrilhado, acompanha o tema
 * de alto contraste sozinho (as cores saem de currentColor) e nao precisa de
 * ref nem de efeito para aparecer.
 *
 * Os modulos viram um unico path em vez de centenas de <rect>: o navegador
 * desenha uma forma so, e o SVG impresso fica pequeno.
 */
export function QrCode({ valor, tamanho = 180 }: { valor: string; tamanho?: number }) {
  const { caminho, lado } = useMemo(() => {
    const matriz = gerarQr(valor)
    const partes: string[] = []
    matriz.forEach((linha, y) => {
      linha.forEach((escuro, x) => {
        if (escuro) partes.push(`M${x} ${y}h1v1h-1z`)
      })
    })
    return { caminho: partes.join(''), lado: matriz.length }
  }, [valor])

  const BORDA = 4 // a norma exige 4 modulos de area livre em volta

  return (
    <svg
      className="qr"
      width={tamanho}
      height={tamanho}
      viewBox={`${-BORDA} ${-BORDA} ${lado + BORDA * 2} ${lado + BORDA * 2}`}
      role="img"
      aria-label={`QR Code do código ${valor.split('').join(' ')}`}
      shapeRendering="crispEdges"
    >
      <rect
        x={-BORDA}
        y={-BORDA}
        width={lado + BORDA * 2}
        height={lado + BORDA * 2}
        fill="var(--qr-fundo, #FFFFFF)"
      />
      <path d={caminho} fill="var(--qr-tinta, #000000)" />
    </svg>
  )
}
