import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'

import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Field, Input, Textarea } from '../../components/ui/Field'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import { formatDuration, formatPrice } from '../../lib/format'
import { createService, dashboardKeys, deleteService, fetchServices, updateService } from './api'
import type { ServiceInput } from './api'
import type { ServiceResponse } from '../../types/api'

const serviceSchema = z.object({
  name: z.string().trim().min(2, 'Informe o nome do servico').max(120, 'Nome muito longo'),
  description: z.string().trim().max(500, 'Descricao muito longa'),
  price: z
    .string()
    .trim()
    .regex(/^\d+([.,]\d{1,2})?$/, 'Informe o valor, por exemplo 40 ou 40,50'),
  // The field is registered with valueAsNumber, so the form already hands over a number.
  durationMinutes: z
    .number('Informe a duracao')
    .int('Use minutos inteiros')
    .min(5, 'Minimo de 5 minutos')
    .max(600, 'Maximo de 600 minutos'),
  active: z.boolean(),
})

type ServiceFormValues = z.infer<typeof serviceSchema>

function toInput(values: ServiceFormValues): ServiceInput {
  return {
    name: values.name,
    description: values.description.length > 0 ? values.description : null,
    priceCents: Math.round(Number(values.price.replace(',', '.')) * 100),
    durationMinutes: values.durationMinutes,
    active: values.active,
  }
}

export function ServicesPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [editing, setEditing] = useState<ServiceResponse | null>(null)
  const [creating, setCreating] = useState(false)
  const [removing, setRemoving] = useState<ServiceResponse | null>(null)

  const servicesQuery = useQuery({ queryKey: dashboardKeys.services, queryFn: fetchServices })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: dashboardKeys.services })
    void queryClient.invalidateQueries({ queryKey: dashboardKeys.professionals })
  }

  const saveMutation = useMutation({
    mutationFn: ({ id, input }: { id: string | null; input: ServiceInput }) =>
      id === null ? createService(input) : updateService(id, input),
    onSuccess: (_service, variables) => {
      notify(variables.id === null ? 'Servico criado.' : 'Servico atualizado.', 'success')
      invalidate()
      setCreating(false)
      setEditing(null)
    },
    onError: (error) => {
      notify(error instanceof ApiError ? error.message : 'Nao foi possivel salvar o servico.', 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteService(id),
    onSuccess: () => {
      notify('Servico removido.', 'success')
      invalidate()
      setRemoving(null)
    },
    onError: (error) => {
      notify(
        error instanceof ApiError
          ? error.message
          : 'Nao foi possivel remover o servico. Se ele tem agendamentos, desative-o.',
        'error',
      )
      setRemoving(null)
    },
  })

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-lg font-semibold text-slate-900">Servicos</h1>
        {!creating && !editing && <Button onClick={() => setCreating(true)}>Novo servico</Button>}
      </div>

      {(creating || editing) && (
        <ServiceForm
          service={editing}
          saving={saveMutation.isPending}
          onCancel={() => {
            setCreating(false)
            setEditing(null)
          }}
          onSubmit={(values) =>
            saveMutation.mutate({ id: editing?.id ?? null, input: toInput(values) })
          }
        />
      )}

      {servicesQuery.isPending && <ListSkeleton rows={3} />}

      {servicesQuery.isError && (
        <ErrorState
          message="Nao foi possivel carregar os servicos."
          action={
            <Button variant="secondary" onClick={() => void servicesQuery.refetch()}>
              Tentar de novo
            </Button>
          }
        />
      )}

      {servicesQuery.data?.length === 0 && !creating && (
        <EmptyState
          title="Nenhum servico cadastrado"
          description="Cadastre os servicos que seus clientes podem agendar."
          action={<Button onClick={() => setCreating(true)}>Novo servico</Button>}
        />
      )}

      {servicesQuery.data && servicesQuery.data.length > 0 && (
        <ul className="space-y-3">
          {servicesQuery.data.map((service) => (
            <li key={service.id}>
              <Card>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-semibold text-slate-900">{service.name}</p>
                      {!service.active && <Badge tone="neutral">Inativo</Badge>}
                    </div>
                    {service.description && (
                      <p className="mt-1 text-sm text-slate-500">{service.description}</p>
                    )}
                    <p className="mt-1 text-xs text-slate-500">
                      {formatDuration(service.durationMinutes)} - {formatPrice(service.priceCents)}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => {
                        setCreating(false)
                        setEditing(service)
                      }}
                    >
                      Editar
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setRemoving(service)}>
                      Remover
                    </Button>
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={removing !== null}
        title="Remover servico?"
        description={
          removing
            ? `"${removing.name}" deixara de aparecer na pagina publica. Esta acao nao pode ser desfeita.`
            : ''
        }
        confirmLabel="Remover"
        destructive
        loading={deleteMutation.isPending}
        onConfirm={() => removing && deleteMutation.mutate(removing.id)}
        onCancel={() => setRemoving(null)}
      />
    </div>
  )
}

interface ServiceFormProps {
  service: ServiceResponse | null
  saving: boolean
  onSubmit: (values: ServiceFormValues) => void
  onCancel: () => void
}

function ServiceForm({ service, saving, onSubmit, onCancel }: ServiceFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceSchema),
    defaultValues: {
      name: service?.name ?? '',
      description: service?.description ?? '',
      price: service ? (service.priceCents / 100).toFixed(2).replace('.', ',') : '',
      durationMinutes: service?.durationMinutes ?? 30,
      active: service?.active ?? true,
    },
  })

  return (
    <Card>
      <CardHeader title={service ? 'Editar servico' : 'Novo servico'} />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <Field label="Nome" htmlFor="name" error={errors.name?.message}>
          <Input id="name" {...register('name')} />
        </Field>
        <Field label="Descricao" htmlFor="description" error={errors.description?.message}>
          <Textarea id="description" rows={2} {...register('description')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Preco (R$)" htmlFor="price" error={errors.price?.message}>
            <Input id="price" inputMode="decimal" placeholder="40,00" {...register('price')} />
          </Field>
          <Field
            label="Duracao (minutos)"
            htmlFor="durationMinutes"
            error={errors.durationMinutes?.message}
          >
            <Input id="durationMinutes" type="number" min={5} step={5} {...register('durationMinutes', { valueAsNumber: true })} />
          </Field>
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" className="size-4 rounded" {...register('active')} />
          Disponivel para agendamento
        </label>
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
