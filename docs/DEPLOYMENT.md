# LinguaMate Agent Deployment Guide

This guide covers local setup, Docker deployment, and production deployment for the Spring Boot application.

## Environment

- JDK 17 (local) or Docker
- MySQL 8.x
- Windows PowerShell or another shell that can run Maven Wrapper
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
| `LINGUAMATE_AI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model name |
| `LINGUAMATE_GNEWS_API_KEY` | *(empty)* | GNews API key (optional) |

When no AI API key is configured, the app still starts and local fallback logic keeps the demo usable.

## Database Setup

Schema and seed data are loaded from:

- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`

> **Production:** Set `DB_INIT_MODE=never` and initialize the database manually once.

## Run Locally

```powershell
.\mvnw.cmd spring-boot:run
```

Then open:

```text
http://localhost:4040
```

Override settings inline:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--DB_PASSWORD=your-password"
```

## Run Tests

```powershell
.\mvnw.cmd test
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
docker compose up -d
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

Confirm the provider base URL ends at the API root, for example `https://api.openai.com/v1`. If the key or provider is unavailable, LinguaMate should continue with local fallback responses.

### Logs

Runtime logs are written under `logs/` and are intentionally ignored by Git.
