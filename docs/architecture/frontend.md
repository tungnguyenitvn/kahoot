# Frontend architecture

Standalone Angular SPA with lazy page routes and OnPush components. Root App owns
navigation and identity display; feature documents own screen behavior.

## Boundaries

| Layer | Responsibility | Must not own |
|---|---|---|
| Page / template | Render state, collect user intent | WebSocket lifecycle, authoritative scoring |
| Signal Forms / linkedSignal | Validation, temporary selection reset per round | Accepted answer or durable command ledger |
| RoomStore | Server snapshot, pending commands, UI derivations | HTTP session credentials |
| RoomConnection | REST reconciliation, socket retry and disposal | Angular views or gameplay mutations |
| Auth / Api | Session bootstrap, HTTP boundary | Room scoring/ranking rules |

RoomConnection is framework-independent and injected with I/O callbacks for
deterministic lifecycle tests. RoomStore remains the signal-facing facade.
Studio uses httpResource for catalog/history rather than a global application store.
RxJS bridges HttpClient requests; it is not duplicated as a second room state store.

## State lifetime

| State | Owner / lifetime |
|---|---|
| Identity | Auth; application lifetime, server session remains authoritative |
| Catalog/history resources and authoring form | Studio route |
| Room snapshot / connection / pending commands | Room route instance |
| Selected option | linkedSignal keyed by roundId |
| Visible ranking | computed from server-visible scores; competition ranks and stable IDs |
| Clock estimate | Client offset from snapshot serverTime; display only |

Full reload loses unsent/pending command memory. Already accepted answers can be
recovered from the server snapshot; do not confuse this with durable client retry.
Route parameter changes reopen the connection and clear previous room state.
Disposal cancels timers and ignores late HTTP/socket callbacks.

## Synchronization

REST room/control responses and WS STATE contain full snapshots and share a
same-room/version guard. Answer POST returns a receipt, then the store refreshes.
Version gaps are valid for full snapshots; no event replay is required.

Initial transient failure keeps reconciliation active. Access failure is terminal.
Reconnect never resubmits mutations. [Realtime design](../modules/realtime.md)
owns exact lifecycle/error behavior; [contracts](../contracts/websocket.md) own
messages and privacy.

## Verification

[Testing](../development/testing.md) distinguishes framework-independent policy/
connection tests, Angular compile checks and browser integration not yet covered.
Signals usage is an implementation choice, not evidence of correctness.
