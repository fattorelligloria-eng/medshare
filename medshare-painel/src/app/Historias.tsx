import { Link } from 'react-router-dom'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'

/**
 * Histórias de quem foi ajudado — ainda não existe, e a tela diz isso.
 *
 * A faixa do Início chega aqui. Melhor uma tela honesta do que uma lista
 * inventada: história de saúde só pode aparecer com autorização de quem conta,
 * sem nada que identifique a pessoa, e depois de a central revisar. Essas três
 * coisas precisam existir antes da primeira história.
 */
export function Historias() {
  return (
    <>
      <CabecalhoInterno titulo="Histórias" para="/app/inicio" />
      <div className="conteudo-app cresce" style={{ paddingTop: 24 }}>
        <h1 className="humano" style={{ fontSize: '1.625rem' }}>Histórias de quem foi ajudado</h1>
        <p className="texto-sobre">
          Em breve, esta tela vai reunir relatos de quem voltou ao tratamento por
          causa de uma caixa doada.
        </p>
        <p className="texto-sobre">
          Cada história só vai aparecer com a autorização de quem conta, sem nome
          e sem nada que identifique a pessoa, e depois de revisada pela central.
          Quem doa e quem recebe continuam sem se conhecer.
        </p>
        <p style={{ marginTop: 24 }}>
          <Link to="/app/inicio">Voltar para o início</Link>
        </p>
      </div>
    </>
  )
}
