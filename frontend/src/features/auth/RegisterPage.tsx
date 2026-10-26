import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { Button } from '../../components/ui/Button'
import { Field, Input } from '../../components/ui/Field'
import { ApiError } from '../../lib/api'
import { useAuth } from './AuthContext'
import { AuthShell } from './AuthShell'

const registerSchema = z.object({
  ownerName: z.string().trim().min(3, 'Informe seu nome').max(120, 'Nome muito longo'),
  businessName: z
    .string()
    .trim()
    .min(3, 'Informe o nome do estabelecimento')
    .max(120, 'Nome muito longo'),
  email: z.string().trim().email('Informe um e-mail valido'),
  password: z.string().min(8, 'A senha precisa ter ao menos 8 caracteres').max(72, 'Senha muito longa'),
})

type RegisterFormValues = z.infer<typeof registerSchema>

export function RegisterPage() {
  const { register: registerAccount } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { ownerName: '', businessName: '', email: '', password: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await registerAccount(values)
      navigate('/painel', { replace: true })
    } catch (error) {
      setFormError(
        error instanceof ApiError ? error.message : 'Nao foi possivel criar a conta. Tente novamente.',
      )
    }
  })

  return (
    <AuthShell
      title="Criar conta"
      footer={
        <p className="text-sm text-slate-500">
          Ja tem conta?{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            Entrar
          </Link>
        </p>
      }
    >
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <Field label="Seu nome" htmlFor="ownerName" error={errors.ownerName?.message}>
          <Input id="ownerName" autoComplete="name" {...register('ownerName')} />
        </Field>
        <Field
          label="Nome do estabelecimento"
          htmlFor="businessName"
          hint="Usamos este nome para gerar o link publico de agendamento."
          error={errors.businessName?.message}
        >
          <Input id="businessName" {...register('businessName')} />
        </Field>
        <Field label="E-mail" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" autoComplete="email" {...register('email')} />
        </Field>
        <Field label="Senha" htmlFor="password" error={errors.password?.message}>
          <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
        </Field>

        {formError && (
          <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {formError}
          </p>
        )}

        <Button type="submit" size="lg" className="w-full" loading={isSubmitting}>
          Criar conta
        </Button>
      </form>
    </AuthShell>
  )
}
