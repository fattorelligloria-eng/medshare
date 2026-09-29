import { Link } from 'react-router-dom'
import { useAutenticacao } from '../contexto/Autenticacao'
import { Marca } from '../componentes/Logo'
import { Procuradores } from '../componentes/Procuradores'

const NOME_DO_PAPEL: Record<string, string> = {
  DOADOR: 'doadora',
  BENEFICIARIO: 'recebe medicamentos',
  FARMACEUTICO: 'farmacêutica',
  ADMIN: 'central de análise',
}

export function Conta() {
  const { sessao, sair, temPapel } = useAutenticacao()

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app" style={{ paddingTop: 28, flex: 1 }}>
        <h1 className="humano" style={{ fontSize: 26 }}>{sessao?.nome}</h1>
        <p style={{ margin: '6px 0 0', fontSize: 14, color: 'var(--tinta-media)' }}>
          {sessao?.papeis.map((p) => NOME_DO_PAPEL[p] ?? p.toLowerCase()).join(' · ')}
        </p>

        {temPapel('BENEFICIARIO') && (
          <div style={{ marginTop: 28, paddingTop: 22, borderTop: '1px solid var(--linha)' }}>
            <h2 className="humano" style={{ fontSize: 19 }}>Verificação no CadÚnico</h2>
            <p style={{ margin: '8px 0 14px', fontSize: 14, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
              Necessária para pedir medicamentos. Vale por 12 meses.
            </p>
            <Link to="/app/cadunico" className="botao secundario">Informar meu NIS</Link>
          </div>
        )}

        {temPapel('BENEFICIARIO') && <Procuradores />}

        {(temPapel('FARMACEUTICO') || temPapel('ADMIN')) && (
          <div style={{ marginTop: 28, paddingTop: 22, borderTop: '1px solid var(--linha)' }}>
            <h2 className="humano" style={{ fontSize: 19 }}>Área de trabalho</h2>
            <p style={{ margin: '8px 0 14px', fontSize: 14, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
              O balcão da farmácia e a central de análise ficam na versão de
              computador.
            </p>
            <Link to="/painel" className="botao secundario">Abrir o painel</Link>
          </div>
        )}
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 20 }}>
        <button className="secundario" onClick={sair}>Sair da conta</button>
      </div>
    </>
  )
}
