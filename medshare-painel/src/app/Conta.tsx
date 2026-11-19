import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Acessibilidade } from '../componentes/Acessibilidade'
import { QueroTambemReceber } from '../componentes/QueroTambemReceber'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { EsqueletoDeBloco } from '../componentes/Esqueleto'
import { DadosDoCadastro } from '../componentes/DadosDoCadastro'
import { TrocaDeSenha } from '../componentes/TrocaDeSenha'
import { Procuradores } from '../componentes/Procuradores'
import { SecaoRecolhivel } from '../componentes/SecaoRecolhivel'
import { FotoDePerfil } from '../componentes/FotoDePerfil'
import { guardarMeusDados, useMeusDados } from '../componentes/MeusDados'
import { Seta } from '../componentes/Icones'
import { perfilDaPessoa } from '../formatos'

/**
 * A conta, organizada como lista: "Sua conta", "Preferências" e "Sobre".
 *
 * Cada linha ou leva a outra tela (foto, notificações, sobre) ou abre ali
 * mesmo (dados do cadastro, senha, quem pode retirar, acessibilidade). Abrir
 * no lugar é de propósito: corrigir o telefone não deveria tirar a pessoa da
 * conta.
 *
 * O que muda a vida da pessoa no MedShare — passar a receber, o CadÚnico, o
 * painel da farmácia — vem depois da lista, em cartões, e sair fica no fim.
 */
export function Conta() {
  const { sessao, sair, temPapel } = useAutenticacao()
  const { dados, erro, recarregar } = useMeusDados()
  const [recado, definirRecado] = useState<string | null>(null)

  const nome = dados?.nome ?? sessao?.nome ?? ''
  const ehBeneficiario = temPapel('BENEFICIARIO')

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
          <FotoDePerfil url={dados?.fotoUrl} tamanho={56} />
          <div>
            <p className="nome-completo">{nome}</p>
            <p className="perfil">{perfilDaPessoa(dados?.papeis ?? sessao?.papeis ?? [])}</p>
            <Link to="/app/conta/foto" className="trocar-foto">
              {dados?.fotoUrl ? 'Trocar foto' : 'Colocar foto'}
            </Link>
          </div>
        </div>

        <AvisoDeErro erro={erro} aoTentarDeNovo={recarregar} />
        {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

        <div className="conta-grade">
          <div className="conta-coluna">
            <h2 className="grupo-da-conta">Sua conta</h2>
            <div className="lista-da-conta">
              <Link to="/app/conta/foto" className="linha-da-conta">
                <span><strong>Foto de perfil</strong><span>Aparece na sua carteirinha digital</span></span>
                <Seta />
              </Link>

              <SecaoRecolhivel titulo="Dados do cadastro" abertaDeInicio={telaGrande()}>
                {!dados && !erro && <EsqueletoDeBloco altura={220} />}
                {dados && (
                  <DadosDoCadastro
                    dados={dados}
                    aoSalvar={(novos) => {
                      if (sessao) guardarMeusDados(sessao.usuarioId, novos)
                      definirRecado('Cadastro atualizado.')
                    }}
                  />
                )}
              </SecaoRecolhivel>

              <SecaoRecolhivel titulo="Trocar senha">
                <TrocaDeSenha aoTrocar={() => definirRecado('Senha alterada.')} />
              </SecaoRecolhivel>

              {ehBeneficiario && (
                <SecaoRecolhivel titulo="Quem pode retirar por você">
                  <Procuradores />
                </SecaoRecolhivel>
              )}
            </div>

            {/* --- o que muda o que a pessoa faz no MedShare --- */}
            {ehBeneficiario && (
              <section className="cartao-conta" aria-labelledby="t-cadunico">
                <h2 id="t-cadunico" className="humano">Verificação no CadÚnico</h2>
                <p className="explicacao-secao">
                  Necessária para pedir medicamentos. Vale por 12 meses.
                </p>
                <Link to="/app/cadunico" className="botao secundario">Informar meu NIS</Link>
              </section>
            )}

            {!ehBeneficiario && <QueroTambemReceber />}

            {(temPapel('FARMACEUTICO') || temPapel('ADMIN')) && (
              <section className="cartao-conta" aria-labelledby="t-trabalho">
                <h2 id="t-trabalho" className="humano">Área de trabalho</h2>
                <p className="explicacao-secao">
                  O balcão da farmácia e a central de análise ficam no painel.
                </p>
                <Link to="/painel" className="botao secundario">Abrir o painel</Link>
              </section>
            )}
          </div>

          <div className="conta-coluna">
            <h2 className="grupo-da-conta">Preferências</h2>
            <div className="lista-da-conta">
              {/* No computador estes controles ficam no menu lateral; no
                  celular não existe menu lateral, e é aqui que a pessoa procura. */}
              <SecaoRecolhivel titulo="Acessibilidade" className="so-no-celular">
                <p className="explicacao-secao" style={{ marginTop: 0 }}>
                  Tamanho da letra e alto contraste. Fica guardado neste aparelho.
                </p>
                <Acessibilidade />
              </SecaoRecolhivel>
              <Link to="/app/notificacoes" className="linha-da-conta">
                <span><strong>Notificações</strong><span>Avisos sobre suas caixas e pedidos</span></span>
                <Seta />
              </Link>
            </div>

            <h2 className="grupo-da-conta">Sobre</h2>
            <div className="lista-da-conta">
              <Link to="/app/sobre" className="linha-da-conta">
                <span><strong>Sobre o MedShare</strong><span>O que ele é e o que ele não é</span></span>
                <Seta />
              </Link>
              <Link to="/app/atendimento" className="linha-da-conta">
                <span><strong>Atendimento</strong><span>Dúvidas frequentes e fale com a central</span></span>
                <Seta />
              </Link>
            </div>

            {/* Fica no fim, e não no rodapé que gruda: sair não é a ação
                principal desta tela. É o único lugar do aplicativo onde sair aparece. */}
            <div style={{ margin: '22px 0 28px' }}>
              <button className="sair-da-conta" onClick={sair}>Sair da conta</button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

/** Computador: a mesma largura em que o menu lateral aparece (estilos.css, 900px). */
function telaGrande(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(min-width: 901px)').matches
}
