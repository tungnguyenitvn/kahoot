# Contracts index

| Boundary | Canonical definition |
|---|---|
| HTTP endpoints, errors, idempotency | [REST API](rest-api.md) |
| Public snapshot/receipt used by HTTP and WS | [Room state](room-state.md) |
| WebSocket messages/delivery | [WebSocket](websocket.md) |
| Private Redis keys/Lua/events | [Redis room](redis-room.md) |

Contracts are manually maintained Markdown, not generated OpenAPI/AsyncAPI.
Implementation/tests must be reviewed alongside changes. Do not duplicate message
examples in architecture or feature documents. Timing and capacity values are
canonical in the [limits table](../architecture/quality-and-risks.md#limits-and-timings);
contracts keep only wire-visible validation bounds.

Domain truth: [game rules](../domain/game.md). Runtime implementation:
[module designs](../modules/README.md). Evidence: [verification](../verification/refactor-review.md).

## Known deviations

Contracts describe current behavior. Deviations from HTTP semantics or project
conventions are listed here so no document silently rewrites them; each needs a
code change and a contract update in the same task.

| Deviation | Current behavior | Decision |
|---|---|---|
| Creation responds 200, not 201 | POST /quizzes and POST /rooms return 200 with the created resource; RFC 9110 recommends 201 for a created resource | Accepted convention: every successful JSON response is 200, and clients must not depend on 201 |

The two former deviations (HTTP 429 for COMMAND_LIMIT, SQL column names in the history
list) were fixed in code on 2026-09-23; see the
[historical record](../verification/refactor-review.md).
