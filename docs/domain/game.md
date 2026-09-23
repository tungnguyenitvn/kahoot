# Domain rules

User is a persisted host account. GuestIdentity is a random identity in a browser session. Participant belongs to one game and is distinct from account identity. Names are trimmed and case-insensitive unique per room. A session may recover its existing participant before start or during play; nickname cannot reclaim another participant.

A published Quiz is immutable. A GameRoom records the selected quiz content, owner identity and a random PIN. GameRound IDs are generated on room creation. Maximum 100 participants per room, counting removed participants because a removed participant keeps its slot; 20 questions, 4 choices each; time limit 5–120 seconds. A room expires 2 hours after creation; Redis keys and receipts are retained through archiving plus the retention window in the [limits table](../architecture/quality-and-risks.md#limits-and-timings).

The Lua command processor serializes room mutations. Duplicate answer means the same participant in the same round. The original choice and receipt are retained. A duplicate does not increment the rank, points, room version or event log. Host command idempotency is per identity + commandId; conflicting reuse is rejected.

The clock is Redis TIME. Deadline rule is now < deadline. Correct rank counts accepted correct answers only. Rank 1..5 earns 1000/900/800/700/600, subsequent ranks earn 500. Ranking shows shared positions for equal total points. The authoritative live score changes on acceptance; the visible score changes only when a round is revealed or the room expires. Host and player are both subject to this projection rule.

## State transitions

Phase is LOBBY, QUESTION, REVEAL or FINISHED. Every accepted transition appends one event; payloads are defined in the [Redis room contract](../contracts/redis-room.md#event-envelope).

| From | Trigger | To | Event |
|---|---|---|---|
| none | init | LOBBY | CREATED |
| LOBBY | join by a new guest, room not expired | LOBBY | JOINED |
| LOBBY | start by owner with at least one active participant, room not expired | QUESTION | OPENED |
| QUESTION | accepted answer before deadline and expiry | QUESTION | ANSWERED |
| QUESTION | reveal by owner for the current round, or timer at deadline | REVEAL | REVEALED |
| REVEAL | next by owner for the current round, questions remain | QUESTION | OPENED |
| REVEAL | next by owner for the current round, last question | FINISHED | FINISHED, reason COMPLETED |
| LOBBY, QUESTION or REVEAL | timer after expiry | FINISHED | FINISHED, reason EXPIRED |
| LOBBY, QUESTION or REVEAL | kick by owner | unchanged | KICKED |

Expiry skips REVEAL: a room expiring during QUESTION finishes directly and publishes visible scores at that moment. A room expiring in LOBBY finishes with no question. Once FINISHED, no command changes state; a duplicate of an accepted command returns its original result without a new event.

Lua atomicity prevents interleaving but does not provide rollback after runtime errors. Key types, schema and bounds are controlled, and infrastructure/write errors stop the command path. Do not continue or invent receipts after Redis errors. AOF everysec and asynchronous PostgreSQL persistence can lose recent acknowledged work after failures; a complete recovery is not promised.
