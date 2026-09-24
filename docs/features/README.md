# Feature documentation

Features are organized by user journey and capability instead of one document that
collects every screen. Each feature has its main route or screen, its states, actions,
errors and its own acceptance criteria.

| Feature | Route / screen | Content |
|---|---|---|
| [Login](login.md) | `/login` | Host authentication, session and logout |
| [Join room](join-room.md) | `/` | A guest enters a room by PIN and nickname |
| [Host studio](host-studio.md) | `/host` | Author a quiz, publish it and open a room |
| [Live room](live-room.md) | `/room/:id` | Lobby, question, submit answer, reveal, ranking and realtime |
| [Room history](room-history.md) | `/host` (panel) | The list of rooms and archived results |

Cross-feature invariants (scoring, concurrency, privacy, archive, fail-closed) carry
`LIVE-xx` IDs and live in the [domain rules](../domain.md#acceptance-invariants); feature
documents reference them and never restate them.

Feature documents answer "what is the user trying to achieve and how must the system
respond"; architecture and contracts answer "through which boundary the system does
it". The two layers are updated together whenever a screen is added or changed.

Every acceptance criterion has a stable ID with the feature's prefix (LOGIN, JOIN,
STUDIO, ROOM, ANSWER, HIST) or LIVE for a domain invariant. One behavior has one ID; IDs
are never renumbered. Tests, issues and pull requests reference the ID instead of
paraphrasing it; the ID → test matrix is in [testing](../development.md#testing-and-evidence).
The [language policy](../README.md#language-policy) applies: the documents are English,
UI strings, error codes and identifiers are quoted verbatim, and a number links to its
owning document.

## Retired IDs

The IDs below were folded into domain invariants; they are never redefined and their
numbers are never reused.

| Old ID | Replaced by |
|---|---|
| ROOM-01 | LIVE-03 |
| ROOM-02 | LIVE-03 |
| ROOM-05 | LIVE-04 |
| ROOM-06 | LIVE-07 |
| ANSWER-02 | LIVE-03 |
| ANSWER-03 | LIVE-02 |
| ANSWER-05 | LIVE-04 |
| HIST-03 | LIVE-06 |
| STUDIO-04 | LIVE-01 |
