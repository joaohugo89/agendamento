import type { BookingConfirmationResponse } from '../../types/api'

function toIcsTimestamp(iso: string): string {
  return `${new Date(iso).toISOString().replace(/[-:]/g, '').split('.')[0]}Z`
}

function escapeText(value: string): string {
  return value.replace(/[\\;,]/g, (match) => `\\${match}`).replace(/\n/g, '\\n')
}

/** Built in the browser: the booking needs no server round trip to become a calendar entry. */
export function buildIcsFile(booking: BookingConfirmationResponse): string {
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//agendamento//booking//PT-BR',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:${booking.id}`,
    `DTSTAMP:${toIcsTimestamp(new Date().toISOString())}`,
    `DTSTART:${toIcsTimestamp(booking.startsAt)}`,
    `DTEND:${toIcsTimestamp(booking.endsAt)}`,
    `SUMMARY:${escapeText(`${booking.serviceName} - ${booking.businessName}`)}`,
    `DESCRIPTION:${escapeText(`Profissional: ${booking.professionalName}`)}`,
  ]

  if (booking.businessAddress) {
    lines.push(`LOCATION:${escapeText(booking.businessAddress)}`)
  }

  lines.push('END:VEVENT', 'END:VCALENDAR')
  return lines.join('\r\n')
}

export function downloadIcsFile(booking: BookingConfirmationResponse): void {
  const blob = new Blob([buildIcsFile(booking)], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'agendamento.ics'
  link.click()
  URL.revokeObjectURL(url)
}
