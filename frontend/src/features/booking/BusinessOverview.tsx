import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { formatDuration, formatPrice, weekdayName } from '../../lib/format'
import type { PublicBusinessResponse } from '../../types/api'

interface BusinessOverviewProps {
  business: PublicBusinessResponse
  onStart: () => void
}

export function BusinessOverview({ business, onStart }: BusinessOverviewProps) {
  const hoursByDay = groupHoursByDay(business)

  return (
    <div className="space-y-5">
      <Card>
        <h1 className="text-xl font-semibold text-slate-900">{business.name}</h1>
        {business.description && <p className="mt-2 text-sm text-slate-600">{business.description}</p>}
        <dl className="mt-4 space-y-2 text-sm">
          {business.address && (
            <div className="flex gap-2">
              <dt className="text-slate-500">Endereco</dt>
              <dd className="text-slate-800">{business.address}</dd>
            </div>
          )}
          {business.phone && (
            <div className="flex gap-2">
              <dt className="text-slate-500">Telefone</dt>
              <dd className="text-slate-800">{business.phone}</dd>
            </div>
          )}
        </dl>
      </Card>

      <Card>
        <h2 className="text-base font-semibold text-slate-900">Servicos</h2>
        {business.services.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">
            Este estabelecimento ainda nao cadastrou servicos.
          </p>
        ) : (
          <ul className="mt-3 divide-y divide-slate-100">
            {business.services.map((service) => (
              <li key={service.id} className="flex items-baseline justify-between gap-4 py-3">
                <div>
                  <p className="text-sm font-medium text-slate-900">{service.name}</p>
                  <p className="text-xs text-slate-500">{formatDuration(service.durationMinutes)}</p>
                </div>
                <p className="text-sm font-semibold text-slate-900">{formatPrice(service.priceCents)}</p>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card>
        <h2 className="text-base font-semibold text-slate-900">Horario de funcionamento</h2>
        <ul className="mt-3 space-y-1.5 text-sm">
          {[1, 2, 3, 4, 5, 6, 7].map((day) => {
            const periods = hoursByDay.get(day) ?? []
            return (
              <li key={day} className="flex justify-between gap-4">
                <span className="text-slate-600">{weekdayName(day)}</span>
                <span className={periods.length === 0 ? 'text-slate-400' : 'text-slate-900'}>
                  {periods.length === 0 ? 'Fechado' : periods.join(' / ')}
                </span>
              </li>
            )
          })}
        </ul>
      </Card>

      <div className="sticky bottom-4">
        <Button size="lg" className="w-full shadow-lg" onClick={onStart}>
          Agendar agora
        </Button>
      </div>
    </div>
  )
}

function groupHoursByDay(business: PublicBusinessResponse): Map<number, string[]> {
  const grouped = new Map<number, string[]>()
  for (const hour of business.openingHours) {
    const periods = grouped.get(hour.dayOfWeek) ?? []
    periods.push(`${hour.startTime} - ${hour.endTime}`)
    grouped.set(hour.dayOfWeek, periods)
  }
  return grouped
}
