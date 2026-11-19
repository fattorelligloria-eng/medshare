import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from '../componentes/Aviso'
import { Marca } from '../componentes/Logo'

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
        <Marca />

        <div className="meio-da-entrada">
          <div>
            <h1 className="humano titulo-entrada">
              <em>Compartilhar</em> também é cuidar.
            </h1>
            <p className="subtitulo-entrada">
              Remédio que sobrou de um tratamento, chegando a quem não tem como
              comprar. Tudo passa por uma farmácia parceira.
            </p>
          </div>

          <form onSubmit={enviar}>
            <AvisoDeErro erro={erro} />

            <label className="campo">
              <span>E-mail</span>
              <input
                type="email"
                value={email}
                onChange={(e) => definirEmail(e.target.value)}
                autoComplete="username"
                placeholder="natasha@email.com"
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
          </form>
        </div>

        <div className="link-de-cadastro">
          <button
            type="button"
            className="texto-botao"
            onClick={() => navegar('/cadastro')}
          >
            Ainda não tenho conta
          </button>
        </div>
      </div>
    </div>
  )
}
