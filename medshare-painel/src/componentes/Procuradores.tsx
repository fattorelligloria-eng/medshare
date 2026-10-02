import { useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { Procurador } from '../api/tipos'
import { AvisoDeErro } from './Aviso'

/**
 * UC07 A3 — quem pode retirar no lugar do beneficiário.
 *
 * O cadastro é prévio: no balcão, o farmacêutico só entrega a quem está nesta
 * lista (ou ao titular), conferindo o documento.
 */
export function Procuradores() {
  const [lista, definirLista] = useState<Procurador[]>([])
  const [nome, definirNome] = useState('')
  const [cpf, definirCpf] = useState('')
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  useEffect(() => {
    api.get<Procurador[]>('/necessidades/procuradores').then(definirLista).catch(definirErro)
  }, [])

  async function adicionar() {
    definirErro(null)
    definirEnviando(true)
    try {
      const novo = await api.post<Procurador>('/necessidades/procuradores', { nome: nome.trim(), cpf })
      definirLista((atual) => [...atual, novo].sort((a, b) => a.nome.localeCompare(b.nome)))
      definirNome('')
      definirCpf('')
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  async function remover(procurador: Procurador) {
    definirErro(null)
    try {
      await api.delete(`/necessidades/procuradores/${procurador.id}`)
      definirLista((atual) => atual.filter((p) => p.id !== procurador.id))
    } catch (e) {
      definirErro(e)
    }
  }

  return (
    <div style={{ marginTop: 28, paddingTop: 22, borderTop: '1px solid var(--linha)' }}>
      <h2 className="humano" style={{ fontSize: '1.1875rem' }}>Quem pode retirar por você</h2>
      <p style={{ margin: '8px 0 14px', fontSize: '0.875rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
        Se você não puder ir à farmácia, cadastre aqui quem vai. A pessoa leva o próprio documento,
        a receita e o código de retirada.
      </p>
      <AvisoDeErro erro={erro} />

      {lista.map((p) => (
        <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0' }}>
          <span style={{ fontSize: '0.875rem' }}>{p.nome} · CPF final {p.cpf.slice(-4)}</span>
          <button type="button" className="texto-botao" onClick={() => remover(p)}>Remover</button>
        </div>
      ))}

      <label className="campo" style={{ marginTop: 10 }}>
        <span>Nome completo</span>
        <input type="text" value={nome} maxLength={120} onChange={(e) => definirNome(e.target.value)} />
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
      <button
        className="secundario"
        disabled={enviando || nome.trim().length < 3 || cpf.length !== 11}
        onClick={adicionar}
      >
        {enviando ? 'Salvando…' : 'Adicionar pessoa'}
      </button>
    </div>
  )
}
