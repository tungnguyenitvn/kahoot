# Room WebSocket contract

Path /ws/rooms/{roomId}. Browser-facing ws locally, wss behind future TLS deployment.
Origin must be allowed; an existing session and owner/active membership are required.
The security filter permits the path only so the handshake can perform these checks.

## Client messages

~~~json
{"type":"SYNC"}
{"type":"PING"}
~~~

SYNC requests the latest full snapshot; requests may coalesce. Optional
lastSeenVersion is currently ignored: there is no replay/delta protocol.
PING returns PONG subject to per-connection control-reply throttling.
Unsupported message type returns ERROR READ_ONLY_CHANNEL, never mutation.
Malformed JSON returns INVALID_MESSAGE; oversized input (>1024 text characters)
closes with 1009. This is an application guard, not a complete ingress flood limiter.

## Close codes

| Code | Meaning | Client behavior |
|---|---|---|
| 1001 GOING_AWAY | Server shutting down | Reconnect with backoff |
| 1003 NOT_ACCEPTABLE | A send to this client failed, so the server dropped it | Reconnect with backoff |
| 1008 POLICY_VIOLATION | Sent right after a REVOKED message | Stop; a new route/session flow is required |
| 1009 TOO_BIG | Inbound text exceeded the limit above | Reconnect with backoff and fix the sender |
| 1012 SERVICE_RESTARTED | Registration refused: per-process or per-room capacity, or the hub is stopping | No retry timer; the next REST reconciliation reopens the socket (ROOM-07) |

Handshake rejections happen before the upgrade and use HTTP status codes, not close codes.

## Server messages

~~~json
{"type":"STATE","room":{"id":"example","version":43}}
{"type":"PONG"}
{"type":"ERROR","code":"READ_ONLY_CHANNEL","retryable":false}
{"type":"REVOKED","code":"ROOM_ACCESS_DENIED"}
~~~

STATE example is abbreviated; room must be a complete
[RoomSnapshot](room-state.md). ERROR codes include INVALID_MESSAGE,
READ_ONLY_CHANNEL and retryable REALTIME_UNAVAILABLE/storage/domain failures.
REVOKED uses code, not reason: SESSION_EXPIRED, ROOM_ACCESS_DENIED or ROOM_NOT_FOUND.

Delivery is best effort, coalesced, potentially duplicated/out-of-order. Do not
treat version gaps as missing mutations: snapshots are complete.
No score or accepted-answer decision depends on WS delivery.

## Client obligations

Mutations never travel over this channel and a reconnecting client resends nothing.
Load a REST snapshot before opening the socket; SYNC after open is optional because
the server sends the first snapshot on registration. Apply STATE only through the
version guard in [room-state](room-state.md#merge-and-privacy). REVOKED, or a
terminal REST 401/403/404, ends the connection until a new route/session flow.

Reconnect timing, periodic REST reconciliation, server limits, session rechecks and
slow-consumer behavior belong to the [realtime module design](../modules/realtime.md)
and the [limits table](../architecture/quality-and-risks.md#limits-and-timings), not
a duplicate wire spec. Origin/identity rejection occurs before upgrade; do not expect
a WS JSON error then.
