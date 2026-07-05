# AGENTS.md

This file gives coding agents project-specific guidance for the whole
repository. It applies to every file under this directory.

## Project Overview

LinguaMate Agent is a Spring Boot English learning assistant for Java
coursework. It combines:

- A visible Agent loop: Observe, Think, Act, Answer.
- Conversation practice, vocabulary learning, review scheduling, writing
  feedback, news reading, and learning reports.
- Lightweight keyword-based RAG over seeded knowledge documents.
- OpenAI-compatible chat API integration with local fallback behavior.
- A vanilla HTML/CSS/JavaScript single-page UI plus login/admin pages.

Core stack:

- Java 17
- Spring Boot 3.5.3
- Maven Wrapper
- MyBatis-Plus 3.5.7
- MySQL 8-compatible database
- Spring AI dependencies, with a custom OpenAI-compatible REST client
- Static frontend assets under `src/main/resources/static`

## CodeGraph

This project has a CodeGraph MCP server (`codegraph_*` tools) configured.
CodeGraph is a tree-sitter-parsed knowledge graph of symbols, edges, and
files. Prefer it for structural exploration before editing Java or frontend
code.

Use CodeGraph for:

- Finding definitions: `codegraph_search`
- Understanding a feature area: `codegraph_context`
- Seeing callers/callees: `codegraph_callers`, `codegraph_callees`
- Estimating change impact: `codegraph_impact`
- Inspecting related source in one pass: `codegraph_explore`
- Listing indexed files: `codegraph_files`

Use native search such as `rg` only for literal text, resource contents,
comments, log messages, static assets, or after a specific file is already
identified.

If `.codegraph/` is missing and the server reports that the project is not
initialized, ask the user before running `codegraph init -i`.

## Repository Layout

```text
src/main/java/com/example/javabigwork
  admin/        Admin dashboard APIs and aggregate management services
  agent/        Agent orchestration, prompts, trace recording, memory
  ai/           OpenAI-compatible settings, DTOs, persistence, REST client
  auth/         Login/register/session auth, roles, current-user context
  chat/         Conversation APIs, messages, persistence
  common/       ApiResponse, PageResponse, errors, global exception handling
  config/       Datasource, MyBatis, web, AI, RAG, migration config
  dictionary/   Local/online word lookup and translation support
  knowledge/    Knowledge documents, chunking, keyword retrieval
  news/         News articles, reading records, recommendations, hover lookup
  report/       Learning report summary
  tool/         Agent tools used by the orchestrator
  ui/           Page routing
  vocabulary/   Word bank, quiz, favorites, progress, review scheduling
  writing/      Essay submission, scoring, feedback, AI review
src/main/resources
  application.properties
  schema.sql
  data.sql
  word-bank/
  static/
docs/
scripts/
```

## Build And Run

Use the Maven Wrapper from the repository root.

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean package
```

The app listens on `http://localhost:4040` by default.

For Docker-based local runs:

```powershell
$env:DB_PASSWORD="your-local-password"
docker compose up --build
```

Important runtime configuration lives in
`src/main/resources/application.properties` and can be overridden with
environment variables:

- `SERVER_PORT`
- `DB_URL`, `DB_DRIVER`, `DB_USER`, `DB_PASSWORD`, `DB_INIT_MODE`
- `LINGUAMATE_AI_BASE_URL`
- `LINGUAMATE_AI_API_KEY`
- `LINGUAMATE_AI_CHAT_MODEL`
- `LINGUAMATE_AI_EMBEDDING_MODEL`
- `LINGUAMATE_GNEWS_API_KEY`

Do not commit real API keys, database passwords, local databases, logs, or
generated build artifacts.

## Testing Expectations

Primary automated test file:

```text
src/test/java/com/example/javabigwork/JavaBigWorkApplicationTests.java
```

The integration tests use `MockMvc` and intentionally point the AI base URL at
an unavailable local port. This verifies fallback behavior without requiring a
real model provider key.

When changing backend behavior:

- Run `.\mvnw.cmd test` when feasible.
- Add or update `MockMvc` tests for API behavior, auth behavior, persistence
  changes, and fallback logic.
- Preserve the existing local fallback paths for AI, writing review, dictionary
  lookup, and seeded news/knowledge data.

When changing frontend behavior:

- Manually verify `http://localhost:4040` after `.\mvnw.cmd spring-boot:run`
  when the change affects visible UI or flows.
- Check login, dashboard navigation, vocabulary quiz, writing review, news
  hover lookup, and admin pages when touched.

## Backend Conventions

- Keep feature code inside its domain package. Use the existing
  `controller` / `dto` / `entity` / `mapper` / `service` layout where present.
- Controllers should return `ApiResponse` or `PageResponse` shapes consistent
  with existing APIs.
- Prefer constructor injection for Spring services.
- Use MyBatis-Plus mappers and query wrappers as the existing services do.
- Use `UserContext` for current-user scoped operations instead of accepting
  user IDs from clients.
- Keep role checks aligned with `AuthInterceptor`: `/api/admin/**` and
  knowledge management endpoints require admin access.
- Throw `IllegalArgumentException` for client-correctable bad input and let
  `GlobalExceptionHandler` map errors.
- Keep user-facing Chinese messages concise and consistent with the existing
  style.
- Avoid adding broad abstractions unless they remove real duplication or match
  an established local pattern.

## Agent, AI, And RAG Conventions

- `AgentOrchestrator` is the main Agent loop entry point. Preserve the trace
  stages `Observe`, `Think`, `Act`, and `Answer` for explainability.
- `RetrievalService.search(query, limit)` is the current lightweight RAG seam.
  It uses keyword matching over `knowledge_chunk`; keep its public contract
  stable if replacing internals with embeddings or a vector database.
- `AgentPromptFactory` is where retrieved snippets should be injected into
  prompts.
- `WebAnswerService` and `OpenAiCompatibleClient` handle online model calls.
  Online failures must degrade to local fallback responses rather than breaking
  the demo.
- Runtime AI settings are user-scoped. Do not make API keys global unless the
  product behavior is intentionally changed and tests/docs are updated.
- Tools under `tool/` should stay deterministic enough to support demos and
  tests without external services.

## Database And Seed Data

- Schema changes belong in `src/main/resources/schema.sql`.
- Seed/demo data belongs in `src/main/resources/data.sql` or
  `src/main/resources/word-bank`.
- Keep schema, entities, mappers, services, and tests in sync.
- The default `spring.sql.init.mode` is controlled by `DB_INIT_MODE`; Docker
  uses database init scripts and sets app-side init to `never`.

## Frontend Conventions

Static frontend files live in:

```text
src/main/resources/static/index.html
src/main/resources/static/login.html
src/main/resources/static/admin.html
src/main/resources/static/css/app.css
src/main/resources/static/js/app.js
src/main/resources/static/js/auth.js
src/main/resources/static/js/admin.js
```

Guidelines:

- This is a real app UI, not a marketing page. Preserve task-first workflows.
- Keep vanilla JavaScript patterns unless introducing a framework is explicitly
  requested.
- Reuse existing API helpers, auth handling, section navigation, and rendering
  patterns.
- Ensure responsive layouts do not overlap or hide text on mobile.
- For vocabulary and quiz flows, keep layout dimensions stable after answering
  to avoid visual jumping.
- For news reading, keep hover/click lookup fast and backed by local dictionary
  fallback.

## Documentation

Useful project docs:

- `README.md`
- `docs/PROJECT_ARCHITECTURE.md`
- `docs/SPRING_AI_AND_RAG_NOTES.md`
- `docs/TEST_REPORT.md`
- `docs/DEPLOYMENT.md`

Some docs may have encoding artifacts in the current working tree. Do not
rewrite large documentation files just to fix encoding unless the user asks.

## Git And Workspace Safety

- The working tree may already contain user changes. Do not revert or overwrite
  unrelated edits.
- Before editing, check the specific files you will touch.
- Keep changes scoped to the requested task.
- Do not delete local runtime files, generated outputs, or untracked files
  unless the user explicitly asks.
- Avoid changing `.gitignore`, Docker files, or deployment config as part of a
  feature unless those files are directly involved.

## Review Checklist Before Finishing

- Code follows the existing package and response conventions.
- Auth/user scoping is preserved.
- AI/network-dependent paths still have local fallback behavior.
- Schema/entity/test changes are synchronized.
- Relevant tests were run, or the reason they were not run is reported.
- The final response names changed files and any verification performed.
