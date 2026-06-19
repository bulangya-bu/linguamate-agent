# LinguaMate Agent Deployment Guide

This guide covers local setup for the Spring Boot application.

## Environment

- JDK 17
- MySQL 8.x
- Windows PowerShell or another shell that can run Maven Wrapper
- Optional OpenAI-compatible API provider

## Database Setup

The default configuration connects to MySQL and creates the `linguamate` database if the configured user has permission:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/linguamate?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8
spring.datasource.username=root
spring.datasource.password=
```

Schema and seed data are loaded from:

- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`

If your local MySQL root user has a password, update `spring.datasource.password` or pass it as a runtime property:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password=your-password"
```

## AI Configuration

LinguaMate uses an OpenAI-compatible REST API client. These environment variables are optional:

```powershell
$env:LINGUAMATE_AI_BASE_URL="https://api.openai.com/v1"
$env:LINGUAMATE_AI_API_KEY="your-api-key"
$env:LINGUAMATE_AI_CHAT_MODEL="gpt-4o-mini"
$env:LINGUAMATE_AI_EMBEDDING_MODEL="text-embedding-3-small"
$env:LINGUAMATE_GNEWS_API_KEY="your-gnews-api-key"
```

When no API key is configured, the app still starts and local fallback logic keeps the demo usable.
The GNews key is optional and only affects the latest-news search panel.

## Run Locally

```powershell
.\mvnw.cmd spring-boot:run
```

Then open:

```text
http://localhost:4040
```

## Run Tests

```powershell
.\mvnw.cmd test
```

The test suite uses a fake local model endpoint and verifies fallback behavior, so it does not require external network access for AI calls.

## Common Issues

### Port 4040 Is Busy

Start with another port:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=4041"
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
