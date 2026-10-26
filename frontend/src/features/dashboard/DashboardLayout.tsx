import { NavLink, Outlet } from 'react-router-dom'

import { useAuth } from '../auth/AuthContext'

const NAV_ITEMS = [
  { to: '/painel', label: 'Agenda', end: true },
  { to: '/painel/servicos', label: 'Servicos', end: false },
  { to: '/painel/profissionais', label: 'Profissionais', end: false },
  { to: '/painel/horarios', label: 'Horarios', end: false },
  { to: '/painel/bloqueios', label: 'Bloqueios', end: false },
] as const

export function DashboardLayout() {
  const { session, logout } = useAuth()
  const publicLink = session ? `/agendar/${session.businessSlug}` : '/'

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-900">
              {session?.businessName ?? 'Painel'}
            </p>
            <a
              href={publicLink}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-brand-600 hover:underline"
            >
              Ver pagina publica
            </a>
          </div>
          <button
            type="button"
            onClick={logout}
            className="shrink-0 text-sm font-medium text-slate-500 hover:text-slate-900"
          >
            Sair
          </button>
        </div>
        <nav className="mx-auto max-w-5xl overflow-x-auto px-2">
          <ul className="flex gap-1 pb-1">
            {NAV_ITEMS.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `inline-block whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition ${
                      isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
