import { NavLink, Outlet } from 'react-router-dom'
import { useAutenticacao } from '../contexto/Autenticacao'
import { Marca } from './Logo'
import { Caixa, Escudo, Farmacia, Fone, Lupa, Pessoa, Receita, Sino } from './Icones'

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
  const ativo = ({ isActive }: { isActive: boolean }) => (isActive ? 'ativo' : '')

  return (
    <div className="painel">
      <aside className="lateral">
        <Marca />

        <nav aria-label="Painel">
          {temPapel('FARMACEUTICO') && (
            <>
              <NavLink to="/painel/balcao" className={ativo}><Caixa tamanho={20} /> Balcão</NavLink>
              <NavLink to="/painel/retirada" className={ativo}><Receita tamanho={20} /> Retirada</NavLink>
            </>
          )}
          {temPapel('ADMIN') && (
            <>
              <NavLink to="/painel/central" className={ativo}><Escudo /> Central de análise</NavLink>
              <NavLink to="/painel/farmacias" className={ativo}><Farmacia tamanho={20} /> Farmácias</NavLink>
              <NavLink to="/painel/atendimento" className={ativo}><Fone tamanho={20} /> Atendimento</NavLink>
            </>
          )}
          <NavLink to="/painel/notificacoes" className={ativo}><Sino /> Notificações</NavLink>
          {(temPapel('FARMACEUTICO') || temPapel('ADMIN')) && (
            <NavLink to="/painel/rastreio" className={ativo}><Lupa tamanho={20} /> Rastreio</NavLink>
          )}
          {(temPapel('DOADOR') || temPapel('BENEFICIARIO')) && (
            <NavLink to="/app/inicio"><Pessoa tamanho={20} /> Abrir o aplicativo</NavLink>
          )}
        </nav>

        {/* Nome completo e o papel de quem está no balcão; sair fica pequeno e
            vermelho, no fim — é uma saída, não um destino. */}
        <div className="rodape">
          <p className="quem">
            <strong>{sessao?.nome}</strong>
            <span>{temPapel('ADMIN') ? 'Equipe MedShare · central' : 'Farmacêutica · farmácia parceira'}</span>
          </p>
          <button className="sair-da-conta" onClick={sair}>Sair</button>
        </div>
      </aside>

      <main className="area"><Outlet /></main>
    </div>
  )
}
