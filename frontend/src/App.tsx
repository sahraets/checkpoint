import { useEffect, useState } from 'react'
import { fetchHealth, type Health } from './api/health'
import { searchGames, type GameSummary } from './api/games'
import './App.css'

type Status =
  | { state: 'loading' }
  | { state: 'connected'; health: Health }
  | { state: 'error'; message: string }

function App() {
  const [status, setStatus] = useState<Status>({ state: 'loading' })
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<GameSummary[]>([])
  const [searchState, setSearchState] = useState<'idle' | 'loading' | 'error'>('idle')

  useEffect(() => {
    let cancelled = false

    fetchHealth()
      .then((health) => {
        if (!cancelled) setStatus({ state: 'connected', health })
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setStatus({
            state: 'error',
            message: error instanceof Error ? error.message : String(error),
          })
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  function handleSearch(event: React.FormEvent) {
    event.preventDefault()
    const trimmed = query.trim()
    if (!trimmed) return

    setSearchState('loading')
    searchGames(trimmed)
      .then((games) => {
        setResults(games)
        setSearchState('idle')
      })
      .catch((error: unknown) => {
        console.error(error)
        setSearchState('error')
      })
  }

  return (
    <main className="app">
      <h1>Checkpoint</h1>
      <p className="tagline">An overview of my played games and want-to-play games.</p>

      <section className="card">
        <h2>Backend connection</h2>
        {status.state === 'loading' && <p>Checking…</p>}
        {status.state === 'connected' && (
          <>
            <p className="ok">Connected to {status.health.service}</p>
            <p className="detail">
              status: {status.health.status} · {status.health.timestamp}
            </p>
          </>
        )}
        {status.state === 'error' && (
          <>
            <p className="fail">Not connected</p>
            <p className="detail">{status.message}</p>
            <p className="detail">Is the Spring Boot app running on port 8080?</p>
          </>
        )}
      </section>

      <section className="card search-card">
        <h2>Search games</h2>
        <form className="search-row" onSubmit={handleSearch}>
          <input
            className="search-input"
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search for a game..."
          />
          <button
            className="search-button"
            type="submit"
            disabled={searchState === 'loading' || !query.trim()}
          >
            {searchState === 'loading' ? 'Searching…' : 'Search'}
          </button>
        </form>

        {searchState === 'loading' && <p className="detail">Searching…</p>}
        {searchState === 'error' && <p className="fail">Search failed.</p>}

        {results.length > 0 && (
          <div className="results-grid">
            {results.map((game) => (
              <div key={game.id} className="result">
                {game.boxArtUrl && (
                  <img className="cover" src={game.boxArtUrl} alt={game.title} />
                )}
                <p className="result-title">{game.title}</p>
                <p className="result-year">{game.releaseDate?.slice(0, 4) ?? '—'}</p>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}

export default App