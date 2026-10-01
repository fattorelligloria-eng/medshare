import { Link } from 'react-router-dom'
import { Marca } from '../componentes/Logo'

/**
 * Sobre o MedShare.
 *
 * Diz o que o sistema é e o que ele não é. A parte do "não é" existe porque
 * quem entrega um remédio de trinta mil reais a um estranho merece saber
 * exatamente onde está se metendo.
 */
export function Sobre() {
  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 28 }}>
        <h1 className="humano" style={{ fontSize: 26 }}>Sobre o MedShare</h1>

        <p className="texto-sobre">
          Todo ano, caixas lacradas de medicamento de alto custo vencem dentro de
          armários. O tratamento terminou, o médico trocou a dose, a pessoa
          morreu. Ao mesmo tempo, alguém com a receita na mão interrompe o
          tratamento porque não tem como comprar a próxima caixa.
        </p>

        <p className="texto-sobre">
          O MedShare liga as duas pontas sem que elas se encontrem. Quem tem a
          caixa leva a uma farmácia parceira. Um farmacêutico confere o lacre, a
          validade e o lote. Só depois disso a caixa fica disponível para quem
          precisa — e a retirada exige receita válida, conferida no balcão.
        </p>

        <h2 className="humano" style={{ fontSize: 20, marginTop: 30 }}>O que ele não é</h2>

        <ul className="lista-sobre">
          <li>
            <strong>Não é entrega em casa.</strong> A caixa passa pela farmácia,
            sempre. É lá que existe alguém habilitado para dizer se o
            medicamento está em condições de uso.
          </li>
          <li>
            <strong>Não é venda.</strong> Ninguém paga nada, em nenhum ponto.
          </li>
          <li>
            <strong>Não é anônimo para o sistema.</strong> Cada caixa tem
            histórico completo, com hora e responsável de cada passo, que não
            pode ser alterado nem apagado. Anônimo é só entre as duas pessoas.
          </li>
          <li>
            <strong>Não decide sozinho.</strong> Uma leitura automática ajuda a
            conferir a foto da embalagem, mas ela só pode deixar passar o caso
            obviamente correto. Recusar é sempre decisão de uma pessoa, com
            justificativa registrada.
          </li>
        </ul>

        <h2 className="humano" style={{ fontSize: 20, marginTop: 30 }}>Onde ele funciona</h2>

        <p className="texto-sobre">
          Nos 39 municípios da Região Metropolitana de São Paulo.
        </p>

        <h2 className="humano" style={{ fontSize: 20, marginTop: 30 }}>Quem fez</h2>

        <p className="texto-sobre">
          Trabalho de Modelagem de Software da Universidade São Judas Tadeu.
        </p>

        <p style={{ marginTop: 30, fontSize: 14, color: 'var(--tinta-media)' }}>
          Dúvidas de quem vai doar estão nas <Link to="/app/ajuda">perguntas frequentes</Link>.
        </p>
      </div>
    </>
  )
}
