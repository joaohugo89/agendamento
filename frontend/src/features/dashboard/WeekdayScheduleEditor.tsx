import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Input } from '../../components/ui/Field'
import { weekdayName } from '../../lib/format'

export const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const

const DEFAULT_PERIOD: SchedulePeriod = { startTime: '09:00', endTime: '18:00' }

export interface SchedulePeriod {
  startTime: string
  endTime: string
}

/** Periods of each weekday, 1 = Monday .. 7 = Sunday; a day with no period is a day off. */
export type WeekSchedule = Record<number, SchedulePeriod[]>

interface HourRow {
  dayOfWeek: number
  startTime: string
  endTime: string
}

export function toWeekSchedule(hours: HourRow[]): WeekSchedule {
  const schedule: WeekSchedule = {}
  for (const day of WEEKDAYS) {
    schedule[day] = []
  }
  for (const hour of hours) {
    schedule[hour.dayOfWeek]?.push({ startTime: hour.startTime, endTime: hour.endTime })
  }
  return schedule
}

export function toScheduleEntries(schedule: WeekSchedule): HourRow[] {
  return WEEKDAYS.flatMap((day) =>
    (schedule[day] ?? []).map((period) => ({
      dayOfWeek: day,
      startTime: period.startTime,
      endTime: period.endTime,
    })),
  )
}

interface WeekdayScheduleEditorProps {
  schedule: WeekSchedule
  onChange: (schedule: WeekSchedule) => void
  /** What an empty weekday means here: the shop is closed, or the professional is off. */
  emptyDayLabel: string
  disabled?: boolean
}

/** Shared by the business opening hours and by each professional's own schedule. */
export function WeekdayScheduleEditor({
  schedule,
  onChange,
  emptyDayLabel,
  disabled = false,
}: WeekdayScheduleEditorProps) {
  const updatePeriod = (day: number, index: number, patch: Partial<SchedulePeriod>) => {
    const periods = [...(schedule[day] ?? [])]
    const existing = periods[index]
    if (!existing) {
      return
    }
    periods[index] = { ...existing, ...patch }
    onChange({ ...schedule, [day]: periods })
  }

  const addPeriod = (day: number) => {
    onChange({ ...schedule, [day]: [...(schedule[day] ?? []), { ...DEFAULT_PERIOD }] })
  }

  const removePeriod = (day: number, index: number) => {
    onChange({ ...schedule, [day]: (schedule[day] ?? []).filter((_, i) => i !== index) })
  }

  return (
    <div className="space-y-3">
      {WEEKDAYS.map((day) => {
        const periods = schedule[day] ?? []
        return (
          <Card key={day}>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm font-semibold text-slate-900">{weekdayName(day)}</p>
              <div className="flex items-center gap-2">
                {periods.length === 0 && (
                  <span className="text-xs text-slate-400">{emptyDayLabel}</span>
                )}
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={disabled}
                  onClick={() => addPeriod(day)}
                >
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
                      disabled={disabled}
                      aria-label={`Inicio do periodo ${index + 1} de ${weekdayName(day)}`}
                      onChange={(event) => updatePeriod(day, index, { startTime: event.target.value })}
                      className="max-w-32"
                    />
                    <span className="text-sm text-slate-400">as</span>
                    <Input
                      type="time"
                      value={period.endTime}
                      disabled={disabled}
                      aria-label={`Fim do periodo ${index + 1} de ${weekdayName(day)}`}
                      onChange={(event) => updatePeriod(day, index, { endTime: event.target.value })}
                      className="max-w-32"
                    />
                    <Button
                      size="sm"
                      variant="ghost"
                      disabled={disabled}
                      onClick={() => removePeriod(day, index)}
                    >
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
  )
}
