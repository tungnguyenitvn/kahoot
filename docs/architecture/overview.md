# System architecture

Scope: one Angular SPA, one Spring Boot application, one Redis standalone and one
PostgreSQL instance. This is a modular monolith; modules are not independent
services. [Product scope](../product/scope.md) defines actors and non-goals.

~~~mermaid
flowchart TB
  Browser["Host / player SPA"] -->|"REST + cookie"| App["Spring Boot application"]
  Browser <-->|"WebSocket snapshots"| App
  App -->|"Live commands / sessions"| Redis[("Redis standalone")]
  App -->|"Catalog / archive transactions"| PG[("PostgreSQL")]
~~~

## Ownership and consistency

| Data | Authority | Other copies |
|---|---|---|
| Host accounts, quizzes | PostgreSQL | Frozen quiz content in each room |
| HTTP session, live room, answer receipts | Redis | Browser snapshot, asynchronous archive |
| Finished history | PostgreSQL archive | Redis retained temporarily for retries |
| UI selection/connectivity | Browser route state | Never authoritative for scoring |

One Lua invocation linearizes a room command: authorization, validation,
deduplication, score and event append. It is atomic with respect to other commands,
not a rollback transaction ([gameplay design](../modules/gameplay.md#atomicity-and-storage-errors)).
Scripts on the same Redis instance share execution capacity even across rooms.

REST returns command results; WebSocket is best-effort full-state notification.
Archive consumes room Streams and commits ordered projections before ACK.
These boundaries are not a distributed transaction. Redis loss may lose
acknowledged live work not durably archived.

## Architecture navigation

- [Backend](backend.md): module boundaries, permitted dependencies, execution model.
- [Frontend](frontend.md): pages, store, transport and state lifetime.
- [Deployment](deployment.md): process/network boundaries and supported topology.
- [Quality and risks](quality-and-risks.md): acceptance measurements and limitations.
- [Module design](../modules/README.md): detailed algorithms and failure paths.
- [Contracts](../contracts/README.md): wire/data shapes; never duplicate them here.
- [ADRs](../adr/README.md): rationale and evolution.

A new screen or endpoint does not require expanding this overview unless it
changes a system boundary or cross-cutting policy.
