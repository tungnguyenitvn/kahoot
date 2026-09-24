# Feature: join room

## Goal

A guest enters a live room with a six-digit PIN and a nickname, without an account. A
browser guest identity is kept in the session and bound to one participant in the room.

## Entry point

- Route: `/`.
- The form has the fields `PIN phòng` and `Tên hiển thị`.
- Success redirects to `/room/{roomId}`.

## Main flow

1. Validate that the PIN has the right format and the nickname is not empty; the length
   bounds are in the [REST contract](../contracts/rest-api.md#room-commands).
2. Call `GET /api/auth/csrf` and `GET /api/auth/me` to create or restore the guest session.
3. Call `POST /api/rooms/join` with `pin` and `name`.
4. Receive the room snapshot, then start the Live room feature.

## UI state and error mapping

| State/error | Behavior |
|---|---|
| Invalid form | Submit locked; no automatic focus on the failing field yet |
| Submitting | Submit locked, `Đang tham gia…` shown |
| `ROOM_NOT_FOUND` | The PIN does not exist or has expired |
| `NAME_TAKEN` | Ask for another nickname |
| `JOIN_CLOSED` | The room has started or expired |
| `ROOM_FULL` | The room reached its participant limit, kicked participants included |
| Network error | Keep the input, allow a retry in the same session |

## Rules

- The PIN is a locator, not a credential.
- A retry never creates a new guest identity while the current session still exists.
- No question, answer or leaderboard is shown before the join succeeds.
- The backend re-checks snapshot read access and WebSocket membership; the frontend
  never treats knowing the PIN as authorization.

## Acceptance criteria

- JOIN-01: A PIN in the wrong format never calls the API.
- JOIN-02: A duplicate nickname is rejected atomically in Redis.
- JOIN-03: A refresh after joining keeps the participant identity and that participant's
  receipt.
- JOIN-04: A guest cannot perform a host action after entering the room.

## Contracts

See the [REST room contract](../contracts/rest-api.md#room-commands), the
[Redis room contract](../contracts/redis-room.md) and [Live room](live-room.md).
