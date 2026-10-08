import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Doacao, FotoEnviada, Medicamento } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { rotuloDaRegra } from '../componentes/Regras'
import { BuscaDeMedicamento } from '../componentes/BuscaDeMedicamento'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { CodigoCopiavel } from '../componentes/CodigoCopiavel'
import { Camera, Confere } from '../componentes/Icones'
import { Passos } from '../componentes/Passos'
import { data } from '../formatos'

/**
 * Cadastro de uma doação, em passos.
 *
 * Era uma tela só, com tudo junto. Virou fluxo por um motivo prático: lote e
 * validade são copiados de uma caixa na mão, e errar um caractere só vira
 * divergência com a leitura automática — ou seja, vira análise na central e
 * dias de espera. A tela de conferência existe para esse erro aparecer antes
 * de virar problema, com a foto ao lado para comparar.
 *
 * O estado mora todo aqui, num objeto só, e os passos leem dele. Assim voltar
 * um passo não perde nada do que já foi preenchido, que é o que a pessoa
 * espera quando aperta "voltar" — e é o que fazia a versão anterior parecer
 * hostil.
 *
 * As regras conferidas aqui (RN01, RN02) não substituem o servidor; elas só
 * evitam que a pessoa preencha tudo para receber um "não" no fim.
 */

const PASSOS = ['Medicamento', 'Foto', 'Dados', 'Conferir']

type Rascunho = {
  medicamento: Medicamento | null
  arquivo: File | null
  previa: string | null
  lote: string
  validade: string
  quantidade: number
  lacreDeclarado: boolean
}

const VAZIO: Rascunho = {
  medicamento: null,
  arquivo: null,
  previa: null,
  lote: '',
  validade: '',
  quantidade: 1,
  lacreDeclarado: false,
}

export function NovaDoacao() {
  const [passo, definirPasso] = useState(0)
  const [rascunho, definirRascunho] = useState<Rascunho>(VAZIO)
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)
  const [criadas, definirCriadas] = useState<Doacao[] | null>(null)

  const mudar = (parte: Partial<Rascunho>) => definirRascunho((r) => ({ ...r, ...parte }))

  const diasDeValidade = rascunho.validade
    ? Math.round((new Date(rascunho.validade).getTime() - Date.now()) / 86_400_000)
    : null
  const validadeCurta = diasDeValidade !== null && diasDeValidade < 30

  function escolherFoto(evento: React.ChangeEvent<HTMLInputElement>) {
    const escolhido = evento.target.files?.[0] ?? null
    if (rascunho.previa) URL.revokeObjectURL(rascunho.previa)
    mudar({ arquivo: escolhido, previa: escolhido ? URL.createObjectURL(escolhido) : null })
  }

  /** O que falta para liberar o botão de cada passo. */
  const podeAvancar = [
    rascunho.medicamento !== null,
    rascunho.arquivo !== null,
    rascunho.lote.trim() !== '' && rascunho.validade !== '' && !validadeCurta && rascunho.lacreDeclarado,
    true,
  ][passo]

  async function enviar() {
    const { medicamento, arquivo } = rascunho
    if (!medicamento || !arquivo) return
    definirErro(null)
    definirEnviando(true)
    try {
      // A foto vai primeiro: sem a URL dela, a doação não pode nem ser criada.
      const foto = await api.enviarArquivo<FotoEnviada>('/fotos', arquivo)
      // Cada caixa vira uma doação com código próprio (RN06).
      const doacoes = await api.post<Doacao[]>('/doacoes', {
        medicamentoId: medicamento.id,
        lote: rascunho.lote.trim().toUpperCase(),
        validade: rascunho.validade,
        fotoUrl: foto.url,
        quantidade: rascunho.quantidade,
        lacreDeclarado: rascunho.lacreDeclarado,
      })
      definirCriadas(doacoes)
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  // ------------------------------------------------------- confirmação --

  if (criadas) {
    const uma = criadas.length === 1
    return (
      <>
        <CabecalhoInterno titulo="Pronto" para="/app/doacoes" />
        <div className="conteudo-app cresce confirmacao">
          <div className="selo-pronto" aria-hidden="true"><Confere tamanho={34} /></div>

          <h1 className="humano" style={{ fontSize: '1.625rem', textAlign: 'center' }}>
            {uma ? 'Doação cadastrada' : `${criadas.length} doações cadastradas`}
          </h1>
          <p className="texto-confirmacao">
            {uma
              ? 'Estamos conferindo a foto da caixa. Assim que ela for liberada, você escolhe a farmácia e o horário da entrega.'
              : 'Cada caixa tem código e caminho próprios. Estamos conferindo a foto; assim que forem liberadas, você agenda a entrega de cada uma.'}
          </p>

          {uma ? (
            <CodigoCopiavel codigo={criadas[0].codigo} rotulo="Código da doação" />
          ) : (
            <ul className="lista-de-codigos">
              {criadas.map((d) => (
                <li key={d.codigo}><span>{d.codigo}</span></li>
              ))}
            </ul>
          )}

          <div className="rodape-acao" style={{ paddingBottom: 26 }}>
            <Link
              className="botao principal"
              to={uma ? `/app/doacoes/${criadas[0].codigo}` : '/app/doacoes'}
            >
              {uma ? 'Acompanhar esta doação' : 'Ver minhas doações'}
            </Link>
            <button
              type="button"
              className="texto-botao"
              style={{ marginTop: 12 }}
              onClick={() => {
                definirCriadas(null)
                definirRascunho(VAZIO)
                definirPasso(0)
              }}
            >
              Doar outro medicamento
            </button>
          </div>
        </div>
      </>
    )
  }

  // ------------------------------------------------------------ passos --

  return (
    <>
      <CabecalhoInterno
        titulo={PASSOS[passo]}
        para={passo === 0 ? '/app/doacoes' : undefined}
        aoVoltar={passo === 0 ? undefined : () => definirPasso((p) => p - 1)}
      />

      <div className="conteudo-app cresce" style={{ paddingTop: 16 }}>
        <Passos passos={PASSOS} atual={passo} />
        <AvisoDeErro erro={erro} />

        {passo === 0 && (
          <BuscaDeMedicamento
            aoEscolher={(m) => {
              mudar({ medicamento: m })
              definirPasso(1)
            }}
          />
        )}

        {passo === 1 && (
          <>
            <ResumoDoMedicamento
              medicamento={rascunho.medicamento!}
              aoTrocar={() => definirPasso(0)}
            />

            <p style={{ margin: '0 0 14px', fontSize: '0.9062rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
              Fotografe a caixa fechada, mostrando o lacre e a aba onde estão
              impressos o lote e a validade. É essa foto que a conferência
              automática compara com o que você digitar no próximo passo.
            </p>

            {rascunho.previa ? (
              <>
                <img src={rascunho.previa} alt="Foto da caixa que você tirou" className="foto-da-caixa" />
                <label className="botao secundario" style={{ marginTop: 10 }}>
                  <Camera /> Tirar outra foto
                  <input type="file" accept="image/*" capture="environment" onChange={escolherFoto} hidden />
                </label>
              </>
            ) : (
              <label className="botao secundario">
                <Camera /> Fotografar a caixa
                <input type="file" accept="image/*" capture="environment" onChange={escolherFoto} hidden />
              </label>
            )}
          </>
        )}

        {passo === 2 && (
          <>
            <ResumoDoMedicamento
              medicamento={rascunho.medicamento!}
              aoTrocar={() => definirPasso(0)}
            />

            <label className="campo">
              <span>Lote</span>
              <input
                type="text"
                value={rascunho.lote}
                onChange={(e) => mudar({ lote: e.target.value.toUpperCase() })}
                placeholder="Ex.: ALC2026A"
                autoComplete="off"
              />
              <span className="apoio">Está impresso na lateral ou no fundo da caixa</span>
            </label>

            <label className="campo">
              <span>Validade</span>
              <input
                type="date"
                value={rascunho.validade}
                onChange={(e) => mudar({ validade: e.target.value })}
              />
              {diasDeValidade !== null && !validadeCurta && (
                <span className="apoio">Faltam {diasDeValidade} dias. Serve.</span>
              )}
            </label>

            {validadeCurta && (
              <div className="aviso erro">
                <span className="regra">{rotuloDaRegra('RN02')}</span>
                Essa caixa vence em {diasDeValidade} dia(s). A rede precisa de pelo
                menos 30 dias de validade para dar tempo de a caixa ser conferida,
                reservada e retirada por alguém.
              </div>
            )}

            <label className="campo">
              <span>Quantas caixas iguais (mesmo lote)?</span>
              <input
                type="number"
                min={1}
                max={10}
                value={rascunho.quantidade}
                onChange={(e) =>
                  mudar({ quantidade: Math.min(10, Math.max(1, Number(e.target.value) || 1)) })
                }
              />
              <span className="apoio">Cada caixa ganha um código e segue seu próprio caminho.</span>
            </label>

            <label className="marcar">
              <input
                type="checkbox"
                checked={rascunho.lacreDeclarado}
                onChange={(e) => mudar({ lacreDeclarado: e.target.checked })}
              />
              <span>Declaro que a embalagem está lacrada de fábrica e nunca foi aberta.</span>
            </label>
          </>
        )}

        {passo === 3 && (
          <Conferencia rascunho={rascunho} aoCorrigir={definirPasso} />
        )}
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 26 }}>
        {passo < 3 ? (
          <button
            className="principal"
            disabled={!podeAvancar}
            onClick={() => definirPasso((p) => p + 1)}
          >
            Continuar
          </button>
        ) : (
          <button className="principal" disabled={enviando} onClick={enviar}>
            {enviando ? 'Enviando…' : 'Confirmar e enviar'}
          </button>
        )}
      </div>
    </>
  )
}

function ResumoDoMedicamento({
  medicamento,
  aoTrocar,
}: {
  medicamento: Medicamento
  aoTrocar: () => void
}) {
  return (
    <div className="resumo-medicamento">
      <div>
        <h2 className="humano" style={{ fontSize: '1.1875rem' }}>{medicamento.nomeComercial}</h2>
        <p className="detalhe">{medicamento.apresentacao}</p>
      </div>
      <button type="button" className="texto-botao" onClick={aoTrocar}>Trocar</button>
    </div>
  )
}

/**
 * O último passo antes de enviar.
 *
 * Cada linha tem o seu próprio "corrigir", que leva direto ao passo de onde o
 * dado veio. Um botão único de "voltar" obrigaria a pessoa a percorrer o fluxo
 * inteiro de novo só para trocar um caractere do lote.
 */
function Conferencia({
  rascunho,
  aoCorrigir,
}: {
  rascunho: Rascunho
  aoCorrigir: (passo: number) => void
}) {
  const linhas: { rotulo: string; valor: string; passo: number }[] = [
    { rotulo: 'Medicamento', valor: rascunho.medicamento?.nomeComercial ?? '—', passo: 0 },
    { rotulo: 'Apresentação', valor: rascunho.medicamento?.apresentacao ?? '—', passo: 0 },
    { rotulo: 'Lote', valor: rascunho.lote || '—', passo: 2 },
    { rotulo: 'Validade', valor: rascunho.validade ? data(rascunho.validade) : '—', passo: 2 },
    {
      rotulo: 'Quantidade',
      valor: rascunho.quantidade === 1 ? '1 caixa' : `${rascunho.quantidade} caixas`,
      passo: 2,
    },
  ]

  return (
    <>
      <p style={{ margin: '0 0 16px', fontSize: '0.9062rem', lineHeight: 1.5, color: 'var(--tinta-media)' }}>
        Confira o lote e a validade com a caixa na mão. Eles são comparados com
        a foto, e um caractere trocado manda a doação para análise.
      </p>

      {rascunho.previa && (
        <img src={rascunho.previa} alt="Foto da caixa que você tirou" className="foto-da-caixa" />
      )}
      <button
        type="button"
        className="texto-botao"
        style={{ margin: '8px 0 20px' }}
        onClick={() => aoCorrigir(1)}
      >
        Trocar a foto
      </button>

      <ul className="conferencia">
        {linhas.map((linha) => (
          <li key={linha.rotulo}>
            <div>
              <p className="rotulo">{linha.rotulo}</p>
              <p className="valor">{linha.valor}</p>
            </div>
            <button
              type="button"
              className="texto-botao"
              onClick={() => aoCorrigir(linha.passo)}
              aria-label={`Corrigir ${linha.rotulo.toLowerCase()}`}
            >
              Corrigir
            </button>
          </li>
        ))}
      </ul>

      <p className="declaracao-conferida">
        <Confere tamanho={16} /> Você declarou que a embalagem está lacrada de fábrica.
      </p>
    </>
  )
}
