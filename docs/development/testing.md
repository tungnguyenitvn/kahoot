# Testing and evidence

## Gates

| Gate | Command | What it proves / does not prove |
|---|---|---|
| Offline policy/lifecycle | node --test frontend/tests/*.test.mjs | Version/ranking + injected connection lifecycle; NOT Angular/browser behavior |
| Lua smoke | texlua scripts/test-room.lua . (any Lua 5.3/5.4 interpreter works) | Sequential invariants with Redis double; NOT Redis concurrency/durability |
| Documentation checks | node scripts/check-docs.mjs | Local links and documentation boundary rules; NOT semantic completeness |
| Backend unit | Docker scripts/test or Gradle test in configured JDK | Mockito fault injection/coalescing tests; NOT real services |
| Backend integration | scripts/test | Real HTTP/cookies/WS/Redis/SQL behavior in isolated services |
| Full gate | scripts/verify | Documentation check, backend tests/package, Angular tests/build |

Read [verification record](../verification/refactor-review.md) for actual executed
results. Never infer pass from the presence or name of a test.

## Required regression areas

Concurrency/deduplication, round fencing, deadline equality, privacy, malformed
requests, WS auth/Origin/membership, reconnect/revoke/disposal, notification bounds,
provisioning failure between stores, ordered archive replay and ACK retry.
New tests name the acceptance ID they cover (ANSWER-02, ROOM-03 ...) so evidence
traces back to feature documents; existing tests predate this rule and are mapped by
content, not by name.

Only some areas have automated coverage. Browser E2E, slow-network load, Redis
loss/OOM and full outage/recovery campaigns remain gaps; track them in
[quality risks](../architecture/quality-and-risks.md).

## CI

scripts/verify is the CI entry point. Its documentation check runs in the frontend
test image (Node available), within the frontend stage; backend and frontend gates run in Docker.
Do not substitute lightweight tests for unavailable full verification.
