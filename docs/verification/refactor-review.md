# Refactor review and verification

Date: 2026-09-23. Artifact: kahoot-sample.zip, documentation/runtime refactor (successor to v8). Scope: single-instance sample; documentation boundaries, build/API
corrections, bounded realtime dispatch, retry repair and connection lifecycle.
This record identifies the delivered refactor artifact, not a production certification.

## Implemented changes

- Layered documentation map, scoped AGENTS instructions, module design and ADRs.
- Fixed invalid CSRF configuration API and Java WS builder call; allowed WS path
  through HTTP filter while retaining handshake authorization and Origin checks.
- Enabled HTTP virtual threads explicitly; retained platform scheduler pool.
- Added bounded/coalesced notification dispatch with per-client single flight.
- Repaired live room registration after partial provisioning failure.
- Extracted client transport for deterministic initial-failure/reconnect/revocation tests.
- Corrected idempotency, snapshot, event and error contracts; recorded unsupported capabilities.
- Moved demo seeding to bootstrap so identity no longer depends on catalog.

## Evidence status

| Check | Result |
|---|---|
| Node policy/connection tests | PASS: 12 tests under Node 24.19.0 |
| Lua double smoke | PASS: 9 sequential invariant checks under texlua |
| Documentation links/import boundaries | PASS: 40 documents, 126 local links/anchors and Java import directions |
| Shell/JSON/YAML syntax | PASS: bash -n scripts/entrypoints, 6 frontend JSON files, 4 YAML files; JS syntax checks |
| Java unit + real Redis/PostgreSQL/WS integration | BLOCKED: scripts/verify attempted, docker command not found; local Java is 17, no Gradle |
| Angular production compilation | BLOCKED: npm run build attempted, ng not found (dependencies not installed) |
| Browser E2E / load / disaster-recovery | NOT IMPLEMENTED / NOT RUN |

The new Node suite initially failed because the extracted connection module was not
yet present, then passed after implementation. This is not evidence of executing
the previous Angular implementation under the new harness.
Java regression tests were added but red/green could not be executed here.

Five backend unit tests were added, plus real-service registration/WS security,
reconnect/revocation assertions. Their compilation and execution remain unverified.
The source tree has no git metadata here; the archive version identifies this snapshot.

Before merge/release run scripts/verify in a Docker-capable environment, fix failures
and record its revision/results. Current source inspection is not a successful build.

## Remaining limits

See [quality/risk register](../architecture/quality-and-risks.md).
No automatic PIN recovery, distributed ownership, server-idempotent draft creation,
metrics pipeline or production-scale benchmark was added.

## Documentation refactor 2026-09-23 (documentation only)

Scope: conceptual corrections and structure. Added the domain state-transition
table, the canonical limits table, an ADR index and template, acceptance IDs for
every feature, the language policy and the known-deviations list in the contracts
index. No code, test or runtime behavior changed; contracts still describe current
behavior, and the two contract deviations (429 for COMMAND_LIMIT, snake_case history
fields) are recorded rather than rewritten.

| Check | Result |
|---|---|
| node scripts/check-docs.mjs | PASS: 41 documents, 165 local links/anchors, wire-example boundary, Java import directions |
| node --test frontend/tests/*.test.mjs | PASS: 12 tests |
| texlua scripts/test-room.lua . | NOT RUN: texlua not installed on the reviewing machine |
| scripts/verify (Docker) | NOT RUN in this session; no code changed |

Environment: macOS, Node 22.22.3, JDK 25.0.4 present but unused. The working tree was
still an extracted archive at the time; it was committed afterwards as 62b9fe4 on main.

## Contract fixes 2026-09-23 (code + docs)

Scope: the two deviations formerly listed in the contracts index. room.lua returns
409 instead of 429 for COMMAND_LIMIT; HistoryController projects explicit camelCase
records (archivedVersion, createdAt, finishedAt) instead of raw SQL column names, and
the frontend HistoryRoom model follows. Regression tests were written first and run
red, then the fix was applied and the full gate run green.

| Check | Result |
|---|---|
| Red run, ./scripts/test before the fix | Unit 5/5 pass; integration 8/10 pass, the 2 new tests failed as intended: `expected: <409> but was: <429>` and a history row still carrying archived_version |
| Green run, ./scripts/verify after the fix | Backend: unit 5/5, integration 10/10, bootJar built. Frontend: check-docs PASS 41 documents/162 links, 12 Node tests, ng build complete |
| Lua smoke, scripts/test-room.lua | PASS 10 checks under Lua 5.4.7 in a throwaway redis:7.4-alpine container; texlua is not installed locally |
| Observed history row | archivedVersion 10, createdAt "2026-09-23T14:47:24.339003Z", finishedAt "2026-09-23T14:47:25.510527Z"; no snake_case field |

Environment: macOS host, Docker Desktop 29.7.2 (linux/aarch64); images
eclipse-temurin:24-jdk with the gradle:8.14.3-jdk21 distribution stage,
postgres:17-alpine, redis:7.4-alpine, node:24.15.0; Gradle 8.14.3. Frontend
dependencies were resolved during this run and written to frontend/package-lock.json.
The verified tree was committed afterwards as revision 62b9fe4 on main; the Gradle
XML reports under backend/build/test-results are the artifact of this run.
