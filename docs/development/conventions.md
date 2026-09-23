# Engineering conventions

Architecture/dependency rules: [backend](../architecture/backend.md) and
[frontend](../architecture/frontend.md). This file owns coding practices, not schemas.

## Shared

- JSON camelCase; Java types PascalCase, members camelCase, constants UPPER_SNAKE_CASE.
- Entity IDs are UUID strings; identity IDs are prefixed U:/G:, answerId is composite,
  Redis stream IDs are not UUIDs. Never assert all IDs share one format.
- Validate shape at boundary; validate authority and state at the owning module.
- Error codes are stable contract values, not text to parse from exception messages.
- No credentials/private answers in logs. No hidden implicit retry of mutations.

## Backend

Constructor injection, small request records, explicit validation. Gameplay decisions
stay in Lua; SQL transactions apply within PostgreSQL only.
Keep I/O budgets explicit. Virtual threads are not admission control.
Separate recoverable timeout from terminal domain error; retain the original
command/payload when retrying a supported idempotent operation.
Clock and ordering rules belong to the [domain](../domain/game.md) (LIVE-02).

## Frontend

Standalone OnPush pages; signals for owned state, computed for derived data,
linkedSignal for per-round selection; Signal Forms for local validation.
Templates render, not manage socket/timer lifecycle; layer ownership is in the
[frontend architecture](../architecture/frontend.md). Derived competition rank may be
calculated from server-visible scores; score allocation remains server-owned.
Runtime network JSON is untrusted even when TypeScript has an interface.
Dispose subscriptions/timers and ignore late responses after navigation/revocation.
