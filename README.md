# checkpoint

An overview of my played games and want to play games.

Built on the [TheGamesDB](https://api.thegamesdb.net/) game database.

## Stack

| Part     | Tech                             | Port |
| -------- | -------------------------------- | ---- |
| Backend  | Spring Boot 4.1 (Java 25, Maven) | 8080 |
| Frontend | React 19 + TypeScript (Vite)     | 5173 |

## Layout

```
backend/    Spring Boot app
  src/main/java/com/checkpoint/backend/
    BackendApplication.java     entry point
    health/HealthController.java  GET /api/health
    games/GamesController.java    GET /api/games/search?q=
    games/GamesService.java       calls TheGamesDB, key stays server-side
frontend/   Vite + React + TypeScript app
  src/
    api/client.ts   fetch wrapper, all calls go through /api
    api/health.ts   typed health endpoint
    api/games.ts    typed game search endpoint
    App.tsx         shows whether the backend is reachable
```

## Running it

Two terminals.

**Backend:**

Needs a TheGamesDB API key ([get one here](https://api.thegamesdb.net/key.php),
requires a free account at [thegamesdb.net](https://thegamesdb.net/)). Set it
as an environment variable — never commit it:

```bash
export THEGAMESDB_API_KEY=your-key-here
```

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install   # first time only
npm run dev
```

Then open http://localhost:5173. The page says "Connected to checkpoint-backend"
when both sides are up.

### How the two talk

The browser only ever calls its own origin, `http://localhost:5173/api/...`.
Vite's dev server proxies everything under `/api` to Spring Boot on port 8080
(configured in `frontend/vite.config.ts`), so there is no CORS setup to
maintain. The TheGamesDB API key lives on the backend only, never in the
browser.

## Checks

```bash
cd backend && ./mvnw test     # backend tests
cd frontend && npm run build  # type-check + production build
cd frontend && npm run lint
```
