# 语伴智学（LinguaMate）Deployment Guide

This guide covers local setup, Docker deployment, and production deployment for 语伴智学（LinguaMate）, a Vue 3 / Vite desktop web application served by Spring Boot. The repository includes production static assets; frontend source changes must be rebuilt before packaging.

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
| `SERVER_PORT` | `4040` | Application HTTP port |
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

Set database credentials for this shell, then start the backend:

```powershell
$env:DB_PASSWORD="your-local-password"
.\mvnw.cmd spring-boot:run
```

Then open:

```text
http://localhost:4040
```

Frontend development uses Vite on port 5173 and proxies `/api` to the backend on port 4040:

```powershell
cd frontend
npm ci
npm run dev
```

To publish frontend source changes to the Spring Boot application, build the assets before starting or packaging it:

```powershell
cd frontend
npm ci
npm run build
cd ..
.\mvnw.cmd package
```

The build writes to `src/main/resources/static`. Alternatively, `.\mvnw.cmd -Pfrontend-build package` invokes the frontend build through Maven. The Dockerfile packages the static assets already in the source tree, so run the frontend build before rebuilding the image when the UI changes.

Application routes include `/login`, `/`, `/mission`, `/chat`, `/vocabulary`, `/review`, `/news`, `/writing`, `/wordbank`, `/report` and `/admin`. Demo accounts for local evaluation are:

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

The app will be available at `http://localhost:4040`.

Health check: `http://localhost:4040/actuator/health`

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

Runtime logs are written under `logs/` and are intentionally ignored by Git.
