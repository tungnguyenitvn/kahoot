# Documentation map

Start here, not by loading every document. This repository is a single-instance
sample, not a production reference implementation.

## One question, one document

| Question | Canonical document | Do not put here |
|---|---|---|
| What does the product do, for whom, and what is out of scope? | [Scope and non-goals](architecture/README.md#scope-and-non-goals) | Screen behavior |
| What does the server enforce whatever the UI does: terms, rules, state machine, invariants? | [Domain](domain.md) | Screens, wire shapes, values that are not game rules |
| What does the user see on a screen, and which acceptance IDs prove it? | [Features](features/README.md) | Server rules (link to the domain), schemas |
| How is the system built: parts, dependencies, data ownership, execution, limits? | [Architecture](architecture/README.md) | Wire schemas, game rules, rationale |
| How is one side structured, and how does each module work inside? | [Backend](architecture/backend.md), [Frontend](architecture/frontend.md) | The other side's internals |
| What crosses a boundary? | [Contracts](#contracts) | Rationale, screen behavior |
| Why this trade-off? | [ADR index](adr/README.md) | Current task progress |
| How do humans and AI change this: conventions, lifecycle, gates, traceability, delivery, gate status? | [Development](development.md); step by step in [CONTRIBUTING](../CONTRIBUTING.md) | Product behavior |
| How to run and recover? | [Operations](operations.md) | Architecture |

Numeric values have one owner each: game rules in the [domain](domain.md),
wire-visible validation bounds in the contract that carries them, every other value
in the [limits table](architecture/README.md#limits-and-timings). Link to the owner
instead of repeating a value; lint rule L4 fails on a repeat.

## Placement test

Before writing a sentence, ask in this order; the first yes decides the document.

1. Does the user see it? Features.
2. Does the server enforce it whatever the UI does? Domain.
3. Is it the shape of data crossing a boundary? Contracts.
4. Is it who calls whom, who owns which data, how work executes? Architecture.
5. Is it how one module works inside, or what happens when it fails? The module's
   section in backend or frontend.
6. Is it why this way was chosen? ADR.
7. Is it a number? Its owner, see above.

A sentence that passes two doors is written at the wrong level. "The host sees Start
only when a player is present" is a feature; "start requires one active participant"
is the domain; the feature links to the domain instead of restating it.

Architecture grows with structural complexity, not feature count. A module section
becomes its own page only when its responsibility splits; a feature may span screens
and a screen may contain several features.

## Contracts

Hand-maintained Markdown, not generated OpenAPI or AsyncAPI; implementation and
tests are reviewed with every contract change. Wire examples live only here.

| Boundary | Canonical definition |
|---|---|
| HTTP endpoints, errors, idempotency, accepted deviations from HTTP semantics | [REST API](contracts/rest-api.md) |
| Public snapshot and receipt shared by HTTP and WebSocket | [Room state](contracts/room-state.md) |
| WebSocket messages, delivery, close codes | [WebSocket](contracts/websocket.md) |
| Private Redis keys, Lua command interface, event envelope | [Redis room](contracts/redis-room.md) |

## Status and authority

- Feature/domain/contracts define accepted requirements, not proof of verification.
- Architecture describes the intended checked-in design. Where the tree differs, the
  page says so in a Known gaps table.
- ADRs record decisions; superseded decisions are history, not current instructions.
- Code shows actual behavior; tests provide scoped evidence, not universal proof.
- A contradiction is a defect to review, never permission to silently rewrite the
  requirement to match a bug. Accepted contract-level deviations are listed in the
  [REST contract](contracts/rest-api.md#known-deviations) until code changes.
- Acceptance criteria carry stable IDs (LIVE-03, ROOM-03, ANSWER-04 ...); one behavior
  has one ID and retired IDs are never reused. Tests, issues and PRs reference IDs,
  not paraphrased criteria.

For a change, record Draft → Accepted → Implemented → Verified in the issue/PR.
Verification includes commands, environment and revision/artifact. Do not label
the whole project verified because one smoke test passes.

## Language policy

English is canonical for the domain, architecture, contracts, ADR, development and
operations documents and for the AGENTS files. Vietnamese is used for README.md and
docs/features. Never mix languages inside one document; UI strings, error codes,
identifiers and acceptance IDs are quoted verbatim.
