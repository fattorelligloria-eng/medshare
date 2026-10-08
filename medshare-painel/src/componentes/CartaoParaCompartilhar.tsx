import { useCallback, useEffect, useRef, useState } from 'react'
import { Compartilhar, Imprimir } from './Icones'

/**
 * O cartão de "eu doei", desenhado em canvas e salvo como imagem.
 *
 * É assim que uma rede de doação cresce: pelo orgulho de quem doou. Mas o
 * cartão sai do aplicativo e vai para o WhatsApp da família, então ele é o
 * lugar mais fácil de vazar alguma coisa sem querer.
 *
 * Por isso a regra aqui é dura: **o cartão não carrega nenhum dado de
 * ninguém.** Nem o nome de quem doou, nem o código da doação, nem a farmácia,
 * nem a data. Código e farmácia parecem inofensivos e não são — com o código
 * dá para cruzar com o histórico, e a farmácia diz o bairro de alguém. O que
 * entra é só: a marca, o princípio ativo e a frase. Nada mais.
 *
 * Canvas e não captura de tela do DOM: assim o que vai para a imagem é
 * exatamente o que este arquivo desenha, e não o que estiver na tela por
 * volta. Uma biblioteca de screenshot capturaria o que estivesse atrás.
 */

const LADO = 1080

type Props = {
  /** O princípio ativo. Nunca o nome de quem doou. */
  principioAtivo: string
  /** Quantas caixas. Só o número, sem código nenhum. */
  caixas?: number
}

export function CartaoParaCompartilhar({ principioAtivo, caixas = 1 }: Props) {
  const tela = useRef<HTMLCanvasElement | null>(null)
  const [pronto, definirPronto] = useState(false)
  const [podeCompartilhar, definirPodeCompartilhar] = useState(false)

  useEffect(() => {
    definirPodeCompartilhar(
      typeof navigator !== 'undefined' && typeof navigator.canShare === 'function',
    )
  }, [])

  const desenhar = useCallback(async () => {
    const canvas = tela.current
    if (!canvas) return
    const c = canvas.getContext('2d')
    if (!c) return

    // Sem esperar as fontes, o canvas desenha com a fonte do sistema e o
    // cartão sai com outra cara — o navegador não espera por nós.
    try {
      await document.fonts.ready
    } catch {
      // Navegador sem a API: segue com o que houver.
    }

    c.fillStyle = '#07614B'
    c.fillRect(0, 0, LADO, LADO)

    // Cápsula enorme, bem clara, como marca d'água no canto. Fica acima da
    // faixa de baixo de propósito: cruzada por ela, parecia defeito.
    c.save()
    c.globalAlpha = 0.07
    c.strokeStyle = '#FFFFFF'
    c.lineWidth = 26
    c.beginPath()
    c.roundRect(LADO - 300, LADO - 430, 420, 240, 120)
    c.stroke()
    c.restore()

    c.fillStyle = '#FFFFFF'
    c.textBaseline = 'alphabetic'

    c.font = '700 40px "Libre Franklin", system-ui, sans-serif'
    c.fillText('MedShare', 90, 150)

    c.font = '600 92px Fraunces, Georgia, serif'
    const titulo = caixas === 1 ? 'Doei um medicamento' : `Doei ${caixas} medicamentos`
    envolver(c, titulo, 90, 360, LADO - 180, 104)

    c.globalAlpha = 0.82
    c.font = '400 40px "Libre Franklin", system-ui, sans-serif'
    envolver(c, `${principioAtivo} que sobrou do meu tratamento e ia vencer na gaveta.`,
      90, 560, LADO - 180, 56)
    c.globalAlpha = 1

    // Faixa de baixo
    c.fillStyle = 'rgba(255,255,255,0.14)'
    c.fillRect(0, LADO - 170, LADO, 170)
    c.fillStyle = '#FFFFFF'
    c.font = '600 36px "Libre Franklin", system-ui, sans-serif'
    c.fillText('Remédio de alto custo que sobrou', 90, LADO - 100)
    c.globalAlpha = 0.8
    c.font = '400 34px "Libre Franklin", system-ui, sans-serif'
    c.fillText('pode ser o tratamento de outra pessoa.', 90, LADO - 50)
    c.globalAlpha = 1

    definirPronto(true)
  }, [principioAtivo, caixas])

  useEffect(() => { void desenhar() }, [desenhar])

  function comoArquivo(): Promise<File | null> {
    return new Promise((resolver) => {
      tela.current?.toBlob((blob) => {
        resolver(blob ? new File([blob], 'medshare.png', { type: 'image/png' }) : null)
      }, 'image/png')
    })
  }

  async function baixar() {
    const arquivo = await comoArquivo()
    if (!arquivo) return
    const endereco = URL.createObjectURL(arquivo)
    const link = document.createElement('a')
    link.href = endereco
    link.download = 'medshare.png'
    link.click()
    // Sem revogar, o blob fica na memória até a aba fechar.
    setTimeout(() => URL.revokeObjectURL(endereco), 1000)
  }

  async function compartilhar() {
    const arquivo = await comoArquivo()
    if (!arquivo) return
    // canShare com o arquivo em mãos: alguns navegadores anunciam share e
    // recusam arquivo. Perguntar antes evita o erro na cara da pessoa.
    if (navigator.canShare?.({ files: [arquivo] })) {
      try {
        await navigator.share({ files: [arquivo], title: 'MedShare' })
        return
      } catch {
        // Fechou a folha de compartilhamento. Não é erro.
        return
      }
    }
    await baixar()
  }

  return (
    <div className="cartao-compartilhar">
      <canvas
        ref={tela}
        width={LADO}
        height={LADO}
        className="previa-do-cartao"
        role="img"
        aria-label={`Cartão para compartilhar: doei um medicamento, ${principioAtivo}`}
      />

      <div className="acoes-do-cartao">
        {podeCompartilhar && (
          <button type="button" className="botao secundario" disabled={!pronto} onClick={compartilhar}>
            <Compartilhar /> Compartilhar
          </button>
        )}
        <button type="button" className="botao secundario" disabled={!pronto} onClick={baixar}>
          <Imprimir /> Baixar imagem
        </button>
      </div>

      <p className="aviso-do-cartao">
        A imagem não leva seu nome, o código da doação nem a farmácia.
      </p>
    </div>
  )
}

/** Quebra o texto na largura disponível, porque canvas não quebra sozinho. */
function envolver(
  c: CanvasRenderingContext2D,
  texto: string,
  x: number,
  y: number,
  largura: number,
  altura: number,
) {
  const palavras = texto.split(' ')
  let linha = ''
  let altura_atual = y

  for (const palavra of palavras) {
    const tentativa = linha ? `${linha} ${palavra}` : palavra
    if (c.measureText(tentativa).width > largura && linha) {
      c.fillText(linha, x, altura_atual)
      linha = palavra
      altura_atual += altura
    } else {
      linha = tentativa
    }
  }
  if (linha) c.fillText(linha, x, altura_atual)
}
