# Quality scenarios and known risks

Limits in code are admission/safety settings, not benchmark results.
No p95/p99 latency or supported concurrent-player capacity has been measured.

## Limits and timings

Ownership precedence for numbers: game rules live in [domain](../domain/game.md),
wire-visible validation bounds in [contracts](../contracts/README.md), and every other
admission, timing, retention and presentation value here. Other documents link instead
of repeating a value; rule L4 in scripts/check-docs.mjs fails the gate when a value is
repeated elsewhere. Change a value here and in its source in the same change.

| Setting | Value | Source |
|---|---|---|
| Participants, questions, options, seconds per question, room expiry, score tiers | game rules | [domain](../domain/game.md) |
| PIN reservation TTL | 2 h from reservation | RoomService.reservePin |
| Active-room admission | 100 active rooms per Redis, advisory check | RoomService.create |
| Host command ledger | 500 entries per room, then COMMAND_LIMIT | room.lua |
| Room key retention | 24 h, starting at final archive commit | ArchiveWorker |
| HTTP session timeout | 4 h | application.yml |
| Redis command timeout | 2 s | application.yml |
| Dev Redis memory | 256 MB, noeviction, AOF everysec | compose.yaml |
| Room timer | fixed delay 250 ms after each pass | RoomTimers |
| Archive worker | fixed delay 1 s after each pass; 100 entries per read | ArchiveWorker |
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
See the [verification record](../verification/refactor-review.md).
