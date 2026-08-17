const BASE_URL = '/api'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/**
 * Thin wrapper around fetch for our own backend. Every call goes through
 * /api, which Vite proxies to Spring Boot in dev (see vite.config.ts).
 */
export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new ApiError(
      `GET ${path} failed with ${response.status}`,
      response.status,
    )
  }

  return (await response.json()) as T
}
