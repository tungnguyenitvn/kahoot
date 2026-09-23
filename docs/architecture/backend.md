# Backend architecture

## Structure and dependency rules

Single Spring Boot MVC deployment; packages express ownership, not independently
enforced service boundaries. The table mirrors the import matrix enforced by
scripts/check-docs.mjs; change both together.

| Module | Responsibility | Allowed dependencies |
|---|---|---|
| bootstrap | Startup demo seed (composition root) | identity, catalog, shared |
| identity | Accounts, session identity, HTTP security | PostgreSQL accounts, Redis session, shared |
| catalog | Host-owned draft/published quizzes | identity, PostgreSQL quiz, shared |
| gameplay | Provisioning, live commands, room projections, notification | identity, catalog, Redis, game_room provisioning, shared |
| archive | Ordered event projection and host history | identity, gameplay (RedisRooms key helpers only), PostgreSQL archive, Redis Stream, shared |

Demo bootstrap is composition-root setup, outside the identity module. It seeds
the catalog through QuizCatalog but inserts the demo account into app_user with
SQL, because identity exposes no account-creation API; the import checker does not
detect SQL table ownership.

Catalog must not call gameplay. Gameplay must not call archive transactions.
Archive reads emitted events, never calls room commands to invent missing results.
Shared contains technical policy, not gameplay rules.

Current explicit coupling: archive uses RedisRooms key helpers; gameplay writes
game_room provisioning metadata, while archive owns its subsequent projection;
bootstrap writes app_user directly. These are sample exceptions, not permission
for arbitrary cross-module SQL. Further service extraction requires redesign;
package layout alone is insufficient.

## Execution model

| Work | Execution | Bound |
|---|---|---|
| HTTP request | Boot virtual threads enabled | Downstream capacity still finite |
| WS snapshot sending | Dedicated virtual-thread executor | Global permits + one in-flight snapshot/client |
| Room timer, archive, notification dispatch | Explicit platform scheduler, small fixed pool | Each scheduled loop runs sequentially |
| Room decision | Redis Lua | One bounded room operation; all rooms share Redis capacity |
| Archive commit | PostgreSQL transaction per event | One worker, ordered per room |

Pool size, delays and caps: [limits table](quality-and-risks.md#limits-and-timings).
Virtual threads are not a nonblocking framework and do not increase Redis or JDBC
capacity. The custom scheduler remains platform-thread based. Actual boot/build
verification is recorded separately, not inferred from configuration.

## Boundaries

Controllers validate request shape and resolve identity. Application services
coordinate repositories/adapters; Lua owns live invariants. No PostgreSQL call is
allowed in answer acceptance. Response receipts do not depend on WS delivery.

Session security runs before endpoint/upgrade handling. The WS HTTP path is allowed
through the filter only to reach the handshake's session/membership checks; this
does not grant anonymous room access. Origin restrictions apply separately.

PostgreSQL transactions cannot roll back Redis writes. Lua cannot roll back SQL.
Detailed provisioning repair and archive ACK order belong to module design.

## Detail, not more pages here

[Identity/catalog](../modules/identity-catalog.md) ·
[Gameplay](../modules/gameplay.md) · [Archive](../modules/archive.md) ·
[Realtime delivery](../modules/realtime.md) · [Contracts](../contracts/README.md)
