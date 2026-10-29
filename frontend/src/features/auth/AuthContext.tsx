import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { clearToken, readToken, request, SESSION_EXPIRED_EVENT, storeToken } from '../../lib/api'
import { queryClient } from '../../lib/queryClient'
import type { AuthResponse } from '../../types/api'

const SESSION_STORAGE_KEY = 'agendamento.session'

export interface Session {
  userName: string
  businessName: string
  businessSlug: string
}

interface AuthContextValue {
  session: Session | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

export interface RegisterInput {
  ownerName: string
  businessName: string
  email: string
  password: string
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredSession(): Session | null {
  if (!readToken()) {
    return null
  }
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(readStoredSession)

  const persist = useCallback((response: AuthResponse) => {
    // Cached rows belong to the previous tenant: drop them before the new session starts.
    queryClient.clear()
    const next: Session = {
      userName: response.userName,
      businessName: response.businessName,
      businessSlug: response.businessSlug,
    }
    storeToken(response.token)
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(next))
    setSession(next)
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: { email, password },
      })
      persist(response)
    },
    [persist],
  )

  const register = useCallback(
    async (input: RegisterInput) => {
      const response = await request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: input,
      })
      persist(response)
    },
    [persist],
  )

  const logout = useCallback(() => {
    queryClient.clear()
    clearToken()
    localStorage.removeItem(SESSION_STORAGE_KEY)
    setSession(null)
  }, [])

  // The API drops the token when it is rejected; here the session follows it, and RequireAuth
  // sends the owner back to the login screen instead of leaving a panel that only errors.
  useEffect(() => {
    window.addEventListener(SESSION_EXPIRED_EVENT, logout)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, logout)
  }, [logout])

  const value = useMemo<AuthContextValue>(
    () => ({ session, isAuthenticated: session !== null, login, register, logout }),
    [session, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth precisa estar dentro de AuthProvider')
  }
  return context
}
