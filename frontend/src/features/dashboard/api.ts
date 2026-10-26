import { request } from '../../lib/api'
import type {
  AppointmentResponse,
  AppointmentStatus,
  BlockResponse,
  BusinessHourResponse,
  BusinessResponse,
  ProfessionalResponse,
  ServiceResponse,
} from '../../types/api'

const authenticated = { authenticated: true } as const

export const dashboardKeys = {
  business: ['business'] as const,
  services: ['services'] as const,
  professionals: ['professionals'] as const,
  businessHours: ['business-hours'] as const,
  blocks: ['blocks'] as const,
  agenda: (date: string, professionalId: string | null) => ['agenda', date, professionalId] as const,
}

export function fetchBusiness(): Promise<BusinessResponse> {
  return request<BusinessResponse>('/api/business', authenticated)
}

export function fetchServices(): Promise<ServiceResponse[]> {
  return request<ServiceResponse[]>('/api/services', authenticated)
}

export interface ServiceInput {
  name: string
  description: string | null
  priceCents: number
  durationMinutes: number
  active: boolean
}

export function createService(input: ServiceInput): Promise<ServiceResponse> {
  return request<ServiceResponse>('/api/services', { ...authenticated, method: 'POST', body: input })
}

export function updateService(id: string, input: ServiceInput): Promise<ServiceResponse> {
  return request<ServiceResponse>(`/api/services/${id}`, { ...authenticated, method: 'PUT', body: input })
}

export function deleteService(id: string): Promise<void> {
  return request<void>(`/api/services/${id}`, { ...authenticated, method: 'DELETE' })
}

export function fetchProfessionals(): Promise<ProfessionalResponse[]> {
  return request<ProfessionalResponse[]>('/api/professionals', authenticated)
}

export interface ProfessionalInput {
  name: string
  active: boolean
  serviceIds: string[]
}

export function createProfessional(input: ProfessionalInput): Promise<ProfessionalResponse> {
  return request<ProfessionalResponse>('/api/professionals', {
    ...authenticated,
    method: 'POST',
    body: input,
  })
}

export function updateProfessional(id: string, input: ProfessionalInput): Promise<ProfessionalResponse> {
  return request<ProfessionalResponse>(`/api/professionals/${id}`, {
    ...authenticated,
    method: 'PUT',
    body: input,
  })
}

export function deleteProfessional(id: string): Promise<void> {
  return request<void>(`/api/professionals/${id}`, { ...authenticated, method: 'DELETE' })
}

export function fetchBusinessHours(): Promise<BusinessHourResponse[]> {
  return request<BusinessHourResponse[]>('/api/business-hours', authenticated)
}

export interface BusinessHourEntry {
  dayOfWeek: number
  startTime: string
  endTime: string
}

export function replaceBusinessHours(entries: BusinessHourEntry[]): Promise<BusinessHourResponse[]> {
  return request<BusinessHourResponse[]>('/api/business-hours', {
    ...authenticated,
    method: 'PUT',
    body: { entries },
  })
}

export function fetchBlocks(): Promise<BlockResponse[]> {
  return request<BlockResponse[]>('/api/blocks', authenticated)
}

export interface BlockInput {
  professionalId: string | null
  startsAt: string
  endsAt: string
  reason: string | null
}

export function createBlock(input: BlockInput): Promise<BlockResponse> {
  return request<BlockResponse>('/api/blocks', { ...authenticated, method: 'POST', body: input })
}

export function deleteBlock(id: string): Promise<void> {
  return request<void>(`/api/blocks/${id}`, { ...authenticated, method: 'DELETE' })
}

export function fetchAgenda(date: string, professionalId: string | null): Promise<AppointmentResponse[]> {
  return request<AppointmentResponse[]>('/api/appointments', {
    ...authenticated,
    query: { date, professionalId: professionalId ?? undefined },
  })
}

export function updateAppointmentStatus(
  id: string,
  status: AppointmentStatus,
): Promise<AppointmentResponse> {
  return request<AppointmentResponse>(`/api/appointments/${id}/status`, {
    ...authenticated,
    method: 'PATCH',
    body: { status },
  })
}
