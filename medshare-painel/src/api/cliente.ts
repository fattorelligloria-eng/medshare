import type { Sessao } from './tipos'

const CHAVE_DA_SESSAO = 'medshare.sessao'

/**
 * Erro vindo da API, ja traduzido.
 *
 * Guarda a regra (RN02, RN07...) separada da mensagem porque a tela mostra as
 * duas coisas de formas diferentes: a regra como etiqueta, a mensagem como
 * texto. O back-end devolve exatamente nesse formato.
 */
export class ErroDaApi extends Error {
  constructor(
    readonly status: number,
    mensagem: string,
    readonly regra?: string,
    readonly campos?: { campo: string; problema: string }[],
  ) {
    super(mensagem)
  }
}

export function lerSessao(): Sessao | null {
  const guardada = localStorage.getItem(CHAVE_DA_SESSAO)
  if (!guardada) return null
  try {
    return JSON.parse(guardada) as Sessao
  } catch {
    // Sessao corrompida no navegador: melhor pedir login de novo do que quebrar.
    localStorage.removeItem(CHAVE_DA_SESSAO)
    return null
  }
}

export function guardarSessao(sessao: Sessao | null) {
  if (sessao) localStorage.setItem(CHAVE_DA_SESSAO, JSON.stringify(sessao))
  else localStorage.removeItem(CHAVE_DA_SESSAO)
}

async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const sessao = lerSessao()
  const resposta = await fetch(`/api${caminho}`, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(sessao ? { Authorization: `Bearer ${sessao.tokenDeAcesso}` } : {}),
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  })

  if (resposta.status === 204) return undefined as T

  const texto = await resposta.text()
  const dados = texto ? JSON.parse(texto) : null

  if (!resposta.ok) {
    if (resposta.status === 401) {
      guardarSessao(null)
      throw new ErroDaApi(401, 'Sua sessao expirou. Entre de novo.')
    }
    const mensagem =
      dados?.mensagem ??
      dados?.campos?.map((c: { campo: string; problema: string }) => `${c.campo}: ${c.problema}`).join('; ') ??
      'Nao foi possivel completar a operacao.'
    throw new ErroDaApi(resposta.status, mensagem, dados?.regra, dados?.campos)
  }

  return dados as T
}

/**
 * Envio de foto: multipart, não JSON.
 *
 * O Content-Type NÃO é definido aqui de propósito — o navegador precisa
 * escrevê-lo sozinho para incluir o "boundary" que separa as partes. Defini-lo
 * na mão quebra o envio de um jeito difícil de descobrir.
 */
async function enviarArquivo<T>(caminho: string, arquivo: File): Promise<T> {
  const sessao = lerSessao()
  const formulario = new FormData()
  formulario.append('arquivo', arquivo)

  const resposta = await fetch(`/api${caminho}`, {
    method: 'POST',
    headers: sessao ? { Authorization: `Bearer ${sessao.tokenDeAcesso}` } : {},
    body: formulario,
  })

  const texto = await resposta.text()
  const dados = texto ? JSON.parse(texto) : null

  if (!resposta.ok) {
    throw new ErroDaApi(
      resposta.status,
      dados?.mensagem ?? 'Não consegui enviar a foto.',
      dados?.regra,
    )
  }
  return dados as T
}

export const api = {
  get: <T>(caminho: string) => chamar<T>('GET', caminho),
  post: <T>(caminho: string, corpo?: unknown) => chamar<T>('POST', caminho, corpo),
  delete: <T>(caminho: string) => chamar<T>('DELETE', caminho),
  enviarArquivo,
}
