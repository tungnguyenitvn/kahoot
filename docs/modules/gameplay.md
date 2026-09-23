# Gameplay design

## Invariant ownership

[Domain rules](../domain/game.md) are canonical. RedisRooms invokes room.lua with
the eight same-room keys listed in the [Redis contract](../contracts/redis-room.md).
Lua owns membership, phase, deadline, accepted answers, score and event version.

Correct order is order of accepted correct commands at Redis, not browser click
time or HTTP arrival time. Snapshot filtering happens before data leaves Redis.
The visible-score ZSET advances on reveal/expiry; browser ranks those visible scores.

## Provisioning across two stores

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

## Atomicity and storage errors

One Lua invocation is the atomic boundary for a room: authority, phase, deadline,
duplicate receipt, correct order and the ZSET score are decided in one execution with
no interleaving. Atomic is not rollback: a runtime error after a write leaves earlier
writes in place, so the script stays short and validates key types before any write.
Redis runs with noeviction for a separate reason: under memory pressure it must reject
writes rather than silently evict room or session keys. A Lua runtime error and a
rejected write both reach the caller as a storage error, never as a domain result.
No PostgreSQL call happens on the answer path; SQL transactions belong to
[archive](archive.md) and catalog only.

## Answer and retry

Membership check → existing receipt lookup → phase/round/deadline/option check →
correct-order/points → answer receipt + ZSET increment + Stream append.

Answer deduplication is room/round/principal, NOT commandId. Same option returns
the original receipt even after a round transition; conflicting option fails.
Host controls have a separate command ledger and fingerprint. See the
[idempotency table](../contracts/rest-api.md#idempotency).

## Notification and timer

A successful command marks clients dirty; no DB archive or WS send is awaited.
Repeated receipts may still request notification, but notification work is coalesced.
Timer runs after a fixed delay plus work duration (value in the
[limits table](../architecture/quality-and-risks.md#limits-and-timings)); it is not an exact clock.
Answers compare Redis TIME directly against deadline.

Tests: Lua smoke, real Redis/HTTP integration, provisioning fault-injection unit tests.
