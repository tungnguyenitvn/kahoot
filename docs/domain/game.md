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

## Acceptance invariants

Cross-feature invariants with stable IDs. Feature documents and tests reference these IDs instead of restating the rule; duplicates that were folded into them are listed under [retired IDs](../features/README.md#retired-ids).

- LIVE-01: An authenticated owner opens a room from a frozen published quiz; a guest session joins atomically by PIN and nickname. The PIN never grants read or host authorization.
- LIVE-02: Room phases change only along the [state transitions](#state-transitions); expiry can finish a room from any phase that is not FINISHED and skips REVEAL. Deadline and correct-answer order are server decisions, never browser time.
- LIVE-03: Accepted answer effects occur once per participant and round: resubmitting the same option returns the original acceptance, a different option is rejected without a new score or event, concurrent correct answers receive distinct orders, and wrong answers consume only the attempt.
- LIVE-04: While a question is open, no response exposes the correct option, unrevealed points or rank; acceptance itself does not disclose correctness or points before reveal.
- LIVE-05: REST mutates; WebSocket and polling reconcile complete privacy-filtered state. Losing notifications never alters an accepted result.
- LIVE-06: The archive applies events in order with SQL deduplication before ACK; replay or worker retry never duplicates a participant, answer or score in PostgreSQL.
- LIVE-07: Lost or corrupt Redis state fails closed; scoring never falls back to incomplete history or stale SQL projections.
