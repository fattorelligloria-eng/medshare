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

const DIA_DA_SEMANA = new Intl.DateTimeFormat('pt-BR', {
  timeZone: 'America/Sao_Paulo',
  weekday: 'long',
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

/** "quinta-feira, 02/10 às 14:00" — como a pessoa fala, não como o banco grava. */
export function diaEHora(iso?: string | null): string {
  if (!iso) return '—'
  const momento = new Date(iso)
  if (Number.isNaN(momento.getTime())) return dataComHora(iso)
  const p = Object.fromEntries(
    DIA_DA_SEMANA.formatToParts(momento).map((parte) => [parte.type, parte.value]),
  )
  return `${p.weekday}, ${p.day}/${p.month} às ${p.hour}:${p.minute}`
}

/**
 * Quanto falta, em português de gente: "faltam 2 dias", "é amanhã", "é hoje".
 *
 * A conta é feita em dias de calendário, não em horas divididas por 24. Um
 * agendamento às 9h de amanhã está a 17 horas de distância, e dizer "falta 1
 * dia" seria mentira menor do que dizer "faltam 0 dias" — mas "é amanhã" é o
 * que a pessoa entende sem pensar.
 */
export function quantoFalta(iso?: string | null): string | null {
  if (!iso) return null
  const alvo = new Date(iso)
  if (Number.isNaN(alvo.getTime())) return null

  const diaDe = (d: Date) => Date.UTC(d.getFullYear(), d.getMonth(), d.getDate())
  const dias = Math.round((diaDe(alvo) - diaDe(new Date())) / 86_400_000)

  if (dias < 0) return 'o horário já passou'
  if (dias === 0) return 'é hoje'
  if (dias === 1) return 'é amanhã'
  if (dias <= 30) return `faltam ${dias} dias`
  return null
}

/** Link de mapa para um endereço escrito. Abre o app no celular. */
export function linkDoMapa(endereco: string): string {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(endereco)}`
}
