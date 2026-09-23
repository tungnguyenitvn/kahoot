# ADR 0001: REST and SSE with in-memory sessions

Status: Superseded by [ADR 0002](0002-redis-game-state.md).
Date: not recorded; normalized to the ADR template on 2026-09-23.

## Context

The first sample had to demonstrate a full quiz round in one Java process.

## Decision

Rooms lived in process memory; REST carried commands and SSE pushed state.

## Alternatives

Not recorded.

## Consequences

Retained as history only. ADR 0002 replaced the room state with Redis and the SSE
push channel with WebSocket; REST commands remain the mutation boundary.
