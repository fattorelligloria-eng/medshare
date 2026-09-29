import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { FotoEnviada } from '../api/tipos'
import { AvisoDeErro } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { Camera } from '../componentes/Icones'

/**
 * RN03 — a receita médica.
 *
 * A foto da receita é dado sensível de saúde: fica com acesso restrito, é
 * aberta só pelo farmacêutico na hora da retirada e nunca é enviada para
 * nenhum serviço externo de inteligência artificial. Só a foto da embalagem
 * passa por leitura automática. A tela diz isso porque a pessoa tem o direito
 * de saber para onde vai a foto do documento dela.
 */
export function EnvioDeReceita() {
  const { id = '' } = useParams()
  const navegar = useNavigate()

  const [arquivo, definirArquivo] = useState<File | null>(null)
  const [previa, definirPrevia] = useState<string | null>(null)
  const [emissao, definirEmissao] = useState('')
  const [validade, definirValidade] = useState('')
  const [crm, definirCrm] = useState('')
  const [uf, definirUf] = useState('SP')
  const [erro, definirErro] = useState<unknown>(null)
  const [enviando, definirEnviando] = useState(false)

  function escolherFoto(evento: React.ChangeEvent<HTMLInputElement>) {
    const escolhido = evento.target.files?.[0] ?? null
    definirArquivo(escolhido)
    definirPrevia(escolhido ? URL.createObjectURL(escolhido) : null)
  }

  const podeEnviar =
    arquivo !== null && emissao !== '' && validade !== '' && crm.trim() !== '' && uf.length === 2

  async function enviar() {
    if (!arquivo) return
    definirErro(null)
    definirEnviando(true)
    try {
      const foto = await api.enviarArquivo<FotoEnviada>('/fotos', arquivo)
      await api.post(`/necessidades/${id}/receita`, {
        fotoUrl: foto.url,
        dataEmissao: emissao,
        validade,
        crmMedico: crm.trim(),
        ufCrm: uf.toUpperCase(),
      })
      navegar('/app/pedidos', { replace: true })
    } catch (e) {
      definirErro(e)
      definirEnviando(false)
    }
  }

  return (
    <>
      <CabecalhoInterno titulo="Enviar receita" para="/app/pedidos" />

      <div className="conteudo-app" style={{ paddingTop: 22, flex: 1 }}>
        <AvisoDeErro erro={erro} />

        <h1 className="humano" style={{ fontSize: 25 }}>A receita do médico</h1>
        <p style={{ margin: '10px 0 0', fontSize: 14.5, lineHeight: 1.5, color: 'var(--tinta-media)' }}>
          Fotografe a receita inteira, com o carimbo e a assinatura visíveis.
        </p>
        <p style={{ margin: '10px 0 18px', fontSize: 13, lineHeight: 1.5, color: 'var(--tinta-fraca)' }}>
          Ela é aberta apenas pelo farmacêutico, na hora da retirada. Nunca sai
          da rede nem passa por leitura automática.
        </p>

        {previa ? (
          <>
            <img
              src={previa}
              alt="Foto da receita"
              style={{ width: '100%', height: 210, objectFit: 'cover', borderRadius: 'var(--raio)' }}
            />
            <label className="botao secundario" style={{ marginTop: 10 }}>
              <Camera /> Tirar outra foto
              <input type="file" accept="image/*" capture="environment" onChange={escolherFoto} hidden />
            </label>
          </>
        ) : (
          <label className="botao secundario">
            <Camera /> Fotografar a receita
            <input type="file" accept="image/*" capture="environment" onChange={escolherFoto} hidden />
          </label>
        )}

        <div style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', gap: 10 }}>
            <label className="campo" style={{ flex: 2 }}>
              <span>CRM do médico</span>
              <input
                type="text"
                inputMode="numeric"
                value={crm}
                onChange={(e) => definirCrm(e.target.value.replace(/\D/g, '').slice(0, 10))}
              />
            </label>
            <label className="campo" style={{ flex: 1 }}>
              <span>UF</span>
              <input
                type="text"
                value={uf}
                onChange={(e) => definirUf(e.target.value.replace(/[^a-zA-Z]/g, '').slice(0, 2).toUpperCase())}
              />
            </label>
          </div>

          <label className="campo">
            <span>Data de emissão</span>
            <input type="date" value={emissao} onChange={(e) => definirEmissao(e.target.value)} />
          </label>

          <label className="campo">
            <span>Válida até</span>
            <input type="date" value={validade} onChange={(e) => definirValidade(e.target.value)} />
            <span className="apoio">Está impresso na receita, em geral 30 ou 180 dias após a emissão.</span>
          </label>
        </div>
      </div>

      <div className="rodape-acao" style={{ paddingBottom: 26 }}>
        <button className="principal" disabled={!podeEnviar || enviando} onClick={enviar}>
          {enviando ? 'Enviando…' : 'Enviar receita'}
        </button>
      </div>
    </>
  )
}
