# ADR 0010: compact documentation map

Status: Accepted.
Date: 2026-09-24.

## Context

[ADR 0003](0003-documentation-and-evidence.md) split the documentation into product,
architecture, module, feature, contract, domain, development, operations and
verification views, each in its own directory. The principles held, the layout did
not: four directories held one file each, four README files existed only to route,
and product, module, domain and feature pages overlapped in the reader's mind even
where their content did not. A contributor asking "where does this sentence go" had
five plausible answers, and the last change of a rule had to be checked in three
places. The count reached 38 files for a sample whose whole tree fits one screen.

## Decision

Three kinds of content, two kinds of reference, one file or directory each:

- `docs/domain.md`: what the server enforces whatever the UI does. Unchanged in
  content; it stays separate from features because its IDs are cross-feature, its
  readers are the backend and the tests, and its language is English.
- `docs/features/`: what the user sees per screen, with screen-scoped acceptance IDs.
- `docs/architecture/`: `README.md` for scope and non-goals, the system, deployment,
  limits and risks; `backend.md` and `frontend.md` for one side each, with every
  module design as a section. Product and module directories are folded in.
- `docs/contracts/`: one file per boundary, indexed from the documentation map; the
  accepted deviations move to the REST contract.
- `docs/adr/`: one file per decision, unchanged.
- `docs/development.md`: conventions, workflow, gates and traceability, delivery and
  the gate status; `docs/operations.md`: the runbook.

`docs/README.md` carries a placement test: seven questions asked in order, the first
yes decides the document. The evidence rule of [ADR 0008](0008-verification-history-in-git.md)
stands: per-change evidence in the pull request, one status page rewritten in place,
history in git; the status page is now the Gate status section of development.md.
This supersedes ADR 0003 and ADR 0008 in their layout statements; their principles
are restated above and in the map.

## Alternatives

- Keep the directories and only remove the index files: rejected, the overlap the
  reader feels comes from the number of kinds, not from the index files.
- Merge domain into features: rejected, see the domain entry above; the previous
  attempt is why eight feature IDs were retired into LIVE invariants.
- One architecture page for everything: rejected, it would exceed what a task loads
  and mix the two sides' different shapes (bounded contexts against layers).
- Module design next to the code (`package-info` or a README per package): deferred;
  it may replace the sections in backend.md once the code moves to the layered layout
  of [ADR 0009](0009-layered-modules-and-feature-folders.md).

## Consequences

Twenty-three files outside features and ADRs become eight. Every link in the tree is
rewritten; links inside earlier ADRs are rewritten mechanically because their targets
moved, their decisions are unchanged. The lint keeps its rules with new paths: the
domain file, the limits owner, the traceability table in development.md. The gate
status now changes development.md on every re-run; that diff is one table row. A
section in backend.md is split out when its responsibility splits, not before.
