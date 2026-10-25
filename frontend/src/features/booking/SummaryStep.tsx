import { Button } from '../../components/ui/Button'
import { formatDuration, formatLongDate, formatPrice } from '../../lib/format'
import type { ProfessionalSummary, PublicBusinessResponse, ServiceResponse, SlotResponse } from '../../types/api'

interface SummaryStepProps {
  business: PublicBusinessResponse
  service: ServiceResponse
  professional: ProfessionalSummary
  date: string
  slot: SlotResponse
  customerName: string
  customerWhatsapp: string
  notes: string
  submitting: boolean
  onConfirm: () => void
}

export function SummaryStep({
  business,
  service,
  professional,
  date,
  slot,
  customerName,
  customerWhatsapp,
  notes,
  submitting,
  onConfirm,
}: SummaryStepProps) {
  const rows: Array<[string, string]> = [
    ['Servico', service.name],
    ['Profissional', professional.name],
    ['Data', formatLongDate(date)],
    ['Horario', `${slot.time} (${formatDuration(service.durationMinutes)})`],
    ['Nome', customerName],
    ['WhatsApp', customerWhatsapp],
  ]

  if (notes.length > 0) {
    rows.push(['Observacao', notes])
  }

  return (
    <div className="space-y-4">
      <div className="rounded-xl bg-white p-5 ring-1 ring-slate-200">
        <h2 className="text-base font-semibold text-slate-900">{business.name}</h2>
        <dl className="mt-4 space-y-3">
          {rows.map(([label, value]) => (
            <div key={label} className="flex justify-between gap-4 text-sm">
              <dt className="text-slate-500">{label}</dt>
              <dd className="text-right font-medium text-slate-900">{value}</dd>
            </div>
          ))}
        </dl>
        <div className="mt-4 flex justify-between border-t border-slate-100 pt-4">
          <span className="text-sm text-slate-500">Total</span>
          <span className="text-base font-semibold text-slate-900">{formatPrice(service.priceCents)}</span>
        </div>
      </div>

      <Button size="lg" className="w-full" loading={submitting} onClick={onConfirm}>
        Confirmar agendamento
      </Button>
    </div>
  )
}
