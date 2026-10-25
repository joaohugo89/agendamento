import { useQuery } from '@tanstack/react-query'

import { EmptyState, ErrorState, SlotSkeleton } from '../../components/ui/Feedback'
import { bookingKeys, fetchSlots } from './api'
import type { SlotResponse } from '../../types/api'

interface TimeStepProps {
  slug: string
  serviceId: string
  professionalId: string
  date: string
  onSelect: (slot: SlotResponse) => void
}

export function TimeStep({ slug, serviceId, professionalId, date, onSelect }: TimeStepProps) {
  const { data, isPending, isError } = useQuery({
    queryKey: bookingKeys.slots(slug, serviceId, professionalId, date),
    queryFn: () => fetchSlots(slug, serviceId, professionalId, date),
    // Slots go stale quickly: someone else may take one while this screen is open.
    staleTime: 0,
  })

  if (isPending) {
    return <SlotSkeleton />
  }

  if (isError) {
    return <ErrorState message="Nao foi possivel carregar os horarios." />
  }

  if (data.length === 0) {
    return (
      <EmptyState
        title="Nenhum horario livre neste dia"
        description="Escolha outra data ou outro profissional."
      />
    )
  }

  return (
    <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
      {data.map((slot) => (
        <button
          key={slot.startsAt}
          type="button"
          onClick={() => onSelect(slot)}
          className="h-11 rounded-lg bg-white text-sm font-medium text-slate-800 ring-1 ring-slate-200
            transition hover:ring-brand-500"
        >
          {slot.time}
        </button>
      ))}
    </div>
  )
}
