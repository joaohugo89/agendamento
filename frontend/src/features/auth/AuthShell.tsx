import type { ReactNode } from 'react'

interface AuthShellProps {
  title: string
  children: ReactNode
  footer?: ReactNode
}

export function AuthShell({ title, children, footer }: AuthShellProps) {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <p className="text-sm font-semibold text-brand-600">Agendamento</p>
          <h1 className="mt-1 text-xl font-semibold text-slate-900">{title}</h1>
        </div>
        <div className="rounded-xl bg-white p-6 ring-1 ring-slate-200">{children}</div>
        {footer && <div className="text-center">{footer}</div>}
      </div>
    </main>
  )
}
