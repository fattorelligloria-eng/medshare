/**
 * A barra de progresso do cadastro de doação.
 *
 * É uma <ol> de verdade, não uma fileira de divs: quem usa leitor de tela
 * ouve "lista de 4 itens, item 2" e sabe onde está sem depender da cor. O
 * aria-current marca o passo atual, e os já feitos ganham o rótulo "concluído"
 * em texto escondido — a bolinha verde não fala.
 */
export function Passos({ passos, atual }: { passos: string[]; atual: number }) {
  return (
    <ol className="passos" aria-label="Progresso do cadastro">
      {passos.map((nome, i) => {
        const estado = i < atual ? 'feito' : i === atual ? 'atual' : 'futuro'
        return (
          <li key={nome} className={estado} aria-current={i === atual ? 'step' : undefined}>
            <span className="bolinha" aria-hidden="true">{i < atual ? '✓' : i + 1}</span>
            <span className="nome">{nome}</span>
            {i < atual && <span className="so-leitor-de-tela"> concluído</span>}
          </li>
        )
      })}
    </ol>
  )
}
