import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '../../components/ui/Button'
import { Field, Input, Textarea } from '../../components/ui/Field'

export const customerSchema = z.object({
  customerName: z.string().trim().min(3, 'Informe seu nome completo').max(120, 'Nome muito longo'),
  customerWhatsapp: z
    .string()
    .trim()
    .min(10, 'Informe o WhatsApp com DDD')
    .max(20, 'Numero muito longo')
    .regex(/^[0-9()+\-\s]+$/, 'Use apenas numeros'),
  notes: z.string().trim().max(500, 'Observacao muito longa'),
})

export type CustomerFormValues = z.infer<typeof customerSchema>

interface CustomerStepProps {
  defaultValues?: Partial<CustomerFormValues>
  onSubmit: (values: CustomerFormValues) => void
}

export function CustomerStep({ defaultValues, onSubmit }: CustomerStepProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CustomerFormValues>({
    resolver: zodResolver(customerSchema),
    defaultValues: {
      customerName: defaultValues?.customerName ?? '',
      customerWhatsapp: defaultValues?.customerWhatsapp ?? '',
      notes: defaultValues?.notes ?? '',
    },
  })

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      className="space-y-4 rounded-xl bg-white p-5 ring-1 ring-slate-200"
    >
      <Field label="Nome" htmlFor="customerName" error={errors.customerName?.message}>
        <Input
          id="customerName"
          autoComplete="name"
          placeholder="Como podemos te chamar"
          {...register('customerName')}
        />
      </Field>

      <Field
        label="WhatsApp"
        htmlFor="customerWhatsapp"
        hint="Usamos apenas para falar sobre este agendamento."
        error={errors.customerWhatsapp?.message}
      >
        <Input
          id="customerWhatsapp"
          inputMode="tel"
          autoComplete="tel"
          placeholder="(11) 99999-9999"
          {...register('customerWhatsapp')}
        />
      </Field>

      <Field label="Observacao (opcional)" htmlFor="notes" error={errors.notes?.message}>
        <Textarea
          id="notes"
          rows={3}
          placeholder="Algo que o profissional precise saber"
          {...register('notes')}
        />
      </Field>

      <Button type="submit" size="lg" className="w-full">
        Continuar
      </Button>
    </form>
  )
}
