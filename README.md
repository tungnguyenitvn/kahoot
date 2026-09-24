# Quiz Room — a Kahoot-style sample game

[![ci](https://github.com/tungnguyenitvn/kahoot/actions/workflows/ci.yml/badge.svg)](https://github.com/tungnguyenitvn/kahoot/actions/workflows/ci.yml)
[![release](https://github.com/tungnguyenitvn/kahoot/actions/workflows/release.yml/badge.svg)](https://github.com/tungnguyenitvn/kahoot/actions/workflows/release.yml)

An end-to-end sample on **Java 25 + Spring Boot 4.1.1 + Gradle 9.7.1 + Angular 22**. A host authors a quiz and opens a room; players join by PIN, answer in order and receive tiered scores. REST handles commands; WebSocket pushes the realtime state. Redis holds the live state and the leaderboard, PostgreSQL holds accounts, immutable quizzes and history.

## Run with Docker

Requires Docker Engine, Docker Compose v2 and Bash (WSL on Windows). Copy `.env.example` to `.env` if you want to change the seeded account, then:

```bash
./scripts/bootstrap             # build the backend and frontend images
docker compose up               # PostgreSQL, Redis, backend, Angular
```

Open `http://localhost:4200`; the backend health endpoint is `http://localhost:8080/actuator/health`. The default host account is `host@example.test` / `local-quiz-only`. The host opens **Host studio**, creates a quiz with at least one question, publishes it and opens a room. A player opens another tab and enters the six-digit PIN and a name. Stop with `Ctrl+C`; development data lives in Docker volumes.

The Gradle wrapper (`backend/gradlew`, `backend/gradle/wrapper`) and `frontend/package-lock.json` are in the source tree; the backend container only needs a JDK and runs the committed wrapper, the frontend container uses `npm ci` with the lockfile. Running the tools outside Docker needs JDK 25, Gradle 9.7.1, Node as pinned by `engines` in `frontend/package.json`, and TypeScript 6.0.x.

## Checks

```bash
./scripts/test                 # temporary Redis/PostgreSQL + backend unit/integration tests
./scripts/verify               # full gate: compose config, Lua smoke, backend, check-docs, Angular tests/build
node scripts/check-docs.mjs    # documentation lint, runs without Docker
node --test scripts/check-docs.test.mjs   # self-test of the lint on fixture trees
node --test scripts/check-pr.test.mjs     # self-test of the pull request title/description check (workflow pr-shape)
node --test "frontend/tests/**/*.test.mjs"
cd frontend && npm run test:ui    # Angular component specs (Vitest + jsdom), needs node_modules
./e2e/run                      # browser check of the release stack (Playwright); run by hand before a release, not in the gate
texlua scripts/test-room.lua . # Lua smoke test with a Redis double; texlua or any Lua 5.3/5.4
```

The offline commands do not exercise Spring, Redis, PostgreSQL, a browser or a real WebSocket. `./scripts/verify` uses an isolated compose project and never touches the development volumes; the Gradle distribution, the Maven dependencies and the npm cache live in `.cache/` (gitignored) so later runs and CI do not download them again. The scope of every gate and the acceptance ID → test matrix: [testing](docs/development.md#testing-and-evidence).

## Release

CI (`ci.yml`) runs `scripts/verify` and then `scripts/smoke-release` on every pull request and
push to `main`; the release images package exactly the jar and the bundle the gate
checked, without rebuilding. Push a tag `vX.Y.Z`: the `release` workflow verifies the
tagged tree again, packages the release images (`backend/Dockerfile.release`,
`frontend/Dockerfile.release`), smoke-tests the `compose.release.yaml` stack, pushes the
images to GHCR (`ghcr.io/tungnguyenitvn/kahoot-backend`,
`ghcr.io/tungnguyenitvn/kahoot-frontend`) and creates a GitHub Release with the jar. Run
the stack from the published images:

```bash
DB_PASSWORD=... DEMO_PASSWORD=... PUBLIC_ORIGIN=http://localhost:8081 docker compose -f compose.release.yaml up
```

The release stack has no TLS; put a TLS terminator in front and set `PUBLIC_ORIGIN` and
`COOKIE_SECURE` as described in [deployment](docs/architecture/README.md#deployment). Cutting
a release, hotfixes and the required checks: [delivery](docs/development.md#delivery).

## Contributing

The step-by-step guide for a developer, from taking an issue, naming the branch and
running the gates locally to opening a pull request on the template and review:
[CONTRIBUTING](CONTRIBUTING.md).

## Documentation and the AI workflow

Start at the [documentation map](docs/README.md): every question has exactly one owning
document, and a placement test says whether a sentence belongs to the domain, a feature,
the architecture or a contract. The [workflow](docs/development.md#workflow) and AGENTS.md
define context, scope and the Definition of Done for humans and AI. The
[verification status](docs/development.md#gate-status) records the latest result of every
gate with its revision and environment.

## Design

- [Feature index](docs/features/README.md) · [live quiz invariants](docs/domain.md#acceptance-invariants) · [game rules](docs/domain.md)
- [System architecture](docs/architecture/README.md) · [Backend architecture](docs/architecture/backend.md) · [Frontend architecture](docs/architecture/frontend.md) · [Redis live state](docs/adr/0002-redis-game-state.md)
- [Contracts index](docs/README.md#contracts) · [REST API](docs/contracts/rest-api.md) · [Redis room](docs/contracts/redis-room.md) · [WebSocket](docs/contracts/websocket.md) · [Realtime delivery](docs/architecture/backend.md#realtime-delivery)
- [Testing and traceability](docs/development.md#testing-and-evidence) · [operations](docs/operations.md)

This is a single-backend-instance sample: the atomicity, ordering and privacy invariants are in the [domain rules](docs/domain.md#acceptance-invariants), the scope and non-goals in the [product scope](docs/architecture/README.md#scope-and-non-goals).
