# Frontend architecture

Target design for the Angular SPA: standalone components, lazy routes, OnPush,
signals for owned state. This page describes the intended checked-in design; where
the current tree differs, [Known gaps](#known-gaps) says so. Screen behavior belongs
to [features](../features/README.md), messages and privacy to
[contracts](../contracts/README.md), the connection lifecycle to
[realtime design](../modules/realtime.md), coding style to
[conventions](../development/conventions.md) and every numeric value to the
[limits table](quality-and-risks.md#limits-and-timings).

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
to core or shared. Routes are the only composition point between features
([ADR 0009](../adr/0009-layered-modules-and-feature-folders.md); steps for a new
feature in [CONTRIBUTING](../../CONTRIBUTING.md#adding-a-frontend-feature)).

Policy that must be testable without a browser is a plain ES module (`*.mjs` with a
`.d.mts` declaration) inside the feature that owns it: it imports nothing from
Angular or the DOM, receives I/O as injected callbacks and is tested by `node --test`.
Angular files hold rendering and wiring only.

## Layers

| Layer | Lives in | Responsibility | Must not own |
|---|---|---|---|
| Page / template | `features/<name>/<name>.page.ts` | Render state, collect intent, bind Signal Forms | Socket or timer lifecycle, scoring |
| Route store | `features/<name>/<name>.store.ts`, provided at the route | Server snapshot, pending commands, derived signals | Credentials, transport retry |
| Policy module | `features/<name>/*.mjs` | Version guard, ranking, snapshot validation, connection lifecycle | Angular, DOM, HttpClient |
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
[Realtime design](../modules/realtime.md) owns the exact lifecycle and error
behavior; [contracts](../contracts/websocket.md) own messages and privacy.

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
| Templates and stores | Compile only, `ng build`; no browser test | npm run build |
| Static invariants | File-content tests: base href, no embedded credentials | npm test |
| Browser flows | Not covered; label it rather than claim it | none, see [quality risks](quality-and-risks.md) |

Tests live under `frontend/tests/` mirroring `features/`; a test name carries the
acceptance ID it covers ([testing](../development/testing.md)). Signals usage is an
implementation choice, not evidence of correctness.

## Known gaps

The current tree predates this design. A change that closes an item also removes
it from this table.

| Gap | Current state | Target |
|---|---|---|
| Folders | Everything flat in `src/app` | core / shared / features |
| core split | `api.ts` holds the HttpClient wrapper, `Auth` and message mapping | core/http, core/auth, core/errors |
| Models | One `models.ts` for every feature | shared/models, one file per contract |
| Tests | Flat `tests/*.test.mjs` and a flat gate glob | `tests/<feature>/` and a recursive glob |
| Boundary enforcement | No check for feature-to-feature imports | A lint rule or a check in `scripts/check-docs.mjs` |
