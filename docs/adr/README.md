# Architecture decision records

| ADR | Title | Status | Date |
|---|---|---|---|
| [0001](0001-rest-sse-in-memory.md) | REST and SSE with in-memory sessions | Superseded by 0002 | not recorded |
| [0002](0002-redis-game-state.md) | Redis room state and PostgreSQL archive | Accepted | not recorded (v0.2 sample) |
| [0003](0003-documentation-and-evidence.md) | Layered documentation and evidence | Accepted | 2026-09-23 |
| [0004](0004-bounded-notification-and-repair.md) | Bounded notification and provisioning retry repair | Accepted | 2026-09-23 |
| [0005](0005-evidence-location.md) | Evidence location and verification status | Superseded by 0008 | 2026-09-23 |
| [0006](0006-ci-cd-release-images.md) | CI/CD on GitHub Actions with release images | Accepted | 2026-09-23 |
| [0007](0007-ci-caches-and-verified-artifacts.md) | CI caches and release images from verified artifacts | Accepted | 2026-09-23 |
| [0008](0008-verification-history-in-git.md) | Verification history lives in git, not in docs | Accepted | 2026-09-24 |
| [0009](0009-layered-modules-and-feature-folders.md) | Layered backend modules and frontend feature folders | Accepted | 2026-09-24 |

## Template

Title `ADR NNNN: <decision>`, then `Status:` (Proposed, Accepted, Deprecated or
Superseded by NNNN), `Date:` (ISO date; write "not recorded" rather than guessing),
and the sections Context, Decision, Alternatives, Consequences; an optional
Verification section may follow. Lint rule L5 fails when a file and this index
disagree on Status. Record a decision
when it changes a system boundary, storage ownership, a cross-cutting policy or a
supported topology, not for small edits. Superseded records stay as history and are
never edited to match current code; write a new ADR instead.
