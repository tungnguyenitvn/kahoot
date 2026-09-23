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
| CI (GitHub Actions) | .github/workflows/ci.yml | NOT RUN: no remote configured | none | none | none | none |

Historical records: [refactor-review.md](refactor-review.md) covers the documentation
refactor and the contract fixes of 2026-09-23.
