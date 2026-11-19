import { useState, type ChangeEvent } from 'react'
import { api } from '../api/cliente'
import type { FotoEnviada, MeusDados } from '../api/tipos'
import { useAutenticacao } from '../contexto/Autenticacao'
import { AvisoDeErro, AvisoDeSucesso } from '../componentes/Aviso'
import { CabecalhoInterno } from '../componentes/CabecalhoInterno'
import { FotoDePerfil } from '../componentes/FotoDePerfil'
import { guardarMeusDados, useMeusDados } from '../componentes/MeusDados'

/**
 * Foto de perfil: tirar, escolher da galeria ou tirar fora.
 *
 * A foto sobe pelo mesmo /api/fotos das caixas e das receitas (nome sorteado,
 * tipo conferido pelos primeiros bytes) e só depois é marcada como a do perfil.
 * Ela aparece na carteirinha digital, no Início e no menu — para a própria
 * pessoa. Quem doa e quem recebe nunca veem a foto um do outro.
 */
export function FotoDePerfilTela() {
  const { sessao } = useAutenticacao()
  const { dados } = useMeusDados()
  const [enviando, definirEnviando] = useState(false)
  const [erro, definirErro] = useState<unknown>(null)
  const [recado, definirRecado] = useState<string | null>(null)

  const temFoto = Boolean(dados?.fotoUrl)

  async function usar(evento: ChangeEvent<HTMLInputElement>) {
    const arquivo = evento.target.files?.[0]
    // Limpa o campo: escolher a mesma foto duas vezes seguidas tem de funcionar.
    evento.target.value = ''
    if (!arquivo || !sessao) return
    definirErro(null)
    definirRecado(null)
    definirEnviando(true)
    try {
      const foto = await api.enviarArquivo<FotoEnviada>('/fotos', arquivo)
      const novos = await api.put<MeusDados>('/usuarios/eu/foto', { url: foto.url })
      guardarMeusDados(sessao.usuarioId, novos)
      definirRecado('Foto atualizada.')
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  async function tirarFora() {
    if (!sessao) return
    definirErro(null)
    definirRecado(null)
    definirEnviando(true)
    try {
      const novos = await api.delete<MeusDados>('/usuarios/eu/foto')
      guardarMeusDados(sessao.usuarioId, novos)
      definirRecado('Foto removida.')
    } catch (e) {
      definirErro(e)
    } finally {
      definirEnviando(false)
    }
  }

  return (
    <>
      <CabecalhoInterno titulo="Foto de perfil" para="/app/conta" />

      <div className="conteudo-app cresce tela-da-foto" style={{ paddingTop: 20 }}>
        <div className="vitrine-da-foto">
          <FotoDePerfil url={dados?.fotoUrl} tamanho={160} />
          <p className="nome">{dados?.nome ?? sessao?.nome}</p>
          <p>Sua foto aparece na carteirinha digital.</p>
        </div>

        <AvisoDeErro erro={erro} />
        {recado && <AvisoDeSucesso>{recado}</AvisoDeSucesso>}

        <div className="botoes-da-foto">
          <label className={enviando ? 'botao principal desativado' : 'botao principal'}>
            {enviando ? 'Enviando…' : 'Tirar uma foto'}
            <input type="file" accept="image/jpeg,image/png,image/webp" capture="user"
                   onChange={usar} disabled={enviando} hidden />
          </label>
          <label className={enviando ? 'botao secundario desativado' : 'botao secundario'}>
            Escolher da galeria
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={usar} disabled={enviando} hidden />
          </label>
          {temFoto && (
            <button type="button" className="texto-botao perigo-texto" onClick={tirarFora} disabled={enviando}>
              Remover foto
            </button>
          )}
        </div>

        <p className="nota-da-foto">Quem doa e quem recebe nunca veem a foto um do outro.</p>
      </div>
    </>
  )
}
