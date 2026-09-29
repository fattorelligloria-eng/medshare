import { NavLink, Outlet } from 'react-router-dom'
import { useAutenticacao } from '../contexto/Autenticacao'
import { Marca } from './Logo'

/**
 * O painel de computador: balcão da farmácia e central de análise.
 *
 * Este é o único pedaço que não é o aplicativo — e é assim de propósito. Quem
 * confere lacre e registra entrega faz isso num computador de balcão, não no
 * celular, com as duas mãos ocupadas segurando a caixa.
 *
 * O menu mostra só o que o papel permite. Isso é conveniência, não segurança:
 * quem controla o acesso é o servidor, com @PreAuthorize em cada endpoint.
 */
export function Layout() {
  const { sessao, sair, temPapel } = useAutenticacao()

  return (
    <div className="painel">
      <aside className="lateral">
        <Marca tamanho={30} />

        <nav>
          {temPapel('FARMACEUTICO') && (
            <>
              <NavLink to="/painel/balcao" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                Balcão
              </NavLink>
              <NavLink to="/painel/retirada" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                Retirada
              </NavLink>
            </>
          )}
          {temPapel('ADMIN') && (
            <>
              <NavLink to="/painel/central" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                Central de análise
              </NavLink>
              <NavLink to="/painel/farmacias" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                Farmácias
              </NavLink>
            </>
          )}
          <NavLink to="/painel/notificacoes" className={({ isActive }) => (isActive ? 'ativo' : '')}>
            Notificações
          </NavLink>
          {(temPapel('FARMACEUTICO') || temPapel('ADMIN')) && (
            <NavLink to="/painel/rastreio" className={({ isActive }) => (isActive ? 'ativo' : '')}>
              Rastreio
            </NavLink>
          )}
          {(temPapel('DOADOR') || temPapel('BENEFICIARIO')) && (
            <NavLink to="/app/doacoes">Abrir o aplicativo</NavLink>
          )}
        </nav>

        <div className="rodape">
          <p className="quem">{sessao?.nome}</p>
          <button className="secundario" onClick={sair}>Sair</button>
        </div>
      </aside>

      <main className="area"><Outlet /></main>
    </div>
  )
}
