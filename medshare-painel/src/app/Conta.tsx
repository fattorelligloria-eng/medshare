import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { MeuImpacto, MeusDados } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { EsqueletoDeBloco } from '../componentes/Esqueleto'
import { DadosDoCadastro } from '../componentes/DadosDoCadastro'
import { TrocaDeSenha } from '../componentes/TrocaDeSenha'
import { Procuradores } from '../componentes/Procuradores'
import { data, reais } from '../formatos'

const NOME_DO_PAPEL: Record<string, string> = {
  DOADOR: 'doadora',
  BENEFICIARIO: 'recebe medicamentos',
  FARMACEUTICO: 'farmacêutica',
  ADMIN: 'central de análise',
}

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

  return (
    <>
      <div className="topo-app">
        <div className="cabecalho-app">
          <Marca />
        </div>
      </div>

      <div className="conteudo-app cresce" style={{ paddingTop: 28 }}>
        <h1 className="humano" style={{ fontSize: 26 }}>{dados?.nome ?? sessao?.nome}</h1>
        <p style={{ margin: '6px 0 0', fontSize: 14, color: 'var(--tinta-media)' }}>
          {(dados?.papeis ?? sessao?.papeis ?? [])
            .map((p) => NOME_DO_PAPEL[p] ?? p.toLowerCase())
            .join(' · ')}
          {dados && ` · no MedShare desde ${data(dados.membroDesde)}`}
        </p>

        <AvisoDeErro erro={erro} aoTentarDeNovo={carregar} />
        {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

        {/* --- o impacto, para quem doa --- */}
        {impacto && impacto.caixasDoadas > 0 && (
          <section className="cartao-conta" aria-labelledby="t-impacto">
            <h2 id="t-impacto" className="humano">Seu impacto</h2>
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
            {impacto.valorDosTratamentos != null && (
              <p className="nota">
                {impacto.caixasEntregues === 1
                  ? `Alguém começou um tratamento de ${reais(impacto.valorDosTratamentos)} que não tinha como comprar.`
                  : `${impacto.caixasEntregues} pessoas começaram tratamentos que não cabiam no orçamento delas — ${reais(impacto.valorDosTratamentos)} no total.`}
              </p>
            )}
          </section>
        )}

        {/* --- os dados do cadastro --- */}
        {carregando && <div style={{ marginTop: 26 }}><EsqueletoDeBloco altura={220} /></div>}

        {dados && (
          <DadosDoCadastro
            dados={dados}
            aoSalvar={(novos) => {
              definirDados(novos)
              definirRecado('Cadastro atualizado.')
            }}
          />
        )}

        {/* --- senha --- */}
        <TrocaDeSenha aoTrocar={() => definirRecado('Senha alterada.')} />

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

        {/* --- virar beneficiário: item 36, só com NIS --- */}
        {!temPapel('BENEFICIARIO') && (
          <section className="cartao-conta" aria-labelledby="t-virar">
            <h2 id="t-virar" className="humano">Precisa de um medicamento?</h2>
            <p className="explicacao-secao">
              Quem doa hoje pode precisar amanhã. Para pedir medicamentos pela
              mesma conta, informe seu NIS do CadÚnico — é o critério oficial
              que decide quem recebe, e sem ele não dá para seguir.
            </p>
            <Link to="/app/cadunico?novo=1" className="botao secundario">
              Quero também receber
            </Link>
          </section>
        )}

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

        {/* Fica no fim, e não no rodapé que gruda: sair não é a ação principal
            desta tela, e grudado ele cobria os dados enquanto a pessoa rolava. */}
        <section className="cartao-conta" style={{ paddingBottom: 28 }}>
          <button className="secundario" onClick={sair}>Sair da conta</button>
        </section>
      </div>
    </>
  )
}
