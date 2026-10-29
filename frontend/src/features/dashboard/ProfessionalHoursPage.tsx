import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import {
  dashboardKeys,
  fetchBusinessHours,
  fetchProfessionals,
  fetchProfessionalSchedule,
  replaceProfessionalSchedule,
} from './api'
import type { ProfessionalScheduleInput } from './api'
import {
  toScheduleEntries,
  toWeekSchedule,
  WEEKDAYS,
  WeekdayScheduleEditor,
} from './WeekdayScheduleEditor'
import type { WeekSchedule } from './WeekdayScheduleEditor'
import { weekdayName } from '../../lib/format'

export function ProfessionalHoursPage() {
  const { id = '' } = useParams<{ id: string }>()
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [ownSchedule, setOwnSchedule] = useState(false)
  const [schedule, setSchedule] = useState<WeekSchedule | null>(null)

  const professionalsQuery = useQuery({
    queryKey: dashboardKeys.professionals,
    queryFn: fetchProfessionals,
  })
  const businessHoursQuery = useQuery({
    queryKey: dashboardKeys.businessHours,
    queryFn: fetchBusinessHours,
  })
  const scheduleQuery = useQuery({
    queryKey: dashboardKeys.professionalHours(id),
    queryFn: () => fetchProfessionalSchedule(id),
  })

  useEffect(() => {
    if (scheduleQuery.data) {
      setOwnSchedule(!scheduleQuery.data.followsBusinessHours)
      setSchedule(toWeekSchedule(scheduleQuery.data.entries))
    }
  }, [scheduleQuery.data])

  const saveMutation = useMutation({
    mutationFn: (input: ProfessionalScheduleInput) => replaceProfessionalSchedule(id, input),
    onSuccess: (saved) => {
      notify('Horario do profissional atualizado.', 'success')
      setOwnSchedule(!saved.followsBusinessHours)
      setSchedule(toWeekSchedule(saved.entries))
      void queryClient.invalidateQueries({ queryKey: dashboardKeys.professionalHours(id) })
    },
    onError: (error) => {
      notify(error instanceof ApiError ? error.message : 'Nao foi possivel salvar os horarios.', 'error')
    },
  })

  if (scheduleQuery.isPending || professionalsQuery.isPending || schedule === null) {
    return <ListSkeleton rows={5} />
  }

  if (scheduleQuery.isError) {
    return (
      <ErrorState
        message="Nao foi possivel carregar os horarios do profissional."
        action={
          <Button variant="secondary" onClick={() => void scheduleQuery.refetch()}>
            Tentar de novo
          </Button>
        }
      />
    )
  }

  const professional = professionalsQuery.data?.find((item) => item.id === id)
  if (!professional) {
    return (
      <EmptyState
        title="Profissional nao encontrado"
        description="Ele pode ter sido removido."
        action={
          <Link to="/painel/profissionais">
            <Button variant="secondary">Voltar para profissionais</Button>
          </Link>
        }
      />
    )
  }

  const openDays = new Set((businessHoursQuery.data ?? []).map((hour) => hour.dayOfWeek))
  const closedDays = WEEKDAYS.filter((day) => !openDays.has(day))

  return (
    <div className="space-y-5">
      <div>
        <Link to="/painel/profissionais" className="text-xs text-brand-600 hover:underline">
          Profissionais
        </Link>
        <h1 className="mt-1 text-lg font-semibold text-slate-900">
          Horarios de {professional.name}
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          O profissional so recebe agendamentos quando o estabelecimento esta aberto e ele esta em
          horario. Periodos fora do funcionamento nao tem efeito.
        </p>
      </div>

      <Card>
        <fieldset className="space-y-3">
          <legend className="text-sm font-medium text-slate-700">Disponibilidade</legend>
          <label className="flex items-start gap-3 text-sm text-slate-700">
            <input
              type="radio"
              name="availability"
              className="mt-1 size-4"
              checked={!ownSchedule}
              onChange={() => setOwnSchedule(false)}
            />
            <span>
              Segue o horario do estabelecimento
              <span className="block text-xs text-slate-500">
                Atende em todo o horario de funcionamento.
              </span>
            </span>
          </label>
          <label className="flex items-start gap-3 text-sm text-slate-700">
            <input
              type="radio"
              name="availability"
              className="mt-1 size-4"
              checked={ownSchedule}
              onChange={() => setOwnSchedule(true)}
            />
            <span>
              Horario proprio
              <span className="block text-xs text-slate-500">
                Define os dias e periodos em que este profissional atende.
              </span>
            </span>
          </label>
        </fieldset>
      </Card>

      {ownSchedule && (
        <>
          {closedDays.length > 0 && (
            <p className="text-xs text-slate-500">
              O estabelecimento nao abre {closedDays.map((day) => weekdayName(day)).join(', ')}.
            </p>
          )}
          <WeekdayScheduleEditor
            schedule={schedule}
            onChange={setSchedule}
            emptyDayLabel="Nao atende"
          />
        </>
      )}

      <div className="sticky bottom-4">
        <Button
          size="lg"
          className="w-full shadow-lg"
          loading={saveMutation.isPending}
          onClick={() =>
            saveMutation.mutate({
              followsBusinessHours: !ownSchedule,
              entries: ownSchedule ? toScheduleEntries(schedule) : [],
            })
          }
        >
          Salvar horarios
        </Button>
      </div>
    </div>
  )
}
