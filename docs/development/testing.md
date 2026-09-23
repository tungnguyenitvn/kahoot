# Testing and evidence

## Gates

| Gate | Command | What it proves / does not prove |
|---|---|---|
| Offline policy/lifecycle | node --test frontend/tests/*.test.mjs | Version/ranking + injected connection lifecycle; NOT Angular/browser behavior |
| Lua smoke | scripts/test-room.lua, run by scripts/verify in a Lua 5.4 container (lua-smoke service); locally texlua or any Lua 5.3/5.4 | Sequential invariants with Redis double; NOT Redis concurrency/durability |
| Documentation checks | node scripts/check-docs.mjs | Local links and documentation boundary rules; NOT semantic completeness |
| Backend unit | Docker scripts/test or Gradle test in configured JDK | Mockito fault injection/coalescing tests; NOT real services |
| Backend integration | scripts/test | Real HTTP/cookies/WS/Redis/SQL behavior in isolated services |
| Full gate | scripts/verify | Lua smoke, backend tests/package, documentation check, Angular tests/build |

Read the [verification status](../verification/README.md) for the latest executed
result per gate. Never infer pass from the presence or name of a test.

## Traceability

Every acceptance ID from [features](../features/README.md) and
[domain invariants](../domain/game.md#acceptance-invariants) has one row. A covered row
names tests whose display name or test name carries the ID, so `grep` finds it; a row
marked NOT COVERED states why. Rule L3 keeps this table and the test sources in sync.
Scenario tests without an ID (notification bounds, provisioning repair, session revocation,
COMMAND_LIMIT capacity) map to the [acceptance scenarios](../architecture/quality-and-risks.md#acceptance-scenarios).

| ID | Test | Gate |
|---|---|---|
| LOGIN-01 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (cookie session is recognized as host after login) | scripts/test |
| LOGIN-02 | NOT COVERED: Angular route guard needs browser E2E | none |
| LOGIN-03 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (logout returns 204, next host request 401) | scripts/test |
| LOGIN-04 | NOT COVERED: login form state needs browser E2E | none |
| LOGIN-05 | frontend/tests/login-template.test.mjs ("LOGIN-05 login template embeds no demo credentials") | node --test |
| JOIN-01 | NOT COVERED: form validation needs browser E2E | none |
| JOIN-02 | GameIntegrationTest#deadlineMembershipAndNameChecksDoNotDependOnPostgres; scripts/test-room.lua "membership, host authority, revoked access and name uniqueness" | scripts/test, Lua smoke |
| JOIN-03 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (snapshot after answer returns the session's receipt) | scripts/test |
| JOIN-04 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (guest start returns 403); scripts/test-room.lua HOST_REQUIRED check | scripts/test, Lua smoke |
| STUDIO-01 | NOT COVERED: route guard needs browser E2E; the API-level 401 for guests is covered under LOGIN-03 | none |
| STUDIO-02 | NOT COVERED: server validation annotations exist but have no test | none |
| STUDIO-03 | NOT COVERED: cross-owner publish untested | none |
| STUDIO-04 | NOT COVERED: no edit API exists yet, so nothing exercises a later catalog change | none |
| STUDIO-05 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (same commandId returns the same room); RoomServiceTest#sqlFailureAfterInitMustNotRemoveLiveRegistrationAndRetryRepairsIt; GameIntegrationTest#activeRegistrationRepairsLiveStateButNeverResurrectsFinishedRoom | scripts/test |
| ROOM-03 | GameIntegrationTest#receiptSurvivesRoundTransitionAndConflictingAnswerIsRejected; scripts/test-room.lua "retry of previous round cannot score or advance the new round" | scripts/test, Lua smoke |
| ROOM-04 | frontend/tests/index-base-href.test.mjs ("ROOM-04 index.html declares base href", a reload on a deep route loads the bundle); scripts/smoke-release deep-link check in CI. The pending-command memory part has no test harness | node --test, ci release images |
| ROOM-07 | frontend/tests/room-connection.test.mjs ("ROOM-07 capacity refusal (close 1012) schedules no retry timer ..."); RoomWebSocketHubTest#registrationRefusesRoomCapacityBeforeSchedulingWork (server side of the refusal) | node --test, scripts/test |
| ANSWER-01 | NOT COVERED: template interaction needs browser E2E | none |
| ANSWER-04 | NOT COVERED: RoomStore retry policy has no test harness | none |
| ANSWER-06 | frontend/tests/room-connection.test.mjs ("socket open sends only SYNC ...", "REVOKED rejects late HTTP result ...") | node --test |
| ANSWER-07 | NOT COVERED: RoomStore error policy has no test harness | none |
| HIST-01 | NOT COVERED: cross-owner history access untested | none |
| HIST-02 | NOT COVERED: ARCHIVE_NOT_READY untested | none |
| HIST-04 | NOT COVERED: reading results after Redis key cleanup untested | none |
| LIVE-01 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (stranger gets 403); GameIntegrationTest#websocketAuthenticatesByCookiePushesStateAndRejectsMutations (no session or bad Origin refused); GameIntegrationTest#deadlineMembershipAndNameChecksDoNotDependOnPostgres (intruder snapshot denied); scripts/test-room.lua membership check | scripts/test, Lua smoke |
| LIVE-02 | GameIntegrationTest#deadlineMembershipAndNameChecksDoNotDependOnPostgres (DEADLINE_PASSED, timer reveal); scripts/test-room.lua "deadline equality rejects even before the timer runs", "room expiry finalizes once" | scripts/test, Lua smoke |
| LIVE-03 | GameIntegrationTest#parallelRetriesProduceOneReceiptOneScoreAndOneAnswerEvent, #parallelCorrectPlayersGetDistinctRanksAndTierScores, #receiptSurvivesRoundTransitionAndConflictingAnswerIsRejected; scripts/test-room.lua "retries preserve receipt ...", "wrong answers do not consume a correct rank ..." | scripts/test, Lua smoke |
| LIVE-04 | GameIntegrationTest#scoreAndCorrectOptionStayHiddenUntilReveal; scripts/test-room.lua "answer receipt and live snapshot hide result until reveal"; frontend/tests/room-state.test.mjs snapshot validator | scripts/test, Lua smoke, node --test |
| LIVE-05 | GameIntegrationTest#websocketAuthenticatesByCookiePushesStateAndRejectsMutations (READ_ONLY_CHANNEL, STATE after a REST command); RoomWebSocketHubTest#burstAndSyncDoNotSpawnAnotherSnapshotWhileOneIsRunning (coalescing keeps the newest state); frontend/tests/room-connection.test.mjs ("LIVE-05 default timers call the globals without an object receiver", reconciliation must start in a real browser) | scripts/test, node --test |
| LIVE-06 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (archive replay of an applied event is a no-op; FINISHED archived) | scripts/test |
| LIVE-07 | GameIntegrationTest#corruptKeyTypeIsRejectedBeforeAnyMutation; scripts/test-room.lua "corrupt type fails before writes"; the ROOM_STATE_LOST path is untested | scripts/test, Lua smoke |

Browser E2E, slow-network load, Redis loss/OOM and full outage/recovery campaigns
remain gaps; track them in [quality risks](../architecture/quality-and-risks.md).

## Documentation lint

scripts/check-docs.mjs enforces the documentation rules from [docs/README.md](../README.md).
Each rule runs in mode `fail`, `warn` or `off`; a rule is switched to `fail` only once
the tree is clean for it, so the gate never blocks on pre-existing debt.

| Rule | Checks |
|---|---|
| links | Every local link and anchor resolves; architecture pages carry no wire examples |
| L1 | Every document under docs/ is linked from docs/README.md or from the README.md of its directory |
| L2 | Each acceptance ID (PREFIX-NN bullet in features/ or domain/) is defined once and retired IDs are not redefined |
| L3 | Each acceptance ID has a row in the traceability table below; a covered row must be greppable in test sources |
| L4 | A numeric limit appears only in its owner document; other documents link to it |
| L5 | Each ADR has an index row whose Status matches the file |
| imports | Java imports follow the module matrix in [backend architecture](../architecture/backend.md) |

The three AGENTS files and the GitHub templates are linted too.

## CI

scripts/verify is what CI runs; the workflows, their triggers, the required checks,
the caches and the release and hotfix procedure are owned by [delivery](delivery.md).
Caches are keyed on the Gradle build files and the npm lockfile
([ADR 0007](../adr/0007-ci-caches-and-verified-artifacts.md)); a dependency change
misses the exact key and falls back to the newest cache for the same operating system.
Do not substitute lightweight tests for unavailable full verification; a green
release-image job proves the images boot and route, not the application behavior.
