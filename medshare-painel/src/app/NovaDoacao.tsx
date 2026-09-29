import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Doacao, FotoEnviada, Medicamento } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { BuscaDeMedicamento } from '../componentes/BuscaDeMedicamento'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { Camera } from '../componentes/Icones'

/**
 * Cadastro de uma doação: escolher o medicamento, fotografar a caixa,
 * informar lote e validade.
 *
 * A validade é conferida aqui também, antes de enviar. Não para substituir a
 * regra do servidor — ela continua valendo — mas para a pessoa não fotografar
 * a caixa e preencher tudo só para receber um "não" no final.
 */
export function NovaDoacao() {
  const navegar = useNavigate()
  const [medicamento, definirMedicamento] = useState<Medicamento | null>(null)
  const [lote, definirLote] = useState('')
  const [validade, definirValidade] = useState('')
  const [quantidade, definirQuantidade] = useState(1)
  const [lacreDeclarado, definirLacreDeclarado] = useState(false)
  const [arquivo, definirArquivo] = useState<File | null>(null)
  const [previa, definirPrevia] = useState<string | null>(null)
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  const diasDeValidade = validade
    ? Math.round((new Date(validade).getTime() - Date.now()) / 86_400_000)
    : null
  const validadeCurta = diasDeValidade !== null && diasDeValidade < 30

  function escolherFoto(evento: React.ChangeEvent<HTMLInputElement>) {
    const escolhido = evento.target.files?.[0] ?? null
    definirArquivo(escolhido)
    definirPrevia(escolhido ? URL.createObjectURL(escolhido) : null)
  }

  const podeEnviar =
    medicamento !== null &&
    lote.trim() !== '' &&
    validade !== '' &&
    !validadeCurta &&
    arquivo !== null &&
    lacreDeclarado

  async function enviar() {
    if (!medicamento || !arquivo) return
    definirErro(null)
    definirEnviando(true)
    try {
      // A foto vai primeiro: sem a URL dela, a doação não pode nem ser criada.
      const foto = await api.enviarArquivo<FotoEnviada>('/fotos', arquivo)
      // Cada caixa vira uma doação com código próprio (RN06).
      const doacoes = await api.post<Doacao[]>('/doacoes', {
        medicamentoId: medicamento.id,
        lote: lote.trim().toUpperCase(),
        validade,
        fotoUrl: foto.url,
        quantidade,
        lacreDeclarado,
      })
      navegar(doacoes.length === 1 ? `/app/doacoes/${doacoes[0].codigo}` : '/app/doacoes', { replace: true })
    } catch (e) {
      definirErro(e)
      definirEnviando(false)
    }
  }

  if (!medicamento) {
    return (
      <>
        <CabecalhoInterno titulo="Qual medicamento?" para="/app/doacoes" />
        <div className="conteudo-app" style={{ paddingTop: 22, flex: 1 }}>
          <BuscaDeMedicamento aoEscolher={definirMedicamento} />
        </div>
      </>
    )
  }

  return (
    <>
      <CabecalhoInterno titulo="Dados da caixa" />

      <div className="conteudo-app" style={{ paddingTop: 22, flex: 1 }}>
        <AvisoDeErro erro={erro} />

        <div style={{ paddingBottom: 18, borderBottom: '1px solid var(--linha)', marginBottom: 20 }}>
          <h2 className="humano" style={{ fontSize: 21 }}>{medicamento.nomeComercial}</h2>
          <p className="detalhe">{medicamento.apresentacao}</p>
          <button
            type="button"
            className="texto-botao"
            onClick={() => definirMedicamento(null)}
          >
            Trocar medicamento
          </button>
        </div>

        <p className="rotulo">Foto da caixa</p>
        <p style={{ margin: '6px 0 12px', fontSize: 13.5, lineHeight: 1.45, color: 'var(--tinta-media)' }}>
          Fotografe a caixa fechada, mostrando o lacre e a aba onde estão
          impressos o lote e a validade. A leitura automática confere os dois
          com o que você digitar.
        </p>

        {previa ? (
          <>
            <img
              src={previa}
              alt="Foto da caixa"
              style={{ width: '100%', height: 190, objectFit: 'cover', borderRadius: 'var(--raio)' }}
            />
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

        <div style={{ marginTop: 24 }}>
          <label className="campo">
            <span>Lote</span>
            <input
              type="text"
              value={lote}
              onChange={(e) => definirLote(e.target.value.toUpperCase())}
              placeholder="Ex.: ALC2026A"
            />
            <span className="apoio">Está impresso na lateral ou no fundo da caixa</span>
          </label>

          <label className="campo">
            <span>Validade</span>
            <input type="date" value={validade} onChange={(e) => definirValidade(e.target.value)} />
          </label>

          <label className="campo">
            <span>Quantas caixas iguais (mesmo lote)?</span>
            <input
              type="number"
              min={1}
              max={10}
              value={quantidade}
              onChange={(e) => definirQuantidade(Math.min(10, Math.max(1, Number(e.target.value) || 1)))}
            />
            <span className="apoio">Cada caixa ganha um código e segue seu próprio caminho.</span>
          </label>

          <label className="marcar">
            <input
              type="checkbox"
              checked={lacreDeclarado}
              onChange={(e) => definirLacreDeclarado(e.target.checked)}
            />
            <span>Declaro que a embalagem está lacrada de fábrica e nunca foi aberta.</span>
          </label>
        </div>

        {validadeCurta && (
          <div className="aviso erro">
            <span className="regra">RN02</span>
            Essa caixa vence em {diasDeValidade} dia(s). A rede precisa de pelo
            menos 30 dias de validade para dar tempo de chegar a quem precisa.
          </div>
        )}
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 26 }}>
        <button className="principal" disabled={!podeEnviar || enviando} onClick={enviar}>
          {enviando ? 'Enviando…' : 'Enviar doação'}
        </button>
      </div>
    </>
  )
}
