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

/** Avisado quando a sessão acaba de vez, para a tela voltar ao login. */
export const EVENTO_SESSAO_ENCERRADA = 'medshare:sessao-encerrada'

export function guardarSessao(sessao: Sessao | null) {
  if (sessao) localStorage.setItem(CHAVE_DA_SESSAO, JSON.stringify(sessao))
  else localStorage.removeItem(CHAVE_DA_SESSAO)
}

function encerrarSessao() {
  guardarSessao(null)
  window.dispatchEvent(new Event(EVENTO_SESSAO_ENCERRADA))
}

/** Resposta de erro que não é JSON (ex.: página HTML de um proxy) não quebra a tela. */
function lerCorpo(texto: string): any {
  if (!texto) return null
  try {
    return JSON.parse(texto)
  } catch {
    return null
  }
}

/**
 * O token de acesso dura 2 horas. Quando ele vence, trocamos pelo de renovação
 * (14 dias) sem a pessoa perceber. Chamadas simultâneas esperam a mesma troca.
 */
let renovacaoEmAndamento: Promise<boolean> | null = null

function renovarSessao(): Promise<boolean> {
  renovacaoEmAndamento ??= (async () => {
    const sessao = lerSessao()
    if (!sessao?.tokenDeRenovacao) return false
    try {
      const resposta = await fetch('/api/autenticacao/renovacao', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenDeRenovacao: sessao.tokenDeRenovacao }),
      })
      if (!resposta.ok) return false
      guardarSessao((await resposta.json()) as Sessao)
      return true
    } catch {
      return false
    }
  })().finally(() => {
    renovacaoEmAndamento = null
  })
  return renovacaoEmAndamento
}

/**
 * A frase para quando a requisição não chega ao servidor.
 *
 * `navigator.onLine` distingue os dois casos que a pessoa resolve de formas
 * diferentes: sem internet ela mexe no Wi-Fi; servidor fora ela espera. Dizer
 * a coisa errada manda a pessoa consertar o que não está quebrado.
 */
function mensagemDeRedeForaDoAr(): string {
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    return 'Você está sem internet. Assim que a conexão voltar, tente de novo.'
  }
  return 'Não consegui falar com o servidor. Verifique sua conexão e tente de novo.'
}

/** Faz a requisição com o token atual e, se ele venceu, renova e tenta mais uma vez. */
async function comAutenticacao(montar: (cabecalhos: Record<string, string>) => RequestInit, caminho: string) {
  const enviar = async () => {
    const sessao = lerSessao()
    const cabecalhos: Record<string, string> = sessao ? { Authorization: `Bearer ${sessao.tokenDeAcesso}` } : {}
    try {
      return await fetch(`/api${caminho}`, montar(cabecalhos))
    } catch {
      // O fetch só estoura assim quando a requisição não chegou a lugar nenhum:
      // sem rede, servidor fora, DNS. O navegador diz "Failed to fetch", em
      // inglês, e isso ia parar na tela da pessoa.
      throw new ErroDaApi(0, mensagemDeRedeForaDoAr())
    }
  }

  let resposta = await enviar()
  if (resposta.status === 401 && lerSessao()) {
    if (await renovarSessao()) {
      resposta = await enviar()
    }
    if (resposta.status === 401) {
      encerrarSessao()
      throw new ErroDaApi(401, 'Sua sessão expirou. Entre de novo.')
    }
  }
  return resposta
}

async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const resposta = await comAutenticacao((cabecalhos) => ({
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...cabecalhos },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  }), caminho)

  if (resposta.status === 204) return undefined as T

  const dados = lerCorpo(await resposta.text())

  if (!resposta.ok) {
    const mensagem =
      dados?.mensagem ??
      dados?.campos?.map((c: { campo: string; problema: string }) => `${c.campo}: ${c.problema}`).join('; ') ??
      'Não foi possível completar a operação.'
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
  const resposta = await comAutenticacao((cabecalhos) => {
    const formulario = new FormData()
    formulario.append('arquivo', arquivo)
    return { method: 'POST', headers: cabecalhos, body: formulario }
  }, caminho)

  const dados = lerCorpo(await resposta.text())

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
  put: <T>(caminho: string, corpo?: unknown) => chamar<T>('PUT', caminho, corpo),
  delete: <T>(caminho: string) => chamar<T>('DELETE', caminho),
  enviarArquivo,
}
