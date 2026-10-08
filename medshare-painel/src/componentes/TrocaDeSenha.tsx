import { useState } from 'react'
import { api } from '../api/cliente'
import type { Sessao } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro } from './Aviso'

/**
 * Troca de senha.
 *
 * A senha atual é pedida porque sem isso quem pegasse o celular destravado
 * trocaria a senha e tomaria a conta. A conferência de verdade é no servidor —
 * aqui só evitamos a viagem quando a pessoa já errou algo óbvio.
 */
export function TrocaDeSenha({ aoTrocar }: { aoTrocar: () => void }) {
  const { definirSessaoManualmente } = useAutenticacao()
  const [aberto, definirAberto] = useState(false)
  const [atual, definirAtual] = useState('')
  const [nova, definirNova] = useState('')
  const [repetida, definirRepetida] = useState('')
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  const curta = nova.length > 0 && nova.length < 8
  const naoBate = repetida.length > 0 && nova !== repetida
  const igualAAnterior = nova.length > 0 && nova === atual
  const podeEnviar = atual && nova.length >= 8 && nova === repetida && !igualAAnterior

  function fechar() {
    definirAberto(false)
    definirAtual(''); definirNova(''); definirRepetida(''); definirErro(null)
  }

  async function enviar() {
    definirErro(null)
    definirEnviando(true)
    try {
      // A troca derruba todos os tokens antigos, inclusive o desta aba; a
      // resposta traz os novos, e guardá-los mantém a pessoa conectada aqui.
      const novaSessao = await api.post<Sessao>('/usuarios/eu/senha', { senhaAtual: atual, novaSenha: nova })
      definirSessaoManualmente(novaSessao)
      aoTrocar()
      fechar()
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <section className="cartao-conta" aria-labelledby="t-senha">
      <div className="topo-cartao">
        <h2 id="t-senha" className="humano">Senha</h2>
        {!aberto && (
          <button type="button" className="texto-botao" onClick={() => definirAberto(true)}>
            Trocar
          </button>
        )}
      </div>

      {!aberto && <p className="explicacao-secao" style={{ marginBottom: 0 }}>••••••••</p>}

      {aberto && (
        <>
          <AvisoDeErro erro={erro} />

          <label className="campo">
            <span>Senha atual</span>
            <input type="password" value={atual} autoComplete="current-password"
                   onChange={(e) => definirAtual(e.target.value)} />
          </label>

          <label className="campo">
            <span>Nova senha</span>
            <input type="password" value={nova} autoComplete="new-password"
                   onChange={(e) => definirNova(e.target.value)} />
            {curta && <small className="erro-de-campo">Pelo menos 8 caracteres.</small>}
            {igualAAnterior && (
              <small className="erro-de-campo">A nova senha precisa ser diferente da atual.</small>
            )}
          </label>

          <label className="campo">
            <span>Repita a nova senha</span>
            <input type="password" value={repetida} autoComplete="new-password"
                   onChange={(e) => definirRepetida(e.target.value)} />
            {naoBate && <small className="erro-de-campo">As duas não são iguais.</small>}
          </label>

          <div className="acoes">
            <button className="principal" onClick={enviar} disabled={!podeEnviar || enviando}>
              {enviando ? 'Trocando…' : 'Trocar senha'}
            </button>
            <button className="secundario" onClick={fechar}>Cancelar</button>
          </div>
        </>
      )}
    </section>
  )
}
