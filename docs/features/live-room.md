# Feature: live room

## Goal

Host and players take part in one realtime room. Redis Lua decides membership,
deadline, receipt, correct-answer order and score; Angular only renders the
authoritative snapshot.

## Entry point and transport

- Route: `/room/:id`, shared by host and players.
- `RoomStore` delegates to `RoomConnection`, which calls `GET /api/rooms/{id}` before it
  opens the WebSocket.
- The WebSocket `/ws/rooms/{id}` accepts only `SYNC` and `PING`; every mutation uses REST.

## Shared layout

- The header shows the title, the PIN and the realtime status.
- The main stage changes with the phase; the phases and the valid transitions are in the
  [state transitions table](../domain.md#state-transitions).
- The player list shows active/removed and answered state.
- The leaderboard shows the number of rows from the [limits table](../architecture/README.md#limits-and-timings);
  equal scores share a competition rank.
- The client never decides score, correct-answer order or deadline; that is LIVE-02 in
  the [domain rules](../domain.md#acceptance-invariants).

## Lobby

- Shows the number of players and the total number of questions.
- The host sees **Bắt đầu**, enabled only when a player is present.
- A player sees **Chờ người dẫn bắt đầu…**.
- The host kicks a player through REST; the revoked participant receives `REVOKED`.

## Question

- Shows the question number, the text, the options and a countdown based on
  `serverTime/deadline`.
- A player picks an option in temporary UI state, then sends the answer through REST.
- The answer carries `roundId`, `option` and `commandId`; the meaning of each field is
  in the [idempotency contract](../contracts/rest-api.md#idempotency), the dedupe rule is
  LIVE-03 in the [domain rules](../domain.md#acceptance-invariants). When the response
  times out, the retry resends the same option and roundId; the client keeps the
  commandId for reconciliation.
- After the receipt, the choice is locked and the accepted answer is shown.
- The host does not answer; the host sees **Chốt câu & công bố**.
- `correctOption` is `null`; no private score delta is shown before the reveal.

## Submit answer

A capability inside this screen, not a new route; the implementation exists, the
verification scope is recorded in [testing](../development.md#testing-and-evidence).

- ANSWER-01: An active player picks one option in the current QUESTION.
- ANSWER-04: While the outcome is unknown, a retry reuses the original option, round and
  commandId. A new round clears the stale pending UI; an accepted receipt stays on the
  server.
- ANSWER-06: A reconnect only refreshes the state; it never sends an answer by itself.
- ANSWER-07: A network timeout, a 5xx and 408/429 keep the pending answer; a terminal
  validation or authorization error clears it. A page reload loses unsent memory. The
  backend never returns 429 for any request; keeping the pending answer on 429 is for
  compatibility with an infrastructure rate limiter, should one exist.

Dedupe by room/round/participant and privacy before the reveal are LIVE-03 and LIVE-04
in the [domain rules](../domain.md#acceptance-invariants). [REST idempotency](../contracts/rest-api.md#idempotency)
defines identity and errors; [gameplay](../architecture/backend.md#gameplay) owns scoring,
the [frontend architecture](../architecture/frontend.md#room-connection-lifecycle) owns
recovery. A browser E2E for the store and the template is not implemented.

## Reveal and finished

- The reveal shows the correct option and the published leaderboard.
- The host sees **Câu tiếp theo** or **Kết thúc**; the controls carry `roundId` fencing and
  `commandId` idempotency.
- Finished locks answer, kick and control and shows the final result. When a room ends
  by expiry according to the [state transitions table](../domain.md#state-transitions),
  the screen goes straight to the result without a separate reveal step.
- **Vào phòng khác** returns to Join room.

## Realtime, reconnect and errors

1. The HTTP response and STATE may arrive out of order; only a snapshot of the same room
   with a version that is not older is applied, as in [room-state](../contracts/room-state.md#merge-and-privacy).
2. An initial REST error keeps polling for recovery; a socket close shows **Đang nối lại**,
   reconnects with bounded backoff and keeps polling REST periodically. The values are in
   the [limits table](../architecture/README.md#limits-and-timings). A refusal for lack of
   capacity is retried only at the next poll (ROOM-07).
3. A pending answer is kept until a receipt or a terminal error arrives.
4. `STALE_ROUND`, `DEADLINE_PASSED` and `ALREADY_ANSWERED` require a snapshot reconciliation.
5. `REVOKED`, 401, 403 and 404 stop the reconnect.

## Acceptance criteria

- ROOM-03: A stale round never affects the new round.
- ROOM-04: A reconnect keeps the pending command in memory; a reload restores the
  accepted receipt from the server but not an unsent command.
- ROOM-07: When the server closes the socket with close code 1012 for lack of capacity,
  the client sets no reconnect timer; the next REST poll reopens the socket.

Server invariants that apply to this screen: LIVE-02, LIVE-03, LIVE-04 and LIVE-07 in
the [domain rules](../domain.md#acceptance-invariants).

## Contracts

See the [WebSocket contract](../contracts/websocket.md), the [REST API](../contracts/rest-api.md),
the [Redis room](../contracts/redis-room.md) and the [domain game rules](../domain.md).
