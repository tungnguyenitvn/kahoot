# Verification status

Current status per gate, rewritten in place after each run
([ADR 0005](../adr/0005-evidence-location.md)). Per-change evidence lives in the PR;
historical records in this directory are immutable. "Revision" is the commit whose tree
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
| Release (GitHub Actions) | .github/workflows/release.yml | PASS for tag v0.2.0 in 124 s: verify 64 s with warm caches, package and smoke 28 s, images pushed as ghcr.io/tungnguyenitvn/kahoot-backend and kahoot-frontend (v0.2.0 and latest, anonymously pullable), GitHub Release with quiz-room-v0.2.0.jar. The tag push itself started no run because the tagged commit carried [skip ci]; the run was dispatched by hand on the tag, see [delivery](../development/delivery.md#cutting-a-release) | d962d44 (tag v0.2.0) | ubuntu-latest runner, Docker from the runner image | 2026-09-24 | https://github.com/tungnguyenitvn/kahoot/actions/runs/35896369794 |
| Branch protection on main | GitHub ruleset `main` (id 23892021) | ENABLED: pull request required, status checks `verify gate` and `release images from verified artifacts` required, force push and deletion blocked, repository admins on the bypass list; no review count yet, see [delivery](../development/delivery.md) | none | github.com, repository public since 2026-09-24 | 2026-09-24 | none |

Historical records: [refactor-review.md](refactor-review.md) covers the documentation
refactor and the contract fixes of 2026-09-23.
