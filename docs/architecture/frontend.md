# Frontend architecture

Target design for the Angular SPA: standalone components, lazy routes, OnPush,
signals for owned state. This page describes the intended checked-in design; where
the current tree differs, [Known gaps](#known-gaps) says so. Screen behavior belongs
to [features](../features/README.md), messages and privacy to
[contracts](../README.md#contracts), the connection lifecycle to the
[room connection lifecycle](#room-connection-lifecycle) below, coding style to
[conventions](../development.md#conventions) and every numeric value to the
[limits table](README.md#limits-and-timings).

## Folders

```
src/app
├── app.ts, app.config.ts, app.routes.ts   shell, providers, lazy routes and guards
├── core/        app-wide singletons: auth, http client, error-code mapping, guards, interceptors
├── shared/      wire models, presentational components, pure helpers; no I/O, no core
└── features/
    ├── entry/   join by PIN and nickname
    ├── login/   host authentication
    ├── studio/  quiz authoring, publish, open room, history panel
    └── room/    live room: page, route-scoped store, framework-free policy modules
```

Dependency direction: features → core and shared; core → shared; shared → nothing
in the application. Features never import each other; what two features need moves
to core or shared. `scripts/check-docs.mjs` enforces these directions on relative
imports (rule `frontend`). Routes are the only composition point between features
([ADR 0009](../adr/0009-layered-modules-and-feature-folders.md); steps for a new
feature in [CONTRIBUTING](../../CONTRIBUTING.md#adding-a-frontend-feature)).

Policy that must be testable without a browser is a plain ES module (`*.mjs` with a
`.d.mts` declaration) inside the feature that owns it: it imports nothing from
Angular or the DOM (the same rule checks that it imports only other `.mjs` modules), receives I/O as injected callbacks and is tested by `node --test`.
Angular files hold rendering and wiring only.

## Layers

| Layer | Lives in | Responsibility | Must not own |
|---|---|---|---|
| Page / template | `features/<name>/<name>.page.ts` | Render state, collect intent, bind Signal Forms | Socket or timer lifecycle, scoring |
| Route store | `features/<name>/<name>.store.ts`, provided at the route | Server snapshot, pending commands, derived signals | Credentials, transport retry |
| Policy module | `features/<name>/*.mjs` | Version guard, ranking, snapshot validation, connection lifecycle, answer pending and retry | Angular, DOM, HttpClient |
| core/auth | `core/` | Session bootstrap, CSRF refresh, identity signal | Room or catalog state |
| core/http | `core/` | HttpClient wrapper, error code to message mapping | Retry of mutations |
| shared/models | `shared/` | Wire types mirrored from contracts | Behavior; runtime validation belongs to a policy module |

Studio reads catalog and history with `httpResource`; there is no global application
store. RxJS bridges HttpClient only; it is not a second state store.

## State lifetime

| State | Owner / lifetime |
|---|---|
| Identity | core/auth; application lifetime, the server session stays authoritative |
| Catalog/history resources and authoring form | studio route |
| Room snapshot, connection, pending commands | room route instance |
| Selected option | linkedSignal keyed by roundId |
| Visible ranking | computed from server-visible scores; competition ranks, stable IDs |
| Clock estimate | client offset from snapshot serverTime; display only |

Full reload loses pending command memory; accepted answers come back in the server
snapshot, which is not durable client retry. Route parameter changes reopen the
connection and clear the previous room. Disposal cancels timers and ignores late
HTTP and socket callbacks.

## Synchronization

REST room/control responses and WS STATE carry full snapshots and share one
same-room/version guard. Answer POST returns a receipt, then the store refreshes.
Version gaps are valid for full snapshots; no event replay exists.

Initial transient failure keeps reconciliation active. Access failure is terminal.
The [room connection lifecycle](#room-connection-lifecycle) below owns the exact
lifecycle and error behavior; [contracts](../contracts/websocket.md) own messages and privacy.

## Room connection lifecycle

RoomConnection owns timers/socket, with injected callbacks and no Angular dependency.
RoomStore owns signals and commands.

- Start: begin REST reconciliation even if the first request fails transiently.
- First valid snapshot: open socket. WS open sends SYNC.
- Close: reconnect with bounded exponential delay and jitter.
- Close with code 1012 (registration refused for capacity): no retry timer; the next
  REST reconciliation reopens the socket (ROOM-07). Close codes are listed in the
  [WebSocket contract](../contracts/websocket.md#close-codes).
- Poll: full REST snapshot on a fixed interval even when WS is healthy.
- 401/403/404 or REVOKED: stop socket/retry/poll; ignore late completions.
- Dispose/route change: detach handlers, cancel timers, reject old-room updates.
- Malformed frame: report protocol error and request REST reconciliation.
- Full snapshots may skip versions; only older/sibling-room snapshots are rejected.

Connection retries never submit answer/control. Pending mutation IDs belong to
RoomStore memory only; accepted receipts can be recovered from Redis after reload.

## Trust boundary

Runtime JSON is untrusted even when a TypeScript interface exists: a snapshot is
applied only after a policy module validates shape, room and version. Derived
competition rank may be computed from server-visible scores; score allocation stays
server-owned. The session cookie is the only credential and the browser owns it;
nothing credential-like goes to localStorage.

## Verification by layer

| Layer | Proof | Gate |
|---|---|---|
| Policy modules | `node --test`, deterministic through injected callbacks and fake timers | npm test |
| Templates, guards and stores | Component specs beside the page (`*.spec.ts`, Vitest + jsdom): route guards through the real router, Signal Forms, template clicks against a fake store; then `ng build` | npm run test:ui, npm run build |
| Static invariants | File-content tests: base href, no embedded credentials | npm test |
| Browser flows | Not covered by a gate: nginx, WebSocket and a real browser are checked by hand on the published stack | none, see [quality risks](README.md#residual-risks--non-goals) |

Policy tests live under `frontend/tests/` mirroring `features/`; component specs sit
beside the page they render; a test name carries the
acceptance ID it covers ([testing](../development.md#testing-and-evidence)). Signals usage is an
implementation choice, not evidence of correctness.

## Known gaps

None open: the tree has the folders above, the tests mirror `features/`, and
`scripts/check-docs.mjs` enforces the folder boundaries (rule `frontend`). A change that
reopens a gap adds a row here with the current state and the target.
