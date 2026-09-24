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
| Offline policy/lifecycle | node --test "frontend/tests/**/*.test.mjs" | Version/ranking + injected connection lifecycle; NOT Angular/browser behavior |
| Angular component specs | npm run test:ui (ng test, Vitest + jsdom, frontend-test stage) | Route guards, Signal Forms and template interaction with mocked HTTP and store; NOT a real browser, nginx or WebSocket |
| Lua smoke | scripts/test-room.lua, run by scripts/verify in a Lua 5.4 container (lua-smoke service); locally texlua or any Lua 5.3/5.4 | Sequential invariants with Redis double; NOT Redis concurrency/durability |
| Documentation checks | node scripts/check-docs.mjs | Local links and documentation boundary rules; NOT semantic completeness |
| Documentation lint self-test | node --test scripts/check-docs.test.mjs | The lint's own rules against fixture trees (module matrix, unknown package, composition root, layer directions and privacy, frontend folder boundaries); NOT the repository content |
| Pull request shape | node scripts/check-pr.mjs with PR_TITLE and PR_BODY (workflow `pr-shape` on every pull request event); its self-test node --test scripts/check-pr.test.mjs runs in the frontend-test stage | The title is a conventional commit subject and every section of the pull request template is filled by the author, a docs box is ticked, a verification box is ticked or NOT RUN is written, a change status is named; NOT that the evidence is true, that the IDs exist or that the docs match the code |
| Backend unit | Docker scripts/test or Gradle test in configured JDK | Mockito fault injection/coalescing tests; NOT real services |
| Backend integration | scripts/test | Real HTTP/cookies/WS/Redis/SQL behavior in isolated services |
| Full gate | scripts/verify | Lua smoke, backend tests/package, documentation check and its self-test, the pull request check's self-test, Node policy tests, Angular component specs and build |
| Published stack in a browser | ./e2e/run (Playwright 1.63, Chromium headless shell 153, release stack from the local artifacts) | PASS 1/1 in 4.7 s: host login, open room, deep link reload, WebSocket badge live through nginx, guest joins by PIN, host starts, guest answers A and sees "Đã ghi nhận đáp án A.", reveal shows 1000 on both screens, host finishes, GET /api/history/{id} returns 1000 after the archive commits. First run found that the browser origin must match PUBLIC_ORIGIN (127.0.0.1 vs localhost), fixed in e2e/run. Earlier manual check on v0.2.2 is in git history | b01ed62 (images built from the Java 25 artifacts) | macOS host, Docker Desktop 29.7.2, Node 22.22.3 for Playwright | 2026-09-24 | local ./e2e/run |

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
| LOGIN-02 | src/app/core/host.guard.spec.ts ("LOGIN-02 STUDIO-01 a guest asking for /host lands on /login") | npm run test:ui |
| LOGIN-03 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (logout returns 204, next host request 401) | scripts/test |
| LOGIN-04 | src/app/features/login/login.page.spec.ts ("LOGIN-04 a failed login shows the error, stays on the route, keeps the username") | npm run test:ui |
| LOGIN-05 | frontend/tests/login/login-template.test.mjs ("LOGIN-05 login template embeds no demo credentials") | node --test |
| JOIN-01 | src/app/features/entry/entry.page.spec.ts ("JOIN-01 a PIN that is not six digits keeps submit disabled and never calls the API") | npm run test:ui |
| JOIN-02 | GameIntegrationTest#deadlineMembershipAndNameChecksDoNotDependOnPostgres; scripts/test-room.lua "membership, host authority, revoked access and name uniqueness" | scripts/test, Lua smoke |
| JOIN-03 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (snapshot after answer returns the session's receipt) | scripts/test |
| JOIN-04 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (guest start returns 403); scripts/test-room.lua HOST_REQUIRED check; IdentitiesTest#hostRequiresAnAccount (a guest identity never passes host()) | scripts/test, Lua smoke |
| STUDIO-01 | src/app/core/host.guard.spec.ts (Studio never renders for a guest); the API-level 401 for guests is covered under LOGIN-03 | npm run test:ui |
| STUDIO-02 | DraftTest#rejectsMissingTitleQuestionsOrOptions and #keepsValidContent (domain invariants; the HTTP 400 mapping of the same bounds has no separate test) | scripts/test |
| STUDIO-03 | QuizCatalogTest#publishIsScopedToTheOwner (application facade over an in-memory repository; the SQL owner clause has no separate test) | scripts/test |
| STUDIO-05 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (same commandId returns the same room); RoomsTest#sqlFailureAfterInitMustNotRemoveLiveRegistrationAndRetryRepairsIt; GameIntegrationTest#activeRegistrationRepairsLiveStateButNeverResurrectsFinishedRoom | scripts/test |
| ROOM-03 | GameIntegrationTest#receiptSurvivesRoundTransitionAndConflictingAnswerIsRejected; scripts/test-room.lua "retry of previous round cannot score or advance the new round" | scripts/test, Lua smoke |
| ROOM-04 | frontend/tests/app/index-base-href.test.mjs ("ROOM-04 index.html declares base href", a reload on a deep route loads the bundle); scripts/smoke-release deep-link check in CI. The pending-command memory part has no test harness | node --test, ci release images |
| ROOM-07 | frontend/tests/room/room-connection.test.mjs ("ROOM-07 capacity refusal (close 1012) schedules no retry timer ..."); RoomWebSocketHubTest#registrationRefusesRoomCapacityBeforeSchedulingWork (server side of the refusal) | node --test, scripts/test |
| ANSWER-01 | src/app/features/room/room.page.spec.ts ("ANSWER-01 an active player picks an option in the open question and sends it through the store") | npm run test:ui |
| ANSWER-04 | frontend/tests/room/answer-policy.test.mjs ("ANSWER-04 a retry reuses the original option, round and commandId", "ANSWER-04 a new round drops a stale pending answer"); the store wires the policy, its own signals have no harness | node --test |
| ANSWER-06 | frontend/tests/room/room-connection.test.mjs ("socket open sends only SYNC ...", "REVOKED rejects late HTTP result ...") | node --test |
| ANSWER-07 | frontend/tests/room/answer-policy.test.mjs ("ANSWER-07 network failures, 5xx, 408 and 429 keep the pending answer") | node --test |
| HIST-01 | HistoryTest#resultsAreScopedToTheOwner (facade over an in-memory repository; the SQL owner clause has no separate test) | scripts/test |
| HIST-02 | HistoryTest#unfinishedRoomIsNotReady | scripts/test |
| HIST-04 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (result and listing answer after the room keys are deleted) | scripts/test |
| LIVE-01 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (stranger gets 403); GameIntegrationTest#websocketAuthenticatesByCookiePushesStateAndRejectsMutations (no session or bad Origin refused); GameIntegrationTest#deadlineMembershipAndNameChecksDoNotDependOnPostgres (intruder snapshot denied); scripts/test-room.lua membership check; RoomDomainTest#freezeCopiesQuestionsWithRoundIds (the room keeps its own copy of the questions) | scripts/test, Lua smoke |
| LIVE-02 | GameIntegrationTest#deadlineMembershipAndNameChecksDoNotDependOnPostgres (DEADLINE_PASSED, timer reveal); scripts/test-room.lua "deadline equality rejects even before the timer runs", "room expiry finalizes once" | scripts/test, Lua smoke |
| LIVE-03 | GameIntegrationTest#parallelRetriesProduceOneReceiptOneScoreAndOneAnswerEvent, #parallelCorrectPlayersGetDistinctRanksAndTierScores, #receiptSurvivesRoundTransitionAndConflictingAnswerIsRejected; scripts/test-room.lua "retries preserve receipt ...", "wrong answers do not consume a correct rank ..." | scripts/test, Lua smoke |
| LIVE-04 | GameIntegrationTest#scoreAndCorrectOptionStayHiddenUntilReveal; scripts/test-room.lua "answer receipt and live snapshot hide result until reveal"; frontend/tests/room/room-state.test.mjs snapshot validator | scripts/test, Lua smoke, node --test |
| LIVE-05 | GameIntegrationTest#websocketAuthenticatesByCookiePushesStateAndRejectsMutations (READ_ONLY_CHANNEL, STATE after a REST command); RoomWebSocketHubTest#burstAndSyncDoNotSpawnAnotherSnapshotWhileOneIsRunning (coalescing keeps the newest state); frontend/tests/room/room-connection.test.mjs ("LIVE-05 default timers call the globals without an object receiver", reconciliation must start in a real browser) | scripts/test, node --test |
| LIVE-06 | GameIntegrationTest#realCookiesCsrfAuthorizationReconnectAndArchiveReplay (archive replay of an applied event is a no-op; FINISHED archived); ProjectionTest#replayIsANoOpAndGapsAreRejected (version gate and projection order over an in-memory repository) | scripts/test |
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
| L3 | Each acceptance ID has a row in the traceability table above; a covered row must be greppable in test sources (backend tests, frontend/tests, *.spec.ts under frontend/src/app, the Lua smoke) |
| L4 | A numeric limit appears only in its owner document; other documents link to it |
| L5 | Each ADR has an index row whose Status matches the file |
| frontend | Under frontend/src/app, features import only core and shared, core only shared, shared nothing in the application; features never import each other or the app root; a policy module (.mjs) imports only other .mjs modules; *.spec.ts files are exempt |
| imports | Java imports follow the module matrix in [backend architecture](architecture/backend.md); a package outside the matrix fails until it is registered there and in the map; inside a layered module the layer directions hold (api → application → domain, infrastructure → application and domain, domain imports the JDK and domain types only) and another module never imports api or infrastructure |

The four AGENTS files and the GitHub templates are linted too.

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
| Pull request opened, edited, synchronized or reopened | `pr-shape` / `pull request shape` | scripts/check-pr.mjs on the title and the body ([testing](#testing-and-evidence)); its own concurrency group, so an edit never cancels a running gate | Nothing |
| Same run, after `verify gate` | `ci` / `release images from verified artifacts` | scripts/smoke-release on the downloaded artifacts: package the release images, boot compose.release.yaml, check the SPA, the API proxy and CSRF | Nothing |
| Tag `v*`, manual | `release` / `verify, package, smoke, publish` | scripts/verify on the tagged tree, scripts/smoke-release on its artifacts, push to GHCR, GitHub Release | `ghcr.io/<owner>/kahoot-backend` and `kahoot-frontend` with the tag and `latest`, artifact `release-jar`, a GitHub Release carrying the jar |

A commit that only rewrites the [verification status](#gate-status) after a run goes
through a pull request like any other change and starts a normal `ci` run; the marker
`[skip ci]` is not used in this repository (see [cutting a release](#cutting-a-release)).

### Required checks before a merge

Three checks must be green before a merge: the `ci` jobs `verify gate` and
`release images from verified artifacts`, and the `pr-shape` job `pull request shape`.
The ruleset `main` (Settings, Rules, Rulesets) enforces it: changes reach `main` through
a pull request, the required status checks must pass, force pushes and branch deletion
are blocked, and repository admins stay on the bypass list so a broken pipeline can
never lock the owner out; a bypassed push is recorded by GitHub and should be the
exception. Merge methods merge, squash and rebase are all allowed. The
[verification status](#gate-status) records the ruleset id and which of the three
checks it requires today; a new check is added to the ruleset by the owner after its
first green run.

`.github/CODEOWNERS` names the owner of the requirements and their evidence: the domain
invariants, the contracts, the ADRs, this page, every test directory, the lints, the
workflows and the AGENTS files. GitHub requests that owner as reviewer on a pull
request that touches one of these paths, so a regression test, a contract or an
invariant is never changed unnoticed. No review count is required yet; once the
project has more than one maintainer, the ruleset gains one required approval and
"require review from code owners", and the file gains a second name per area.

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
   tag, revision, environment, run URL. That commit goes through a pull request like
   any other change, because the ruleset requires one; the `ci` run it starts is short
   and proves nothing new.
6. Run the browser check before announcing it: `./e2e/run` on the tagged tree, or
   start compose.release.yaml from the pulled images as described in
   [deployment](architecture/README.md#deployment) and run `E2E_BASE_URL=... npm test`
   in `e2e/`. The gate proves the images boot and route; the browser check proves the
   round a user plays. Record it in the same pull request as step 5.

`[skip ci]` is not used here: GitHub applies the marker to the push event of a tag as
well as to pull request events, and matches it anywhere in the message, including the
body, so a tagged commit carrying it starts no `release` run and a pull request carrying
it skips its required checks. Never write the marker in a commit message, not even in
prose (v0.2.0 was dispatched by hand for that reason). If a tag started no run, start
the workflow on the existing tag by hand; the dispatched run still sees a tag ref and
publishes the same way:

```bash
gh workflow run release.yml --ref v0.3.0
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
| Lua smoke | scripts/test-room.lua (lua-smoke stage) | PASS 10/10 | 4eb108a | Alpine 3.21 + Lua 5.4 container | 2026-09-24 | local scripts/verify |
| Backend unit + integration | backend-test stage (clean test integrationTest bootJar) | PASS unit 17/17, integration 10/10, bootJar built | 4eb108a | Docker Desktop 29.7.2, eclipse-temurin:25-jdk (Temurin 25.0.4 LTS), Gradle 9.7.1, postgres:17-alpine, redis:7.4-alpine | 2026-09-24 | local scripts/verify |
| Documentation lint | node scripts/check-docs.mjs (frontend-test stage) | PASS 38 documents, 246 links; rules links, wire, imports, frontend and L1 to L5 in fail mode, 0 warnings | 4eb108a | node:24.15.0 container | 2026-09-24 | local scripts/verify |
| Documentation lint self-test | node --test scripts/check-docs.test.mjs (frontend-test stage) | PASS 12/12 | 4eb108a | node:24.15.0 container | 2026-09-24 | local scripts/verify |
| Pull request shape | node scripts/check-pr.mjs (workflow pr-shape); self-test node --test scripts/check-pr.test.mjs (frontend-test stage) | PASS on pull request 27, job "pull request shape" 4 s; self-test PASS 8/8 in the gate; the titles and bodies of the merged pull requests 1, 3, 19, 22 and 26 pass the check locally | 747b422 | ubuntu-latest runner, Node from the runner image; replay on macOS host, Node 22.22.3 | 2026-09-24 | https://github.com/tungnguyenitvn/kahoot/actions/runs/35972509412 |
| Offline policy/lifecycle | node --test "frontend/tests/**/*.test.mjs" (frontend-test stage) | PASS 19/19 | 4eb108a | node:24.15.0 container | 2026-09-24 | local scripts/verify |
| Angular component specs | npm run test:ui (frontend-test stage) | PASS 5/5 in 4 files | 4eb108a | node:24.15.0 container, Vitest 4.1, jsdom 30 | 2026-09-24 | local scripts/verify |
| Angular build | npm run build (frontend-test stage) | PASS, application bundle generated | 4eb108a | node:24.15.0 container, Angular 22 | 2026-09-24 | local scripts/verify |
| Full gate | ./scripts/verify | PASS, all stages above | 4eb108a | macOS host, Docker Desktop 29.7.2 | 2026-09-24 | local |
| Release stack smoke | scripts/smoke-release | PASS: SPA served, /api proxied, CSRF enforced through nginx | 8ce5941 | macOS host, Docker Desktop 29.7.2, images built from Dockerfile.release | 2026-09-23 | local |
| CI (GitHub Actions) | .github/workflows/ci.yml | PASS with warm caches: "verify gate" 118 s (Gradle build 35 s, npm install 7 s, both caches hit), "release images from verified artifacts" 47 s. Cold-cache run 35892588183: 170 s and 44 s. Before ADR 0007: 189 s and 131 s (run 35891087371). Two earlier runs failed at startup while Actions was disabled for the account; fixed by the owner on 2026-09-23 | 82dc8b0 | ubuntu-latest runner, Docker from the runner image, actions on Node 24 | 2026-09-24 | https://github.com/tungnguyenitvn/kahoot/actions/runs/35893152449 |
| Release (GitHub Actions) | .github/workflows/release.yml | PASS for tag v0.3.0, started by the tag push: verify, package, smoke, images pushed as ghcr.io/tungnguyenitvn/kahoot-backend and kahoot-frontend (v0.3.0 and latest, the first on eclipse-temurin:25-jre), GitHub Release with quiz-room-v0.3.0.jar; job "verify, package, smoke, publish" 133 s. Earlier tags v0.2.0 to v0.2.2 are in git history | 8b405c9 (tag v0.3.0) | ubuntu-latest runner, Docker from the runner image | 2026-09-24 | https://github.com/tungnguyenitvn/kahoot/actions/runs/35963617118 |
| Published stack in a browser | docker compose -f compose.release.yaml with the GHCR images, Claude desktop browser | PASS 1/1 in 9.4 s against the pulled GHCR images (docker compose -f compose.release.yaml with BACKEND_IMAGE and FRONTEND_IMAGE at v0.3.0, no build; backend reports Temurin 25.0.4 LTS): host login, open room, deep-link reload, WebSocket badge live through nginx, guest joins by PIN, host starts, guest answers A and sees "Đã ghi nhận đáp án A.", reveal shows 1000 on both screens, host finishes, GET /api/history/{id} returns 1000 after the archive commits. Also PASS on the local artifacts of b01ed62 (4.7 s) | 8b405c9 (images v0.3.0) | macOS host, Docker Desktop 29.7.2, amd64 images under emulation, Playwright 1.63.0, Chromium headless shell 153, Node 22.22.3 | 2026-09-24 | local: e2e/ npm test with E2E_BASE_URL |
| Branch protection on main | GitHub ruleset `main` (id 23892021) | ENABLED: pull request required, status checks `verify gate` and `release images from verified artifacts` required, force push and deletion blocked, repository admins on the bypass list; `pull request shape` not yet required and no review count yet, see [delivery](#delivery) | none | github.com, repository public since 2026-09-24 | 2026-09-24 | none |
