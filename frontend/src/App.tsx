import { useEffect, useState } from 'react'
import { fetchHealth, type Health } from './api/health'
import './App.css'

type Status =
  | { state: 'loading' }
  | { state: 'connected'; health: Health }
  | { state: 'error'; message: string }

function App() {
  const [status, setStatus] = useState<Status>({ state: 'loading' })

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
    </main>
  )
}

export default App