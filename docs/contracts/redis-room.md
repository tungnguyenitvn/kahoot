# Redis room and event contract

Private protocol between RedisRooms, room.lua and ArchiveWorker. Do not expose
these raw values to browsers. Deployment supported: Redis standalone only.

## Keyspace

Let prefix be game:{<room UUID>}: (literal braces form the room hash tag).

| Suffix | Type | Content |
|---|---|---|
| meta | string JSON | Frozen questions, owner, phase/index, version, deadlines, correctCount |
| members | hash | principal → {id,name,principal,active} |
| names | hash | normalized name → principal |
| answers | hash | roundId:principal → private answer receipt |
| scores | ZSET | participant → live score |
| visible | ZSET | participant → last published score |
| commands | hash | principal:commandId → op:roundId:participantId fingerprint |
| events | stream | event field containing JSON event envelope |

Global quiz:active-rooms is a set; quiz:pin:<six digits> maps PIN to room (TTL in the
[limits table](../architecture/quality-and-risks.md#limits-and-timings), no literal braces
around the PIN). HTTP sessions use quiz:http-session namespace.
Registration/terminal cleanup combine room keys and the global active key; unlike
room.lua, those operations are NOT cluster-slot compatible.

Room keys expire after the retention TTL in the limits table, which starts at final
archive commit. Active rooms have logical expiresAt; timer emits FINISHED, archive then
initiates actual key TTL.

## Command interface

KEYS in table order. ARGV: operation, authenticated principal, JSON input.
Operations: init/join/snapshot/answer/start/reveal/next/kick/tick.
Result: {ok:true,result:...} or {ok:false,status,code}.
Adapter strips ok and maps failures to REST errors.

One invocation performs validation and state effects without interleaving, not
rollback. Type validation precedes writes. The same Redis server also serves other
rooms/session data, so bounded scripts share a global execution budget.

## Private answer receipt

{answerId,roundId,participantId,option,acceptedAt,correctOrder,points,commandId}.
Wrong answer correctOrder=0, points=0. Public subset is defined in
[room-state](room-state.md). CommandId is metadata for answers; only host controls
use the commands hash. See [REST idempotency](rest-api.md#idempotency).

## Event envelope

Stream field event stores {roomId,version,type,at,data}. version is room-monotonic;
at is Redis epoch milliseconds. Duplicate accepted commands do not append events.

| type | data |
|---|---|
| CREATED | {quizId,owner} |
| JOINED | Membership object |
| OPENED | {roundId,deadline} |
| ANSWERED | Private answer receipt |
| REVEALED | {roundId} |
| KICKED | Membership object with active=false |
| FINISHED | {reason:COMPLETED or EXPIRED} |

Archive consumer group postgres, consumer single-instance; commit ordered version
before ACK. Replay <= archived_version is a no-op. A version gap is an error.
No event schemaVersion or rolling incompatible upgrade protocol exists; stop active
games before incompatible changes and design a migration explicitly.
