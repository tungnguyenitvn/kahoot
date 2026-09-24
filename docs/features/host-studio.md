# Feature: host studio

## Goal

The host manages quizzes, creates drafts, publishes quizzes and opens live rooms from
one control screen.

## Entry point

- Route: `/host`, protected by the host route guard.
- Catalog data uses `httpResource`; the authoring form uses Angular Signal Forms.
- The studio has three areas: quiz catalog, quiz authoring and recent rooms.

## Quiz catalog

- `GET /api/quizzes` returns the host's quizzes.
- A card shows the title, the number of questions and the status `DRAFT`/`PUBLISHED`.
- A draft has the action **Xuất bản**.
- A published quiz has the action **Mở phòng**.
- After publishing, the catalog reloads; opening a room successfully navigates to the room.

## Quiz authoring

- The host enters a title and builds a temporary list of questions.
- Each question has the options A–D with one correct answer and a time limit; the
  maximum number of questions, the number of options and the time range are
  [domain rules](../domain.md), the validation bounds are in the
  [REST contract](../contracts/rest-api.md#catalog-host).
- A question can be removed before saving.
- **Lưu bản nháp** calls `POST /api/quizzes` and clears the saved question list on success;
  the other editor fields keep their values.
- A published quiz is not edited in place; a room copies the immutable question content
  when it is created, which is LIVE-01 in the [domain rules](../domain.md#acceptance-invariants).

## Open room and idempotency

- **Mở phòng** calls `POST /api/rooms` with the `quizId` and a `commandId` that the studio
  keeps stable in memory for that quiz until a room opens successfully.
- If the response is lost, a retry with the same commandId must return the same room,
  never a second one. After success, the next **Mở phòng** uses a new commandId and so
  creates a new room for the same quiz.
- After a successful snapshot, navigate to `/room/{id}`.
- Draft creation has no server-side commandId yet; on an ambiguous timeout, reload the
  catalog before retrying to avoid a duplicate draft.

## UI state and failure behavior

| State | Behavior |
|---|---|
| Loading catalog/history | The catalog has a loading text; the history has no loading or empty state of its own yet |
| Form invalid | Actions locked by Signal Forms; per-field errors and text limits on the UI side are still missing |
| Mutation busy | Button locked to prevent a double click |
| `QUIZ_NOT_PUBLISHED` | No room is opened; publish first |
| Storage/network error | Keep the draft being edited, show a retryable alert |
| Unauthorized | The guard blocks the route; a session error inside the route shows an alert, no automatic redirect yet |

## Acceptance criteria

- STUDIO-01: A guest cannot render the studio; the route guard blocks it.
- STUDIO-02: A quiz without a title, a question or an option is not saved.
- STUDIO-03: Publishing affects only the current host's quiz.
- STUDIO-05: A retried open-room command never creates a duplicate room.

## Contracts

See the [REST catalog and room contract](../contracts/rest-api.md), the
[backend architecture](../architecture/backend.md) and [Live room](live-room.md).
