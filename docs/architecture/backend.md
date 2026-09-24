# Backend architecture

Target design for one Spring Boot MVC deployment organized as a modular monolith:
modules are bounded contexts expressed as Java packages, not independently deployed
services. This page describes the intended checked-in design; where the current tree
differs, [Known gaps](#known-gaps) says so. Game rules belong to the
[domain](../domain/game.md), wire shapes to [contracts](../contracts/README.md),
algorithms and failure paths to [module designs](../modules/README.md), coding style
to [conventions](../development/conventions.md) and every numeric value to the
[limits table](quality-and-risks.md#limits-and-timings).

## Modules

| Module | Bounded context | Public surface | Design |
|---|---|---|---|
| identity | Host accounts, guest and host session identity, HTTP security | `Identity` value; `Accounts` facade (register, look up); `Sessions` port (liveness) | [identity/catalog](../modules/identity-catalog.md) |
| catalog | Host-owned draft and published quizzes | `QuizCatalog` facade and `Quiz` read model | [identity/catalog](../modules/identity-catalog.md) |
| gameplay | Room provisioning, live commands, snapshots, realtime delivery | `Rooms` facade (create, join, snapshot, answer, control) | [gameplay](../modules/gameplay.md), [realtime](../modules/realtime.md) |
| archive | Ordered event projection and host history | `History` read facade | [archive](../modules/archive.md) |
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
this table on Java imports; the two change together.

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
sequence is idempotent retry repair, designed in [gameplay](../modules/gameplay.md)
and [archive](../modules/archive.md).

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

Values live in the [limits table](quality-and-risks.md#limits-and-timings). Virtual
threads are not a nonblocking framework and do not add Redis or JDBC capacity.

## Cross-cutting policies

| Policy | Rule | Owner |
|---|---|---|
| Security order | Session filter and CSRF run before any endpoint or WS upgrade; the WS path passes the filter only to reach the handshake's session, membership and Origin checks | identity (filter), gameplay (handshake) |
| Error model | One `ApiException` with a stable code; codes are contract values in [REST](../contracts/rest-api.md) and [WebSocket](../contracts/websocket.md); storage failure maps to one unavailable code | shared |
| Idempotency | Command IDs are per identity; conflicting reuse is rejected; rules in [domain](../domain/game.md) | gameplay |
| Logging | No credentials, session IDs, private answers or live points in logs | every module; invariant in AGENTS |
| Configuration | Environment-driven `application.yml`; no secrets in the tree; values in the limits table | shared |
| Clock | Redis TIME decides deadlines and order; the JVM clock serves display and scheduling only | gameplay |

## Startup

`QuizApplication` is the composition root. Order: Flyway migrations, Spring
context, Lua script from the classpath, scheduled workers, demo seed.
`bootstrap.DemoSeed` seeds through the `Accounts` and `QuizCatalog` facades and owns
no data. Demo seeding is configuration-controlled and off outside local development
([deployment](deployment.md)).

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
covers; the matrix is in [testing](../development/testing.md).

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

## Detail, not more pages here

[Identity/catalog](../modules/identity-catalog.md) ·
[Gameplay](../modules/gameplay.md) · [Archive](../modules/archive.md) ·
[Realtime delivery](../modules/realtime.md) · [Contracts](../contracts/README.md)
