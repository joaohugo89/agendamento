import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'

import { App } from './App'
import { queryClient } from './lib/queryClient'
import { ToastProvider } from './components/ui/Toast'
import { AuthProvider } from './features/auth/AuthContext'
import './index.css'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Elemento #root nao encontrado')
}

createRoot(rootElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  </StrictMode>,
)
