# 语伴智学（LinguaMate）Deployment Guide

This guide covers the independently served Vue 3 / Vite frontend and Spring Boot API. Frontend source builds into `frontend/dist`; Maven builds the API JAR separately.

## Environment

- JDK 17 (local) or Docker
- MySQL 8.x
- Windows PowerShell or another shell that can run Maven Wrapper
- Node.js 20.x (20.19 or newer) or 22.12+ with npm when developing or rebuilding the Vite frontend
- Optional Redis for caching; use `LINGUAMATE_REDIS_CACHE_ENABLED=false` when running without Redis
- Optional OpenAI-compatible API provider

## Configuration via Environment Variables

All sensitive or environment-specific settings are configurable through environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `4040` | API HTTP port |
| `FRONTEND_PORT` | `5173` | Frontend HTTP port |
| `VITE_API_PROXY_TARGET` | `http://127.0.0.1:4040` | Vite development/preview API upstream |
| `VITE_API_BASE_URL` | *(empty)* | Optional browser-facing API origin |
| `LINGUAMATE_WEB_ALLOWED_ORIGINS` | Local frontend origins on port 5173 | Exact credentialed CORS allowlist |
| `SESSION_COOKIE_SECURE` | `false` | Set true for HTTPS deployment |
| `DB_URL` | `jdbc:mysql://localhost:3306/linguamate?...` | MySQL JDBC URL |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | JDBC driver class |
| `DB_USER` | `root` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `DB_INIT_MODE` | `always` | SQL init mode (`always`/`never`; use `never` in production) |
| `LINGUAMATE_AI_BASE_URL` | `https://api.openai.com/v1` | AI provider base URL |
| `LINGUAMATE_AI_API_KEY` | *(empty)* | AI API key |
| `LINGUAMATE_AI_CHAT_MODEL` | `gpt-4o-mini` | Chat model name |
| `LINGUAMATE_AI_EMBEDDING_MODEL` | `text-embedding-3-small` | Reserved embedding setting; current RAG uses keyword retrieval |
| `LINGUAMATE_REDIS_CACHE_ENABLED` | `true` | Enable Redis cache |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis connection |

When no AI API key is configured, the app still starts and local fallback logic keeps the demo usable.

These AI environment variables supply deployment defaults. Signed-in users can save their own base URL, API key and model through the sidebar's “模型配置”; the administrator has a separate “模型连接” page. Use the chat-completion connection test after configuration. Model-list retrieval alone does not prove the provider can answer requests. Local fallback and online-model feedback are labeled separately.

## Database Setup

Schema and seed data are loaded from:

- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`

> **Production:** Set `DB_INIT_MODE=never` and initialize the database manually once.

## Run Locally

On Windows, run the project launcher:

```powershell
.\start.cmd
```

The script prepares dependencies and starts the configured database, API, and frontend. Use `-NoOpen` to skip opening a browser, or `-CheckOnly` to check the environment. The browser entry is:

```text
http://127.0.0.1:5173
```

The API listens on port 4040. Optional local settings can be copied from `scripts/local.config.example.ps1` to ignored `.runtime/config.ps1`. The launcher can reuse the Windows-encrypted database credentials under `%LOCALAPPDATA%\LinguaMate`, or use explicit `DB_URL`, `DB_USER` and `DB_PASSWORD` values. On a new Windows computer with MySQL 8 installed, initialize a private instance with `scripts/setup-local-db.ps1`.

Use `.\stop.cmd` to stop the managed frontend/API. Stop and start after backend source or dependency changes. Frontend edits use Vite hot reload. Launcher logs and process records are in `.runtime/`.

For separate development terminals, start the backend from the project root with `.\mvnw.cmd spring-boot:run`, with the database environment configured. Start Vite from `frontend/`:

```powershell
cd frontend
npm ci
npm run dev
```

Build the two projects independently:

```powershell
cd frontend
npm ci
npm run build
cd ..
.\mvnw.cmd package
```

The frontend build writes to `frontend/dist`. The backend JAR contains API resources. The frontend container serves the assets through Nginx, proxies `/api` to `app:4040`, and falls back to `index.html` for Vue history routes. Local `npm run preview` also proxies the API.

Application routes include `/login`, `/`, `/chat`, `/vocabulary`, `/review`, `/news`, `/writing`, `/wordbank`, `/report` and `/admin`. Demo accounts for local evaluation are:

```text
Student: student / student123
Administrator: admin / admin123
```

Change or disable seeded demonstration accounts before exposing a deployment to the public internet.

## Run Tests

```powershell
.\mvnw.cmd test
cd frontend
npm test
npm run build
```

The test suite uses a fake local model endpoint and verifies fallback behavior, so it does not require external network access for AI calls.

---

## Docker Deployment

### Prerequisites

- Docker & Docker Compose (v2)

### Quick Start

```powershell
# 1. Set your secrets (PowerShell)
$env:DB_PASSWORD="your-strong-password"
$env:LINGUAMATE_AI_API_KEY="your-api-key"

# 2. Build and start
docker compose up -d --build
```

The frontend will be available at `http://localhost:5173`. The API uses port 4040.

Readiness check: `http://localhost:4040/actuator/health/readiness`

### Stop

```powershell
docker compose down
```

To also delete the database volume:

```powershell
docker compose down -v
```

### Build and Run Separately

```powershell
# Build only
docker compose build

# Run without rebuilding
docker compose up -d

# View logs
docker compose logs -f app
```

### Environment File (`.env`)

Create a `.env` file in the project root for convenience:

```env
DB_PASSWORD=your-strong-password
LINGUAMATE_AI_API_KEY=your-api-key
LINGUAMATE_AI_BASE_URL=https://api.openai.com/v1
LINGUAMATE_AI_CHAT_MODEL=gpt-4o-mini
```

Then simply run:

```powershell
docker compose up -d
```

Keep real `.env` files, API keys, local databases and runtime logs out of commits and screenshots. Only placeholder configuration belongs in public documentation.

---

## Production Deployment Checklist

- [ ] Set `DB_INIT_MODE=never` and manually initialize the database with `schema.sql`
- [ ] Set a strong `DB_PASSWORD`
- [ ] Configure `LINGUAMATE_AI_API_KEY`
- [ ] Use a reverse proxy (Nginx/Caddy) with **HTTPS** in front of the app
- [ ] If using Docker, restrict MySQL port (remove `ports:` for `mysql` service so it's only accessible via the internal Docker network)
- [ ] Regularly back up the `mysql-data` volume
- [ ] Monitor via `/actuator/health`

---

## Common Issues

### Port 4040 Is Busy

Start with another port:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=4041"
```

Or with Docker:

```powershell
$env:SERVER_PORT="4041"
docker compose up -d
```

### MySQL Connection Fails

Check that MySQL is running and that the configured user can connect to `localhost:3306`. If database creation fails, manually create the database:

```sql
CREATE DATABASE linguamate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### AI Requests Fail

Confirm the provider base URL ends at the API root, for example `https://api.openai.com/v1`. Check the current account's saved settings and run its connection test. If the key or provider is unavailable, 语伴智学（LinguaMate）continues with clearly labeled local fallback responses. Drafts and already saved learning records remain available.

### Logs

Launcher logs and process state are in ignored `.runtime/`. Application logs also use `logs/`.
