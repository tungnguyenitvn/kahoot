# Backend architecture

Target design for one Spring Boot MVC deployment organized as a modular monolith:
modules are bounded contexts expressed as Java packages, not independently deployed
services. This page describes the intended checked-in design; where the current tree
differs, [Known gaps](#known-gaps) says so. Game rules belong to the
[domain](../domain.md), wire shapes to [contracts](../README.md#contracts),
algorithms and failure paths to the [module designs](#module-designs) below, coding style
to [conventions](../development.md#conventions) and every numeric value to the
[limits table](README.md#limits-and-timings).

## Modules

| Module | Bounded context | Public surface | Design |
|---|---|---|---|
| identity | Host accounts, guest and host session identity, HTTP security | `Identity` value; `Accounts` facade (register, look up); `Sessions` port (liveness) | [identity/catalog](#identity-and-catalog) |
| catalog | Host-owned draft and published quizzes | `QuizCatalog` facade and `Quiz` read model | [identity/catalog](#identity-and-catalog) |
| gameplay | Room provisioning, live commands, snapshots, realtime delivery | `Rooms` facade (create, join, snapshot, answer, control) | [gameplay](#gameplay), [realtime](#realtime-delivery) |
| archive | Ordered event projection and host history | `History` read facade | [archive](#archive) |
| shared | Technical policy: API error mapping, scheduling, store configuration | Exceptions and configuration | none |
| bootstrap | Composition root: demo seed at startup | none | none |

Facade names are targets; the classes that play these roles today are listed under
Known gaps. `catalog` is the reference module: the smallest one with every layer.
A new module copies its shape.

### Layers inside a module

Every module is `dev.sample.quiz.<module>` with the sub-packages below
([ADR 0009](../adr/0009-layered-modules-and-feature-folders.md)). A layer is
omitted only when the module has nothing to put in it.

```
dev.sample.quiz.<module>
├── api             HTTP and WS entry points, request/response records, shape validation, identity resolution
├── application     use cases and facades, transaction boundaries, ports (interfaces) to stores
├── domain          entities, value objects, invariants, domain errors; no Spring, no I/O
└── infrastructure  adapters that implement the ports: JDBC, Redis and Lua, WebSocket, schedulers
```

Inside a module: api → application → domain, and infrastructure → application and
domain. `domain` depends on the JDK only. `application` depends on the ports it
declares, never on an adapter. Another module may import only `application` facades
and `domain` public types; `api` and `infrastructure` are private to their module.

Gameplay exception: the room aggregate's transactional invariants execute in Lua
(`gameplay/infrastructure`, script on the classpath), not in Java. `gameplay.domain`
holds command and snapshot types plus the checks that need no store; the script is
the single writer for a room ([ADR 0002](../adr/0002-redis-game-state.md)).

## Dependency matrix

Directions between modules. The `allowed` map in `scripts/check-docs.mjs` enforces
this table on Java imports; the two change together. A package that is not in the map
fails the lint, so a new module cannot skip registration; the root package holds the
composition root and is not a module.

| Module | May import | Must not import |
|---|---|---|
| bootstrap | identity, catalog, shared | gameplay, archive |
| identity | shared | every other module |
| catalog | identity, shared | gameplay, archive |
| gameplay | identity, catalog, shared | archive |
| archive | identity, shared | gameplay, catalog |
| shared | nothing | every module |

Catalog never calls gameplay. Gameplay never calls archive. Archive consumes the
room event streams by their documented key format in the
[Redis room contract](../contracts/redis-room.md): it imports no gameplay code and
never issues room commands to fill a gap in the events. A new edge changes this
table and the checker map in the same commit, and needs an ADR when it changes a
boundary.

## Data ownership

One module writes a table or key family. Other modules read it only through the
owner's facade or a read model the owner exposes on purpose.

| Store | Data | Owner (writes) | Readers |
|---|---|---|---|
| PostgreSQL | app_user | identity | none outside identity |
| PostgreSQL | quiz | catalog | gameplay through `QuizCatalog` when a room is created |
| PostgreSQL | game_room provisioning columns: id, quiz, owner, pin, title, frozen content, provisioning phase | gameplay | archive |
| PostgreSQL | game_room projection columns: phase after provisioning, archived_version, finished_at; game_event, participant, answer | archive | none outside archive |
| Redis | HTTP session namespace | identity through Spring Session | gameplay through the `Sessions` port |
| Redis | room key family, PIN reservations, active-room set | gameplay through Lua | archive reads event streams and ACKs its own consumer group |

game_room is the one shared table: two column groups, one owner each, split by
lifecycle. This is accepted coupling, not permission for cross-module SQL.
Migrations are versioned Flyway files under `backend/src/main/resources/db/migration`,
one file per change, named after the owning module.

Consistency stance: PostgreSQL transactions cannot roll back Redis writes and Lua
cannot roll back SQL. There is no distributed transaction; every cross-store
sequence is idempotent retry repair, designed in [gameplay](#gameplay)
and [archive](#archive).

## Decision points

| Decision | Made by | Never by |
|---|---|---|
| Request shape, content type, size | api layer | application, Lua |
| Who is calling | identity, from the HTTP session or security context | a controller reading the body |
| Authority over a quiz | catalog application | gameplay |
| Every live room transition: phase, deadline, membership, duplicate receipt, correct order, score, event version | Lua, one invocation per command | Java read-then-write, per-session locks |
| Archive projection order | archive, one transaction per event | gameplay |
| Error code and HTTP status | the deciding layer throws `ApiException(status, code)`; shared maps it once | inline response building |

Answer acceptance touches Redis only: no PostgreSQL read or write on that path.
Response receipts never depend on WebSocket delivery.

## Execution model

| Work | Execution | Bound |
|---|---|---|
| HTTP request | Boot virtual threads | Downstream capacity still finite |
| WS snapshot sending | Dedicated virtual-thread executor | Global permits and one in-flight snapshot per client |
| Room timer, archive, notification dispatch | Explicit platform scheduler, small fixed pool | Each scheduled loop runs sequentially |
| Room decision | Redis Lua | One bounded room operation; all rooms share Redis capacity |
| Archive commit | PostgreSQL transaction per event | One worker, ordered per room |

Values live in the [limits table](README.md#limits-and-timings). Virtual
threads are not a nonblocking framework and do not add Redis or JDBC capacity.

## Cross-cutting policies

| Policy | Rule | Owner |
|---|---|---|
| Security order | Session filter and CSRF run before any endpoint or WS upgrade; the WS path passes the filter only to reach the handshake's session, membership and Origin checks | identity (filter), gameplay (handshake) |
| Error model | One `ApiException` with a stable code; codes are contract values in [REST](../contracts/rest-api.md) and [WebSocket](../contracts/websocket.md); storage failure maps to one unavailable code | shared |
| Idempotency | Command IDs are per identity; conflicting reuse is rejected; rules in [domain](../domain.md) | gameplay |
| Logging | No credentials, session IDs, private answers or live points in logs | every module; invariant in AGENTS |
| Configuration | Environment-driven `application.yml`; no secrets in the tree; values in the limits table | shared |
| Clock | Redis TIME decides deadlines and order; the JVM clock serves display and scheduling only | gameplay |

## Startup

`QuizApplication` is the composition root. Order: Flyway migrations, Spring
context, Lua script from the classpath, scheduled workers, demo seed.
`bootstrap.DemoSeed` seeds through the `Accounts` and `QuizCatalog` facades and owns
no data. Demo seeding is configuration-controlled and off outside local development
([deployment](README.md#deployment)).

## Extension rules

- Add a module when a new bounded context owns data nobody else writes. Add a
  layer or a use case, not a module, when the change fits an existing context.
- A new module registers in the Modules table, the dependency matrix, the checker
  map and the data ownership table in the same change; steps in
  [CONTRIBUTING](../../CONTRIBUTING.md#adding-a-backend-module).
- Record an ADR when a change adds a store, a matrix edge, a shared table or an
  external service; not for a new use case inside a module.
- Extracting a module into a service is a redesign, not a package move: the shared
  table and the Redis keyspace would have to be re-owned first.

## Verification by layer

| Layer | Proof | Gate |
|---|---|---|
| domain | Plain unit tests, no Spring context | scripts/test, unit |
| application | Unit tests with fake ports; fault injection for retry repair | scripts/test, unit |
| Lua | Smoke against a Redis double, then real Redis | lua-smoke, scripts/test, integration |
| api and infrastructure | Real HTTP, cookies, WS, Redis and PostgreSQL in isolated Compose | scripts/test, integration |
| Module boundaries | Import matrix check | check-docs |

Test sources mirror `src/main` packages: `src/test` for isolated tests,
`src/integrationTest` for real services. A test name carries the acceptance ID it
covers; the matrix is in [testing](../development.md#testing-and-evidence).

## Known gaps

The current tree predates this design. A change that closes an item also removes
it from this table.

| Gap | Current state | Target |
|---|---|---|
| Layers | Modules are flat packages; role is a class-name suffix | api / application / domain / infrastructure per module |
| archive → gameplay | archive imports `RedisRooms` for key helpers; the checker map still allows the edge | archive derives keys from the contract; the edge leaves the map in the same change |
| bootstrap → app_user | `DemoSeed` inserts the demo account with SQL | identity exposes `Accounts` |
| gameplay stores | `RoomService` runs JDBC for game_room provisioning and calls Redis directly | JDBC and Redis behind ports in `gameplay.infrastructure` |
| Command pre-validation | Name and PIN checks live in `RoomService` | `gameplay.domain` command types |
| Session liveness | `RoomWebSocketHub` reads the Spring Session repository directly | identity `Sessions` port |
| identity user lookup | `SecurityConfig` runs an inline JDBC query | `identity.infrastructure` account repository |
| archive read model | `HistoryController` queries JDBC directly | `History` facade over an archive repository |
| Layer enforcement | The checker sees only the module segment of an import | A rule for api/infrastructure privacy; ArchUnit needs task authority and an ADR |

## Module designs

How each module works inside: algorithms, invariants, failure paths and implementation
links. Read only the module the task touches. Split a section into its own page when its
responsibility splits, not when another screen is added.

### Identity and catalog

#### Identity

Identity.current resolves host principal or existing guestId from Spring Session.
Identity.ensure bootstraps a guest identity when necessary; PIN and display name
are not credentials. Guest session IDs must never appear in public room state.

SecurityConfig owns CSRF, form login/logout and HTTP filter authorization.
REST room handlers and the WS handshake perform room authorization after the
filter. Catalog/history require HOST role. Ws transport does not replace CSRF for
HTTP mutations. Logout/session invalidation is rechecked before snapshot sending.

#### Catalog

Host ownership is derived from identity, never client ownerId. QuizCatalog validates
a draft with bounded title/questions/options/duration, stores JSONB in PostgreSQL,
and publishes status. Publish may be repeated; published content has no edit API.

Creating a room freezes quiz content with generated round IDs. Catalog is not
consulted during answers. Draft POST has no idempotency key, unlike room creation.

Feature behavior: [login](../features/login.md), [studio](../features/host-studio.md).
Wire schema and validation limits: [REST](../contracts/rest-api.md).

### Gameplay

#### Invariant ownership

[Domain rules](../domain.md) are canonical. RedisRooms invokes room.lua with
the eight same-room keys listed in the [Redis contract](../contracts/redis-room.md).
Lua owns membership, phase, deadline, accepted answers, score and event version.

Correct order is the acceptance order at Redis (LIVE-02 in the domain rules).
Snapshot filtering happens before data leaves Redis. The visible-score ZSET advances
on reveal/expiry; browser ranks those visible scores.

#### Provisioning across two stores

Room ID is deterministic for host identity + create commandId. SQL insert uses
ON CONFLICT and validates quizId on replay. PIN reservation remains outside SQL.

| Step / failure | Remaining state | Retry behavior |
|---|---|---|
| SQL insert fails | Possible PIN reservation | Existing compensating delete |
| SQL row committed, init not completed | PROVISIONING row | Retry idempotent init |
| Redis init succeeds, registration fails | Live meta, SQL may still be PROVISIONING | Retry registers the existing live room |
| Active registration succeeds, SQL update fails | Live room remains visible to timer/archive | Do NOT remove active membership; retry repairs SQL |
| Meta absent after established phase | SQL history only | Fail ROOM_STATE_LOST; no automatic reconstruction |
| Room already FINISHED | Archived/retained state | Do not resurrect active registration after cleanup |

Active registration occurs AFTER successful init. A short standalone Redis script
checks FINISHED and registers atomically, so retry cannot race terminal cleanup
and re-add a finished room. This is retry repair, not an
autonomous saga or distributed transaction. An abandoned create before registration
requires client retry/operator action. PIN loss remains a known limitation.

#### Atomicity and storage errors

One Lua invocation is the atomic boundary for a room: authority, phase, deadline,
duplicate receipt, correct order and the ZSET score are decided in one execution with
no interleaving. Atomic is not rollback: a runtime error after a write leaves earlier
writes in place, so the script stays short and validates key types before any write.
Redis runs with noeviction for a separate reason: under memory pressure it must reject
writes rather than silently evict room or session keys. A Lua runtime error and a
rejected write both reach the caller as a storage error, never as a domain result.
No PostgreSQL call happens on the answer path; SQL transactions belong to
[archive](#archive) and catalog only.

#### Answer and retry

Membership check → existing receipt lookup → phase/round/deadline/option check →
correct-order/points → answer receipt + ZSET increment + Stream append.

Answer deduplication is room/round/principal, NOT commandId. Same option returns
the original receipt even after a round transition; conflicting option fails.
Host controls have a separate command ledger and fingerprint. See the
[idempotency table](../contracts/rest-api.md#idempotency).

#### Answer sequence

~~~mermaid
sequenceDiagram
  participant B as Browser (RoomStore)
  participant C as RoomController
  participant S as RoomService
  participant R as Redis (room.lua)
  participant H as RoomWebSocketHub
  B->>C: POST /api/rooms/{id}/answers {roundId, option, commandId}
  C->>S: answer(identity, roundId, option, commandId)
  S->>R: EVAL answer: membership, existing receipt, phase/round/deadline, order and points, HSET + ZINCRBY + XADD
  R-->>S: public receipt, or {status, code}
  S->>H: broadcast(roomId) marks connections dirty, nothing awaited
  S-->>B: 200 AnswerReceipt, or the error envelope
  H-->>B: STATE snapshot later, coalesced and best effort
~~~

The receipt never waits for archive or notification work; the browser refreshes its
snapshot after the receipt and applies STATE through the version guard.

#### Notification and timer

A successful command marks clients dirty; no DB archive or WS send is awaited.
Repeated receipts may still request notification, but notification work is coalesced.
Timer runs after a fixed delay plus work duration (value in the
[limits table](README.md#limits-and-timings)); it is not an exact clock.
Answers compare Redis TIME directly against deadline.

Tests: Lua smoke, real Redis/HTTP integration, provisioning fault-injection unit tests.

### Archive

One scheduled platform-thread worker runs on a fixed delay AFTER the previous pass
(value in the [limits table](README.md#limits-and-timings)).
Each active room has its own Stream and postgres consumer group; the stable
consumer name is single-instance. This is not a global one-second persistence SLA.

#### Processing and failure order

1. Drain this consumer's pending entries before reading new entries for that room.
2. Lock game_room in a PostgreSQL transaction.
3. Ignore version <= archived_version; require exactly the next version otherwise.
4. Write event and participant/answer/phase projection, then commit.
5. XACK only after commit. Crash before ACK replays a no-op SQL effect.
6. On FINISHED, one Redis script ACKs, starts the retention TTL and removes active entry.

This gives idempotent database effects under at-least-once delivery, not universal
exactly-once execution. The terminal event can only commit after preceding versions.
Steps 2 to 4 are one PostgreSQL transaction in ArchiveTransactions.apply; step 6 is one
Lua script. HTTP responses and WebSocket sends are outside every transaction.

#### Boundaries and limitations

Archive never recomputes score from UI input and cannot invent lost Redis events.
History queries only expose host-owned rooms and completed results.
A sequence gap blocks that room; errors are logged and later retried.
No poison-event quarantine or multi-consumer claiming is implemented.

Retention starts after final SQL commit, not merely after UI FINISHED. A long DB
outage retains backlog and consumes Redis memory. Cleanup combines global ACTIVE
with room keys, so this path is Redis-standalone only.

Schema: [Redis events](../contracts/redis-room.md);
SQL: [migration](../../backend/src/main/resources/db/migration/V1__schema.sql).

### Realtime delivery

REST mutates; WS delivers full privacy-filtered snapshots. The
[WebSocket contract](../contracts/websocket.md) is the only message schema source.

#### Server dispatch

Commands mark matching connections dirty instead of allocating a task per event.
A scheduler dispatches dirty/periodic checks with:
- one in-flight snapshot per connection;
- a global cap on in-flight snapshot tasks;
- a minimum interval between snapshots per connection;
- periodic permission/session reconciliation for idle connections;
- atomic registration caps per process and per room.

Values: [limits table](README.md#limits-and-timings).

Intermediate dirty states coalesce into the newest snapshot. Limits are protective
settings, not measured capacity or guaranteed deadlines. A send failure disconnects
the client; HTTP receipts remain valid. The executor uses virtual threads but a
permit is acquired BEFORE task submission, preventing an unbounded task queue.

Each snapshot still traverses players and is principal-specific. No common snapshot
cache or cross-instance fan-out exists. PING/protocol errors are rate-limited per
connection separately; SYNC only marks dirty. Oversized inbound text is closed.
