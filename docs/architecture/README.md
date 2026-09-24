# System architecture

Scope: one Angular SPA, one Spring Boot application, one Redis standalone and one
PostgreSQL instance. This is a modular monolith; modules are not independent
services.

~~~mermaid
flowchart TB
  Browser["Host / player SPA"] -->|"REST + cookie"| App["Spring Boot application"]
  Browser <-->|"WebSocket snapshots"| App
  App -->|"Live commands / sessions"| Redis[("Redis standalone")]
  App -->|"Catalog / archive transactions"| PG[("PostgreSQL")]
~~~

A new screen or endpoint does not require expanding this overview unless it
changes a system boundary or cross-cutting policy.

## Scope and non-goals

Host authors a bounded multiple-choice quiz, publishes it, opens a room and
controls rounds. Guests join by PIN/nickname and receive tiered scores based on
the order in which correct answers are accepted by the server.

### Actors and journeys

- Host: login → author/publish quiz → open room → host rounds → view archive.
- Player: join → wait → answer → reveal → final ranking.
- Developer/operator: start isolated development services, verify changes, inspect
  archive failures and stop/recover affected rooms.

### Constraints and non-goals

Java 24, Gradle, Spring Boot, Angular 22, Redis standalone, PostgreSQL and Docker
Compose. One backend instance and one scheduled archive consumer are supported.
Exact score/capacity/deadline rules belong to [domain](../domain.md).

No HA, multi-region fairness, offline submission, production SSO, payment,
automatic disaster recovery or full quiz CMS. Network speed is not equalized:
fairness means consistent acceptance order at Redis, not browser click order.

### Quality priorities

Correctness/privacy before notification freshness; bounded notification work;
explicit retry semantics; durable archive only after PostgreSQL commit.
No measured latency/capacity SLO is claimed. See the
[residual risks](#residual-risks--non-goals).

## Ownership and consistency

| Data | Authority | Other copies |
|---|---|---|
| Host accounts, quizzes | PostgreSQL | Frozen quiz content in each room |
| HTTP session, live room, answer receipts | Redis | Browser snapshot, asynchronous archive |
| Finished history | PostgreSQL archive | Redis retained temporarily for retries |
| UI selection/connectivity | Browser route state | Never authoritative for scoring |

One Lua invocation linearizes a room command: authorization, validation,
deduplication, score and event append. It is atomic with respect to other commands,
not a rollback transaction ([gameplay design](backend.md#atomicity-and-storage-errors)).
Scripts on the same Redis instance share execution capacity even across rooms.

REST returns command results; WebSocket is best-effort full-state notification.
Archive consumes room Streams and commits ordered projections before ACK.
These boundaries are not a distributed transaction. Redis loss may lose
acknowledged live work not durably archived.

## Deployment

Supported environment: local development/test with Docker Compose. No production
deployment recipe or HA claim is implied.

| Process | Access / dependency | Persistence |
|---|---|---|
| Browser → Angular dev server | localhost:4200 | No client session tokens stored |
| Angular dev proxy → backend | /api and /ws upgrade to backend:8080 | None |
| Backend → Redis | Internal redis:6379 | AOF everysec on dev volume |
| Backend → PostgreSQL | Internal postgres:5432 | Dev volume |

Host-published ports bind loopback. Session cookie belongs to the browser-facing
origin; REST uses CSRF cookie/header, WS validates Origin and session/membership.
Local HTTP uses insecure cookies only for local development.

compose.test.yaml uses isolated PostgreSQL temporary storage and separate Redis.
scripts/verify runs the Lua smoke, backend tests/build and frontend tests/build, with
cleanup.

### Release images and stack

The release images package what the verify gate built, they compile nothing:
backend/Dockerfile.release puts backend/build/libs/quiz-room.jar on a JRE running as a
non-root user; frontend/Dockerfile.release serves frontend/dist/quiz-room-ui/browser
from nginx, which proxies /api and /ws to the backend service
(frontend/nginx.release.conf). compose.release.yaml runs both images with PostgreSQL and
Redis on one host: DB_PASSWORD and DEMO_PASSWORD have no default, PUBLIC_ORIGIN must be
the browser-facing origin because the WebSocket Origin check uses it, and the demo seed
remains the only account provisioning. scripts/smoke-release packages the artifacts
(run ./scripts/verify first, or set BUILD_ARTIFACTS=1 to build them without tests),
boots the stack and checks that the SPA, the API proxy and CSRF enforcement answer; CI
runs it after the verify job on every push and pull request, and the release workflow
runs it before publishing images to GHCR ([delivery pipeline](../development.md#delivery),
[ADR 0006](../adr/0006-ci-cd-release-images.md),
[ADR 0007](../adr/0007-ci-caches-and-verified-artifacts.md)).

Still not implemented for production: TLS termination in front of nginx and the
secure-cookie flag behind it, idle timeouts, secret management, resource budgets,
backup/restore exercises, operational monitoring and multi-architecture images (the
published images are linux/amd64; arm64 hosts run them under emulation). Publishing an
image is delivery, not deployment; do not expose the development server as a
production web server.

Redis Cluster is NOT supported: although room keys share a hash tag, registration and archive
cleanup touch a global active-room key in the same scripts. Multiple backend
instances also need notification fan-out and archive/provisioning coordination.
See [runbook](../operations.md).

## Limits and timings

Limits in code are admission/safety settings, not benchmark results.
No p95/p99 latency or supported concurrent-player capacity has been measured.

Ownership precedence for numbers: game rules live in [domain](../domain.md),
wire-visible validation bounds in [contracts](../README.md#contracts), and every other
admission, timing, retention and presentation value here. Other documents link instead
of repeating a value; rule L4 in scripts/check-docs.mjs fails the gate when a value is
repeated elsewhere. Change a value here and in its source in the same change.

| Setting | Value | Source |
|---|---|---|
| Participants, questions, options, seconds per question, room expiry, score tiers | game rules | [domain](../domain.md) |
| PIN reservation TTL | 2 h from reservation | RoomService.reservePin |
| Active-room admission | 100 active rooms per Redis, advisory check | RoomService.create |
| Host command ledger | 500 entries per room, then COMMAND_LIMIT | room.lua |
| Room key retention | 24 h, starting at final archive commit | RedisRoomEvents.finish |
| HTTP session timeout | 4 h | application.yml |
| Redis command timeout | 2 s | application.yml |
| Dev Redis memory | 256 MB, noeviction, AOF everysec | compose.yaml |
| Room timer | fixed delay 250 ms after each pass | RoomTimers |
| Archive worker | fixed delay 1 s after each pass; 100 entries per read | ArchiveWorker (delay), RedisRoomEvents (batch) |
| Notification flush | every 50 ms; at least 250 ms between snapshots per connection; idle session recheck after 5 s | RoomWebSocketHub |
| In-flight snapshot tasks | 16 per process, 1 per connection | RoomWebSocketHub |
| WebSocket registration | 1,000 connections per process, 150 per room | RoomWebSocketHub |
| WebSocket control replies | at most 1 PONG/ERROR per second per connection | RoomWebSocketHub |
| WebSocket send guard | 10 s send time, 256 KB buffer per session | RoomWebSocketHub |
| WebSocket inbound text | wire bound owned by the [WebSocket contract](../contracts/websocket.md) | RoomWebSocketHub |
| PIN reservation attempts | 100 random PINs per room creation, then PIN_UNAVAILABLE | RoomService.reservePin |
| Client REST reconciliation | every 5 s while the room route is open | room-connection.mjs |
| Client reconnect backoff | 500 ms doubling to a 10 s cap, jitter factor 0.8–1.2 | room-connection.mjs |
| Client clock refresh | every 100 ms | room-store.ts |
| Leaderboard rows shown | top 10 visible scores | room.ts |
| Scheduler pool | 3 platform threads shared by timer, archive and flush | SchedulingConfig |

## Acceptance scenarios

| Scenario | Required outcome | Evidence route |
|---|---|---|
| Concurrent duplicate answer | One receipt, score effect and event | Real Redis integration test |
| Deadline equality / stale round | Reject without new score | Lua smoke + real Redis test |
| SQL failure after room init | Keep live room discoverable; retry repairs registration | Unit fault injection; full integration still required |
| Initial REST outage / socket close | Reconcile without command replay | Deterministic connection tests; browser E2E still required |
| Broadcast burst / slow snapshot | Coalesce, one in-flight/client, global task bound | Hub unit tests; load test still required |
| Session/membership revoked | No new snapshots; stop reconnect | Integration and connection tests |
| DB archive outage | Retain unacknowledged events, ordered replay | Existing replay coverage; outage campaign pending |

## Measurements before any capacity claim

Record hardware, Redis memory, JDBC pool, rooms, sockets per room, simultaneous
answers, slow clients and payload sizes. Measure answer p50/p95/p99 separately from
notification freshness, Redis Lua time, rejected work, archive lag and reconnect
rate. Set thresholds with the product owner BEFORE the load run, not afterwards.

## Residual risks / non-goals

- Redis AOF everysec + async archive can lose acknowledged live work; no zero-loss RPO.
- Lua runtime failures may leave partial effects; type preflight is not rollback.
- Session, live state and backlog share one dev Redis memory budget with
  noeviction (see the limits table). Long archive outages can exhaust capacity;
  notification bounds do not fix storage growth.
- Snapshot work still scales with clients × players; coalescing mitigates bursts,
  not a proof of latency. No room-common projection cache is implemented.
- Admission checks for active room count are advisory, not distributed reservations.
- PIN registry loss/collision is not automatically repaired; operator intervention
  is required. Provisioning repair assumes the existing PIN mapping survives.
- No automated Redis data-loss recovery, poison-event quarantine or cluster support.
- No production metrics exporter, login/join rate limiter, browser E2E suite or
  accessibility audit yet.
- JDBC/Redis pool sizing, session fixation effects on active WS and cross-tab logout
  need production-specific testing.
- Draft save is not server-idempotent; ambiguous timeouts require catalog inspection.
- Java 24 is a non-LTS release pinned by the build, Dockerfile and docs; no upgrade
  decision to a long-term-support release is recorded yet.

Each unresolved item must stay visible until code and appropriate evidence exist.
See the [verification status](../development.md#gate-status).
