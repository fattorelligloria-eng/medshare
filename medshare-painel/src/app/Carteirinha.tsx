import { Link } from 'react-router-dom'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { EsqueletoDeBloco } from '../componentes/Esqueleto'
import { FotoDePerfil } from '../componentes/FotoDePerfil'
import { Marca } from '../componentes/Logo'
import { useMeusDados } from '../componentes/MeusDados'
import { data, mesEAno, numeroDaCarteirinha, perfilDaPessoa } from '../formatos'

/**
 * A carteirinha digital.
 *
 * Mostra quem a pessoa é no MedShare: nome completo, foto, o que ela faz ali,
 * o número da carteirinha (sorteado, nunca o id do cadastro) e, para quem
 * recebe, até quando o CadÚnico vale. O CPF vai mascarado, como em toda a API.
 *
 * No celular ela fica em pé, com a foto grande; no computador, deitada, no
 * formato de cartão. A diferença é só de CSS.
 */
export function Carteirinha() {
  const { sessao, temPapel } = useAutenticacao()
  const { dados, erro, recarregar } = useMeusDados()

  const ehBeneficiario = temPapel('BENEFICIARIO')
  const nome = dados?.nome ?? sessao?.nome ?? ''

  return (
    <>
      <CabecalhoInterno titulo="Carteirinha digital" para="/app/inicio" />

      <div className="conteudo-app cresce" style={{ paddingTop: 16 }}>
        <AvisoDeErro erro={erro} aoTentarDeNovo={recarregar} />
        {!dados && !erro && <EsqueletoDeBloco altura={420} />}

        {dados && (
          <div className="carteirinha-e-acoes">
            <section className="carteirinha" aria-label="Carteirinha digital MedShare">
              <div className="carteirinha-dentro">
                <div className="carteirinha-topo">
                  <Marca tamanho={28} />
                  <span>Carteirinha digital</span>
                </div>

                <div className="carteirinha-pessoa">
                  <FotoDePerfil url={dados.fotoUrl} tamanho={104} />
                  <div>
                    <p className="nome">{nome}</p>
                    <p className="perfil">{perfilDaPessoa(dados.papeis)}</p>
                  </div>
                </div>

                <dl className="carteirinha-dados">
                  <div><dt>CPF</dt><dd>{dados.cpfMascarado ?? '—'}</dd></div>
                  <div><dt>Nº da carteirinha</dt><dd>{numeroDaCarteirinha(dados.numeroCarteirinha)}</dd></div>
                  {ehBeneficiario && (
                    <div><dt>CadÚnico confirmado até</dt><dd>{dados.cadUnicoValidoAte ? mesEAno(dados.cadUnicoValidoAte) : 'Precisa confirmar'}</dd></div>
                  )}
                  <div><dt>No MedShare desde</dt><dd>{data(dados.membroDesde)}</dd></div>
                </dl>
              </div>
            </section>

            <div className="acoes-da-carteirinha-tela">
              {ehBeneficiario && (
                <Link to="/app/reservas" className="botao principal">Mostrar código de retirada</Link>
              )}
              <p className="nota-da-foto">
                Para trocar a foto, vá em <Link to="/app/conta/foto">Conta › Foto de perfil</Link>.
              </p>
            </div>
          </div>
        )}
      </div>
    </>
  )
}
