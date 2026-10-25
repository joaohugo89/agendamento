import type { ReactNode } from 'react'

export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-slate-200/70 ${className}`} />
}

export function SlotSkeleton() {
  return (
    <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} className="h-11" />
      ))}
    </div>
  )
}

export function ListSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} className="h-20" />
      ))}
    </div>
  )
}

interface EmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white/60 px-6 py-10 text-center">
      <p className="text-sm font-medium text-slate-700">{title}</p>
      {description && <p className="mx-auto mt-1 max-w-sm text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}

interface ErrorStateProps {
  title?: string
  message: string
  action?: ReactNode
}

export function ErrorState({ title = 'Algo deu errado', message, action }: ErrorStateProps) {
  return (
    <div className="rounded-xl bg-rose-50 px-5 py-6 text-center ring-1 ring-rose-100">
      <p className="text-sm font-semibold text-rose-900">{title}</p>
      <p className="mx-auto mt-1 max-w-sm text-sm text-rose-700">{message}</p>
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}
