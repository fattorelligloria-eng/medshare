import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { RespostaDoCadUnico } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { Senha } from '../componentes/Icones'
import { data } from '../formatos'

/**
 * RN08 — a verificação do NIS.
 *
 * Pedir um número de documento sem dizer para quê é o tipo de coisa que faz a
 * pessoa desistir, ou desconfiar com razão. Por isso a tela explica de onde
 * vem o critério antes de pedir o número: ele não foi inventado pelo projeto,
 * é o cadastro oficial do governo federal para programas sociais.
 */
export function CadUnico() {
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  const acabouDeSeCadastrar = parametros.get('novo') === '1'

  const [nis, definirNis] = useState('')
  const [resposta, definirResposta] = useState<RespostaDoCadUnico | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  async function verificar() {
    definirErro(null)
    definirResposta(null)
    definirEnviando(true)
    try {
      definirResposta(await api.post<RespostaDoCadUnico>('/necessidades/cadunico', { nis }))
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <>
      <CabecalhoInterno titulo="CadÚnico" para={acabouDeSeCadastrar ? undefined : '/app/conta'} />

      <div className="conteudo-app cresce" style={{ paddingTop: 22 }}>
        {acabouDeSeCadastrar && (
          <AvisoDeSucesso>Conta criada. Falta só um passo.</AvisoDeSucesso>
        )}

        <div style={{ marginTop: acabouDeSeCadastrar ? 18 : 0 }}>
          <Senha tamanho={30} cor="var(--verde)" />
          <h1 className="humano" style={{ fontSize: '1.625rem', marginTop: 14 }}>
            Precisamos confirmar seu NIS
          </h1>
          <p style={{ margin: '12px 0 0', fontSize: '0.9062rem', lineHeight: 1.55, color: 'var(--tinta-media)' }}>
            Os medicamentos desta rede são caros e são poucos. Para que cheguem a
            quem mais precisa, usamos o <strong>CadÚnico</strong> — o cadastro do
            governo federal para programas sociais. O critério não é nosso, é o
            oficial.
          </p>
          <p style={{ margin: '12px 0 0', fontSize: '0.8438rem', lineHeight: 1.5, color: 'var(--tinta-fraca)' }}>
            Seu NIS está no Cartão do Cidadão, no aplicativo CadÚnico ou no
            extrato do Bolsa Família. A verificação vale por 12 meses.
          </p>
        </div>

        <div style={{ marginTop: 26 }}>
          <AvisoDeErro erro={erro} />

          {resposta && resposta.confirmado && (
            <AvisoDeSucesso>
              Tudo certo. Sua verificação vale até {data(resposta.validoAte)}.
            </AvisoDeSucesso>
          )}

          {resposta && !resposta.confirmado && resposta.precisaDeAnaliseHumana && (
            <div className="aviso erro">
              <span className="regra">RN08</span>
              Não encontramos esse NIS como beneficiário de programa social. Isso
              não é um não: seu caso foi para a nossa equipe, que confere à mão e
              responde em até 48 horas.
            </div>
          )}

          <label className="campo">
            <span>Número do NIS</span>
            <input
              type="text"
              inputMode="numeric"
              value={nis}
              onChange={(e) => definirNis(e.target.value.replace(/\D/g, '').slice(0, 11))}
              placeholder="11 dígitos"
            />
          </label>
        </div>
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 26 }}>
        {resposta?.confirmado ? (
          <button className="principal" onClick={() => navegar('/app/pedidos', { replace: true })}>
            Continuar
          </button>
        ) : resposta?.precisaDeAnaliseHumana ? (
          /* Sem isto a tela virava beco sem saída: o NIS foi para conferência
             humana, não há "Continuar", e a pessoa ficava parada olhando o
             aviso. Ela avisa quando a equipe responder. */
          <button className="principal" onClick={() => navegar('/app/conta', { replace: true })}>
            Entendi, voltar para a conta
          </button>
        ) : (
          <button className="principal" disabled={nis.length !== 11 || enviando} onClick={verificar}>
            {enviando ? 'Consultando…' : 'Verificar'}
          </button>
        )}
      </div>
    </>
  )
}
