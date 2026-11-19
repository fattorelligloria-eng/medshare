import { Link, NavLink, Outlet } from 'react-router-dom'
import { Caixa, Cartao, Casa, Farmacia, Fone, Informacao, Lupa, Pessoa, Receita, Sino } from './Icones'
import { Marca } from './Logo'
import { useAutenticacao } from '../contexto/Autenticacao'
import { useNaoLidas } from './BotaoDeNotificacoes'
import { Acessibilidade } from './Acessibilidade'
import { FotoDePerfil } from './FotoDePerfil'
import { useMeusDados } from './MeusDados'

/**
 * A casca das telas do cidadão.
 *
 * Um mesmo conjunto de telas, dois jeitos de navegar: no computador, menu fixo
 * à esquerda; no celular, barra embaixo. Quem decide qual aparece é o CSS, não
 * este componente — os dois menus estão sempre no markup, e um deles está
 * escondido. Isso evita o erro clássico de medir a janela no JavaScript e
 * renderizar a versão errada no primeiro quadro.
 *
 * A barra de baixo tem sempre quatro abas, no mesmo lugar: Início, a lista do
 * papel da pessoa (doações ou pedidos), Atendimento e Conta. O resto se alcança
 * pelos atalhos do Início. No computador sobra espaço, e o menu mostra tudo,
 * agrupado pelo que a pessoa veio fazer: doar, receber, pedir ajuda.
 *
 * O que cada papel vê é conveniência, não segurança: quem controla o acesso é
 * o servidor, com @PreAuthorize em cada endpoint.
 */
export function AreaDoApp() {
  return <Casca comNavegacao />
}

/**
 * Telas internas — nova doação, detalhe, agendamento, carteirinha.
 *
 * No celular elas não mostram a barra de baixo, porque já têm o botão voltar no
 * topo e a pessoa está no meio de uma tarefa. No computador o menu lateral
 * continua ali: sair do meio de um cadastro é um clique, não um problema.
 */
export function TelaInterna() {
  return <Casca />
}

function Casca({ comNavegacao = false }: { comNavegacao?: boolean }) {
  const { sessao, temPapel } = useAutenticacao()
  const naoLidas = useNaoLidas()
  const { dados } = useMeusDados()

  const ehDoador = temPapel('DOADOR')
  const ehBeneficiario = temPapel('BENEFICIARIO')
  const temPainel = temPapel('FARMACEUTICO') || temPapel('ADMIN')

  const ativo = ({ isActive }: { isActive: boolean }) => (isActive ? 'ativo' : '')

  return (
    <div className={comNavegacao ? 'area-app' : 'area-app interna'}>
      <aside className="menu-app">
        <Marca />

        {/* A ação principal do papel, sempre à mão. */}
        {ehDoador ? (
          <Link to="/app/doacoes/nova" className="botao principal acao-do-menu">Doar um medicamento</Link>
        ) : ehBeneficiario && (
          <Link to="/app/pedidos/novo" className="botao principal acao-do-menu">Pedir um medicamento</Link>
        )}

        <nav aria-label="Menu principal">
          <NavLink to="/app/inicio" className={ativo}><Casa tamanho={19} /> Início</NavLink>
          <NavLink to="/app/notificacoes" className={ativo}>
            <Sino tamanho={19} /> Notificações
            {naoLidas > 0 && <span className="selo">{naoLidas > 99 ? '99+' : naoLidas}</span>}
          </NavLink>
          <NavLink to="/app/farmacias" className={ativo}><Farmacia tamanho={19} /> Farmácias parceiras</NavLink>
          <NavLink to="/app/carteirinha" className={ativo}><Cartao tamanho={19} /> Carteirinha digital</NavLink>

          {ehDoador && (
            <>
              <p className="grupo-do-menu">Doar</p>
              <NavLink to="/app/doacoes" className={ativo}><Caixa tamanho={19} /> Minhas doações</NavLink>
            </>
          )}

          {ehBeneficiario && (
            <>
              <p className="grupo-do-menu">Receber</p>
              <NavLink to="/app/pedidos" className={ativo}><Receita tamanho={19} /> Pedidos</NavLink>
              <NavLink to="/app/reservas" className={ativo}><Lupa tamanho={19} /> Reservas</NavLink>
            </>
          )}

          <p className="grupo-do-menu">Ajuda</p>
          <NavLink to="/app/atendimento" className={ativo}><Fone tamanho={19} /> Atendimento</NavLink>
          <NavLink to="/app/sobre" className={ativo}><Informacao tamanho={19} /> Sobre o MedShare</NavLink>

          {temPainel && (
            <NavLink to="/painel" className="separado">Painel da farmácia</NavLink>
          )}
        </nav>

        {/* Sair saiu daqui de propósito. Um botão desse tamanho, sozinho no
            canto, lido como a última coisa do menu, parece um convite a ir
            embora — e não é isso que o aplicativo quer de ninguém. Ele vive no
            fim da tela de Conta, que é onde se procura por ele. */}
        <div className="rodape">
          <div className="cartao-acessibilidade">
            <p className="rotulo">Acessibilidade</p>
            <Acessibilidade />
          </div>
          <NavLink to="/app/conta" className={({ isActive }) => (isActive ? 'quem ativo' : 'quem')}>
            <FotoDePerfil url={dados?.fotoUrl} tamanho={36} />
            <span className="nome">
              <strong>{dados?.nome ?? sessao?.nome}</strong>
              <span>Ver minha conta</span>
            </span>
          </NavLink>
        </div>
      </aside>

      <div className="tela"><Outlet /></div>

      {comNavegacao && (
        <nav className="nav-app" aria-label="Menu">
          <NavLink to="/app/inicio" className={ativo}><Casa /> Início</NavLink>
          {ehDoador ? (
            <NavLink to="/app/doacoes" className={ativo}><Caixa /> Doações</NavLink>
          ) : ehBeneficiario && (
            <NavLink to="/app/pedidos" className={ativo}><Receita /> Pedidos</NavLink>
          )}
          <NavLink to="/app/atendimento" className={ativo}><Fone /> Atendimento</NavLink>
          <NavLink to="/app/conta" className={ativo}><Pessoa /> Conta</NavLink>
        </nav>
      )}
    </div>
  )
}
