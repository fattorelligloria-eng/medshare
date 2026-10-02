import { useEffect, useRef, useState } from 'react'
import { Copiar, Confere } from './Icones'

/**
 * O codigo de retirada, grande, com um botao que copia.
 *
 * O aviso de "copiado" sai num elemento com role="status": leitor de tela
 * anuncia a troca sem tirar o foco do botao, que e o que a pessoa espera
 * depois de apertar. O timer e limpo no desmonte para nao tentar mexer num
 * componente que ja saiu da tela.
 *
 * navigator.clipboard so existe em contexto seguro (https ou localhost). Fora
 * dele cai no caminho antigo com execCommand, que ainda e o que funciona num
 * celular acessando o servidor pelo IP da rede local — exatamente o cenario da
 * demonstracao em sala.
 */
export function CodigoCopiavel({ codigo, rotulo = 'Código de entrega' }: { codigo: string; rotulo?: string }) {
  const [copiado, definirCopiado] = useState(false)
  const relogio = useRef<number | undefined>(undefined)

  useEffect(() => () => window.clearTimeout(relogio.current), [])

  async function copiar() {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(codigo)
      } else {
        const campo = document.createElement('textarea')
        campo.value = codigo
        campo.setAttribute('readonly', '')
        campo.style.position = 'fixed'
        campo.style.opacity = '0'
        document.body.appendChild(campo)
        campo.select()
        document.execCommand('copy')
        document.body.removeChild(campo)
      }
      definirCopiado(true)
      window.clearTimeout(relogio.current)
      relogio.current = window.setTimeout(() => definirCopiado(false), 2200)
    } catch {
      // Copiar falhou: o codigo continua na tela para ser lido em voz alta.
      definirCopiado(false)
    }
  }

  return (
    <div className="codigo-grande">
      <p className="rotulo">{rotulo}</p>
      <div className="codigo-linha">
        <p className="valor">{codigo}</p>
        <button
          type="button"
          className="icone-botao"
          onClick={copiar}
          aria-label={`Copiar o código ${codigo.split('').join(' ')}`}
        >
          {copiado ? <Confere /> : <Copiar />}
        </button>
      </div>
      <p className="so-leitor-de-tela" role="status">
        {copiado ? 'Código copiado.' : ''}
      </p>
      <span className={copiado ? 'copiado visivel' : 'copiado'} aria-hidden="true">
        Copiado
      </span>
    </div>
  )
}
