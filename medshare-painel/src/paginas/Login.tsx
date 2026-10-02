import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Logo } from '../componentes/Logo'

/**
 * Entrada.
 *
 * As contas de demonstração saíram da tela: senha impressa na interface é
 * coisa de ferramenta inacabada, e esta tela vai ser projetada numa sala de
 * aula. Quem apresenta já sabe o login; quem assiste não precisa ver.
 */
export function Login() {
  const { entrar } = useAutenticacao()
  const navegar = useNavigate()
  const [email, definirEmail] = useState('')
  const [senha, definirSenha] = useState('')
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    definirErro(null)
    definirEnviando(true)
    try {
      await entrar(email, senha)
    } catch (e) {
      definirErro(e)
      definirEnviando(false)
    }
  }

  return (
    <div className="entrada">
      <div className="cartao-entrada">

          <div>
            <Logo tamanho={52} />

            <h1 className="humano" style={{ fontSize: '1.9375rem', marginTop: 26, lineHeight: 1.15 }}>
              Remédio que sobrou<br />de um tratamento
            </h1>
            <p style={{ margin: '12px 0 0', fontSize: '0.9688rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
              chegando a quem não tem como comprar. Doador e quem recebe nunca
              se encontram: tudo passa por uma farmácia parceira.
            </p>
          </div>

          <form onSubmit={enviar} style={{ paddingTop: 30 }}>
            <AvisoDeErro erro={erro} />

            <label className="campo">
              <span>E-mail</span>
              <input
                type="email"
                value={email}
                onChange={(e) => definirEmail(e.target.value)}
                autoComplete="username"
                placeholder="voce@exemplo.com"
                required
              />
            </label>

            <label className="campo">
              <span>Senha</span>
              <input
                type="password"
                value={senha}
                onChange={(e) => definirSenha(e.target.value)}
                autoComplete="current-password"
                required
              />
            </label>

            <button className="principal" disabled={enviando || !email || !senha}>
              {enviando ? 'Entrando…' : 'Entrar'}
            </button>

            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <button
                type="button"
                className="texto-botao"
                onClick={() => navegar('/cadastro')}
              >
                Ainda não tenho conta
              </button>
            </div>
          </form>

      </div>
    </div>
  )
}
