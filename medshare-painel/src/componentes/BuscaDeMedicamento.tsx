import { useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { Medicamento, Pagina } from '../api/tipos'
import { AvisoDeErro } from './Aviso'
import { reais } from '../formatos'

/**
 * Busca no catálogo, usada pelo doador e por quem precisa.
 *
 * Espera 400 ms depois da última tecla antes de chamar a API. Sem isso,
 * digitar "alecensa" dispararia oito requisições e as respostas poderiam
 * chegar fora de ordem — a tela mostraria o resultado de "alec" depois do
 * resultado completo.
 *
 * O catálogo devolve só medicamentos de alto custo (RN07). Quem procura
 * dipirona não encontra, e isso é a regra funcionando.
 */
export function BuscaDeMedicamento({ aoEscolher }: { aoEscolher: (m: Medicamento) => void }) {
  const [termo, definirTermo] = useState('')
  const [resultados, definirResultados] = useState<Medicamento[]>([])
  const [buscando, definirBuscando] = useState(false)
  const [erro, definirErro] = useState<unknown>(null)

  useEffect(() => {
    if (termo.trim().length < 2) {
      definirResultados([])
      return
    }
    const relogio = setTimeout(async () => {
      definirBuscando(true)
      try {
        const pagina = await api.get<Pagina<Medicamento>>(
          `/medicamentos?termo=${encodeURIComponent(termo.trim())}&size=20`,
        )
        definirResultados(pagina.content)
        definirErro(null)
      } catch (e) {
        definirErro(e)
      } finally {
        definirBuscando(false)
      }
    }, 400)
    return () => clearTimeout(relogio)
  }, [termo])

  return (
    <>
      <label className="campo">
        <span>Nome ou princípio ativo</span>
        <input
          type="text"
          value={termo}
          onChange={(e) => definirTermo(e.target.value)}
          placeholder="Ex.: Alecensa, alectinibe"
          autoFocus
        />
      </label>

      <AvisoDeErro erro={erro} />

      {termo.trim().length < 2 && (
        <div className="vazio">
          <h3>Digite para buscar</h3>
          <p>A rede trabalha com medicamentos de alto custo, acima de R$ 150.</p>
        </div>
      )}

      {buscando && <p style={{ color: 'var(--tinta-fraca)', fontSize: '0.875rem' }}>Buscando…</p>}

      {!buscando && termo.trim().length >= 2 && resultados.length === 0 && (
        <div className="vazio">
          <h3>Nada encontrado</h3>
          <p>Confira a grafia. Medicamentos abaixo de R$ 150 não entram na rede.</p>
        </div>
      )}

      {resultados.map((m) => (
        <button
          key={m.id}
          type="button"
          onClick={() => aoEscolher(m)}
          className="item"
          style={{
            display: 'block', width: '100%', textAlign: 'left', background: 'none',
            padding: '18px 0', borderRadius: 0, fontWeight: 400,
          }}
        >
          <h3 className="humano" style={{ fontSize: '1.1875rem' }}>{m.nomeComercial}</h3>
          <p className="detalhe">{m.principioAtivo}</p>
          <p className="detalhe">{m.apresentacao}</p>
          <p style={{ margin: '7px 0 0', fontSize: '0.8125rem', color: 'var(--verde-escuro)', fontWeight: 600 }}>
            Preço de referência {reais(m.pmc)}
          </p>
        </button>
      ))}
    </>
  )
}
