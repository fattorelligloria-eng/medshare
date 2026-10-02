import { useEffect, useState } from 'react'
import { Contraste, Letra, Menos, Mais } from './Icones'

/**
 * Tamanho da fonte e alto contraste, juntos.
 *
 * Ficam juntos porque quem mexe num costuma precisar do outro: a mesma pessoa
 * que aumenta a letra e a que reclama de cinza claro sobre branco.
 *
 * O ajuste é feito na raiz do documento, não neste componente: mudar a fonte
 * do <html> move junto tudo que estiver em rem.
 *
 * É por causa deste botão que o CSS do aplicativo está em rem. Ele nasceu
 * mexendo no <html> enquanto os 73 tamanhos do estilos.css e os 68 em linha
 * nos componentes ainda estavam em px — ou seja, apertar mais e menos não
 * mudava nada, porque não havia nada para mudar. Se um tamanho novo entrar em
 * px, ele simplesmente deixa de obedecer a este controle, sem erro nenhum.
 * A conta é tamanho em px dividido por 16.
 *
 * A escolha fica no navegador da pessoa, não na conta: é preferência daquele
 * aparelho. Em aba anônima ou com o armazenamento bloqueado, ler e gravar
 * podem lançar exceção, então as duas pontas estão protegidas e o padrão
 * continua valendo.
 */

const CHAVE_FONTE = 'medshare.fonte'
const CHAVE_CONTRASTE = 'medshare.contraste'

/** 100% é o tamanho do sistema; o teto evita quebrar a tela do balcão. */
const NIVEIS = [100, 112.5, 125, 137.5, 150]

function lerGuardado(chave: string): string | null {
  try {
    return window.localStorage.getItem(chave)
  } catch {
    return null
  }
}

function guardar(chave: string, valor: string) {
  try {
    window.localStorage.setItem(chave, valor)
  } catch {
    // Sem armazenamento a preferência vale só para esta visita. Tudo bem.
  }
}

export function Acessibilidade() {
  const [nivel, definirNivel] = useState(() => {
    const guardado = Number(lerGuardado(CHAVE_FONTE))
    const indice = NIVEIS.indexOf(guardado)
    return indice >= 0 ? indice : 0
  })
  const [altoContraste, definirAltoContraste] = useState(
    () => lerGuardado(CHAVE_CONTRASTE) === 'sim',
  )

  useEffect(() => {
    document.documentElement.style.fontSize = `${NIVEIS[nivel]}%`
    guardar(CHAVE_FONTE, String(NIVEIS[nivel]))
  }, [nivel])

  useEffect(() => {
    document.documentElement.dataset.contraste = altoContraste ? 'alto' : 'normal'
    guardar(CHAVE_CONTRASTE, altoContraste ? 'sim' : 'nao')
  }, [altoContraste])

  const porcentagem = NIVEIS[nivel]

  return (
    <div className="acessibilidade" role="group" aria-label="Acessibilidade">
      <button
        type="button"
        className="icone-botao"
        onClick={() => definirNivel((n) => Math.max(0, n - 1))}
        disabled={nivel === 0}
        aria-label="Diminuir o tamanho da letra"
      >
        <Menos tamanho={15} />
      </button>

      <span className="medida-da-fonte" aria-hidden="true">
        <Letra tamanho={15} />
      </span>

      <button
        type="button"
        className="icone-botao"
        onClick={() => definirNivel((n) => Math.min(NIVEIS.length - 1, n + 1))}
        disabled={nivel === NIVEIS.length - 1}
        aria-label="Aumentar o tamanho da letra"
      >
        <Mais tamanho={15} />
      </button>

      <button
        type="button"
        className={altoContraste ? 'icone-botao ligado' : 'icone-botao'}
        onClick={() => definirAltoContraste((v) => !v)}
        aria-pressed={altoContraste}
        aria-label="Alto contraste"
      >
        <Contraste tamanho={15} />
      </button>

      <p className="so-leitor-de-tela" role="status">
        Letra em {porcentagem}%. Alto contraste {altoContraste ? 'ligado' : 'desligado'}.
      </p>
    </div>
  )
}
