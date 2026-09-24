# ADR 0003: layered documentation and evidence

Status: Superseded by [ADR 0010](0010-compact-documentation-map.md).
Date: 2026-09-23.

## Context

Architecture, UI details, contracts and implementation claims had been mixed.
Feature count must not make backend architecture grow into a feature manual.
AI needs task-specific context and explicit authority, not the entire repository
loaded into every prompt.

## Decision

Use product → architecture → module → feature/contracts as linked views.
Keep schemas canonical in contracts, rationale in ADRs, current requirements in
feature/domain docs, and change plans/evidence in issues/PRs. docs/README is the
routing index. AGENTS contains workflow/authority rules, not duplicated design.

A review request is read-only. Implementation requires task scope; production
actions and destructive migrations require separate authorization. Report a
spec/code conflict before silently changing intent.

## Alternatives

Keeping one mixed document per area, or adopting a mandatory spec-kit layout.
Neither was chosen: the first loads unrelated context into every task, the second
adds structure that trivial changes do not need.

## Consequences

More small purposeful documents; fewer duplicate claims. Links and scoped evidence
need maintenance. No fixed page count, no mandatory spec-kit dependency, no
requirement to create a design file for trivial changes.
