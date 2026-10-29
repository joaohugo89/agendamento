import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '../../components/ui/Button'
import { ErrorState, ListSkeleton, Skeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import { bookingKeys, createAppointment, fetchBusiness } from './api'
import { BusinessOverview } from './BusinessOverview'
import { CustomerStep } from './CustomerStep'
import type { CustomerFormValues } from './CustomerStep'
import { DateStep } from './DateStep'
import { ProfessionalStep } from './ProfessionalStep'
import { ServiceStep } from './ServiceStep'
import { SuccessStep } from './SuccessStep'
import { SummaryStep } from './SummaryStep'
import { TimeStep } from './TimeStep'
import type {
  BookingConfirmationResponse,
  ProfessionalSummary,
  ServiceResponse,
  SlotResponse,
} from '../../types/api'

type Step = 'overview' | 'service' | 'professional' | 'date' | 'time' | 'customer' | 'summary' | 'success'

const FLOW: Step[] = ['service', 'professional', 'date', 'time', 'customer', 'summary']

const STEP_TITLES: Record<Step, string> = {
  overview: '',
  service: 'Escolha o servico',
  professional: 'Escolha o profissional',
  date: 'Escolha a data',
  time: 'Escolha o horario',
  customer: 'Seus dados',
  summary: 'Confira e confirme',
  success: '',
}

interface Selection {
  service?: ServiceResponse
  professional?: ProfessionalSummary
  date?: string
  slot?: SlotResponse
  customer?: CustomerFormValues
}

export function BookingPage() {
  const { slug = '' } = useParams<{ slug: string }>()
  const queryClient = useQueryClient()
  const { notify } = useToast()

  const [step, setStep] = useState<Step>('overview')
  const [selection, setSelection] = useState<Selection>({})
  const [booking, setBooking] = useState<BookingConfirmationResponse | null>(null)

  const businessQuery = useQuery({
    queryKey: bookingKeys.business(slug),
    queryFn: () => fetchBusiness(slug),
  })

  const bookingMutation = useMutation({
    mutationFn: (input: Required<Pick<Selection, 'service' | 'professional' | 'slot' | 'customer'>>) =>
      createAppointment(slug, {
        serviceId: input.service.id,
        professionalId: input.professional.id,
        startsAt: input.slot.startsAt,
        customerName: input.customer.customerName,
        customerWhatsapp: input.customer.customerWhatsapp,
        notes: input.customer.notes.length > 0 ? input.customer.notes : undefined,
      }),
    onSuccess: (confirmation) => {
      setBooking(confirmation)
      setStep('success')
    },
    onError: (error) => {
      // Someone took the slot between choosing it and confirming: back to the time step.
      if (error instanceof ApiError && error.isSlotConflict) {
        notify(error.message, 'error')
        void queryClient.invalidateQueries({ queryKey: bookingKeys.slotsRoot })
        setSelection((current) => ({ ...current, slot: undefined }))
        setStep('time')
        return
      }
      notify(
        error instanceof ApiError ? error.message : 'Nao foi possivel concluir o agendamento.',
        'error',
      )
    },
  })

  if (businessQuery.isPending) {
    return (
      <BookingShell>
        <Skeleton className="h-28" />
        <ListSkeleton rows={2} />
      </BookingShell>
    )
  }

  if (businessQuery.isError) {
    const notFound = businessQuery.error instanceof ApiError && businessQuery.error.status === 404
    return (
      <BookingShell>
        <ErrorState
          title={notFound ? 'Estabelecimento nao encontrado' : 'Algo deu errado'}
          message={
            notFound
              ? 'Confira o link que voce recebeu.'
              : 'Nao foi possivel carregar esta pagina. Tente novamente.'
          }
          action={
            notFound ? undefined : (
              <Button variant="secondary" onClick={() => void businessQuery.refetch()}>
                Tentar de novo
              </Button>
            )
          }
        />
      </BookingShell>
    )
  }

  const business = businessQuery.data
  const openWeekdays = new Set(business.openingHours.map((hour) => hour.dayOfWeek))
  // A professional with a schedule of their own is only offered on the days they work; with no
  // schedule of their own they follow the opening days.
  const workingDays = selection.professional?.workingDays ?? []
  const bookableWeekdays =
    workingDays.length === 0
      ? openWeekdays
      : new Set(workingDays.filter((day) => openWeekdays.has(day)))
  const stepIndex = FLOW.indexOf(step)

  const confirmPayload =
    selection.service && selection.professional && selection.slot && selection.customer
      ? {
          service: selection.service,
          professional: selection.professional,
          slot: selection.slot,
          customer: selection.customer,
        }
      : null

  const goBack = () => {
    if (step === 'overview' || step === 'success') {
      return
    }
    const previous = FLOW[stepIndex - 1]
    setStep(previous ?? 'overview')
  }

  const restart = () => {
    setSelection({})
    setBooking(null)
    setStep('overview')
  }

  return (
    <BookingShell>
      {step !== 'overview' && step !== 'success' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Button variant="ghost" size="sm" onClick={goBack}>
              &lt; Voltar
            </Button>
            <span className="text-xs text-slate-500">
              Etapa {stepIndex + 1} de {FLOW.length}
            </span>
          </div>
          <div className="h-1 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-brand-600 transition-all"
              style={{ width: `${((stepIndex + 1) / FLOW.length) * 100}%` }}
            />
          </div>
          <h1 className="text-lg font-semibold text-slate-900">{STEP_TITLES[step]}</h1>
        </div>
      )}

      {step === 'overview' && (
        <BusinessOverview business={business} onStart={() => setStep('service')} />
      )}

      {step === 'service' && (
        <ServiceStep
          services={business.services}
          onSelect={(service) => {
            setSelection({ service })
            setStep('professional')
          }}
        />
      )}

      {step === 'professional' && selection.service && (
        <ProfessionalStep
          slug={slug}
          serviceId={selection.service.id}
          onSelect={(professional) => {
            // Another professional has another agenda: the day and the slot start over.
            setSelection((current) => ({
              ...current,
              professional,
              date: undefined,
              slot: undefined,
            }))
            setStep('date')
          }}
        />
      )}

      {step === 'date' && (
        <DateStep
          openWeekdays={bookableWeekdays}
          onSelect={(date) => {
            setSelection((current) => ({ ...current, date }))
            setStep('time')
          }}
        />
      )}

      {step === 'time' && selection.service && selection.professional && selection.date && (
        <TimeStep
          slug={slug}
          serviceId={selection.service.id}
          professionalId={selection.professional.id}
          date={selection.date}
          onSelect={(slot) => {
            setSelection((current) => ({ ...current, slot }))
            setStep('customer')
          }}
        />
      )}

      {step === 'customer' && (
        <CustomerStep
          defaultValues={selection.customer}
          onSubmit={(customer) => {
            setSelection((current) => ({ ...current, customer }))
            setStep('summary')
          }}
        />
      )}

      {step === 'summary' && confirmPayload && selection.date && (
        <SummaryStep
          business={business}
          service={confirmPayload.service}
          professional={confirmPayload.professional}
          date={selection.date}
          slot={confirmPayload.slot}
          customerName={confirmPayload.customer.customerName}
          customerWhatsapp={confirmPayload.customer.customerWhatsapp}
          notes={confirmPayload.customer.notes}
          submitting={bookingMutation.isPending}
          onConfirm={() => bookingMutation.mutate(confirmPayload)}
        />
      )}

      {step === 'success' && booking && <SuccessStep booking={booking} onBookAgain={restart} />}
    </BookingShell>
  )
}

function BookingShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="mx-auto w-full max-w-lg space-y-5 px-4 py-6 sm:py-10">{children}</main>
  )
}
