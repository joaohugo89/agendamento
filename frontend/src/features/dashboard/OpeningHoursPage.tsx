import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Input } from '../../components/ui/Field'
import { ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import { weekdayName } from '../../lib/format'
import { dashboardKeys, fetchBusinessHours, replaceBusinessHours } from './api'
import type { BusinessHourEntry } from './api'
import type { BusinessHourResponse } from '../../types/api'

const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const
const DEFAULT_PERIOD = { startTime: '09:00', endTime: '18:00' } as const

interface Period {
  startTime: string
  endTime: string
}

type Schedule = Record<number, Period[]>

function toSchedule(hours: BusinessHourResponse[]): Schedule {
  const schedule: Schedule = {}
  for (const day of WEEKDAYS) {
    schedule[day] = []
  }
  for (const hour of hours) {
    schedule[hour.dayOfWeek]?.push({ startTime: hour.startTime, endTime: hour.endTime })
  }
  return schedule
}

function toEntries(schedule: Schedule): BusinessHourEntry[] {
  return WEEKDAYS.flatMap((day) =>
    (schedule[day] ?? []).map((period) => ({
      dayOfWeek: day,
      startTime: period.startTime,
      endTime: period.endTime,
    })),
  )
}

export function OpeningHoursPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [schedule, setSchedule] = useState<Schedule | null>(null)

  const hoursQuery = useQuery({ queryKey: dashboardKeys.businessHours, queryFn: fetchBusinessHours })

  useEffect(() => {
    if (hoursQuery.data) {
      setSchedule(toSchedule(hoursQuery.data))
    }
  }, [hoursQuery.data])

  const saveMutation = useMutation({
    mutationFn: (entries: BusinessHourEntry[]) => replaceBusinessHours(entries),
    onSuccess: (hours) => {
      notify('Horario de funcionamento atualizado.', 'success')
      setSchedule(toSchedule(hours))
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

  const updatePeriod = (day: number, index: number, patch: Partial<Period>) => {
    setSchedule((current) => {
      if (!current) {
        return current
      }
      const periods = [...(current[day] ?? [])]
      const existing = periods[index]
      if (!existing) {
        return current
      }
      periods[index] = { ...existing, ...patch }
      return { ...current, [day]: periods }
    })
  }

  const addPeriod = (day: number) => {
    setSchedule((current) =>
      current ? { ...current, [day]: [...(current[day] ?? []), { ...DEFAULT_PERIOD }] } : current,
    )
  }

  const removePeriod = (day: number, index: number) => {
    setSchedule((current) =>
      current ? { ...current, [day]: (current[day] ?? []).filter((_, i) => i !== index) } : current,
    )
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">Horario de funcionamento</h1>
        <p className="mt-1 text-sm text-slate-500">
          Um dia sem periodos fica fechado. Use mais de um periodo para intervalos, como o almoco.
        </p>
      </div>

      <div className="space-y-3">
        {WEEKDAYS.map((day) => {
          const periods = schedule[day] ?? []
          return (
            <Card key={day}>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm font-semibold text-slate-900">{weekdayName(day)}</p>
                <div className="flex items-center gap-2">
                  {periods.length === 0 && <span className="text-xs text-slate-400">Fechado</span>}
                  <Button size="sm" variant="secondary" onClick={() => addPeriod(day)}>
                    Adicionar periodo
                  </Button>
                </div>
              </div>

              {periods.length > 0 && (
                <ul className="mt-3 space-y-2">
                  {periods.map((period, index) => (
                    <li key={index} className="flex items-center gap-2">
                      <Input
                        type="time"
                        value={period.startTime}
                        aria-label={`Inicio do periodo ${index + 1} de ${weekdayName(day)}`}
                        onChange={(event) => updatePeriod(day, index, { startTime: event.target.value })}
                        className="max-w-32"
                      />
                      <span className="text-sm text-slate-400">as</span>
                      <Input
                        type="time"
                        value={period.endTime}
                        aria-label={`Fim do periodo ${index + 1} de ${weekdayName(day)}`}
                        onChange={(event) => updatePeriod(day, index, { endTime: event.target.value })}
                        className="max-w-32"
                      />
                      <Button size="sm" variant="ghost" onClick={() => removePeriod(day, index)}>
                        Remover
                      </Button>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          )
        })}
      </div>

      <div className="sticky bottom-4">
        <Button
          size="lg"
          className="w-full shadow-lg"
          loading={saveMutation.isPending}
          onClick={() => saveMutation.mutate(toEntries(schedule))}
        >
          Salvar horarios
        </Button>
      </div>
    </div>
  )
}
