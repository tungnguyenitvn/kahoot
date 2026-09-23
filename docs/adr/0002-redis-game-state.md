# ADR 0002: Redis room state and PostgreSQL archive

Status: Accepted.
Date: not recorded (v0.2 sample); normalized to the ADR template on 2026-09-23.

## Context

[ADR 0001](0001-rest-sse-in-memory.md) kept rooms in one process's memory and pushed
state over SSE. The v0.2 sample replaces that with store-owned live state, atomic
per-room commands, a durable archive and a push channel that never decides scores.

## Decision

Use one Spring Boot deployment, PostgreSQL for accounts/immutable published quizzes/
archived events, and Redis for HTTP sessions and live rooms. A short Lua processor
performs conditional room transitions, answer idempotency, correct-answer ordering,
tier scoring, synchronous leaderboard updates and XADD. All room keys use one hash
tag. Application reads used for snapshots are also atomic.

A single scheduled archive worker uses one stable Stream consumer name. It drains its
own pending entries before new ones, commits PostgreSQL deduplication and effects
together, then XACKs. Every state mutation logs an event. Final retention starts
after the terminal event and all preceding events have committed.

WebSocket uses versioned snapshots and periodic REST reconciliation, not Redis
Pub/Sub. A handshake checks Origin, session and room membership; inbound messages are
limited to SYNC and PING. REST remains the command transport so HTTP status, retry
and idempotency stay explicit. A restart reconnects from Redis state. This avoids a
new broker for the one-instance scope.

## Alternatives

A room actor keeps domain logic in Java but introduces ownership/fencing and
persistence; PostgreSQL transactions offer stronger single-store semantics but add
storage latency/contended room locks; processing all answers through a log delays
scoring and requires a reveal barrier.

## Consequences

Spring Session may perform additional Redis I/O before a game command; the complete
HTTP request is not advertised as a single Redis round trip. Deploying multiple
archive workers/instances is outside this sample's supported configuration. AOF
everysec reduces loss but does not make acknowledgements durable across all
failures. Redis unavailable means fail closed. Lost Redis state requires operator
intervention, not automatic replay from incomplete archive data. No performance
superiority has been demonstrated; benchmark scripts and real Redis integration
tests are required before changing scale claims.

Notification bounds, virtual-thread scope and provisioning retry repair are refined
by [ADR 0004](0004-bounded-notification-and-repair.md); this decision remains the
basis for Redis/SQL ownership.
