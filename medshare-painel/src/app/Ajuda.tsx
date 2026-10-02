import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Marca } from '../componentes/Logo'

/**
 * Perguntas frequentes.
 *
 * As respostas aqui são as regras de negócio ditas em português. Quando a RN02
 * mudar de 30 para outro número, esta tela mente — então cada resposta diz a
 * regra em vez de repetir o valor solto, e fica mais fácil achar o que
 * atualizar.
 */
const PERGUNTAS = [
  {
    p: 'Que medicamento eu posso doar?',
    r: `Caixa lacrada, de fábrica, que nunca foi aberta. A validade precisa estar
        a mais de 30 dias de hoje — menos que isso não dá tempo de a caixa ser
        conferida, reservada e retirada por alguém. E o medicamento precisa ser
        de alto custo: a partir de R$ 150 pelo preço de tabela da CMED.`,
  },
  {
    p: 'Por que só medicamento caro?',
    r: `Porque é onde a falta dói. Dipirona de doze reais a pessoa compra na
        esquina; o remédio de trinta mil reais é o que faz alguém interromper um
        tratamento. A rede existe para esse segundo caso.`,
  },
  {
    p: 'Preciso de receita para doar?',
    r: `Não. Receita é exigida de quem recebe, não de quem doa. Você precisa da
        caixa lacrada e de uma foto dela.`,
  },
  {
    p: 'Quem confere se o remédio está bom?',
    r: `Um farmacêutico, na farmácia parceira, com a caixa na mão. Antes disso
        uma leitura automática confere a foto — mas ela só pode deixar passar o
        caso obviamente correto. Ela nunca recusa nada sozinha: qualquer dúvida
        vai para uma pessoa decidir.`,
  },
  {
    p: 'Eu vou saber quem recebeu meu remédio?',
    r: `Não, e isso é proposital. Quem doa e quem recebe nunca se encontram nem
        se identificam. Você acompanha o caminho da sua caixa até ela ser
        entregue; a identidade de quem recebeu não é informação sua.`,
  },
  {
    p: 'Como eu sei que minha doação chegou a alguém?',
    r: `Pela tela de doações. Cada caixa tem um histórico completo, com data e
        hora de cada passo, e ele não pode ser alterado nem apagado depois.`,
  },
  {
    p: 'Quem pode receber um medicamento?',
    r: `Quem tem NIS ativo no CadÚnico, o cadastro do governo federal para
        programas sociais, e uma receita válida. O critério não é nosso, é o
        oficial — e a receita é conferida de novo no balcão, na hora da retirada.`,
  },
  {
    p: 'Posso doar e também receber?',
    r: `Pode. Quem doa hoje pode precisar amanhã. Para receber, é preciso
        informar o NIS, na tela da sua conta.`,
  },
  {
    p: 'E se eu não puder levar a caixa no horário marcado?',
    r: `Dá para reagendar uma vez, pela própria doação. Depois disso é preciso
        cadastrar de novo — a caixa não pode ficar indefinidamente esperando,
        porque a validade continua correndo.`,
  },
  {
    p: 'O MedShare cobra alguma coisa?',
    r: `Não. Nem de quem doa, nem de quem recebe, nem da farmácia.`,
  },
]

export function Ajuda() {
  const [aberta, definirAberta] = useState<number | null>(0)

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 28 }}>
        <h1 className="humano" style={{ fontSize: '1.625rem' }}>Perguntas frequentes</h1>
        <p style={{ margin: '8px 0 24px', fontSize: '0.9062rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
          O que as pessoas mais perguntam antes de doar pela primeira vez.
        </p>

        <div className="perguntas">
          {PERGUNTAS.map(({ p, r }, i) => (
            <div className={aberta === i ? 'pergunta aberta' : 'pergunta'} key={p}>
              <h2>
                <button
                  type="button"
                  aria-expanded={aberta === i}
                  onClick={() => definirAberta(aberta === i ? null : i)}
                >
                  <span>{p}</span>
                  <span className="seta" aria-hidden="true">{aberta === i ? '−' : '+'}</span>
                </button>
              </h2>
              {aberta === i && <p>{r.replace(/\s+/g, ' ').trim()}</p>}
            </div>
          ))}
        </div>

        <p style={{ marginTop: 30, fontSize: '0.875rem', color: 'var(--tinta-media)' }}>
          Não achou o que procurava? Veja <Link to="/app/sobre">sobre o MedShare</Link>.
        </p>
      </div>
    </>
  )
}
