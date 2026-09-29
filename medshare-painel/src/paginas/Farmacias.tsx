import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/cliente'
import type { EnderecoDoCep, Municipio, PontoAdministrado } from '../api/tipos'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'

const DIAS = [
  { n: 1, nome: 'Seg' }, { n: 2, nome: 'Ter' }, { n: 3, nome: 'Qua' }, { n: 4, nome: 'Qui' },
  { n: 5, nome: 'Sex' }, { n: 6, nome: 'Sáb' }, { n: 7, nome: 'Dom' },
]

/**
 * UC08 — a rede de farmácias parceiras.
 *
 * A farmácia nasce inativa e só aparece para doação e retirada quando tem um
 * farmacêutico responsável com CRF. Para desativar, o estoque disponível vai
 * antes para outra farmácia (A2).
 */
export function Farmacias() {
  const [pontos, definirPontos] = useState<PontoAdministrado[]>([])
  const [municipios, definirMunicipios] = useState<Municipio[]>([])
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)
  const [criando, definirCriando] = useState(false)

  const carregar = useCallback(async () => {
    try {
      definirPontos(await api.get<PontoAdministrado[]>('/admin/pontos'))
    } catch (e) {
      definirErro(e)
    }
  }, [])

  useEffect(() => {
    void carregar()
    api.get<Municipio[]>('/municipios').then(definirMunicipios).catch(definirErro)
  }, [carregar])

  async function agir(acao: () => Promise<unknown>, mensagem: string) {
    definirErro(null)
    definirRecado(null)
    try {
      await acao()
      definirRecado(mensagem)
      await carregar()
      return true
    } catch (e) {
      definirErro(e)
      return false
    }
  }

  return (
    <>
      <div className="titulo-area">
        <h1>Farmácias parceiras</h1>
        <p>Cadastro, farmacêutico responsável, horário de atendimento e desativação.</p>
      </div>

      <AvisoDeErro erro={erro} />
      {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

      {criando ? (
        <NovaFarmacia
          municipios={municipios}
          aoCancelar={() => definirCriando(false)}
          aoSalvar={async (dados) => {
            if (await agir(() => api.post('/admin/pontos', dados),
              `${dados.nome} cadastrada. Vincule um farmacêutico para ativá-la.`)) {
              definirCriando(false)
            }
          }}
        />
      ) : (
        <div className="acoes" style={{ marginBottom: 18 }}>
          <button className="principal" onClick={() => definirCriando(true)}>Cadastrar farmácia</button>
        </div>
      )}

      {pontos.map((p) => (
        <CartaoDaFarmacia key={p.id} ponto={p} outras={pontos.filter((o) => o.id !== p.id && o.ativo)} agir={agir} />
      ))}
    </>
  )
}

function CartaoDaFarmacia({ ponto: p, outras, agir }: {
  ponto: PontoAdministrado
  outras: PontoAdministrado[]
  agir: (acao: () => Promise<unknown>, mensagem: string) => Promise<boolean>
}) {
  const [email, definirEmail] = useState('')
  const [crf, definirCrf] = useState('')
  const [uf, definirUf] = useState('SP')
  const [destino, definirDestino] = useState('')
  const [editandoHorario, definirEditandoHorario] = useState(false)
  const [horario, definirHorario] = useState({ dias: p.dias, abreAs: p.abreAs.slice(0, 5), fechaAs: p.fechaAs.slice(0, 5), vagasPorHora: p.vagasPorHora })

  return (
    <article className="bloco">
      <div className="bloco-topo">
        <div>
          <h2>{p.nome}</h2>
          <p className="codigo">CNPJ {p.cnpj}</p>
        </div>
        <div className={`status ${p.ativo ? 'andamento' : 'atencao'}`}>
          <span className="ponto" />
          <span className="texto">{p.ativo ? 'Ativa' : 'Inativa'}</span>
        </div>
      </div>

      <div className="grade">
        <div className="dado"><p className="rotulo">Endereço</p><p className="valor">{p.endereco} — {p.municipio}</p></div>
        <div className="dado"><p className="rotulo">Atendimento</p><p className="valor">{p.horario} · {p.vagasPorHora} entregas/h</p></div>
        <div className="dado">
          <p className="rotulo">Farmacêuticos</p>
          <p className="valor">{p.farmaceuticos.length ? p.farmaceuticos.map((f) => `${f.nome} (${f.crf})`).join(', ') : 'nenhum vinculado — a farmácia só é ativada com um farmacêutico'}</p>
        </div>
      </div>

      <div style={{ marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--linha)' }}>
        <p className="rotulo" style={{ marginBottom: 8 }}>Vincular farmacêutico responsável</p>
        <div className="linha-form">
          <label className="campo"><span>E-mail da conta no MedShare</span>
            <input type="email" value={email} onChange={(e) => definirEmail(e.target.value)} /></label>
          <label className="campo" style={{ maxWidth: 140 }}><span>CRF</span>
            <input type="text" inputMode="numeric" value={crf} onChange={(e) => definirCrf(e.target.value.replace(/\D/g, '').slice(0, 8))} /></label>
          <label className="campo" style={{ maxWidth: 80 }}><span>UF</span>
            <input type="text" value={uf} onChange={(e) => definirUf(e.target.value.replace(/[^a-zA-Z]/g, '').slice(0, 2).toUpperCase())} /></label>
        </div>
        <button
          className="secundario"
          disabled={!email.includes('@') || !crf || uf.length !== 2}
          onClick={() => agir(() => api.post(`/admin/pontos/${p.id}/farmaceuticos`, { email, crf, ufCrf: uf }),
            `Farmacêutico vinculado a ${p.nome}. A pessoa precisa sair e entrar de novo para ver o balcão.`)}
        >
          Vincular e ativar
        </button>
      </div>

      <div style={{ marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--linha)' }}>
        {!editandoHorario ? (
          <button type="button" className="texto-botao" onClick={() => definirEditandoHorario(true)}>Alterar horário de atendimento</button>
        ) : (
          <>
            <CamposDeHorario valor={horario} aoMudar={definirHorario} />
            <div className="acoes">
              <button className="secundario" onClick={async () => {
                if (await agir(() => api.put(`/admin/pontos/${p.id}/horario`, horario), `Horário de ${p.nome} atualizado.`)) {
                  definirEditandoHorario(false)
                }
              }}>Salvar horário</button>
              <button className="texto-botao" onClick={() => definirEditandoHorario(false)}>Cancelar</button>
            </div>
          </>
        )}
      </div>

      {p.ativo && (
        <div style={{ marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--linha)' }}>
          <p className="rotulo" style={{ marginBottom: 8 }}>Desativar</p>
          <p style={{ margin: '0 0 10px', fontSize: 13.5, color: 'var(--tinta-media)' }}>
            Antes, transfira o estoque disponível. Caixas agendadas ou reservadas precisam terminar o ciclo.
          </p>
          <div className="linha-form">
            <label className="campo"><span>Transferir estoque para</span>
              <select value={destino} onChange={(e) => definirDestino(e.target.value)}>
                <option value="">Escolha…</option>
                {outras.map((o) => <option key={o.id} value={o.id}>{o.nome}</option>)}
              </select>
            </label>
          </div>
          <div className="acoes">
            <button className="secundario" disabled={!destino} onClick={() =>
              agir(async () => {
                const r = await api.post<{ transferidas: number }>(`/admin/pontos/${p.id}/transferencia`, { destinoId: Number(destino) })
                return r
              }, 'Estoque transferido.')}>
              Transferir estoque
            </button>
            <button className="perigo" onClick={() => {
              if (window.confirm(`Desativar ${p.nome}? Ela deixa de aparecer para doação e retirada.`)) {
                void agir(() => api.post(`/admin/pontos/${p.id}/desativacao`), `${p.nome} desativada.`)
              }
            }}>
              Desativar farmácia
            </button>
          </div>
        </div>
      )}
    </article>
  )
}

interface Horario { dias: number[]; abreAs: string; fechaAs: string; vagasPorHora: number }

function CamposDeHorario({ valor, aoMudar }: { valor: Horario; aoMudar: (h: Horario) => void }) {
  return (
    <>
      <p className="rotulo" style={{ marginBottom: 8 }}>Dias de atendimento</p>
      <div className="dias" style={{ marginBottom: 12 }}>
        {DIAS.map((d) => (
          <button
            key={d.n}
            type="button"
            className={valor.dias.includes(d.n) ? 'marcado' : ''}
            onClick={() => aoMudar({
              ...valor,
              dias: valor.dias.includes(d.n) ? valor.dias.filter((x) => x !== d.n) : [...valor.dias, d.n].sort(),
            })}
          >
            {d.nome}
          </button>
        ))}
      </div>
      <div className="linha-form">
        <label className="campo"><span>Abre às</span>
          <input type="time" value={valor.abreAs} onChange={(e) => aoMudar({ ...valor, abreAs: e.target.value })} /></label>
        <label className="campo"><span>Fecha às</span>
          <input type="time" value={valor.fechaAs} onChange={(e) => aoMudar({ ...valor, fechaAs: e.target.value })} /></label>
        <label className="campo"><span>Entregas por hora</span>
          <input type="number" min={1} max={50} value={valor.vagasPorHora}
            onChange={(e) => aoMudar({ ...valor, vagasPorHora: Math.max(1, Number(e.target.value) || 1) })} /></label>
      </div>
    </>
  )
}

interface DadosDaNovaFarmacia extends Horario {
  nome: string; cnpj: string; cep: string; logradouro: string; numero: string
  complemento: string | null; bairro: string; municipioId: number
}

function NovaFarmacia({ municipios, aoSalvar, aoCancelar }: {
  municipios: Municipio[]
  aoSalvar: (dados: DadosDaNovaFarmacia) => Promise<void>
  aoCancelar: () => void
}) {
  const [nome, definirNome] = useState('')
  const [cnpj, definirCnpj] = useState('')
  const [cep, definirCep] = useState('')
  const [logradouro, definirLogradouro] = useState('')
  const [numero, definirNumero] = useState('')
  const [complemento, definirComplemento] = useState('')
  const [bairro, definirBairro] = useState('')
  const [municipioId, definirMunicipioId] = useState('')
  const [avisoDoCep, definirAvisoDoCep] = useState<string | null>(null)
  const [horario, definirHorario] = useState<Horario>({ dias: [1, 2, 3, 4, 5], abreAs: '08:00', fechaAs: '18:00', vagasPorHora: 4 })
  const [salvando, definirSalvando] = useState(false)

  useEffect(() => {
    definirAvisoDoCep(null)
    if (cep.length !== 8) return
    api.get<EnderecoDoCep>(`/enderecos/${cep}`)
      .then((e) => {
        if (e.logradouro) definirLogradouro(e.logradouro)
        if (e.bairro) definirBairro(e.bairro)
        if (e.atendido && e.municipioId) definirMunicipioId(String(e.municipioId))
        else definirAvisoDoCep(`CEP de ${e.municipio}/${e.uf}: fora da Grande São Paulo.`)
      })
      .catch(() => definirAvisoDoCep('CEP não encontrado.'))
  }, [cep])

  const completo = nome.trim() && cnpj.length === 14 && cep.length === 8 && logradouro.trim()
    && numero.trim() && bairro.trim() && municipioId && horario.dias.length > 0

  return (
    <article className="bloco">
      <h2 style={{ marginTop: 0 }}>Nova farmácia</h2>
      <div className="linha-form">
        <label className="campo"><span>Nome</span>
          <input type="text" maxLength={120} value={nome} onChange={(e) => definirNome(e.target.value)} /></label>
        <label className="campo"><span>CNPJ</span>
          <input type="text" inputMode="numeric" value={cnpj} onChange={(e) => definirCnpj(e.target.value.replace(/\D/g, '').slice(0, 14))} placeholder="Somente números" /></label>
      </div>
      <div className="linha-form">
        <label className="campo"><span>CEP</span>
          <input type="text" inputMode="numeric" value={cep} onChange={(e) => definirCep(e.target.value.replace(/\D/g, '').slice(0, 8))} />
          {avisoDoCep && <span className="apoio" style={{ color: 'var(--terracota-texto)' }}>{avisoDoCep}</span>}
        </label>
        <label className="campo"><span>Município</span>
          <select value={municipioId} onChange={(e) => definirMunicipioId(e.target.value)}>
            <option value="">Escolha…</option>
            {municipios.map((m) => <option key={m.id} value={m.id}>{m.nome}</option>)}
          </select>
        </label>
      </div>
      <div className="linha-form">
        <label className="campo"><span>Rua ou avenida</span>
          <input type="text" value={logradouro} onChange={(e) => definirLogradouro(e.target.value)} /></label>
        <label className="campo" style={{ maxWidth: 120 }}><span>Número</span>
          <input type="text" value={numero} onChange={(e) => definirNumero(e.target.value)} /></label>
      </div>
      <div className="linha-form">
        <label className="campo"><span>Complemento</span>
          <input type="text" value={complemento} onChange={(e) => definirComplemento(e.target.value)} /></label>
        <label className="campo"><span>Bairro</span>
          <input type="text" value={bairro} onChange={(e) => definirBairro(e.target.value)} /></label>
      </div>
      <CamposDeHorario valor={horario} aoMudar={definirHorario} />
      <div className="acoes">
        <button className="principal" disabled={!completo || salvando} onClick={async () => {
          definirSalvando(true)
          await aoSalvar({
            nome: nome.trim(), cnpj, cep, logradouro: logradouro.trim(), numero: numero.trim(),
            complemento: complemento.trim() || null, bairro: bairro.trim(), municipioId: Number(municipioId), ...horario,
          })
          definirSalvando(false)
        }}>
          {salvando ? 'Salvando…' : 'Cadastrar'}
        </button>
        <button className="secundario" onClick={aoCancelar}>Cancelar</button>
      </div>
    </article>
  )
}
