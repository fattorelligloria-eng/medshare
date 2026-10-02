import { Link } from 'react-router-dom'

/**
 * UC04 — quem já doa descobre que pode também receber, na mesma conta.
 *
 * O botão só leva para a verificação do CadÚnico; ele não muda nada na conta.
 * Quem concede o papel de beneficiário é a confirmação do NIS, no servidor.
 *
 * Decisão da Glória, e é a parte que importa: enquanto o NIS não confirmar,
 * Pedidos e Reservas não aparecem em lugar nenhum — nem no menu, nem na barra
 * de baixo, nem como rota. Quem não tem CadÚnico não precisa nem saber que
 * essa parte do aplicativo existe. Mostrar e depois bloquear seria oferecer
 * uma porta para em seguida dizer que ela não é para aquela pessoa.
 */
export function QueroTambemReceber() {
  return (
    <section className="cartao-conta" aria-labelledby="t-virar">
      <h2 id="t-virar" className="humano">Precisa de um medicamento?</h2>
      <p className="explicacao-secao">
        Quem doa hoje pode precisar amanhã. Dá para pedir medicamentos pela
        mesma conta — não precisa criar outra. Para isso, confirmamos seu NIS do
        CadÚnico, que é o critério oficial de quem recebe.
      </p>
      <Link to="/app/cadunico" className="botao secundario">Quero também receber</Link>
    </section>
  )
}
