import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'

import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Field, Input, Select } from '../../components/ui/Field'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import { createBlock, dashboardKeys, deleteBlock, fetchBlocks, fetchProfessionals } from './api'
import type { BlockResponse } from '../../types/api'

const blockSchema = z
  .object({
    professionalId: z.string(),
    startsAt: z.string().min(1, 'Informe o inicio'),
    endsAt: z.string().min(1, 'Informe o fim'),
    reason: z.string().trim().max(255, 'Motivo muito longo'),
  })
  .refine((values) => new Date(values.startsAt) < new Date(values.endsAt), {
    message: 'O inicio deve ser antes do fim',
    path: ['endsAt'],
  })

type BlockFormValues = z.infer<typeof blockSchema>

export function BlocksPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [creating, setCreating] = useState(false)
  const [removing, setRemoving] = useState<BlockResponse | null>(null)

  const blocksQuery = useQuery({ queryKey: dashboardKeys.blocks, queryFn: fetchBlocks })
  const professionalsQuery = useQuery({
    queryKey: dashboardKeys.professionals,
    queryFn: fetchProfessionals,
  })

  const createMutation = useMutation({
    mutationFn: (values: BlockFormValues) =>
      createBlock({
        professionalId: values.professionalId === '' ? null : values.professionalId,
        // The input is local time in the browser; toISOString sends the exact instant.
        startsAt: new Date(values.startsAt).toISOString(),
        endsAt: new Date(values.endsAt).toISOString(),
        reason: values.reason.length > 0 ? values.reason : null,
      }),
    onSuccess: () => {
      notify('Bloqueio criado.', 'success')
      void queryClient.invalidateQueries({ queryKey: dashboardKeys.blocks })
      setCreating(false)
    },
    onError: (error) => {
      notify(error instanceof ApiError ? error.message : 'Nao foi possivel criar o bloqueio.', 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteBlock(id),
    onSuccess: () => {
      notify('Bloqueio removido.', 'success')
      void queryClient.invalidateQueries({ queryKey: dashboardKeys.blocks })
      setRemoving(null)
    },
    onError: (error) => {
      notify(error instanceof ApiError ? error.message : 'Nao foi possivel remover o bloqueio.', 'error')
      setRemoving(null)
    },
  })

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-slate-900">Bloqueios</h1>
          <p className="mt-1 text-sm text-slate-500">
            Periodos sem atendimento. Sem profissional, bloqueia o estabelecimento todo.
          </p>
        </div>
        {!creating && <Button onClick={() => setCreating(true)}>Novo bloqueio</Button>}
      </div>

      {creating && (
        <BlockForm
          professionals={(professionalsQuery.data ?? []).map((professional) => ({
            id: professional.id,
            name: professional.name,
          }))}
          saving={createMutation.isPending}
          onCancel={() => setCreating(false)}
          onSubmit={(values) => createMutation.mutate(values)}
        />
      )}

      {blocksQuery.isPending && <ListSkeleton rows={2} />}

      {blocksQuery.isError && (
        <ErrorState
          message="Nao foi possivel carregar os bloqueios."
          action={
            <Button variant="secondary" onClick={() => void blocksQuery.refetch()}>
              Tentar de novo
            </Button>
          }
        />
      )}

      {blocksQuery.data?.length === 0 && !creating && (
        <EmptyState
          title="Nenhum bloqueio cadastrado"
          description="Use bloqueios para ferias, feriados ou compromissos."
          action={<Button onClick={() => setCreating(true)}>Novo bloqueio</Button>}
        />
      )}

      {blocksQuery.data && blocksQuery.data.length > 0 && (
        <ul className="space-y-3">
          {blocksQuery.data.map((block) => (
            <li key={block.id}>
              <Card>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">
                      {formatPeriod(block.startsAt, block.endsAt)}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      {block.professionalName ?? 'Estabelecimento todo'}
                      {block.reason ? ` - ${block.reason}` : ''}
                    </p>
                  </div>
                  <Button size="sm" variant="ghost" onClick={() => setRemoving(block)}>
                    Remover
                  </Button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={removing !== null}
        title="Remover bloqueio?"
        description="Os horarios voltam a ficar disponiveis para agendamento."
        confirmLabel="Remover"
        destructive
        loading={deleteMutation.isPending}
        onConfirm={() => removing && deleteMutation.mutate(removing.id)}
        onCancel={() => setRemoving(null)}
      />
    </div>
  )
}

function formatPeriod(startsAt: string, endsAt: string): string {
  const options: Intl.DateTimeFormatOptions = {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }
  return `${new Date(startsAt).toLocaleString('pt-BR', options)} - ${new Date(endsAt).toLocaleString(
    'pt-BR',
    options,
  )}`
}

interface BlockFormProps {
  professionals: Array<{ id: string; name: string }>
  saving: boolean
  onSubmit: (values: BlockFormValues) => void
  onCancel: () => void
}

function BlockForm({ professionals, saving, onSubmit, onCancel }: BlockFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BlockFormValues>({
    resolver: zodResolver(blockSchema),
    defaultValues: { professionalId: '', startsAt: '', endsAt: '', reason: '' },
  })

  return (
    <Card>
      <CardHeader title="Novo bloqueio" />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <Field label="Profissional" htmlFor="professionalId">
          <Select id="professionalId" {...register('professionalId')}>
            <option value="">Estabelecimento todo</option>
            {professionals.map((professional) => (
              <option key={professional.id} value={professional.id}>
                {professional.name}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Inicio" htmlFor="startsAt" error={errors.startsAt?.message}>
            <Input id="startsAt" type="datetime-local" {...register('startsAt')} />
          </Field>
          <Field label="Fim" htmlFor="endsAt" error={errors.endsAt?.message}>
            <Input id="endsAt" type="datetime-local" {...register('endsAt')} />
          </Field>
        </div>
        <Field label="Motivo (opcional)" htmlFor="reason" error={errors.reason?.message}>
          <Input id="reason" placeholder="Ferias, feriado, consulta" {...register('reason')} />
        </Field>
        <div className="flex gap-2">
          <Button type="submit" loading={saving}>
            Salvar
          </Button>
          <Button type="button" variant="secondary" onClick={onCancel} disabled={saving}>
            Cancelar
          </Button>
        </div>
      </form>
    </Card>
  )
}
