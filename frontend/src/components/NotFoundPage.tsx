import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <div className="text-center">
        <p className="text-sm font-semibold text-brand-600">404</p>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">Pagina nao encontrada</h1>
        <p className="mt-2 text-sm text-slate-500">O endereco acessado nao existe.</p>
        <Link to="/" className="mt-6 inline-block text-sm font-medium text-brand-600 hover:underline">
          Voltar ao inicio
        </Link>
      </div>
    </main>
  )
}
