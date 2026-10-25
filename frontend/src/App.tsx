import { Route, Routes } from 'react-router-dom'

import { BookingPage } from './features/booking/BookingPage'
import { NotFoundPage } from './components/NotFoundPage'

export function App() {
  return (
    <Routes>
      <Route path="/agendar/:slug" element={<BookingPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
