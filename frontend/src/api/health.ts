import { apiGet } from './client'

export interface Health {
  status: string
  service: string
  timestamp: string
}

export function fetchHealth(): Promise<Health> {
  return apiGet<Health>('/health')
}
