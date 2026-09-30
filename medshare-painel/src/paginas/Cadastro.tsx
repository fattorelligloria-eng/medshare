import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import type { EnderecoDoCep, Municipio, Sessao } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Logo } from '../componentes/Logo'
import { Voltar } from '../componentes/Icones'

/**
 * Cadastro em três passos.
 *
 * O primeiro passo é a escolha do papel, e ela vem ANTES de qualquer dado —
 * porque é ela que decide o resto do caminho: quem vai receber precisa
 * comprovar o NIS (RN08), quem vai doar não precisa. Perguntar isso no fim,
 * depois de a pessoa já ter digitado tudo, seria fazê-la descobrir tarde que
 * falta mais uma etapa.
 *
 * Dá para marcar os dois: quem doou um medicamento hoje pode precisar de outro
 * amanhã, e o modelo do sistema sempre permitiu acumular papéis.
 */
export function Cadastro() {
  const navegar = useNavigate()
  const { definirSessaoManualmente } = useAutenticacao()

  const [passo, definirPasso] = useState<1 | 2 | 3>(1)
  const [querDoar, definirQuerDoar] = useState(false)
  const [querReceber, definirQuerReceber] = useState(false)

  const [nome, definirNome] = useState('')
  const [cpf, definirCpf] = useState('')
  const [email, definirEmail] = useState('')
  const [senha, definirSenha] = useState('')
  const [telefone, definirTelefone] = useState('')

  const [cep, definirCep] = useState('')
  const [logradouro, definirLogradouro] = useState('')
  const [numero, definirNumero] = useState('')
  const [complemento, definirComplemento] = useState('')
  const [bairro, definirBairro] = useState('')
  const [municipioId, definirMunicipioId] = useState('')
  const [municipios, definirMunicipios] = useState<Municipio[]>([])

  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  useEffect(() => {
    api.get<Municipio[]>('/municipios')
      .then(definirMunicipios)
      .catch(definirErro)
  }, [])

  // RN09 — com o CEP completo, o endereço vem do ViaCEP e já avisa se está
  // fora da Grande São Paulo, antes de a pessoa preencher o resto.
  const [avisoDoCep, definirAvisoDoCep] = useState<string | null>(null)
  useEffect(() => {
    definirAvisoDoCep(null)
    if (cep.length !== 8) return
    let atual = true
    api.get<EnderecoDoCep>(`/enderecos/${cep}`)
      .then((e) => {
        if (!atual) return
        if (e.logradouro) definirLogradouro(e.logradouro)
        if (e.bairro) definirBairro(e.bairro)
        if (e.atendido && e.municipioId) {
          definirMunicipioId(String(e.municipioId))
        } else {
          definirMunicipioId('')
          definirAvisoDoCep(`Este CEP é de ${e.municipio}/${e.uf}. O MedShare atende só os 39 municípios da Grande São Paulo.`)
        }
      })
      .catch(() => { if (atual) definirAvisoDoCep('Não encontramos este CEP. Confira os números ou preencha o endereço à mão.') })
    return () => { atual = false }
  }, [cep])

  const passo1Ok = querDoar || querReceber
  const passo2Ok =
    nome.trim().length > 2 &&
    cpf.length === 11 &&
    email.includes('@') &&
    senha.length >= 8
  const passo3Ok =
    cep.length === 8 &&
    logradouro.trim() !== '' &&
    numero.trim() !== '' &&
    bairro.trim() !== '' &&
    municipioId !== ''

  async function criarConta() {
    definirErro(null)
    definirEnviando(true)
    try {
      const papeis = [
        ...(querDoar ? ['DOADOR'] : []),
        ...(querReceber ? ['BENEFICIARIO'] : []),
      ]
      const sessao = await api.post<Sessao>('/autenticacao/cadastro', {
        nome: nome.trim(),
        cpf,
        email: email.trim().toLowerCase(),
        senha,
        telefone: telefone || null,
        cep,
        logradouro: logradouro.trim(),
        numero: numero.trim(),
        complemento: complemento.trim() || null,
        bairro: bairro.trim(),
        municipioId: Number(municipioId),
        papeis,
      })
      definirSessaoManualmente(sessao)

      // Quem vai receber cai direto na verificação do NIS: sem ela não há
      // reserva, e descobrir isso só na hora de reservar seria frustrante.
      navegar(querReceber ? '/app/cadunico?novo=1' : '/app/doacoes', { replace: true })
    } catch (e) {
      definirErro(e)
      definirEnviando(false)
    }
  }

  function voltar() {
    if (passo === 1) navegar('/')
    else definirPasso((passo - 1) as 1 | 2)
  }

  return (
    <div className="entrada">
      <div className="cartao-entrada">

          <div style={{ paddingBottom: 4 }}>
            <div className="cabecalho-app">
              <button type="button" className="icone-botao" onClick={voltar} aria-label="Voltar">
                <Voltar />
              </button>
              <Logo tamanho={26} />
            </div>

            <div style={{ display: 'flex', gap: 6, marginTop: 22 }}>
              {[1, 2, 3].map((n) => (
                <span
                  key={n}
                  style={{
                    height: 3,
                    flex: 1,
                    borderRadius: 999,
                    background: n <= passo ? 'var(--verde)' : 'var(--linha)',
                  }}
                />
              ))}
            </div>
          </div>

          <div style={{ paddingTop: 26 }}>
            <AvisoDeErro erro={erro} />

            {passo === 1 && (
              <>
                <h1 className="humano" style={{ fontSize: 27 }}>O que traz você aqui?</h1>
                <p style={{ margin: '10px 0 22px', color: 'var(--tinta-media)', fontSize: 14.5, lineHeight: 1.5 }}>
                  Pode marcar os dois. Quem doa hoje pode precisar amanhã.
                </p>

                <label className={`escolha ${querDoar ? 'marcada' : ''}`}>
                  <input
                    type="checkbox"
                    checked={querDoar}
                    onChange={(e) => definirQuerDoar(e.target.checked)}
                  />
                  <span className="titulo">Quero doar</span>
                  <p className="sub">
                    Sobraram caixas lacradas de um tratamento e eu quero que sirvam
                    para alguém.
                  </p>
                </label>

                <label className={`escolha ${querReceber ? 'marcada' : ''}`}>
                  <input
                    type="checkbox"
                    checked={querReceber}
                    onChange={(e) => definirQuerReceber(e.target.checked)}
                  />
                  <span className="titulo">Preciso de um medicamento</span>
                  <p className="sub">
                    Tenho receita mas não tenho como comprar. Vou precisar informar
                    meu NIS do CadÚnico.
                  </p>
                </label>
              </>
            )}

            {passo === 2 && (
              <>
                <h1 className="humano" style={{ fontSize: 27 }}>Seus dados</h1>
                <p style={{ margin: '10px 0 22px', color: 'var(--tinta-media)', fontSize: 14.5 }}>
                  O CPF é usado só para identificar você na farmácia, na hora da entrega.
                </p>

                <label className="campo">
                  <span>Nome completo</span>
                  <input type="text" value={nome} onChange={(e) => definirNome(e.target.value)} />
                </label>

                <label className="campo">
                  <span>CPF</span>
                  <input
                    type="text"
                    inputMode="numeric"
                    value={cpf}
                    onChange={(e) => definirCpf(e.target.value.replace(/\D/g, '').slice(0, 11))}
                    placeholder="Somente números"
                  />
                </label>

                <label className="campo">
                  <span>E-mail</span>
                  <input type="email" value={email} onChange={(e) => definirEmail(e.target.value)} />
                </label>

                <label className="campo">
                  <span>Senha</span>
                  <input
                    type="password"
                    value={senha}
                    onChange={(e) => definirSenha(e.target.value)}
                  />
                  <span className="apoio">Pelo menos 8 caracteres</span>
                </label>

                <label className="campo">
                  <span>Telefone (opcional)</span>
                  <input
                    type="tel"
                    value={telefone}
                    onChange={(e) => definirTelefone(e.target.value.replace(/\D/g, '').slice(0, 11))}
                    placeholder="11999990000"
                  />
                </label>
              </>
            )}

            {passo === 3 && (
              <>
                <h1 className="humano" style={{ fontSize: 27 }}>Onde você mora</h1>
                <p style={{ margin: '10px 0 22px', color: 'var(--tinta-media)', fontSize: 14.5, lineHeight: 1.5 }}>
                  O MedShare atende os 39 municípios da Grande São Paulo. É o
                  endereço que define qual farmácia fica mais perto de você.
                </p>

                <label className="campo">
                  <span>CEP</span>
                  <input
                    type="text"
                    inputMode="numeric"
                    value={cep}
                    onChange={(e) => definirCep(e.target.value.replace(/\D/g, '').slice(0, 8))}
                    placeholder="Somente números"
                  />
                  {avisoDoCep && <span className="apoio" style={{ color: 'var(--terracota-texto)' }}>{avisoDoCep}</span>}
                </label>

                <label className="campo">
                  <span>Rua ou avenida</span>
                  <input type="text" value={logradouro} onChange={(e) => definirLogradouro(e.target.value)} />
                </label>

                <div style={{ display: 'flex', gap: 10 }}>
                  <label className="campo" style={{ flex: 1 }}>
                    <span>Número</span>
                    <input type="text" value={numero} onChange={(e) => definirNumero(e.target.value)} />
                  </label>
                  <label className="campo" style={{ flex: 1.4 }}>
                    <span>Complemento</span>
                    <input type="text" value={complemento} onChange={(e) => definirComplemento(e.target.value)} />
                  </label>
                </div>

                <label className="campo">
                  <span>Bairro</span>
                  <input type="text" value={bairro} onChange={(e) => definirBairro(e.target.value)} />
                </label>

                <label className="campo">
                  <span>Município</span>
                  <select value={municipioId} onChange={(e) => definirMunicipioId(e.target.value)}>
                    <option value="">Escolha…</option>
                    {municipios.map((m) => (
                      <option key={m.id} value={m.id}>{m.nome}</option>
                    ))}
                  </select>
                </label>
              </>
            )}
          </div>

          <div style={{ paddingTop: 26 }}>
            {passo < 3 ? (
              <button
                className="principal"
                disabled={passo === 1 ? !passo1Ok : !passo2Ok}
                onClick={() => definirPasso((passo + 1) as 2 | 3)}
              >
                Continuar
              </button>
            ) : (
              <button className="principal" disabled={!passo3Ok || enviando} onClick={criarConta}>
                {enviando ? 'Criando conta…' : 'Criar conta'}
              </button>
            )}
          </div>

      </div>
    </div>
  )
}
