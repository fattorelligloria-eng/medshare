import { useState } from 'react'
import { api } from '../api/cliente'
import type { EnderecoDoCep, MeusDados, Municipio } from '../api/tipos'
import { AvisoDeErro } from './Aviso'
import { useEffect } from 'react'

/**
 * Os dados do cadastro: ver e corrigir.
 *
 * Começa fechado, em modo de leitura. Formulário aberto por padrão convida ao
 * erro de digitação em campo que ninguém queria mexer — e aqui esses campos
 * decidem para qual farmácia a pessoa é mandada.
 *
 * CPF e e-mail aparecem mas não editam: um é a identidade dela no sistema, o
 * outro é como ela entra. Mudar qualquer um é outra operação, com conferência
 * própria.
 */
export function DadosDoCadastro(
  { dados, aoSalvar }: { dados: MeusDados; aoSalvar: (novos: MeusDados) => void },
) {
  const [editando, definirEditando] = useState(false)
  const [erro, definirErro] = useState<unknown>(null)
  const [salvando, definirSalvando] = useState(false)
  const [municipios, definirMunicipios] = useState<Municipio[]>([])

  const [form, definirForm] = useState({
    nome: dados.nome,
    telefone: dados.telefone ?? '',
    cep: dados.endereco?.cep ?? '',
    logradouro: dados.endereco?.logradouro ?? '',
    numero: dados.endereco?.numero ?? '',
    complemento: dados.endereco?.complemento ?? '',
    bairro: dados.endereco?.bairro ?? '',
    municipioId: dados.endereco?.municipioId ?? 0,
  })

  useEffect(() => {
    if (editando && municipios.length === 0) {
      api.get<Municipio[]>('/municipios').then(definirMunicipios).catch(() => {})
    }
  }, [editando, municipios.length])

  const mudar = (campo: keyof typeof form, valor: string | number) =>
    definirForm((f) => ({ ...f, [campo]: valor }))

  /** Oito dígitos digitados: busca o endereço e preenche o resto. */
  async function cepDigitado(valor: string) {
    const limpo = valor.replace(/\D/g, '').slice(0, 8)
    mudar('cep', limpo)
    if (limpo.length !== 8) return
    try {
      const achado = await api.get<EnderecoDoCep>(`/enderecos/${limpo}`)
      definirForm((f) => ({
        ...f,
        logradouro: achado.logradouro || f.logradouro,
        bairro: achado.bairro || f.bairro,
        municipioId: achado.municipioId ?? f.municipioId,
      }))
    } catch {
      // CEP não encontrado não trava nada: a pessoa digita o endereço na mão.
    }
  }

  async function salvar() {
    definirErro(null)
    definirSalvando(true)
    try {
      const novos = await api.put<MeusDados>('/usuarios/eu', {
        ...form,
        municipioId: Number(form.municipioId),
        complemento: form.complemento || null,
      })
      aoSalvar(novos)
      definirEditando(false)
    } catch (e) {
      definirErro(e)
    } finally {
      definirSalvando(false)
    }
  }

  if (!editando) {
    return (
      <section className="cartao-conta" aria-labelledby="t-cadastro">
        <div className="topo-cartao">
          <h2 id="t-cadastro" className="humano">Meus dados</h2>
          <button type="button" className="texto-botao" onClick={() => definirEditando(true)}>
            Editar
          </button>
        </div>

        <dl className="lista-de-dados">
          <div><dt>Nome</dt><dd>{dados.nome}</dd></div>
          <div><dt>CPF</dt><dd>{dados.cpfMascarado ?? '—'}</dd></div>
          <div><dt>E-mail</dt><dd>{dados.email}</dd></div>
          <div><dt>Telefone</dt><dd>{dados.telefone || '—'}</dd></div>
          {dados.endereco && (
            <>
              <div>
                <dt>Endereço</dt>
                <dd>
                  {dados.endereco.logradouro}, {dados.endereco.numero}
                  {dados.endereco.complemento && ` — ${dados.endereco.complemento}`}
                </dd>
              </div>
              <div>
                <dt>Bairro e cidade</dt>
                <dd>{dados.endereco.bairro} · {dados.endereco.municipio}</dd>
              </div>
              <div><dt>CEP</dt><dd>{dados.endereco.cep}</dd></div>
            </>
          )}
        </dl>
      </section>
    )
  }

  return (
    <section className="cartao-conta" aria-labelledby="t-cadastro">
      <h2 id="t-cadastro" className="humano">Meus dados</h2>
      <p className="explicacao-secao">
        O endereço decide quais farmácias aparecem para você, então vale conferir.
      </p>

      <AvisoDeErro erro={erro} />

      <label className="campo">
        <span>Nome</span>
        <input value={form.nome} onChange={(e) => mudar('nome', e.target.value)} />
      </label>

      <label className="campo">
        <span>Telefone</span>
        <input value={form.telefone} onChange={(e) => mudar('telefone', e.target.value)}
               placeholder="11999998888" />
      </label>

      <label className="campo">
        <span>CEP</span>
        <input value={form.cep} onChange={(e) => cepDigitado(e.target.value)}
               inputMode="numeric" placeholder="01001000" />
      </label>

      <div className="linha-form">
        <label className="campo">
          <span>Logradouro</span>
          <input value={form.logradouro} onChange={(e) => mudar('logradouro', e.target.value)} />
        </label>
        <label className="campo" style={{ maxWidth: 120 }}>
          <span>Número</span>
          <input value={form.numero} onChange={(e) => mudar('numero', e.target.value)} />
        </label>
      </div>

      <label className="campo">
        <span>Complemento</span>
        <input value={form.complemento} onChange={(e) => mudar('complemento', e.target.value)} />
      </label>

      <label className="campo">
        <span>Bairro</span>
        <input value={form.bairro} onChange={(e) => mudar('bairro', e.target.value)} />
      </label>

      <label className="campo">
        <span>Município</span>
        <select value={form.municipioId} onChange={(e) => mudar('municipioId', e.target.value)}>
          <option value={0}>Escolha…</option>
          {municipios.map((m) => <option key={m.id} value={m.id}>{m.nome}</option>)}
        </select>
      </label>

      <div className="acoes">
        <button className="principal" onClick={salvar} disabled={salvando || !form.nome.trim()}>
          {salvando ? 'Salvando…' : 'Salvar'}
        </button>
        <button className="secundario" onClick={() => { definirEditando(false); definirErro(null) }}>
          Cancelar
        </button>
      </div>
    </section>
  )
}
