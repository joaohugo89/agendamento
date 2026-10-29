import type { BookingConfirmationResponse } from '../../types/api'

function toIcsTimestamp(iso: string): string {
  return `${new Date(iso).toISOString().replace(/[-:]/g, '').split('.')[0]}Z`
}

/**
 * RFC 5545 escaping. Every kind of line break becomes a literal \n and other control
 * characters are dropped, so a value can never break out of its own property line.
 */
function escapeText(value: string): string {
  return value
    .replace(/[\\;,]/g, (match) => `\\${match}`)
    .replace(/\r\n|\r|\n/g, '\\n')
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '')
}

/** Built in the browser: the booking needs no server round trip to become a calendar entry. */
export function buildIcsFile(booking: BookingConfirmationResponse): string {
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//agendamento//booking//PT-BR',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:${escapeText(booking.id)}`,
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
  // Firefox only follows the click of an anchor that is in the document, and revoking the URL
  // in the same tick can cancel a download that has not started yet.
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.setTimeout(() => URL.revokeObjectURL(url), 0)
}
