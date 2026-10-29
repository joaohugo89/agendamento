import { request } from '../../lib/api'
import type {
  BookingConfirmationResponse,
  ProfessionalSummary,
  PublicBusinessResponse,
  SlotResponse,
} from '../../types/api'

const publicPath = (slug: string) => `/api/public/businesses/${encodeURIComponent(slug)}`

export const bookingKeys = {
  business: (slug: string) => ['public-business', slug] as const,
  professionals: (slug: string, serviceId: string) => ['public-professionals', slug, serviceId] as const,
  slotsRoot: ['public-slots'] as const,
  slots: (slug: string, serviceId: string, professionalId: string, date: string) =>
    ['public-slots', slug, serviceId, professionalId, date] as const,
}

export function fetchBusiness(slug: string): Promise<PublicBusinessResponse> {
  return request<PublicBusinessResponse>(publicPath(slug))
}

export function fetchProfessionals(slug: string, serviceId: string): Promise<ProfessionalSummary[]> {
  return request<ProfessionalSummary[]>(`${publicPath(slug)}/services/${serviceId}/professionals`)
}

export function fetchSlots(
  slug: string,
  serviceId: string,
  professionalId: string,
  date: string,
): Promise<SlotResponse[]> {
  return request<SlotResponse[]>(`${publicPath(slug)}/availability`, {
    query: { serviceId, professionalId, date },
  })
}

export interface BookingInput {
  serviceId: string
  professionalId: string
  startsAt: string
  customerName: string
  customerWhatsapp: string
  notes?: string
}

export function createAppointment(
  slug: string,
  input: BookingInput,
): Promise<BookingConfirmationResponse> {
  return request<BookingConfirmationResponse>(`${publicPath(slug)}/appointments`, {
    method: 'POST',
    body: input,
  })
}
