import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '../../components/ui/Button'
import { ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import { dashboardKeys, fetchBusinessHours, replaceBusinessHours } from './api'
import type { BusinessHourEntry } from './api'
import {
  toScheduleEntries,
  toWeekSchedule,
  WeekdayScheduleEditor,
} from './WeekdayScheduleEditor'
import type { WeekSchedule } from './WeekdayScheduleEditor'

export function OpeningHoursPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [schedule, setSchedule] = useState<WeekSchedule | null>(null)

  const hoursQuery = useQuery({ queryKey: dashboardKeys.businessHours, queryFn: fetchBusinessHours })

  useEffect(() => {
    if (hoursQuery.data) {
      setSchedule(toWeekSchedule(hoursQuery.data))
    }
  }, [hoursQuery.data])

  const saveMutation = useMutation({
    mutationFn: (entries: BusinessHourEntry[]) => replaceBusinessHours(entries),
    onSuccess: (hours) => {
      notify('Horario de funcionamento atualizado.', 'success')
      setSchedule(toWeekSchedule(hours))
      void queryClient.invalidateQueries({ queryKey: dashboardKeys.businessHours })
    },
    onError: (error) => {
      notify(error instanceof ApiError ? error.message : 'Nao foi possivel salvar os horarios.', 'error')
    },
  })

  if (hoursQuery.isPending || schedule === null) {
    return <ListSkeleton rows={5} />
  }

  if (hoursQuery.isError) {
    return (
      <ErrorState
        message="Nao foi possivel carregar os horarios."
        action={
          <Button variant="secondary" onClick={() => void hoursQuery.refetch()}>
            Tentar de novo
          </Button>
        }
      />
    )
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">Horario de funcionamento</h1>
        <p className="mt-1 text-sm text-slate-500">
          Um dia sem periodos fica fechado. Use mais de um periodo para intervalos, como o almoco.
          Cada profissional pode ter um horario proprio dentro deste.
        </p>
      </div>

      <WeekdayScheduleEditor schedule={schedule} onChange={setSchedule} emptyDayLabel="Fechado" />

      <div className="sticky bottom-4">
        <Button
          size="lg"
          className="w-full shadow-lg"
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate(toScheduleEntries(schedule))}
        >
          Salvar horarios
        </Button>
      </div>
    </div>
  )
}
