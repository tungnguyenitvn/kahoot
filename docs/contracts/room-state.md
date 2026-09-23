# Room snapshot and receipt

Canonical public shapes shared by REST and WS. These are application protocol
fields, not the private Redis schema. Unknown additive fields may be ignored.

## RoomSnapshot

| Field | Type / meaning |
|---|---|
| id, pin, title | Room UUID string, six-digit PIN string, title |
| phase | LOBBY / QUESTION / REVEAL / FINISHED |
| version | Nonnegative integer, incremented per state event |
| serverTime, deadline | Epoch milliseconds; deadline=0 outside active question |
| questionNumber, questionCount | Current 1-based index (0 in lobby), total |
| question | null before first question; otherwise QuestionView |
| players | Object keyed by participant UUID; values PlayerView |
| me | {role:HOST or PLAYER, participantId:UUID or null, answer:OwnAnswer or null} |

QuestionView = {roundId,text,options:string[4],seconds,correctOption:integer or null}.
QUESTION always has correctOption=null. Reveal/finished may disclose the current
question's answer; future questions are never sent. A room expired in lobby can
finish with question=null.

PlayerView = {id,name,active:boolean,answered:boolean,score:number}.
Score is the last visible score, not the unrevealed live ZSET value.
Removed participants remain in the displayed record; backend start requires at
least one active participant. Browser computes competition ranks from visible
scores; tied display order uses participant ID, not nickname.

OwnAnswer = {answerId,option,acceptedAt}. It describes only the snapshot's current
question. Its round is identified by question.roundId.
AnswerReceipt from POST = {answerId,roundId,option,acceptedAt}; no points/correctOrder.
answerId is roundId:participantId (composite string).

## Merge and privacy

Apply only same-room, nonolder versions. Equal versions are allowed for serverTime
refresh; version gaps are expected when notifications coalesce full snapshots.
No private principal, credential, commandId, hidden live points or correctOrder.
Client clock is approximate presentation, not authority for accepting an answer.

Server and frontend types/validation are manually maintained. These Markdown
contracts are not generated OpenAPI or a machine-enforced schema registry.
