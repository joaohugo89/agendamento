import { Navigate, Route, Routes } from 'react-router-dom'

import { NotFoundPage } from './components/NotFoundPage'
import { LoginPage } from './features/auth/LoginPage'
import { RegisterPage } from './features/auth/RegisterPage'
import { RequireAuth } from './features/auth/RequireAuth'
import { BookingPage } from './features/booking/BookingPage'
import { AgendaPage } from './features/dashboard/AgendaPage'
import { BlocksPage } from './features/dashboard/BlocksPage'
import { DashboardLayout } from './features/dashboard/DashboardLayout'
import { OpeningHoursPage } from './features/dashboard/OpeningHoursPage'
import { ProfessionalHoursPage } from './features/dashboard/ProfessionalHoursPage'
import { ProfessionalsPage } from './features/dashboard/ProfessionalsPage'
import { ServicesPage } from './features/dashboard/ServicesPage'

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/painel" replace />} />
      <Route path="/agendar/:slug" element={<BookingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cadastro" element={<RegisterPage />} />
      <Route
        path="/painel"
        element={
          <RequireAuth>
            <DashboardLayout />
          </RequireAuth>
        }
      >
        <Route index element={<AgendaPage />} />
        <Route path="servicos" element={<ServicesPage />} />
        <Route path="profissionais" element={<ProfessionalsPage />} />
        <Route path="profissionais/:id/horarios" element={<ProfessionalHoursPage />} />
        <Route path="horarios" element={<OpeningHoursPage />} />
        <Route path="bloqueios" element={<BlocksPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
