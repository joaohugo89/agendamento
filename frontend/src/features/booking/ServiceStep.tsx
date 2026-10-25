import { EmptyState } from '../../components/ui/Feedback'
import { formatDuration, formatPrice } from '../../lib/format'
import type { ServiceResponse } from '../../types/api'

interface ServiceStepProps {
  services: ServiceResponse[]
  onSelect: (service: ServiceResponse) => void
}

export function ServiceStep({ services, onSelect }: ServiceStepProps) {
  if (services.length === 0) {
    return <EmptyState title="Nenhum servico disponivel" description="Tente novamente mais tarde." />
  }

  return (
    <ul className="space-y-2">
      {services.map((service) => (
        <li key={service.id}>
          <button
            type="button"
            onClick={() => onSelect(service)}
            className="flex w-full items-center justify-between gap-4 rounded-xl bg-white p-4 text-left
              ring-1 ring-slate-200 transition hover:ring-brand-500"
          >
            <span>
              <span className="block text-sm font-medium text-slate-900">{service.name}</span>
              {service.description && (
                <span className="mt-0.5 block text-xs text-slate-500">{service.description}</span>
              )}
              <span className="mt-1 block text-xs text-slate-500">
                {formatDuration(service.durationMinutes)}
              </span>
            </span>
            <span className="text-sm font-semibold text-slate-900">{formatPrice(service.priceCents)}</span>
          </button>
        </li>
      ))}
    </ul>
  )
}
