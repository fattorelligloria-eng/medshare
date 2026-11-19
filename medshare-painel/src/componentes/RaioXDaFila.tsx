import { useState } from 'react'
import type { RaioXDaFila as Dados } from '../api/tipos'

/**
 * O que dá para dizer com honestidade sobre a espera.
 *
 * O que esta caixa NÃO mostra é o mais importante: a posição na fila. Ela não
 * existe neste sistema. O emparelhamento ordena por prioridade, depois pela
 * faixa de distância até a farmácia onde a caixa apareceu, e só então pelo
 * tempo de espera — a mesma pessoa é a segunda para uma caixa que chega perto
 * dela e a nona para uma que chega do outro lado da cidade.
 *
 * Um "você é o 3º" que sobe e desce sozinho, por um motivo invisível, é pior
 * do que nenhum número: ensina a pessoa a desconfiar da tela. Então vão os
 * fatos estáveis, e um texto curto explicando como a ordem funciona de
 * verdade. Quem espera por um remédio caro merece saber a regra do jogo.
 */
/** "hoje", "ontem", "há 9 dias" — nunca "há 9 dia(s)". */
function haQuantoTempo(dias: number): string {
  if (dias <= 0) return 'hoje'
  if (dias === 1) return 'ontem'
  return `há ${dias} dias`
}

export function RaioXDaFila({ dados }: { dados: Dados }) {
  const [aberto, definirAberto] = useState(false)

  const frequencia =
    dados.caixasRecentes === 0
      ? `Nenhuma caixa deste princípio ativo apareceu nos últimos ${dados.diasDaJanela} dias.`
      : dados.caixasRecentes === 1
        ? `1 caixa apareceu nos últimos ${dados.diasDaJanela} dias.`
        : `${dados.caixasRecentes} caixas apareceram nos últimos ${dados.diasDaJanela} dias.`

  return (
    <div className="raio-x">
      {dados.prioridade && (
        <p className="selo-prioridade">
          Seu pedido está na frente da fila, porque você perdeu uma caixa por validade.
        </p>
      )}

      <dl className="numeros-da-fila">
        <div>
          <dt>Esperando por este medicamento</dt>
          <dd>{dados.pessoasNaFila === 1 ? 'só você' : `${dados.pessoasNaFila} pessoas`}</dd>
        </div>
        <div>
          <dt>Você espera há</dt>
          <dd>{dados.diasEsperando === 0 ? 'hoje' : dados.diasEsperando === 1 ? '1 dia' : `${dados.diasEsperando} dias`}</dd>
        </div>
        {dados.entraramDepoisDeVoce > 0 && (
          <div>
            <dt>Entraram depois de você</dt>
            <dd>{dados.entraramDepoisDeVoce}</dd>
          </div>
        )}
      </dl>

      <p className="frequencia">
        {frequencia}
        {dados.diasDesdeAUltima !== null && <> A última, {haQuantoTempo(dados.diasDesdeAUltima)}.</>}
      </p>

      <div className={aberto ? 'como-funciona aberta' : 'como-funciona'}>
        <button type="button" aria-expanded={aberto} onClick={() => definirAberto(!aberto)}>
          Como a ordem é decidida
          <span className="seta" aria-hidden="true">{aberto ? '−' : '+'}</span>
        </button>
        {aberto && (
          <div className="explicacao-da-ordem">
            <p>Quando uma caixa fica disponível, ela é oferecida a uma pessoa só. A escolha segue três critérios, nesta ordem:</p>
            <ol>
              <li><strong>Quem já perdeu uma caixa por validade</strong> passa à frente.</li>
              <li><strong>Quem está mais perto</strong> da farmácia onde aquela caixa está.</li>
              <li><strong>Quem espera há mais tempo.</strong></li>
            </ol>
            <p>
              É por isso que não existe um número de posição: ela depende de em
              qual farmácia a próxima caixa vai aparecer. Alguém pode estar à
              frente para uma caixa e atrás para a seguinte.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
