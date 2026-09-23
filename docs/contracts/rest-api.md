# REST API contract

Base /api. Browser sends its session cookie. Mutations require X-XSRF-TOKEN from
the XSRF-TOKEN cookie obtained at /auth/csrf; refresh CSRF after login/logout.
JSON request/response except URL-encoded form login and empty 204 logout response.
Current sample has no versioned public API compatibility guarantee.

## Identity and security

| Method/path | Request | Success |
|---|---|---|
| GET /auth/csrf | None | 200 {token}; CSRF cookie |
| GET /auth/me | None; creates guest identity if needed | 200 {id,name,host} |
| POST /auth/login | Form username/password | 200 {ok:true}; session |
| POST /auth/logout | CSRF protected | 204 |

Identity id is U:<account UUID> or G:<guest UUID>, not a bare UUID. Password and
session ID never appear in identity JSON. Login failure is 401 INVALID_CREDENTIALS.
Security filter errors: 401 SESSION_REQUIRED; 403 CSRF_REQUIRED or ACCESS_DENIED.

## Catalog (host)

| Method/path | Body | Success |
|---|---|---|
| GET /quizzes | None | 200 Quiz[] owned by current host |
| POST /quizzes | {title,questions} | 200 Quiz with status DRAFT |
| POST /quizzes/{id}/publish | Empty object | 200 Quiz with status PUBLISHED |

Quiz = {id,title,status,questions}. Each question has text, options[4],
correctOption (0..3), seconds (5..120). Limits: title 1..120 characters;
1..20 questions; question text 1..300; each option 1..120.
POST draft has no server idempotency key; inspect catalog after ambiguous timeout.
No edit/delete published quiz API exists.

## Room commands

| Method/path | Body | Authority / success |
|---|---|---|
| POST /rooms | {quizId,commandId} | Host; 200 RoomSnapshot |
| POST /rooms/join | {pin,name} | Guest session; 200 RoomSnapshot |
| GET /rooms/{id} | None | Owner/active member; 200 RoomSnapshot |
| POST /rooms/{id}/answers | {roundId,option,commandId} | Active player; 200 AnswerReceipt |
| POST /rooms/{id}/start | {commandId,roundId?} | Owner; 200 RoomSnapshot |
| POST /rooms/{id}/reveal | {commandId,roundId} | Owner; 200 RoomSnapshot |
| POST /rooms/{id}/next | {commandId,roundId} | Owner; 200 RoomSnapshot |
| POST /rooms/{id}/kick | {commandId,participantId,roundId?} | Owner; 200 RoomSnapshot |

PIN is a six-digit string; trimmed name length 1..24; option is integer 0..3.
Entity, round and command IDs are UUID strings. The command result is not a
guarantee that any socket received notification or that SQL archive committed.
Snapshot/receipt fields: [room-state](room-state.md).

## Idempotency

| Operation | Scope/key | Replay / conflicting reuse |
|---|---|---|
| Create room | Principal + commandId → deterministic room ID | Same quiz returns current snapshot; other quiz COMMAND_CONFLICT |
| Join | Room + principal | Existing active member reused; nickname is not a new identity |
| Answer | Room + roundId + principal | Same option returns original receipt; changed option ALREADY_ANSWERED |
| Host control | Room + principal + commandId | Same op/round/target accepted again without effects; different fingerprint COMMAND_CONFLICT |
| Publish | Host-owned quiz status | Repeated publish retains PUBLISHED |
| Draft create | None | May create another draft on retry |

Answer commandId is required metadata, NOT a unique deduplication constraint.
Host replay returns a CURRENT snapshot, not necessarily the original HTTP body.
Live answer/control receipts survive until room-key retention expires after final
archive; revoked members still fail authorization before receipt replay.

## History (host)

GET /history → 200 up to 50 rooms newest first:
{id,title,phase,archivedVersion,createdAt,finishedAt}[]; finishedAt is null until
the archive commits the terminal event. Timestamps are serialized by the runtime JSON
serializer, currently ISO-8601 UTC strings; no client date parser depends on a frozen
timestamp representation yet.

GET /history/{id} → 200 {id,name,score}[] ordered score descending then name.
Host ownership is required; missing/other owner's room gives ROOM_NOT_FOUND.
Unfinished archive gives 409 ARCHIVE_NOT_READY.

## Errors and retry

Application error envelope: {code:string}. Validation returns INVALID_REQUEST;
there is no per-field errors array today. Unknown failures outside these handlers
may use framework errors; clients must tolerate a missing code.

| HTTP | Codes / handling |
|---|---|
| 400 | INVALID_REQUEST, INVALID_NAME, INVALID_OPTION, UNKNOWN_COMMAND: fix input |
| 401 | SESSION_REQUIRED, INVALID_CREDENTIALS: restore/authenticate session |
| 403 | ACCESS_DENIED, CSRF_REQUIRED, LOGIN_REQUIRED, HOST_REQUIRED, ROOM_ACCESS_DENIED, MEMBERSHIP_REVOKED |
| 404 | ROOM_NOT_FOUND, QUIZ_NOT_FOUND, PLAYER_NOT_FOUND |
| 409 | JOIN_CLOSED, ROOM_FULL, NAME_TAKEN, HOST_CANNOT_JOIN, QUIZ_NOT_PUBLISHED, INVALID_PHASE, PLAYERS_REQUIRED, ROUND_CLOSED, STALE_ROUND, DEADLINE_PASSED, ALREADY_ANSWERED, COMMAND_CONFLICT, ARCHIVE_NOT_READY, COMMAND_LIMIT (host control ledger is full; retrying never clears it, open a new room; capacity in the [limits table](../architecture/quality-and-risks.md#limits-and-timings)) |
| 503 | STORAGE_UNAVAILABLE, ROOM_LIMIT, PIN_UNAVAILABLE, ROOM_STATE_LOST, STATE_CORRUPT, REDIS_NO_RESULT, REDIS_ERROR |

On ambiguous timeout, reconcile. Retry supported live commands with original input,
not a newly generated identity/command. Corrupt/lost state requires operator action;
do not endlessly retry every 503 or reconstruct scores from SQL.
