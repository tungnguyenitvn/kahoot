# Browser check of the published stack

The release checklist's last step as a script. It boots `compose.release.yaml` from the
artifacts the verify gate built, drives a host and a guest through one full round in
Chromium (deep link through nginx, live WebSocket badge, join by PIN, start, answer,
reveal, leaderboard, finish, archived history) and tears the stack down.

```bash
./scripts/verify        # produces backend/build/libs/quiz-room.jar and frontend/dist
./e2e/run               # first run installs @playwright/test and Chromium under e2e/
```

`BUILD_ARTIFACTS=1 ./e2e/run` builds the artifacts first without tests; `E2E_KEEP=1`
leaves the stack up on http://127.0.0.1:8081 for a look; `E2E_BASE_URL` points
`npm test` at any other running stack, for example the images pulled from GHCR.

By decision this is not part of `./scripts/verify` or CI. Record each run in the
[gate status](../docs/development.md#gate-status) row "Published stack in a browser"
with the revision and the images used.
