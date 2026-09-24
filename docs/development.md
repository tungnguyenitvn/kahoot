# Development

How humans and AI agents change this repository: the coding conventions, the change
lifecycle, the gates and what each proves, the acceptance ID to test matrix, the delivery
pipeline and the current status of every gate. The step-by-step guide for a contributor is
[CONTRIBUTING](../CONTRIBUTING.md); authority and invariants are in [AGENTS.md](../AGENTS.md).

## Conventions

Architecture/dependency rules: [backend](architecture/backend.md) and
[frontend](architecture/frontend.md). This section owns coding practices, not schemas.

### Shared

- JSON camelCase; Java types PascalCase, members camelCase, constants UPPER_SNAKE_CASE.
- Entity IDs are UUID strings; identity IDs are prefixed U:/G:, answerId is composite,
  Redis stream IDs are not UUIDs. Never assert all IDs share one format.
- Validate shape at boundary; validate authority and state at the owning module.
- Error codes are stable contract values, not text to parse from exception messages.
- No credentials/private answers in logs. No hidden implicit retry of mutations.

### Backend

Constructor injection, small request records, explicit validation. Gameplay decisions
stay in Lua; SQL transactions apply within PostgreSQL only.
Keep I/O budgets explicit. Virtual threads are not admission control.
Separate recoverable timeout from terminal domain error; retain the original
command/payload when retrying a supported idempotent operation.
Clock and ordering rules belong to the [domain](domain.md) (LIVE-02).

### Frontend

Standalone OnPush pages; signals for owned state, computed for derived data,
linkedSignal for per-round selection; Signal Forms for local validation.
Templates render, not manage socket/timer lifecycle; layer ownership is in the
[frontend architecture](architecture/frontend.md). Derived competition rank may be
calculated from server-visible scores; score allocation remains server-owned.
Runtime network JSON is untrusted even when TypeScript has an interface.
Dispose subscriptions/timers and ignore late responses after navigation/revocation.

## Workflow

### Smallest sufficient context

Read root/nested AGENTS, the relevant feature, architecture section, contract and
tests. The documentation map is the navigation map. Do not require every feature document for every task.
For answer retry: domain + live-room + gameplay/realtime + receipt
contract + store/Lua tests. Login/catalog content is not required unless impacted.

### Change lifecycle

| Stage | Artifact | Gate |
|---|---|---|
| Define | Issue: goal, non-goals, acceptance IDs | Resolve material ambiguity |
| Design | Impacted module/contracts; ADR for structural change | Review trade-offs/security |
| Plan | Small tasks and verification commands in issue/PR | Scope fits authorization |
| Implement | Code + tests + affected canonical docs | Preserve invariants |
| Verify | Commands, results, environment, revision/artifact | Distinguish passed, failed, not run |
| Review/release | PR + risks + migration/rollback if needed | Required CI/review/approval |

Use Draft, Accepted, Implemented, Verified on the change record. They are not
interchangeable; docs often state an accepted requirement before it is verified.
What the pipeline does after the merge, and how a release or hotfix is cut, is owned by
[delivery](#delivery).

### Task brief template

- Goal / acceptance IDs:
- Non-goals:
- Required context links:
- Allowed files/modules and contract impact:
- Verification commands and required environment:
- Open questions / stop conditions:

No separate planning document is required for a trivial fix. Never duplicate issue
history under docs/changes. No automatic commits, PRs or deployment from a request
that only asks to review files.

### Completion checklist

- Acceptance criteria and negative paths checked.
- No private data leak, scoring/idempotency regression or unintended scope change.
- Contract, feature and implementation agree; breaking changes are explicit.
- Relevant test/build gate executed, or blocker clearly reported.
- Links and documentation boundary checker pass.
- Review records remaining risk; release is a separate authorized action.

## Testing and evidence

### Gates

| Gate | Command | What it proves / does not prove |
|---|---|---|
| Offline policy/lifecycle | node --test frontend/tests/*.test.mjs | Version/ranking + injected connection lifecycle; NOT Angular/browser behavior |
| Lua smoke | scripts/test-room.lua, run by scripts/verify in a Lua 5.4 container (lua-smoke service); locally texlua or any Lua 5.3/5.4 | Sequential invariants with Redis double; NOT Redis concurrency/durability |
| Documentation checks | node scripts/check-docs.mjs | Local links and documentation boundary rules; NOT semantic completeness |
| Documentation lint self-test | node --test scripts/check-docs.test.mjs | The lint's own rules against fixture trees (module matrix, unknown package, composition root); NOT the repository content |
| Backend unit | Docker scripts/test or Gradle test in configured JDK | Mockito fault injection/coalescing tests; NOT real services |
| Backend integration | scripts/test | Real HTTP/cookies/WS/Redis/SQL behavior in isolated services |
| Full gate | scripts/verify | Lua smoke, backend tests/package, documentation check and its self-test, Angular tests/build |

Read the [verification status](#gate-status) for the latest executed
result per gate. Never infer pass from the presence or name of a test.

### Traceability

Every acceptance ID from [features](features/README.md) and
[domain invariants](domain.md#acceptance-invariants) has one row. A covered row
names tests whose display name or test name carries the ID, so `grep` finds it; a row
marked NOT COVERED states why. Rule L3 keeps this table and the test sources in sync.
Scenario tests without an ID (notification bounds, provisioning repair, session revocation,
COMMAND_LIMIT capacity) map to the [acceptance scenarios](architecture/README.md#acceptance-scenarios).

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
remain gaps; track them in [quality risks](architecture/README.md#limits-and-timings).

### Documentation lint

scripts/check-docs.mjs enforces the documentation rules from [docs/README.md](README.md).
Each rule runs in mode `fail`, `warn` or `off`; a rule is switched to `fail` only once
the tree is clean for it, so the gate never blocks on pre-existing debt.

| Rule | Checks |
|---|---|
| links | Every local link and anchor resolves; architecture pages carry no wire examples |
| L1 | Every document under docs/ is linked from docs/README.md or from the README.md of its directory |
| L2 | Each acceptance ID (PREFIX-NN bullet in features/ or domain.md) is defined once and retired IDs are not redefined |
| L3 | Each acceptance ID has a row in the traceability table above; a covered row must be greppable in test sources |
| L4 | A numeric limit appears only in its owner document; other documents link to it |
| L5 | Each ADR has an index row whose Status matches the file |
| imports | Java imports follow the module matrix in [backend architecture](architecture/backend.md); a package outside the matrix fails until it is registered there and in the map |

The three AGENTS files and the GitHub templates are linted too.

### CI

scripts/verify is what CI runs; the workflows, their triggers, the required checks,
the caches and the release and hotfix procedure are owned by [delivery](#delivery).
Caches are keyed on the Gradle build files and the npm lockfile
([ADR 0007](adr/0007-ci-caches-and-verified-artifacts.md)); a dependency change
misses the exact key and falls back to the newest cache for the same operating system.
Do not substitute lightweight tests for unavailable full verification; a green
release-image job proves the images boot and route, not the application behavior.

## Delivery

What happens to a change after it leaves the developer's hands: which workflow runs
on which trigger, which checks must be green before a merge, how a release is cut and
how a hotfix ships. The developer-facing side (context, task brief, change lifecycle)
is [workflow](#workflow); gate scopes and the traceability matrix are
[testing](#testing-and-evidence); decisions are [ADR 0006](adr/0006-ci-cd-release-images.md)
and [ADR 0007](adr/0007-ci-caches-and-verified-artifacts.md).

### Branch model

Trunk-based: `main` is the only long-lived branch and every change reaches it through
a pull request from a short-lived branch (naming in [CONTRIBUTING](../CONTRIBUTING.md)).
Releases are cut from tags, not from branches, and the same artifacts that passed the
gate are what gets published. No cherry-picking between branches; nothing is pushed
to `main` outside a pull request. Long-lived integration or staging branches are not
used: a staging environment, if one ever exists, receives the same image tag as
production (artifact promotion), not a separate branch.

### Workflows by trigger

| Trigger | Workflow and job | Runs | Publishes |
|---|---|---|---|
| Pull request, push to `main`, manual | `ci` / `verify gate` | scripts/verify with the Gradle and npm caches restored: Lua smoke, backend unit and integration tests plus bootJar, documentation lint, Node tests, Angular build | Workflow artifacts `verification-reports` and `build-artifacts` (boot jar, Angular bundle) |
| Same run, after `verify gate` | `ci` / `release images from verified artifacts` | scripts/smoke-release on the downloaded artifacts: package the release images, boot compose.release.yaml, check the SPA, the API proxy and CSRF | Nothing |
| Tag `v*`, manual | `release` / `verify, package, smoke, publish` | scripts/verify on the tagged tree, scripts/smoke-release on its artifacts, push to GHCR, GitHub Release | `ghcr.io/<owner>/kahoot-backend` and `kahoot-frontend` with the tag and `latest`, artifact `release-jar`, a GitHub Release carrying the jar |

A commit that only rewrites the [verification status](#gate-status)
after a run carries `[skip ci]` so it does not start another run.

### Required checks before a merge

Both `ci` jobs, `verify gate` and `release images from verified artifacts`, must be
green before a merge. The ruleset `main` (Settings, Rules, Rulesets) enforces it:
changes reach `main` through a pull request, both status checks must pass, force
pushes and branch deletion are blocked, and repository admins stay on the bypass list
so a broken pipeline can never lock the owner out; a bypassed push is recorded by
GitHub and should be the exception. Merge methods merge, squash and rebase are all
allowed. No review count is required yet; add one in the ruleset once the project has
more than one maintainer. The ruleset id and its state are recorded in the
[verification status](#gate-status).

### Cutting a release

Releases are semantic versions on annotated tags `vX.Y.Z`; the GitHub Release notes
are generated from the merged pull requests since the previous tag, so pull request
titles are the changelog.

1. Prepare the version in the last pull request before the tag: `version` in
   backend/build.gradle.kts, and `version` in frontend/package.json together with the
   two `version` fields of frontend/package-lock.json (running
   `npm version --no-git-tag-version X.Y.Z` inside frontend/ updates both JSON files).
   Bump the patch number for fixes, the minor number for features.
2. Confirm the merge commit on `main` is green in `ci` and that its message does not
   contain `[skip ci]` (see below).
3. Tag that commit and push the tag:

   ```bash
   git tag -a v0.2.2 -m "v0.2.2: what this release changes" && git push origin v0.2.2
   ```

4. Watch the `release` workflow (Actions, workflow `release`); it takes a few minutes.
   Green means: the tagged tree passed scripts/verify again, the images passed
   scripts/smoke-release, `ghcr.io/<owner>/kahoot-backend` and `kahoot-frontend`
   carry the new tag and `latest`, and the GitHub Release exists with
   `quiz-room-vX.Y.Z.jar` attached.
5. Record the run in the [verification status](#gate-status): result,
   tag, revision, environment, run URL. That commit carries `[skip ci]`.
6. Try the published stack once before announcing it: pull the images by tag, start
   compose.release.yaml as described in [deployment](architecture/README.md#deployment),
   and open a room in a browser. The gate proves the images boot and route; a browser
   session is the only check of the UI today.

If the tag push started no `release` run, the tagged commit's message carries
`[skip ci]`: GitHub applies the marker to the push event of a tag as well, and to
pull request events, and it matches the marker anywhere in the message, including the
body. Never write the marker in prose inside a commit message. Either tag a commit
without the marker or start the workflow on the existing tag by hand; the dispatched
run still sees a tag ref and publishes the same way:

```bash
gh workflow run release.yml --ref v0.2.2
```

Rollback is a redeploy of the previous tag: every version stays on GHCR, only
`latest` moves. Delete a GitHub Release only when its images were never used. Images
are built on the `ubuntu-latest` runner and are linux/amd64 only; Apple Silicon and
other arm64 hosts run them under emulation until a multi-architecture build exists.

### Hotfix

- Normal case: fix forward. Branch `fix/<topic>` from `main`, write the failing
  regression test first, merge through a pull request, then tag the next patch
  version from `main`.
- `main` holds unreleased work that must not ship: branch `hotfix/<topic>` from the
  last release tag, open the pull request into `main` as usual, and cut the patch tag
  from the hotfix branch head once its `ci` run is green. The tag's `release` run
  verifies exactly that tree. Merge the hotfix branch into `main` so the fix is not
  lost; never cherry-pick it.

### Secrets and environment values

Workflows use only `GITHUB_TOKEN`; no repository secret exists today. The release
stack takes `DB_PASSWORD`, `DEMO_PASSWORD`, `PUBLIC_ORIGIN` and `COOKIE_SECURE` from
the operator's environment or a `.env` file that is never committed (`.env.example`
holds development-only values). A future deployment credential (SSH key, registry
token) belongs in GitHub Actions secrets or a GitHub Environment, never in the tree,
and its name is added here when it exists.

### When a deployment target exists

Add a `deploy` job that runs after the `release` job on the same tag, bound to a
GitHub Environment (`staging`, then `production`) with required reviewers. It deploys
the images already published for that tag, either by `docker compose pull` on the
host or, where the host cannot reach the registry, by `docker save` in the workflow,
transfer over SSH and `docker load` on the host, followed by
`docker compose -f compose.release.yaml up -d` with the host's `.env`. Promotion from
staging to production is the same tag deployed to the next environment, never a
rebuild and never a branch merge. TLS termination, secure cookies, backups and
monitoring remain prerequisites listed in [deployment](architecture/README.md#deployment).

## Gate status

Current status per gate, rewritten in place after each run
([ADR 0010](adr/0010-compact-documentation-map.md)). Per-change evidence lives in the PR;
replaced results live in git history, not in this document. "Revision" is the commit whose tree
was verified: a later documentation-only commit does not invalidate a gate, a code
change does.

| Gate | Command | Latest result | Revision | Environment | Date | Run |
|---|---|---|---|---|---|---|
| Lua smoke | scripts/test-room.lua (lua-smoke stage) | PASS 10/10 | 51a6880 | Alpine 3.21 + Lua 5.4 container | 2026-09-23 | local scripts/verify |
| Backend unit + integration | backend-test stage (clean test integrationTest bootJar) | PASS unit 5/5, integration 10/10, bootJar built | 51a6880 | Docker Desktop 29.7.2, eclipse-temurin:24-jdk, postgres:17-alpine, redis:7.4-alpine | 2026-09-23 | local scripts/verify |
| Documentation lint | node scripts/check-docs.mjs (frontend-test stage) | PASS 44 documents, 175 links; rules L1 to L5 in fail mode, 0 warnings | 51a6880 | node:24.15.0 container | 2026-09-23 | local scripts/verify |
| Offline policy/lifecycle | node --test frontend/tests/*.test.mjs (frontend-test stage) | PASS 14/14 | 51a6880 | node:24.15.0 container | 2026-09-23 | local scripts/verify |
| Angular build | npm run build (frontend-test stage) | PASS, application bundle generated | 51a6880 | node:24.15.0 container, Angular 22 | 2026-09-23 | local scripts/verify |
| Full gate | ./scripts/verify | PASS, all stages above | 51a6880 | macOS host, Docker Desktop 29.7.2 | 2026-09-23 | local |
| Release stack smoke | scripts/smoke-release | PASS: SPA served, /api proxied, CSRF enforced through nginx | 8ce5941 | macOS host, Docker Desktop 29.7.2, images built from Dockerfile.release | 2026-09-23 | local |
| CI (GitHub Actions) | .github/workflows/ci.yml | PASS with warm caches: "verify gate" 118 s (Gradle build 35 s, npm install 7 s, both caches hit), "release images from verified artifacts" 47 s. Cold-cache run 35892588183: 170 s and 44 s. Before ADR 0007: 189 s and 131 s (run 35891087371). Two earlier runs failed at startup while Actions was disabled for the account; fixed by the owner on 2026-09-23 | 82dc8b0 | ubuntu-latest runner, Docker from the runner image, actions on Node 24 | 2026-09-24 | https://github.com/tungnguyenitvn/kahoot/actions/runs/35893152449 |
| Release (GitHub Actions) | .github/workflows/release.yml | PASS for tag v0.2.2, started by the tag push: verify, package, smoke, images pushed as ghcr.io/tungnguyenitvn/kahoot-backend and kahoot-frontend (v0.2.2 and latest, anonymously pullable), GitHub Release with quiz-room-v0.2.2.jar. Earlier: v0.2.1 (run 35899396562, 164 s) and v0.2.0 (run 35896369794, dispatched by hand because the tagged commit carried [skip ci]). v0.2.0 and v0.2.1 are superseded: their frontend fails in a browser (see the next row) | 0e2ef6e (tag v0.2.2) | ubuntu-latest runner, Docker from the runner image | 2026-09-24 | https://github.com/tungnguyenitvn/kahoot/actions/runs/35900688309 |
| Published stack in a browser | docker compose -f compose.release.yaml with the GHCR images, Claude desktop browser | PASS on v0.2.2: deep link /room/{id} loads, the WebSocket badge shows live through nginx, a guest joins by PIN, the host starts via REST, the guest submits an answer in the UI ("Đã ghi nhận đáp án"), the timer reveals the correct option, the leaderboard shows 1000, the host finishes and GET /api/history/{id} returns the archived score. Found and fixed on the way: v0.2.0 never loaded a room (browser timers called with an object receiver, PR #1) and v0.2.1 showed a blank page on any deep link or reload (no base href, PR #3); both were invisible to the Node gate and the release smoke, which now checks deep links | 0e2ef6e (images v0.2.2) | macOS host, Docker Desktop 29.7.2, amd64 images under emulation | 2026-09-24 | local |
| Branch protection on main | GitHub ruleset `main` (id 23892021) | ENABLED: pull request required, status checks `verify gate` and `release images from verified artifacts` required, force push and deletion blocked, repository admins on the bypass list; no review count yet, see [delivery](#delivery) | none | github.com, repository public since 2026-09-24 | 2026-09-24 | none |
