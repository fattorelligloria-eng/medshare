import { NavLink, Outlet } from 'react-router-dom'
import { Caixa, Lupa, Pessoa, Receita } from './Icones'
import { useAutenticacao } from '../contexto/Autenticacao'

/**
 * A moldura de celular.
 *
 * As telas do cidadão são do APLICATIVO, não de um site. No navegador elas
 * aparecem dentro de um aparelho desenhado — para ninguém confundir as duas
 * coisas, e para a apresentação projetar o app na parede. Em tela pequena a
 * moldura some e a tela ocupa tudo, que é exatamente como fica no aparelho.
 */
export function MolduraDoApp() {
  const { temPapel } = useAutenticacao()

  return (
    <div className="palco">
      <div className="moldura">
        <div className="tela"><Outlet /></div>

        <nav className="nav-app">
          {temPapel('DOADOR') && (
            <NavLink to="/app/doacoes" className={({ isActive }) => (isActive ? 'ativo' : '')}>
              <Caixa /> Doações
            </NavLink>
          )}
          {temPapel('BENEFICIARIO') && (
            <>
              <NavLink to="/app/pedidos" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                <Receita /> Pedidos
              </NavLink>
              <NavLink to="/app/reservas" className={({ isActive }) => (isActive ? 'ativo' : '')}>
                <Lupa /> Reservas
              </NavLink>
            </>
          )}
          <NavLink to="/app/conta" className={({ isActive }) => (isActive ? 'ativo' : '')}>
            <Pessoa /> Conta
          </NavLink>
        </nav>
      </div>
    </div>
  )
}

/** Tela interna do app: mesma moldura, sem a navegação de baixo. */
export function TelaDoApp() {
  return (
    <div className="palco">
      <div className="moldura">
        <div className="tela"><Outlet /></div>
      </div>
    </div>
  )
}
