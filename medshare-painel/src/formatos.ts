/**
 * Formatação de exibição.
 *
 * A API manda data como "2026-09-28" e data com hora em ISO. O painel só
 * exibe — nunca calcula com essas datas — então recortar o texto resolve sem
 * arrastar uma biblioteca de datas para dentro do pacote.
 */

export function data(iso?: string | null): string {
  if (!iso) return '—'
  const partes = iso.slice(0, 10).split('-')
  return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : iso
}

export function dataComHora(iso?: string | null): string {
  if (!iso) return '—'
  const hora = iso.split('T')[1]?.slice(0, 5)
  return hora ? `${data(iso)} às ${hora}` : data(iso)
}

export function reais(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

/** "Maria Aparecida da Silva" vira "Maria" — para a saudação do app. */
export function primeiroNome(nome: string): string {
  return nome.trim().split(/\s+/)[0] ?? nome
}
