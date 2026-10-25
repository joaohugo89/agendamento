import { useQuery } from '@tanstack/react-query'

import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { bookingKeys, fetchProfessionals } from './api'
import type { ProfessionalSummary } from '../../types/api'

interface ProfessionalStepProps {
  slug: string
  serviceId: string
  onSelect: (professional: ProfessionalSummary) => void
}

export function ProfessionalStep({ slug, serviceId, onSelect }: ProfessionalStepProps) {
  const { data, isPending, isError } = useQuery({
    queryKey: bookingKeys.professionals(slug, serviceId),
    queryFn: () => fetchProfessionals(slug, serviceId),
  })

  if (isPending) {
    return <ListSkeleton rows={2} />
  }

  if (isError) {
    return <ErrorState message="Nao foi possivel carregar os profissionais." />
  }

  if (data.length === 0) {
    return (
      <EmptyState
        title="Nenhum profissional para este servico"
        description="Volte e escolha outro servico."
      />
    )
  }

  return (
    <ul className="space-y-2">
      {data.map((professional) => (
        <li key={professional.id}>
          <button
            type="button"
            onClick={() => onSelect(professional)}
            className="flex w-full items-center gap-3 rounded-xl bg-white p-4 text-left ring-1
              ring-slate-200 transition hover:ring-brand-500"
          >
            <span
              aria-hidden
              className="flex size-10 shrink-0 items-center justify-center rounded-full bg-brand-50
                text-sm font-semibold text-brand-700"
            >
              {professional.name.slice(0, 2).toUpperCase()}
            </span>
            <span className="text-sm font-medium text-slate-900">{professional.name}</span>
          </button>
        </li>
      ))}
    </ul>
  )
}
