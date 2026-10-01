import { NavLink, Outlet } from 'react-router-dom'
import { Caixa, Duvida, Farmacia, Informacao, Lupa, Pessoa, Receita, Sino } from './Icones'
import { Marca } from './Logo'
import { useAutenticacao } from '../contexto/Autenticacao'
import { useNaoLidas } from './BotaoDeNotificacoes'

/**
 * A casca das telas do cidadão.
 *
 * Um mesmo conjunto de telas, dois jeitos de navegar: no computador, menu fixo
 * à esquerda; no celular, barra embaixo. Quem decide qual aparece é o CSS, não
 * este componente — os dois menus estão sempre no markup, e um deles está
 * escondido. Isso evita o erro clássico de medir a janela no JavaScript e
 * renderizar a versão errada no primeiro quadro.
 *
 * O menu de baixo leva só o que a pessoa usa todo dia; ajuda e sobre ficam no
 * rodapé do menu lateral e dentro da conta, que é onde se procura por isso.
 *
 * O que cada papel vê é conveniência, não segurança: quem controla o acesso é
 * o servidor, com @PreAuthorize em cada endpoint.
 */
export function AreaDoApp() {
  return <Casca comNavegacao />
}

/**
 * Telas internas — nova doação, detalhe, agendamento.
 *
 * No celular elas não mostram a barra de baixo, porque já têm o botão voltar no
 * topo e a pessoa está no meio de uma tarefa. No computador o menu lateral
 * continua ali: sair do meio de um cadastro é um clique, não um problema.
 */
export function TelaInterna() {
  return <Casca />
}

function Casca({ comNavegacao = false }: { comNavegacao?: boolean }) {
  const { sessao, sair, temPapel } = useAutenticacao()
  const naoLidas = useNaoLidas()

  const ehDoador = temPapel('DOADOR')
  const ehBeneficiario = temPapel('BENEFICIARIO')
  const temPainel = temPapel('FARMACEUTICO') || temPapel('ADMIN')

  const ativo = ({ isActive }: { isActive: boolean }) => (isActive ? 'ativo' : '')

  return (
    <div className="area-app">
      <aside className="menu-app">
        <Marca tamanho={30} />

        <nav aria-label="Menu principal">
          {ehDoador && (
            <NavLink to="/app/doacoes" className={ativo}><Caixa tamanho={18} /> Doações</NavLink>
          )}
          {ehBeneficiario && (
            <>
              <NavLink to="/app/pedidos" className={ativo}><Receita tamanho={18} /> Pedidos</NavLink>
              <NavLink to="/app/reservas" className={ativo}><Lupa tamanho={18} /> Reservas</NavLink>
            </>
          )}

          <NavLink to="/app/farmacias" className={ativo}>
            <Farmacia tamanho={18} /> Farmácias
          </NavLink>

          <NavLink to="/app/notificacoes" className={ativo}>
            <Sino tamanho={18} /> Notificações
            {naoLidas > 0 && <span className="selo">{naoLidas > 99 ? '99+' : naoLidas}</span>}
          </NavLink>

          <NavLink to="/app/conta" className={ativo}><Pessoa tamanho={18} /> Conta</NavLink>

          {temPainel && (
            <NavLink to="/painel" className="separado">Painel da farmácia</NavLink>
          )}

          <div className="menu-rodape">
            <NavLink to="/app/ajuda" className={ativo}><Duvida tamanho={18} /> Ajuda</NavLink>
            <NavLink to="/app/sobre" className={ativo}><Informacao tamanho={18} /> Sobre</NavLink>
          </div>
        </nav>

        <div className="rodape">
          <p className="quem">{sessao?.nome}</p>
          <button className="secundario" onClick={sair}>Sair</button>
        </div>
      </aside>

      <div className="tela"><Outlet /></div>

      {comNavegacao && (
        <nav className="nav-app" aria-label="Menu">
          {ehDoador && (
            <NavLink to="/app/doacoes" className={ativo}><Caixa /> Doações</NavLink>
          )}
          {ehBeneficiario && (
            <>
              <NavLink to="/app/pedidos" className={ativo}><Receita /> Pedidos</NavLink>
              <NavLink to="/app/reservas" className={ativo}><Lupa /> Reservas</NavLink>
            </>
          )}
          <NavLink to="/app/farmacias" className={ativo}><Farmacia /> Farmácias</NavLink>
          <NavLink to="/app/conta" className={ativo}><Pessoa /> Conta</NavLink>
        </nav>
      )}
    </div>
  )
}
