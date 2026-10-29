import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'

import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardHeader } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Field, Input } from '../../components/ui/Field'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/ui/Feedback'
import { useToast } from '../../components/ui/Toast'
import { ApiError } from '../../lib/api'
import {
  createProfessional,
  dashboardKeys,
  deleteProfessional,
  fetchProfessionals,
  fetchServices,
  updateProfessional,
} from './api'
import type { ProfessionalResponse, ServiceResponse } from '../../types/api'

const professionalSchema = z.object({
  name: z.string().trim().min(2, 'Informe o nome').max(120, 'Nome muito longo'),
  active: z.boolean(),
  serviceIds: z.array(z.string()).min(1, 'Selecione ao menos um servico'),
})

type ProfessionalFormValues = z.infer<typeof professionalSchema>

export function ProfessionalsPage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<ProfessionalResponse | null>(null)
  const [removing, setRemoving] = useState<ProfessionalResponse | null>(null)

  const professionalsQuery = useQuery({
    queryKey: dashboardKeys.professionals,
    queryFn: fetchProfessionals,
  })
  const servicesQuery = useQuery({ queryKey: dashboardKeys.services, queryFn: fetchServices })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: dashboardKeys.professionals })
  }

  const saveMutation = useMutation({
    mutationFn: ({ id, input }: { id: string | null; input: ProfessionalFormValues }) =>
      id === null ? createProfessional(input) : updateProfessional(id, input),
    onSuccess: (_professional, variables) => {
      notify(variables.id === null ? 'Profissional criado.' : 'Profissional atualizado.', 'success')
      invalidate()
      setCreating(false)
      setEditing(null)
    },
    onError: (error) => {
      notify(
        error instanceof ApiError ? error.message : 'Nao foi possivel salvar o profissional.',
        'error',
      )
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteProfessional(id),
    onSuccess: () => {
      notify('Profissional removido.', 'success')
      invalidate()
      setRemoving(null)
    },
    onError: (error) => {
      notify(
        error instanceof ApiError
          ? error.message
          : 'Nao foi possivel remover. Se ele tem agendamentos, desative-o.',
        'error',
      )
      setRemoving(null)
    },
  })

  const services = servicesQuery.data ?? []

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-lg font-semibold text-slate-900">Profissionais</h1>
        {!creating && !editing && services.length > 0 && (
          <Button onClick={() => setCreating(true)}>Novo profissional</Button>
        )}
      </div>

      {services.length === 0 && !servicesQuery.isPending && (
        <EmptyState
          title="Cadastre um servico primeiro"
          description="Cada profissional precisa estar ligado aos servicos que realiza."
        />
      )}

      {(creating || editing) && (
        <ProfessionalForm
          professional={editing}
          services={services}
          saving={saveMutation.isPending}
          onCancel={() => {
            setCreating(false)
            setEditing(null)
          }}
          onSubmit={(values) => saveMutation.mutate({ id: editing?.id ?? null, input: values })}
        />
      )}

      {professionalsQuery.isPending && <ListSkeleton rows={2} />}

      {professionalsQuery.isError && (
        <ErrorState
          message="Nao foi possivel carregar os profissionais."
          action={
            <Button variant="secondary" onClick={() => void professionalsQuery.refetch()}>
              Tentar de novo
            </Button>
          }
        />
      )}

      {professionalsQuery.data?.length === 0 && services.length > 0 && !creating && (
        <EmptyState
          title="Nenhum profissional cadastrado"
          description="Cadastre quem atende os clientes."
          action={<Button onClick={() => setCreating(true)}>Novo profissional</Button>}
        />
      )}

      {professionalsQuery.data && professionalsQuery.data.length > 0 && (
        <ul className="space-y-3">
          {professionalsQuery.data.map((professional) => (
            <li key={professional.id}>
              <Card>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-semibold text-slate-900">{professional.name}</p>
                      {!professional.active && <Badge tone="neutral">Inativo</Badge>}
                    </div>
                    <p className="mt-1 text-xs text-slate-500">
                      {professional.services.length === 0
                        ? 'Nenhum servico vinculado'
                        : professional.services.map((service) => service.name).join(', ')}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Link to={`/painel/profissionais/${professional.id}/horarios`}>
                      <Button size="sm" variant="secondary">
                        Horarios
                      </Button>
                    </Link>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => {
                        setCreating(false)
                        setEditing(professional)
                      }}
                    >
                      Editar
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setRemoving(professional)}>
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
        title="Remover profissional?"
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

interface ProfessionalFormProps {
  professional: ProfessionalResponse | null
  services: ServiceResponse[]
  saving: boolean
  onSubmit: (values: ProfessionalFormValues) => void
  onCancel: () => void
}

function ProfessionalForm({
  professional,
  services,
  saving,
  onSubmit,
  onCancel,
}: ProfessionalFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfessionalFormValues>({
    resolver: zodResolver(professionalSchema),
    defaultValues: {
      name: professional?.name ?? '',
      active: professional?.active ?? true,
      serviceIds: professional?.services.map((service) => service.id) ?? [],
    },
  })

  return (
    <Card>
      <CardHeader title={professional ? 'Editar profissional' : 'Novo profissional'} />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <Field label="Nome" htmlFor="name" error={errors.name?.message}>
          <Input id="name" {...register('name')} />
        </Field>

        <fieldset>
          <legend className="text-sm font-medium text-slate-700">Servicos que realiza</legend>
          <div className="mt-2 space-y-2">
            {services.map((service) => (
              <label key={service.id} className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  type="checkbox"
                  value={service.id}
                  className="size-4 rounded"
                  {...register('serviceIds')}
                />
                {service.name}
              </label>
            ))}
          </div>
          {errors.serviceIds && (
            <p role="alert" className="mt-1 text-xs font-medium text-rose-600">
              {errors.serviceIds.message}
            </p>
          )}
        </fieldset>

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
