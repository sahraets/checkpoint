import { apiGet } from './client'

export interface GameSummary {
  id: number
  title: string
  releaseDate: string | null
  platformId: number | null
  boxArtUrl: string | null
}

export function searchGames(query: string): Promise<GameSummary[]> {
  return apiGet<GameSummary[]>(`/games/search?q=${encodeURIComponent(query)}`)
}
