/**
 * Formatação de exibição.
 *
 * A API manda data como "2026-09-28" e data com hora em ISO, com fuso. Data
 * pura é só recortada; data com hora é convertida para o horário de Brasília,
 * porque o servidor pode devolver em UTC ("...T17:00:00Z" são 14h aqui).
 */

const HORARIO_DE_BRASILIA = new Intl.DateTimeFormat('pt-BR', {
  timeZone: 'America/Sao_Paulo',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function data(iso?: string | null): string {
  if (!iso) return '—'
  const partes = iso.slice(0, 10).split('-')
  return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : iso
}

export function dataComHora(iso?: string | null): string {
  if (!iso) return '—'
  const momento = new Date(iso)
  if (Number.isNaN(momento.getTime())) return data(iso)
  const p = Object.fromEntries(
    HORARIO_DE_BRASILIA.formatToParts(momento).map((parte) => [parte.type, parte.value]),
  )
  return `${p.day}/${p.month}/${p.year} às ${p.hour}:${p.minute}`
}

export function reais(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

/** "Maria Aparecida da Silva" vira "Maria" — para a saudação do app. */
export function primeiroNome(nome: string): string {
  return nome.trim().split(/\s+/)[0] ?? nome
}
