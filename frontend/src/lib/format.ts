const WEEKDAY_NAMES = [
  'Segunda-feira',
  'Terca-feira',
  'Quarta-feira',
  'Quinta-feira',
  'Sexta-feira',
  'Sabado',
  'Domingo',
] as const

export function formatPrice(priceCents: number): string {
  return (priceCents / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function formatDuration(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`
  }
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours}h` : `${hours}h ${rest}min`
}

/** 1 = Monday .. 7 = Sunday, as the API sends it. */
export function weekdayName(dayOfWeek: number): string {
  return WEEKDAY_NAMES[dayOfWeek - 1] ?? ''
}

export function formatIsoDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year ?? 1970, (month ?? 1) - 1, day ?? 1)
}

export function formatLongDate(value: string, timezone?: string): string {
  const date = value.includes('T') ? new Date(value) : parseIsoDate(value)
  return date.toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    timeZone: value.includes('T') ? timezone : undefined,
  })
}

export function formatShortDate(value: string): string {
  const date = parseIsoDate(value)
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
}

export function formatTime(iso: string, timezone: string): string {
  return new Date(iso).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: timezone,
  })
}
