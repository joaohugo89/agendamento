import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ToastProvider } from '../../components/ui/Toast'
import { BookingPage } from './BookingPage'

const business = {
  name: 'Barbearia Prime',
  slug: 'barbearia-prime',
  description: 'Cortes classicos',
  address: 'Rua das Palmeiras, 120',
  phone: '(11) 4002-8922',
  timezone: 'America/Sao_Paulo',
  services: [
    {
      id: 'service-1',
      name: 'Corte',
      description: 'Corte na tesoura',
      priceCents: 4000,
      durationMinutes: 30,
      active: true,
    },
  ],
  // Open every weekday, so any future day in the calendar can be picked.
  openingHours: [1, 2, 3, 4, 5, 6, 7].map((dayOfWeek) => ({
    id: `hour-${dayOfWeek}`,
    dayOfWeek,
    startTime: '09:00',
    endTime: '18:00',
  })),
}

const defaultProfessionals = [{ id: 'professional-1', name: 'Joao', workingDays: [] as number[] }]

/** Overridden by a single test; reset before each one, so the mock stays shared and honest. */
let professionals = defaultProfessionals
let openingHours = business.openingHours
const slots = [{ startsAt: '2030-06-10T12:00:00Z', time: '09:00' }]

const confirmation = {
  id: 'appointment-1',
  businessName: business.name,
  businessAddress: business.address,
  serviceName: 'Corte',
  professionalName: 'Joao',
  customerName: 'Rafael Lima',
  startsAt: slots[0]?.startsAt,
  endsAt: '2030-06-10T12:30:00Z',
  priceCents: 4000,
  timezone: business.timezone,
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

let postedBodies: unknown[] = []
let conflictOnFirstPost = false

function mockFetch() {
  return vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const url = input instanceof Request ? input.url : input.toString()

    if (init?.method === 'POST' && url.includes('/appointments')) {
      postedBodies.push(JSON.parse(String(init.body)))
      if (conflictOnFirstPost) {
        conflictOnFirstPost = false
        return jsonResponse(
          { title: 'Horario indisponivel', detail: 'Este horario acabou de ser reservado.' },
          409,
        )
      }
      return jsonResponse(confirmation, 201)
    }
    if (url.includes('/availability')) {
      return jsonResponse(slots)
    }
    if (url.includes('/professionals')) {
      return jsonResponse(professionals)
    }
    return jsonResponse({ ...business, openingHours })
  })
}

function renderBookingPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={['/agendar/barbearia-prime']}>
          <Routes>
            <Route path="/agendar/:slug" element={<BookingPage />} />
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

/** Walks the flow up to the summary screen. */
async function walkToSummary(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: 'Agendar agora' }))
  await user.click(await screen.findByRole('button', { name: /Corte/ }))
  await user.click(await screen.findByRole('button', { name: 'Joao' }))

  const calendar = await screen.findByText(/Dias sem atendimento/)
  const dayButtons = within(calendar.parentElement as HTMLElement)
    .getAllByRole('button')
    .filter((button) => /^\d+$/.test(button.textContent ?? '') && !button.hasAttribute('disabled'))
  await user.click(dayButtons[0] as HTMLElement)

  await user.click(await screen.findByRole('button', { name: '09:00' }))

  await user.type(screen.getByLabelText('Nome'), 'Rafael Lima')
  await user.type(screen.getByLabelText('WhatsApp'), '11988887777')
  await user.click(screen.getByRole('button', { name: 'Continuar' }))

  await screen.findByRole('button', { name: 'Confirmar agendamento' })
}

describe('BookingPage', () => {
  beforeEach(() => {
    postedBodies = []
    conflictOnFirstPost = false
    professionals = defaultProfessionals
    openingHours = business.openingHours
    vi.stubGlobal('fetch', mockFetch())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the business page with services and opening hours', async () => {
    renderBookingPage()

    expect(await screen.findByRole('heading', { name: 'Barbearia Prime' })).toBeInTheDocument()
    expect(screen.getByText('Rua das Palmeiras, 120')).toBeInTheDocument()
    expect(screen.getByText('R$ 40,00')).toBeInTheDocument()
  })

  it('books an appointment through every step and shows the confirmation', async () => {
    const user = userEvent.setup()
    renderBookingPage()

    await walkToSummary(user)
    expect(screen.getByText('Rafael Lima')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Confirmar agendamento' }))

    expect(await screen.findByText('Agendamento confirmado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Adicionar ao calendario' })).toBeInTheDocument()
    expect(postedBodies).toHaveLength(1)
    expect(postedBodies[0]).toMatchObject({
      serviceId: 'service-1',
      professionalId: 'professional-1',
      customerName: 'Rafael Lima',
    })
  })

  it('returns to the time step when the slot was taken meanwhile', async () => {
    conflictOnFirstPost = true
    const user = userEvent.setup()
    renderBookingPage()

    await walkToSummary(user)
    await user.click(screen.getByRole('button', { name: 'Confirmar agendamento' }))

    expect(await screen.findByRole('heading', { name: 'Escolha o horario' })).toBeInTheDocument()
    expect(screen.getByText('Este horario acabou de ser reservado.')).toBeInTheDocument()
  })

  it('offers no date when the professional does not work on any open weekday', async () => {
    // The shop opens on Mondays only and this professional works on Tuesdays.
    openingHours = [{ id: 'hour-1', dayOfWeek: 1, startTime: '09:00', endTime: '18:00' }]
    professionals = [{ id: 'professional-1', name: 'Joao', workingDays: [2] }]
    const user = userEvent.setup()
    renderBookingPage()

    await user.click(await screen.findByRole('button', { name: 'Agendar agora' }))
    await user.click(await screen.findByRole('button', { name: /Corte/ }))
    await user.click(await screen.findByRole('button', { name: 'Joao' }))

    const calendar = await screen.findByText(/Dias sem atendimento/)
    const dayButtons = within(calendar.parentElement as HTMLElement)
      .getAllByRole('button')
      .filter((button) => /^\d+$/.test(button.textContent ?? ''))

    expect(dayButtons.length).toBeGreaterThan(0)
    expect(dayButtons.every((button) => button.hasAttribute('disabled'))).toBe(true)
  })

  it('does not advance while the customer form is invalid', async () => {
    const user = userEvent.setup()
    renderBookingPage()

    await user.click(await screen.findByRole('button', { name: 'Agendar agora' }))
    await user.click(await screen.findByRole('button', { name: /Corte/ }))
    await user.click(await screen.findByRole('button', { name: 'Joao' }))

    const calendar = await screen.findByText(/Dias sem atendimento/)
    const dayButtons = within(calendar.parentElement as HTMLElement)
      .getAllByRole('button')
      .filter((button) => /^\d+$/.test(button.textContent ?? '') && !button.hasAttribute('disabled'))
    await user.click(dayButtons[0] as HTMLElement)
    await user.click(await screen.findByRole('button', { name: '09:00' }))

    await user.type(screen.getByLabelText('Nome'), 'Ra')
    await user.click(screen.getByRole('button', { name: 'Continuar' }))

    await waitFor(() => {
      expect(screen.getByText('Informe seu nome completo')).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: 'Confirmar agendamento' })).not.toBeInTheDocument()
  })
})
