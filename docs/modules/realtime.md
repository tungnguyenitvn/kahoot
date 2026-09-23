# Realtime delivery design

REST mutates; WS delivers full privacy-filtered snapshots. The
[WebSocket contract](../contracts/websocket.md) is the only message schema source.

## Server dispatch

Commands mark matching connections dirty instead of allocating a task per event.
A scheduler dispatches dirty/periodic checks with:
- one in-flight snapshot per connection;
- a global cap on in-flight snapshot tasks;
- a minimum interval between snapshots per connection;
- periodic permission/session reconciliation for idle connections;
- atomic registration caps per process and per room.

Values: [limits table](../architecture/quality-and-risks.md#limits-and-timings).

Intermediate dirty states coalesce into the newest snapshot. Limits are protective
settings, not measured capacity or guaranteed deadlines. A send failure disconnects
the client; HTTP receipts remain valid. The executor uses virtual threads but a
permit is acquired BEFORE task submission, preventing an unbounded task queue.

Each snapshot still traverses players and is principal-specific. No common snapshot
cache or cross-instance fan-out exists. PING/protocol errors are rate-limited per
connection separately; SYNC only marks dirty. Oversized inbound text is closed.

## Browser lifecycle

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
