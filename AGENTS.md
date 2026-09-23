# Working agreement for humans and AI

## Scope and authority

Read this file, README.md and docs/README.md first. Then load only the relevant
feature, domain, contract, module and ADR. Nested instructions add local rules.
Review/diagnosis is read-only unless the user asks to implement. Do not deploy,
publish, change credentials or run destructive data operations without task authority.
Preserve single backend + Redis standalone scope.

## Commands, language and numbers

Full gate: ./scripts/verify (Docker). Backend only: ./scripts/test. Documentation:
node scripts/check-docs.mjs. Offline: node --test frontend/tests/*.test.mjs and
texlua scripts/test-room.lua . Follow the language policy in docs/README.md.
Numeric limits and timings have one canonical table linked from docs/README.md;
link to it instead of repeating values.

## Change protocol

- Bug: reproduce → failing regression test → fix → rerun. If the required toolchain
  is unavailable, add the test but state that red/green was not executed.
- Feature: agree scope/non-goals/acceptance → update spec → tests → implementation.
- Refactor: preserve contracts or explicitly document the accepted change.
- Architectural decision: record rationale/trade-offs in ADR, not every small edit.
- Contradictory spec/code: report the conflict; never silently weaken requirements.
- Keep current specs in docs; task plans/progress in issue/PR, not docs/changes.

## Non-negotiable invariants

Session identities are credentials. Do not expose/log credentials or pre-reveal
correct answers/live points. Redis time and accepted command order decide score.
Atomic room commands are not Java read-then-write sequences or per-session locks.
REST mutates; WS sends privacy-filtered full snapshots. No DB read during answer.

## Definition of done

Meet scoped acceptance criteria, run relevant checks and scripts/verify where
available, update affected canonical docs, and report residual risks and unrun checks.
A test file is not proof that it compiled or ran. Never call a sample production-ready
or claim capacity/latency without measurements. Evidence names environment/revision.
