# ADR 0005: evidence location and verification status

Status: Superseded by [ADR 0008](0008-verification-history-in-git.md).
Date: 2026-09-23.

## Context

[ADR 0003](0003-documentation-and-evidence.md) places change plans and evidence in
issues and PRs, while docs/README.md made docs/verification/refactor-review.md the
canonical answer to "what was actually checked". That record grew into an append-only
log of three sessions in which an early BLOCKED table is contradicted by a later green
run, so a reader who stops halfway draws the wrong conclusion. Evidence also needs a
revision to be meaningful, and ADRs are immutable, so amending 0003 is not an option.

## Decision

Per-change evidence (commands, results, environment, revision) belongs in the PR that
carries the change, as ADR 0003 says. docs/verification/README.md is the single
canonical status page: one row per gate with the latest result, revision, environment,
date and run reference, rewritten in place whenever a gate is re-run. Historical records
under docs/verification/ stay immutable and carry a header pointing to the status page.
A change's evidence may be linked from the status page, never duplicated into it.

## Alternatives

Amending ADR 0003 (rejected: ADRs are history). Keeping the append-only log as the
canonical record (rejected: stale sections mislead). Dropping docs/verification/
entirely in favor of PRs (rejected: the repository has no remote yet and readers need
a status view inside the tree).

## Consequences

Two places with distinct roles instead of one contradictory one: PRs hold the evidence
of a change, the status page holds the current state per gate. The PR template keeps
its evidence block. The status page is updated in the same commit as any gate re-run
and cites the revision whose tree was verified; lint rule L5 keeps the ADR index in
sync with this and every other decision record.
