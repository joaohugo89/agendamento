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

const loginSchema = z.object({
  email: z.string().trim().email('Informe um e-mail valido'),
  password: z.string().min(1, 'Informe sua senha'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await login(values.email, values.password)
      navigate('/painel', { replace: true })
    } catch (error) {
      setFormError(
        error instanceof ApiError ? error.message : 'Nao foi possivel entrar. Tente novamente.',
      )
    }
  })

  return (
    <AuthShell
      title="Entrar no painel"
      footer={
        <p className="text-sm text-slate-500">
          Ainda nao tem conta?{' '}
          <Link to="/cadastro" className="font-medium text-brand-600 hover:underline">
            Criar conta
          </Link>
        </p>
      }
    >
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <Field label="E-mail" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" autoComplete="email" {...register('email')} />
        </Field>
        <Field label="Senha" htmlFor="password" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            {...register('password')}
          />
        </Field>

        {formError && (
          <p role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {formError}
          </p>
        )}

        <Button type="submit" size="lg" className="w-full" loading={isSubmitting}>
          Entrar
        </Button>
      </form>
    </AuthShell>
  )
}
