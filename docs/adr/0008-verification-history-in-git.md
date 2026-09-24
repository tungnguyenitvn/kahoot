# ADR 0008: verification history lives in git, not in docs

Status: Superseded by [ADR 0010](0010-compact-documentation-map.md).
Date: 2026-09-24.

## Context

[ADR 0005](0005-evidence-location.md) split evidence between the PR of a change and a
status page, and kept historical records under docs/verification/ as immutable files.
It rejected dropping them because the repository had no remote. It now has one: every
change reaches main through a pull request on GitHub, and the status page links the CI
runs. The only historical record, refactor-review.md, describes an archive snapshot of
2026-09-23 whose BLOCKED and NOT RUN rows the status page has since replaced. Files
under docs/ are what humans and agents load as current context, so a record that is
true only for an old revision competes with the status page and adds noise to every
task that follows its links.

## Decision

Per-change evidence (commands, results, environment, revision) belongs in the PR that
carries the change. docs/verification/README.md is the single status page: one row per
gate with the latest result, revision, environment, date and run reference, rewritten
in place whenever a gate is re-run. A change's evidence may be linked from the status
page, never duplicated into it. docs/verification/ keeps no historical records; what a
replaced row said is found through the PR or run it cited and through git history. A
decision record that cited a removed file links to it at a fixed revision on GitHub.
This supersedes ADR 0005 in full.

## Alternatives

Keeping immutable records under docs/verification/ (ADR 0005; rejected: the remote now
holds the history and the in-tree copy reads as current). Moving them to an archive
directory (rejected: still in the tree, still found by search, still needs link
maintenance). Folding their results into the status page (rejected: duplicates evidence
the status page must only link).

## Consequences

Three places with distinct roles: the PR for a change's evidence, the status page for
the current state, git history for everything replaced. The PR template keeps its
evidence block. refactor-review.md is removed; ADR 0004 now links to it at revision
91658f5, which changes where its cited evidence is found, not what it decided. Evidence
from before the remote existed is reachable only through git history; offline, read it
with `git show 91658f5:docs/verification/refactor-review.md`.
