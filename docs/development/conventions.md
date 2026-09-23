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
Use Java/Redis clock only as documented; browser timestamps never decide score.

## Frontend

Standalone OnPush pages; signals for owned state, computed for derived data,
linkedSignal for per-round selection; Signal Forms for local validation.
RoomStore owns gameplay commands; RoomConnection owns transport. Templates render,
not manage socket/timer lifecycle. Derived competition rank may be calculated from
server-visible scores; score allocation remains server-owned.
Runtime network JSON is untrusted even when TypeScript has an interface.
Dispose subscriptions/timers and ignore late responses after navigation/revocation.

## Changes and evidence

Keep schemas in [contracts](../contracts/README.md), behavior in
[features](../features/README.md). Follow [workflow](workflow.md).
Breaking changes require compatibility/migration notes; useful architecture changes
require an ADR. Test coverage claims belong in [testing](testing.md) and scoped
verification records.
