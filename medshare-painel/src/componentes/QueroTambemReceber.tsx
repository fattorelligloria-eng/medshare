import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Sessao } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from './Aviso'

/**
 * UC04 — quem já doa passa a também poder receber, na mesma conta.
 *
 * São três coisas em ordem, e nenhuma pode ser pulada:
 *
 *   1. o servidor acrescenta o papel BENEFICIARIO ao usuário da sessão;
 *   2. a sessão é renovada, porque o papel vive dentro do token — sem isso o
 *      banco diz uma coisa e o crachá na mão da pessoa diz outra, e as telas
 *      de quem recebe continuariam fechadas;
 *   3. só então a navegação vai para a tela do NIS.
 *
 * Se a renovação falhar depois do papel concedido, a navegação não acontece e
 * a tela diz para entrar de novo: é melhor um recado claro do que despejar a
 * pessoa numa tela que vai recusá-la.
 *
 * O NIS não é pedido aqui. Ele é a RN08, tem tela própria com explicação de
 * onde vem o critério, e é a mesma tela de quem se cadastra direto como
 * beneficiário — um caminho só, não dois para manter.
 */
export function QueroTambemReceber() {
  const navegar = useNavigate()
  const { definirSessaoManualmente, sessao } = useAutenticacao()
  const [erro, definirErro] = useState<unknown>(null)
  const [indo, definirIndo] = useState(false)

  async function seguir() {
    definirErro(null)
    definirIndo(true)
    try {
      await api.post('/usuarios/eu/papeis/beneficiario', {})

      const renovada = await api.post<Sessao>('/autenticacao/renovacao', {
        tokenDeRenovacao: sessao?.tokenDeRenovacao,
      })
      definirSessaoManualmente(renovada)

      navegar('/app/cadunico')
    } catch (e) {
      definirErro(e)
      definirIndo(false)
    }
  }

  return (
    <section className="cartao-conta" aria-labelledby="t-virar">
      <h2 id="t-virar" className="humano">Precisa de um medicamento?</h2>
      <p className="explicacao-secao">
        Quem doa hoje pode precisar amanhã. Dá para pedir medicamentos pela
        mesma conta — não precisa criar outra. O próximo passo é informar seu
        NIS do CadÚnico, que é o critério oficial de quem recebe.
      </p>

      <AvisoDeErro erro={erro} />

      <button type="button" className="secundario" onClick={seguir} disabled={indo}>
        {indo ? 'Um momento…' : 'Quero também receber'}
      </button>
    </section>
  )
}
