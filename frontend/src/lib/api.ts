const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const TOKEN_STORAGE_KEY = 'agendamento.token'

/** Fired when the token is rejected, so the app can drop a session that is no longer valid. */
export const SESSION_EXPIRED_EVENT = 'agendamento:session-expired'

/** Mirrors the ProblemDetail the backend returns on every error. */
export interface ProblemDetail {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? 'Nao foi possivel completar a operacao.')
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  /** The slot was taken between choosing it and confirming. */
  get isSlotConflict(): boolean {
    return this.status === 409
  }
}

export function readToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function storeToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  authenticated?: boolean
  query?: Record<string, string | undefined>
}

export async function request<TResponse>(path: string, options: RequestOptions = {}): Promise<TResponse> {
  const { method = 'GET', body, authenticated = false, query } = options

  const url = new URL(`${API_URL}${path}`)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined) {
        url.searchParams.set(key, value)
      }
    }
  }

  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (authenticated) {
    const token = readToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  // An expired or revoked token: there is no point keeping the session around.
  if (response.status === 401 && authenticated) {
    clearToken()
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  }

  if (response.status === 204) {
    return undefined as TResponse
  }

  const text = await response.text()
  // A gateway or a proxy can answer with something that is not a ProblemDetail at all.
  let payload: unknown = null
  if (text.length > 0) {
    try {
      payload = JSON.parse(text)
    } catch {
      payload = null
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, (payload ?? { title: `Erro ${response.status}` }) as ProblemDetail)
  }

  return payload as TResponse
}
