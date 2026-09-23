# Human + AI workflow

[Documentation map](../README.md) defines which artifact owns which information.

## Smallest sufficient context

Read root/nested AGENTS, relevant feature, module, contract and tests. Architecture
is the navigation map. Do not require every feature document for every task.
For answer retry: domain + live-room + gameplay/realtime + receipt
contract + store/Lua tests. Login/catalog content is not required unless impacted.

## Change lifecycle

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

## Task brief template

- Goal / acceptance IDs:
- Non-goals:
- Required context links:
- Allowed files/modules and contract impact:
- Verification commands and required environment:
- Open questions / stop conditions:

No separate planning document is required for a trivial fix. Never duplicate issue
history under docs/changes. No automatic commits, PRs or deployment from a request
that only asks to review files.

## Completion checklist

- Acceptance criteria and negative paths checked.
- No private data leak, scoring/idempotency regression or unintended scope change.
- Contract, feature and implementation agree; breaking changes are explicit.
- Relevant test/build gate executed, or blocker clearly reported.
- Links and documentation boundary checker pass.
- Review records remaining risk; release is a separate authorized action.
