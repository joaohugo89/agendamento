import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Select } from '../../components/ui/Field'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import { formatIsoDate, formatLongDate, formatPrice, parseIsoDate } from '../../lib/format'
import {
  dashboardKeys,
  fetchAgenda,
  fetchProfessionals,
  updateAppointmentStatus,
} from './api'
import type { AppointmentResponse, AppointmentStatus } from '../../types/api'

const STATUS_LABELS: Record<AppointmentStatus, string> = {
  CONFIRMED: 'Confirmado',
  COMPLETED: 'Concluido',
  CANCELLED: 'Cancelado',
  NO_SHOW: 'Nao compareceu',
}

const STATUS_TONES: Record<AppointmentStatus, 'brand' | 'success' | 'danger' | 'warning'> = {
  CONFIRMED: 'brand',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  NO_SHOW: 'warning',
}

interface PendingChange {
  appointment: AppointmentResponse
  status: AppointmentStatus
}

export function AgendaPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [date, setDate] = useState(() => formatIsoDate(new Date()))
  const [professionalId, setProfessionalId] = useState<string | null>(null)
  const [pending, setPending] = useState<PendingChange | null>(null)

  const professionalsQuery = useQuery({
    queryKey: dashboardKeys.professionals,
    queryFn: fetchProfessionals,
  })

  const agendaQuery = useQuery({
    queryKey: dashboardKeys.agenda(date, professionalId),
    queryFn: () => fetchAgenda(date, professionalId),
  })

  const statusMutation = useMutation({
    mutationFn: ({ appointment, status }: PendingChange) =>
      updateAppointmentStatus(appointment.id, status),
    onSuccess: (_updated, variables) => {
      notify(`Agendamento marcado como ${STATUS_LABELS[variables.status].toLowerCase()}.`, 'success')
      void queryClient.invalidateQueries({ queryKey: ['agenda'] })
      setPending(null)
    },
    onError: (error) => {
      notify(error instanceof ApiError ? error.message : 'Nao foi possivel alterar o status.', 'error')
      setPending(null)
    },
  })

  const shiftDay = (days: number) => {
    const next = parseIsoDate(date)
    next.setDate(next.getDate() + days)
    setDate(formatIsoDate(next))
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-slate-900">Agenda</h1>
          <p className="text-sm capitalize text-slate-500">{formatLongDate(date)}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => shiftDay(-1)} aria-label="Dia anterior">
            &lt;
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setDate(formatIsoDate(new Date()))}>
            Hoje
          </Button>
          <Button variant="secondary" size="sm" onClick={() => shiftDay(1)} aria-label="Dia seguinte">
            &gt;
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        <input
          type="date"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          className="rounded-lg bg-white px-3 py-2 text-sm ring-1 ring-slate-200"
          aria-label="Data da agenda"
        />
        <Select
          value={professionalId ?? ''}
          onChange={(event) => setProfessionalId(event.target.value === '' ? null : event.target.value)}
          aria-label="Filtrar por profissional"
          className="max-w-xs"
        >
          <option value="">Todos os profissionais</option>
          {(professionalsQuery.data ?? []).map((professional) => (
            <option key={professional.id} value={professional.id}>
              {professional.name}
            </option>
          ))}
        </Select>
      </div>

      {agendaQuery.isPending && <ListSkeleton rows={4} />}

      {agendaQuery.isError && (
        <ErrorState
          message="Nao foi possivel carregar a agenda."
          action={
            <Button variant="secondary" onClick={() => void agendaQuery.refetch()}>
              Tentar de novo
            </Button>
          }
        />
      )}

      {agendaQuery.data?.length === 0 && (
        <EmptyState
          title="Nenhum agendamento neste dia"
          description="Compartilhe o link publico para receber agendamentos."
        />
      )}

      {agendaQuery.data && agendaQuery.data.length > 0 && (
        <ul className="space-y-3">
          {agendaQuery.data.map((appointment) => (
            <li
              key={appointment.id}
              className="rounded-xl bg-white p-4 ring-1 ring-slate-200/80"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-900">
                    {appointment.startTime} - {appointment.endTime}
                  </p>
                  <p className="mt-1 text-sm text-slate-900">{appointment.customerName}</p>
                  <p className="text-xs text-slate-500">
                    {appointment.serviceName} com {appointment.professionalName}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">{appointment.customerWhatsapp}</p>
                  {appointment.notes && (
                    <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
                      {appointment.notes}
                    </p>
                  )}
                </div>
                <div className="flex flex-col items-end gap-2">
                  <Badge tone={STATUS_TONES[appointment.status]}>
                    {STATUS_LABELS[appointment.status]}
                  </Badge>
                  <span className="text-sm font-medium text-slate-700">
                    {formatPrice(appointment.priceCents)}
                  </span>
                </div>
              </div>

              {appointment.status === 'CONFIRMED' && (
                <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3">
                  <Button
                    size="sm"
                    onClick={() => setPending({ appointment, status: 'COMPLETED' })}
                  >
                    Concluir
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setPending({ appointment, status: 'NO_SHOW' })}
                  >
                    Nao compareceu
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setPending({ appointment, status: 'CANCELLED' })}
                  >
                    Cancelar
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={pending !== null}
        title={pending ? `Marcar como ${STATUS_LABELS[pending.status].toLowerCase()}?` : ''}
        description={
          pending
            ? `${pending.appointment.customerName}, ${pending.appointment.startTime}. Esta acao nao pode ser desfeita.`
            : ''
        }
        confirmLabel="Confirmar"
        destructive={pending?.status !== 'COMPLETED'}
        loading={statusMutation.isPending}
        onConfirm={() => pending && statusMutation.mutate(pending)}
        onCancel={() => setPending(null)}
      />
    </div>
  )
}
