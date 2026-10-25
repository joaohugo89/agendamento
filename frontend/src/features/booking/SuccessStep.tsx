import { Button } from '../../components/ui/Button'
import { formatLongDate, formatPrice, formatTime } from '../../lib/format'
import { downloadIcsFile } from './ics'
import type { BookingConfirmationResponse } from '../../types/api'

interface SuccessStepProps {
  booking: BookingConfirmationResponse
  onBookAgain: () => void
}

export function SuccessStep({ booking, onBookAgain }: SuccessStepProps) {
  return (
    <div className="space-y-4">
      <div className="rounded-xl bg-white p-6 text-center ring-1 ring-slate-200">
        <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-emerald-50">
          <span aria-hidden className="text-xl text-emerald-600">
            &#10003;
          </span>
        </div>
        <h1 className="mt-4 text-lg font-semibold text-slate-900">Agendamento confirmado</h1>
        <p className="mt-1 text-sm text-slate-500">
          Enviamos os detalhes abaixo. Guarde este horario.
        </p>

        <dl className="mt-6 space-y-3 text-left">
          <Row label="Estabelecimento" value={booking.businessName} />
          <Row label="Servico" value={booking.serviceName} />
          <Row label="Profissional" value={booking.professionalName} />
          <Row label="Data" value={formatLongDate(booking.startsAt, booking.timezone)} />
          <Row
            label="Horario"
            value={`${formatTime(booking.startsAt, booking.timezone)} - ${formatTime(
              booking.endsAt,
              booking.timezone,
            )}`}
          />
          <Row label="Cliente" value={booking.customerName} />
          <Row label="Valor" value={formatPrice(booking.priceCents)} />
          {booking.businessAddress && <Row label="Endereco" value={booking.businessAddress} />}
        </dl>
      </div>

      <Button size="lg" className="w-full" onClick={() => downloadIcsFile(booking)}>
        Adicionar ao calendario
      </Button>
      <Button variant="secondary" size="lg" className="w-full" onClick={onBookAgain}>
        Fazer outro agendamento
      </Button>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium text-slate-900">{value}</dd>
    </div>
  )
}
