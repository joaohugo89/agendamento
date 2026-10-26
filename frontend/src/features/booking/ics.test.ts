import { describe, expect, it } from 'vitest'

import { buildIcsFile } from './ics'
import type { BookingConfirmationResponse } from '../../types/api'

const booking: BookingConfirmationResponse = {
  id: 'f1b9c2d4-0000-4000-8000-000000000001',
  businessName: 'Barbearia Prime',
  businessAddress: 'Rua das Palmeiras, 120 - Centro',
  serviceName: 'Corte + Barba',
  professionalName: 'Joao',
  customerName: 'Rafael',
  startsAt: '2026-03-10T13:00:00Z',
  endsAt: '2026-03-10T14:00:00Z',
  priceCents: 6500,
  timezone: 'America/Sao_Paulo',
}

describe('buildIcsFile', () => {
  it('writes a single event with the booking window', () => {
    const ics = buildIcsFile(booking)

    expect(ics).toContain('BEGIN:VEVENT')
    expect(ics).toContain('DTSTART:20260310T130000Z')
    expect(ics).toContain('DTEND:20260310T140000Z')
    expect(ics).toContain(`UID:${booking.id}`)
    expect(ics.split('\r\n').filter((line) => line === 'END:VEVENT')).toHaveLength(1)
  })

  it('escapes commas so the address stays in one property', () => {
    const ics = buildIcsFile(booking)

    expect(ics).toContain('LOCATION:Rua das Palmeiras\\, 120 - Centro')
  })

  it('never lets a value break out of its own line', () => {
    const ics = buildIcsFile({
      ...booking,
      businessAddress: 'Rua A\r\nSUMMARY:injetado',
      serviceName: 'Corte\nBarba',
    })

    // The injected text survives only as escaped content, never as a property of its own.
    expect(ics.split('\r\n')).not.toContain('SUMMARY:injetado')
    expect(ics).toContain('LOCATION:Rua A\\nSUMMARY:injetado')
    expect(ics).toContain('SUMMARY:Corte\\nBarba - Barbearia Prime')
  })
})
