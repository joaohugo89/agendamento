export interface ServiceResponse {
  id: string
  name: string
  description: string | null
  priceCents: number
  durationMinutes: number
  active: boolean
}

export interface ProfessionalSummary {
  id: string
  name: string
  /** Weekdays the professional works, 1 = Monday .. 7 = Sunday; empty = the business hours. */
  workingDays: number[]
}

export interface ProfessionalResponse {
  id: string
  name: string
  active: boolean
  services: ProfessionalSummary[]
}

export interface BusinessHourResponse {
  id: string
  dayOfWeek: number
  startTime: string
  endTime: string
}

export interface ProfessionalHourResponse {
  id: string
  dayOfWeek: number
  startTime: string
  endTime: string
}

export interface ProfessionalScheduleResponse {
  followsBusinessHours: boolean
  entries: ProfessionalHourResponse[]
}

export interface PublicBusinessResponse {
  name: string
  slug: string
  description: string | null
  address: string | null
  phone: string | null
  timezone: string
  services: ServiceResponse[]
  openingHours: BusinessHourResponse[]
}

export interface SlotResponse {
  startsAt: string
  time: string
}

export interface BookingConfirmationResponse {
  id: string
  businessName: string
  businessAddress: string | null
  serviceName: string
  professionalName: string
  customerName: string
  startsAt: string
  endsAt: string
  priceCents: number
  timezone: string
}

export type AppointmentStatus = 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'

export interface AppointmentResponse {
  id: string
  startsAt: string
  endsAt: string
  startTime: string
  endTime: string
  status: AppointmentStatus
  customerName: string
  customerWhatsapp: string
  notes: string | null
  serviceId: string
  serviceName: string | null
  professionalId: string
  professionalName: string | null
  priceCents: number
}

export interface BusinessResponse {
  id: string
  name: string
  slug: string
  description: string | null
  address: string | null
  phone: string | null
  timezone: string
}

export interface AuthResponse {
  token: string
  expiresInSeconds: number
  userId: string
  userName: string
  businessId: string
  businessName: string
  businessSlug: string
}

export interface BlockResponse {
  id: string
  professionalId: string | null
  professionalName: string | null
  startsAt: string
  endsAt: string
  reason: string | null
}
