# Documentation map

Start here, not by loading every document. This repository is a single-instance
sample, not a production reference implementation.

## Document ownership

| Question | Canonical document | Do not duplicate here |
|---|---|---|
| Why and for whom? | [Product scope](product/scope.md) | Implementation recipes |
| System boundaries and dependencies? | [Architecture](architecture/overview.md) | Endpoint schemas, screen-by-screen flows |
| How does a module work? | [Module index](modules/README.md) | Other modules' internals |
| What does the user experience? | [Features](features/README.md) | Shared wire schemas |
| What crosses a boundary? | [Contracts](contracts/README.md) | Architectural rationale |
| How is the backend structured? | [Backend architecture](architecture/backend.md) | Module algorithms and failure paths |
| How is the frontend structured? | [Frontend architecture](architecture/frontend.md) | Screen behavior |
| Where does it run? | [Deployment](architecture/deployment.md) | Recovery steps |
| Which coding conventions apply? | [Conventions](development/conventions.md) | Architecture rules, schemas |
| Which gates exist and which acceptance ID has evidence? | [Testing and traceability](development/testing.md) | Executed results |
| Which game rules and state transitions apply? | [Domain rules](domain/game.md) | Screen behavior, wire shapes |
| Which numeric limits and timings apply? | Game rules in [domain](domain/game.md), wire-visible bounds in [contracts](contracts/README.md), every other value in the [limits table](architecture/quality-and-risks.md#limits-and-timings) | The same values in other documents |
| Why this trade-off? | [ADR index](adr/README.md) | Current task progress |
| How do humans/AI change this safely? | [Workflow](development/workflow.md) | Product behavior |
| What was actually checked? | [Verification record](verification/refactor-review.md) | Claims inferred from test names |
| How to run and recover? | [Runbook](operations/runbook.md) | Feature specifications |

Architecture grows with structural complexity, not feature count. Add module
detail only when it earns its maintenance cost. A feature may span screens;
a screen may contain several features.

## Status and authority

- Feature/domain/contracts define accepted requirements, not proof of verification.
- Architecture/module docs describe the intended checked-in design. Unsupported
  behavior and known implementation gaps must be explicitly linked.
- ADRs record decisions; superseded decisions are history, not current instructions.
- Code shows actual behavior; tests provide scoped evidence, not universal proof.
- A contradiction is a defect to review, never permission to silently rewrite the
  requirement to match a bug. Accepted contract-level deviations are listed in the
  [contracts index](contracts/README.md#known-deviations) until code changes.
- Acceptance criteria carry stable IDs (LIVE-03, ROOM-03, ANSWER-04 ...); one behavior
  has one ID and retired IDs are never reused. Tests, issues and PRs reference IDs,
  not paraphrased criteria.

For a change, record Draft → Accepted → Implemented → Verified in the issue/PR.
Verification includes commands, environment and revision/artifact. Do not label
the whole project verified because one smoke test passes.

## Language policy

English is canonical for architecture, contracts, domain, modules, ADRs,
development, operations, product and verification documents and for AGENTS files.
Vietnamese is used for README.md and docs/features. Never mix languages inside one document;
UI strings, error codes, identifiers and acceptance IDs are quoted verbatim.
