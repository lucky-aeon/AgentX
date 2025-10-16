# Repository Guidelines

## Project Structure & Module Organization
- `AgentX` — Spring Boot (Java 17) backend. Source in `AgentX/src/main`, tests in `AgentX/src/test`. Built with Maven Wrapper (`./mvnw`).
- `agentx-frontend-plus` — Next.js 15 + TypeScript UI. App routes under `app/`, shared UI in `components/`.
- Deployment & ops: `deploy/` (Docker Compose scripts), `docker/`, `docs/`, `config/`, `production/`. Logs in `logs/`.

## Build, Test, and Development Commands
- Backend (from `AgentX`):
  - Build: `./mvnw -DskipTests package`
  - Run (dev): `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
  - Test: `./mvnw test`
  - Format: `./mvnw spotless:apply`
- Frontend (from `agentx-frontend-plus`):
  - Install deps: `npm install` (or `pnpm install`)
  - Dev server: `npm run dev` (http://localhost:3000)
  - Build/Start: `npm run build && npm start`
  - Lint: `npm run lint`
- All‑in‑one (Docker): `cd deploy && ./start.sh` to spin up backend, frontend, DB, MQ.

## Coding Style & Naming Conventions
- Java (backend): formatted via Spotless using `AgentX/eclipse-formatter.xml`. 4‑space indent, PascalCase classes, camelCase methods/fields, packages start with `org.xhy.*`.
- TypeScript/React (frontend): 2‑space indent, React components PascalCase, files kebab‑case (e.g., `model-select-dialog.tsx`). Keep hooks in `hooks/`, UI in `components/`.
- Run linters/formatters before pushing; keep functions small and cohesive.

## Testing Guidelines
- Backend: JUnit via Spring Boot starter. Place tests under `AgentX/src/test/java`, name `*Test.java`. Run with `./mvnw test` and target service/controller layers.
- Frontend: No default test runner configured; for UI changes include a minimal test plan or interactive demo steps in the PR.

## Commit & Pull Request Guidelines
- Use Conventional Commits: `feat(scope): ...`, `fix(scope): ...`, `refactor: ...`, `docs: ...`, `chore: ...`.
- PRs include: clear description, linked issues (`Fixes #123`), test plan (commands + expected result), and screenshots/GIFs for UI.
- Keep PRs small and focused; update docs when behavior changes.

## Security & Configuration Tips
- Copy `.env.example` to `.env` and set secure values (e.g., `JWT_SECRET`, admin and DB passwords). Never commit secrets.
- Prefer `deploy/start.sh` for a consistent local environment. Review `docs/deployment/` for production hardening.

