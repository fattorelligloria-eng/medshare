import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Acessibilidade } from '../componentes/Acessibilidade'
import { QueroTambemReceber } from '../componentes/QueroTambemReceber'
import { api } from '../api/cliente'
import type { MeuImpacto, MeusDados } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { EsqueletoDeBloco } from '../componentes/Esqueleto'
import { DadosDoCadastro } from '../componentes/DadosDoCadastro'
import { TrocaDeSenha } from '../componentes/TrocaDeSenha'
import { Procuradores } from '../componentes/Procuradores'
import { SecaoRecolhivel } from '../componentes/SecaoRecolhivel'
import { Acessivel, Ficha, Senha } from '../componentes/Icones'

/**
 * A conta.
 *
 * Esta tela tinha o nome da pessoa e um botão de sair. Todo o resto do cadastro
 * — CPF, telefone, endereço inteiro — estava no banco desde o primeiro dia e
 * nunca apareceu. Agora aparece, e dá para corrigir.
 *
 * As seções vêm na ordem de quem se pergunta "meus dados estão certos?", e não
 * na ordem em que foram programadas.
 */
export function Conta() {
  const { sessao, sair, temPapel } = useAutenticacao()
  const [dados, definirDados] = useState<MeusDados | null>(null)
  const [impacto, definirImpacto] = useState<MeuImpacto | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)
  const [carregando, definirCarregando] = useState(true)

  const carregar = useCallback(() => {
    definirCarregando(true)
    definirErro(null)
    api.get<MeusDados>('/usuarios/eu')
      .then(definirDados)
      .catch(definirErro)
      .finally(() => definirCarregando(false))
  }, [])

  useEffect(() => { carregar() }, [carregar])

  useEffect(() => {
    if (!temPapel('DOADOR')) return
    api.get<MeuImpacto>('/usuarios/eu/impacto').then(definirImpacto).catch(() => {})
  }, [temPapel])

  const nome = dados?.nome ?? sessao?.nome ?? ''

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 16 }}>
        <h1 className="humano" style={{ fontSize: '1.75rem' }}>Conta</h1>

        <div className="cartao-de-perfil" style={{ marginTop: 14 }}>
          <span className="iniciais" aria-hidden="true">{iniciais(nome)}</span>
          <p className="nome-completo">{nome}</p>
        </div>

        <AvisoDeErro erro={erro} aoTentarDeNovo={carregar} />
        {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

        {/* --- o impacto, para quem doa ---
            Sem valor em reais, de propósito: ver quanto custava a caixa doada
            é o caminho mais curto para a pessoa se arrepender de ter doado. */}
        {impacto && impacto.caixasDoadas > 0 && (
          <section className="cartao-conta" aria-labelledby="t-impacto">
            <h2 id="t-impacto" className="humano">Seu <em>impacto</em></h2>
            <div className="numeros">
              <div>
                <span className="n">{impacto.caixasDoadas}</span>
                <span className="r">{impacto.caixasDoadas === 1 ? 'caixa doada' : 'caixas doadas'}</span>
              </div>
              <div>
                <span className="n">{impacto.caixasEntregues}</span>
                <span className="r">
                  {impacto.caixasEntregues === 1 ? 'chegou a alguém' : 'chegaram a alguém'}
                </span>
              </div>
            </div>
          </section>
        )}

        {/* --- CadÚnico, para quem recebe --- */}
        {temPapel('BENEFICIARIO') && (
          <section className="cartao-conta" aria-labelledby="t-cadunico">
            <h2 id="t-cadunico" className="humano">Verificação no CadÚnico</h2>
            <p className="explicacao-secao">
              Necessária para pedir medicamentos. Vale por 12 meses.
            </p>
            <Link to="/app/cadunico" className="botao secundario">Informar meu NIS</Link>
          </section>
        )}

        {!temPapel('BENEFICIARIO') && <QueroTambemReceber />}

        {temPapel('BENEFICIARIO') && <Procuradores />}

        {(temPapel('FARMACEUTICO') || temPapel('ADMIN')) && (
          <section className="cartao-conta" aria-labelledby="t-trabalho">
            <h2 id="t-trabalho" className="humano">Área de trabalho</h2>
            <p className="explicacao-secao">
              O balcão da farmácia e a central de análise ficam no painel.
            </p>
            <Link to="/painel" className="botao secundario">Abrir o painel</Link>
          </section>
        )}

        {/* --- o que se consulta de vez em quando: fechado até a pessoa abrir --- */}
        <SecaoRecolhivel titulo="Dados do cadastro" icone={<Ficha cor="var(--verde)" />}>
          {carregando && <EsqueletoDeBloco altura={220} />}
          {dados && (
            <DadosDoCadastro
              dados={dados}
              aoSalvar={(novos) => {
                definirDados(novos)
                definirRecado('Cadastro atualizado.')
              }}
            />
          )}
        </SecaoRecolhivel>

        <SecaoRecolhivel titulo="Trocar senha" icone={<Senha tamanho={20} cor="var(--verde)" />}>
          <TrocaDeSenha aoTrocar={() => definirRecado('Senha alterada.')} />
        </SecaoRecolhivel>

        {/* No computador estes controles ficam no menu lateral; no celular não
            existe menu lateral, e é aqui que a pessoa vem procurar. */}
        <SecaoRecolhivel
          titulo="Acessibilidade"
          icone={<Acessivel cor="var(--verde)" />}
          className="so-no-celular"
        >
          <p className="explicacao-secao" style={{ marginTop: 0 }}>
            Tamanho da letra e alto contraste. Fica guardado neste aparelho.
          </p>
          <Acessibilidade />
        </SecaoRecolhivel>

        {/* Fica no fim, e não no rodapé que gruda: sair não é a ação principal
            desta tela. É o único lugar do aplicativo onde sair aparece. */}
        <div style={{ margin: '18px 0 28px' }}>
          <button className="sair-da-conta" onClick={sair}>Sair da conta</button>
        </div>
      </div>
    </>
  )
}

/** "Glória Fattorelli" vira "GF": a primeira letra do primeiro e do último nome. */
function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return ''
  const primeira = partes[0][0]
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}
