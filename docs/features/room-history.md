# Feature: room history

## Goal

The host reviews the rooms they created and the results durably archived in
PostgreSQL. It is a panel inside the host studio, not a route of its own yet.

## Main flow

1. The studio calls `GET /api/history` for the host's recent rooms; the maximum count is
   in the [REST contract](../contracts/rest-api.md#history-host).
2. Each card shows the title, the phase and a link back to the room while its snapshot
   is still reachable.
3. A `FINISHED` room shows the action **Kết quả đã lưu**.
4. The action calls `GET /api/history/{id}` and renders the participants and scores from
   the archive.

## State and consistency

| State | Behavior |
|---|---|
| Loading | No placeholder or empty state of its own for the history panel yet |
| `ARCHIVE_NOT_READY` | Show the error code in an alert; allow a reload, never substitute an empty result |
| Archived | Show the score projection from PostgreSQL |
| Storage/auth error | Keep the studio layout and show a retryable alert |

The archive may lag behind the live room. The Redis live leaderboard and the PostgreSQL
history are never mixed in one response; the archive worker replays the Stream events
idempotently by room version before it ACKs.

## Acceptance criteria

- HIST-01: A host reads only their own history.
- HIST-02: A room that is not FINISHED is never presented as a complete archived result.
- HIST-04: The archived result stays readable after the Redis live keys are cleaned up.

That a replay or a worker retry never duplicates an answer or a score is invariant
LIVE-06 in the [domain rules](../domain.md#acceptance-invariants).

## Contracts

See the [REST API](../contracts/rest-api.md), the [Redis room contract](../contracts/redis-room.md)
and the [archive design](../architecture/backend.md#archive).
