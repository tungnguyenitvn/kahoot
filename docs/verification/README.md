# Verification status

Current status per gate, rewritten in place after each run
([ADR 0005](../adr/0005-evidence-location.md)). Per-change evidence lives in the PR;
historical records in this directory are immutable. "Revision" is the commit whose tree
was verified: a later documentation-only commit does not invalidate a gate, a code
change does.

| Gate | Command | Latest result | Revision | Environment | Date | Run |
|---|---|---|---|---|---|---|
| Documentation lint | node scripts/check-docs.mjs | PASS, rules L1 to L4 in fail mode with 0 warnings | 37d0d39 | macOS, Node 22.22.3 | 2026-09-23 | local |
| Offline policy/lifecycle | node --test frontend/tests/*.test.mjs | PASS 12/12 | 4763892 | macOS, Node 22.22.3 | 2026-09-23 | local |
| Lua smoke | scripts/test-room.lua | PASS 10/10 | 4763892 | Lua 5.4.7 in a redis:7.4-alpine container | 2026-09-23 | local |
| Backend unit + integration | ./scripts/test | PASS unit 5/5, integration 10/10 | 4763892 | Docker Desktop 29.7.2, eclipse-temurin:24-jdk, postgres:17-alpine, redis:7.4-alpine | 2026-09-23 | local |
| Full gate | ./scripts/verify | PASS backend, check-docs, 12 Node tests, ng build | 62b9fe4 | Docker Desktop 29.7.2, node:24.15.0 | 2026-09-23 | local |
| CI (GitHub Actions) | .github/workflows/ci.yml | NOT RUN: no remote configured | none | none | none | none |

Historical records: [refactor-review.md](refactor-review.md) covers the documentation
refactor and the contract fixes of 2026-09-23.
