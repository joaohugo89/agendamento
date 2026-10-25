import { useState } from 'react'

import { Button } from '../../components/ui/Button'
import { formatIsoDate } from '../../lib/format'

const WEEKDAY_INITIALS = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'] as const
const MONTHS_AHEAD = 2

interface DateStepProps {
  /** Weekdays the business opens, 1 = Monday .. 7 = Sunday, as the API sends them. */
  openWeekdays: Set<number>
  onSelect: (date: string) => void
}

export function DateStep({ openWeekdays, onSelect }: DateStepProps) {
  const today = startOfToday()
  const [monthOffset, setMonthOffset] = useState(0)
  const visibleMonth = new Date(today.getFullYear(), today.getMonth() + monthOffset, 1)

  const isSelectable = (date: Date) =>
    date.getTime() >= today.getTime() && openWeekdays.has(isoWeekday(date))

  return (
    <div className="rounded-xl bg-white p-4 ring-1 ring-slate-200">
      <div className="flex items-center justify-between">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setMonthOffset((offset) => offset - 1)}
          disabled={monthOffset === 0}
          aria-label="Mes anterior"
        >
          &lt;
        </Button>
        <p className="text-sm font-semibold text-slate-900">
          {visibleMonth.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}
        </p>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setMonthOffset((offset) => offset + 1)}
          disabled={monthOffset >= MONTHS_AHEAD}
          aria-label="Proximo mes"
        >
          &gt;
        </Button>
      </div>

      <div className="mt-4 grid grid-cols-7 gap-1 text-center text-xs text-slate-400">
        {WEEKDAY_INITIALS.map((initial, index) => (
          <span key={index}>{initial}</span>
        ))}
      </div>

      <div className="mt-1 grid grid-cols-7 gap-1">
        {buildMonthCells(visibleMonth).map((date, index) =>
          date === null ? (
            <span key={`empty-${index}`} />
          ) : (
            <button
              key={date.toISOString()}
              type="button"
              disabled={!isSelectable(date)}
              onClick={() => onSelect(formatIsoDate(date))}
              className="aspect-square rounded-lg text-sm font-medium text-slate-700 transition
                hover:bg-brand-50 hover:text-brand-700
                disabled:cursor-not-allowed disabled:text-slate-300 disabled:hover:bg-transparent"
            >
              {date.getDate()}
            </button>
          ),
        )}
      </div>

      <p className="mt-4 text-xs text-slate-500">Dias sem atendimento aparecem desabilitados.</p>
    </div>
  )
}

function startOfToday(): Date {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

/** JavaScript weeks start on Sunday (0); the API uses ISO weekdays (Monday = 1). */
function isoWeekday(date: Date): number {
  const day = date.getDay()
  return day === 0 ? 7 : day
}

function buildMonthCells(month: Date): (Date | null)[] {
  const firstDay = new Date(month.getFullYear(), month.getMonth(), 1)
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()
  const leadingEmpty = firstDay.getDay()

  const cells: (Date | null)[] = Array.from({ length: leadingEmpty }, () => null)
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push(new Date(month.getFullYear(), month.getMonth(), day))
  }
  return cells
}
