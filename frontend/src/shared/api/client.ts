const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
  }
}

interface ErrorPayload {
  code?: string
  message?: string
}

export async function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    let payload: ErrorPayload = {}
    try {
      payload = (await response.json()) as ErrorPayload
    } catch {
      // Non-JSON error responses are mapped to a generic message below.
    }
    throw new ApiRequestError(
      response.status,
      payload.code ?? 'REQUEST_FAILED',
      payload.message ?? `Yêu cầu thất bại (HTTP ${response.status})`,
    )
  }

  return (await response.json()) as T
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) return error.message
  if (error instanceof Error) return error.message
  return 'Không thể kết nối tới máy chủ'
}
