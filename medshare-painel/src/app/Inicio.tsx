import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Doacao, Pagina } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'
import { BotaoDeNotificacoes } from '../componentes/BotaoDeNotificacoes'
import { FotoDePerfil } from '../componentes/FotoDePerfil'
import { useMeusDados } from '../componentes/MeusDados'
import { Seta } from '../componentes/Icones'
import { mesEAno, perfilDaPessoa, primeiroNome } from '../formatos'
import { ENCERRADOS } from './Doacoes'

type Atalho = { nome: string; para: string; frase: string; selo?: string }

/**
 * O Início: a porta de entrada de quem doa e de quem recebe.
 *
 * Em cima, o que identifica a pessoa — a carteirinha, num fundo verde, que é
 * um dos três lugares do aplicativo com cor de destaque (os outros são as
 * histórias e o "fale com a central"). Embaixo, "o que você precisa hoje?":
 * atalhos só para o que o papel dela permite, sem ícone, com o nome da coisa.
 */
export function Inicio() {
  const { sessao, temPapel } = useAutenticacao()
  const { dados, erro, recarregar } = useMeusDados()
  const [emAndamento, definirEmAndamento] = useState(0)

  const ehDoador = temPapel('DOADOR')
  const ehBeneficiario = temPapel('BENEFICIARIO')

  // Só a contagem: a lista inteira, com o próximo passo, fica em Minhas doações.
  useEffect(() => {
    if (!ehDoador) return
    api.get<Pagina<Doacao>>('/doacoes/minhas?size=50')
      .then((p) => definirEmAndamento(p.content.filter((d) => !ENCERRADOS.includes(d.status)).length))
      .catch(() => { /* o número é um enfeite útil; sem ele a tela continua de pé */ })
  }, [ehDoador])

  const nome = dados?.nome ?? sessao?.nome ?? ''
  const papeis = dados?.papeis ?? sessao?.papeis ?? []

  const atalhos: Atalho[] = [
    ...(ehDoador ? [
      { nome: 'Doar um medicamento', para: '/app/doacoes/nova', frase: 'Cadastre uma caixa que sobrou' },
      {
        nome: 'Minhas doações',
        para: '/app/doacoes',
        frase: 'Acompanhe cada caixa',
        selo: emAndamento > 0 ? `${emAndamento} em andamento` : undefined,
      },
    ] : []),
    { nome: 'Farmácias perto de mim', para: '/app/farmacias', frase: 'Onde entregar e retirar' },
    ...(ehBeneficiario ? [
      { nome: 'Pedir um medicamento', para: '/app/pedidos/novo', frase: 'Com receita e CadÚnico' },
      { nome: 'Minhas reservas', para: '/app/reservas', frase: 'Caixas guardadas para você' },
    ] : []),
    { nome: 'Ajuda e atendimento', para: '/app/atendimento', frase: 'Fale com a central' },
  ]

  return (
    <>
      <div className="inicio-destaque manchas-verdes">
        <div className="topo-app">
          <div className="cabecalho-app">
            <Marca />
            <BotaoDeNotificacoes />
          </div>
        </div>

        <div className="conteudo-app">
          <h1 className="humano saudacao">Olá, {primeiroNome(nome)}</h1>

          <section className="carteirinha-resumo" aria-label="Sua carteirinha">
            <div className="quem-e">
              <FotoDePerfil url={dados?.fotoUrl} tamanho={52} />
              <div>
                <p className="nome">{nome}</p>
                <p>{perfilDaPessoa(papeis)}</p>
                {ehBeneficiario && dados?.cadUnicoValidoAte && (
                  <p>CadÚnico confirmado até {mesEAno(dados.cadUnicoValidoAte)}</p>
                )}
              </div>
            </div>
            <div className="acoes-da-carteirinha">
              <Link to="/app/carteirinha">Carteirinha digital</Link>
              {ehBeneficiario ? (
                <Link to="/app/reservas">Código de retirada</Link>
              ) : (
                <Link to="/app/doacoes">Minhas doações</Link>
              )}
            </div>
          </section>
        </div>
      </div>

      <div className="conteudo-app cresce inicio-corpo">
        <AvisoDeErro erro={erro} aoTentarDeNovo={recarregar} />

        <h2 className="titulo-de-secao">O que você precisa hoje?</h2>
        <nav className="atalhos" aria-label="O que você precisa hoje">
          {atalhos.map((a) => (
            <Link key={a.para} to={a.para} className="atalho">
              <span className="nome">{a.nome}</span>
              {a.selo ? <span className="etiqueta-andamento">{a.selo}</span> : <span className="frase">{a.frase}</span>}
            </Link>
          ))}
        </nav>

        <Link to="/app/historias" className="faixa-historias manchas-verdes">
          <span className="textos">
            <strong>Histórias de quem foi ajudado</strong>
            <span>Em breve: gente que voltou ao tratamento por causa de uma caixa doada.</span>
          </span>
          <span className="saiba-mais">Saiba mais <Seta tamanho={18} /></span>
        </Link>
      </div>
    </>
  )
}
