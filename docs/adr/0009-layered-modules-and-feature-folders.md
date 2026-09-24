# ADR 0009: layered backend modules and frontend feature folders

Status: Accepted.
Date: 2026-09-24.

## Context

Backend modules are flat packages whose class-name suffix is the only hint of role:
a controller runs SQL, a service talks to two stores directly, archive imports
gameplay for key names and the composition root writes identity's table. The import
checker sees the module segment of an import and nothing below it, so nothing inside
a module is protected. The frontend is one flat folder with a file per screen and one
file for every model; the framework-free policy modules that make the connection
lifecycle testable sit beside Angular files with no rule saying which code must stay
framework-free. Both trees are small enough to reorganize now and large enough that
the next module or feature would copy whatever shape it finds. [ADR 0003](0003-documentation-and-evidence.md)
fixed the documentation layers; the code layout had no recorded decision.

## Decision

Backend: every module is `dev.sample.quiz.<module>` with `api`, `application`,
`domain` and `infrastructure` sub-packages; api → application → domain, and
infrastructure → application and domain. Another module imports only `application`
facades and `domain` public types. The room aggregate stays in Lua and
`gameplay.domain` holds types and store-free checks. Archive derives Redis keys from
the contract instead of importing gameplay; identity exposes an accounts facade for
the seed. Data ownership is one writer per table or key family, with game_room as
the one accepted column-split table. Layout, matrix and ownership tables live in the
[backend architecture](../architecture/backend.md).

Frontend: `core` for singletons, `shared` for models, presentational components and
pure helpers, `features/<name>` for page, route store and policy modules;
features → core and shared, never another feature. Policy that must be testable
without a browser is a plain `.mjs` module with a `.d.mts` declaration inside its
feature. Layout in the [frontend architecture](../architecture/frontend.md).

Migration is per module and per feature, one pull request each, closing the matching
Known gaps row; no big-bang move. The checker map tightens in the same change that
removes an import.

## Alternatives

- Keep flat packages and only fix the couplings: rejected, because the next class
  would again pick a layer by habit and the checker could never protect anything
  below the module segment.
- Hexagonal naming (`domain`, `application`, `adapters.in`, `adapters.out`): the same
  dependency rule with more packages per module; rejected for modules of a handful of
  classes. It can be adopted later by renaming `api` and `infrastructure` without
  changing the rule.
- A Gradle subproject per module, or Spring Modulith, to enforce boundaries at build
  time: rejected because backend/AGENTS forbids a new framework for a sample-local
  problem and the import checker already covers the module edge. Revisit with an ADR
  if the layer rule is violated repeatedly.
- Frontend by technical layer (`pages/`, `stores/`, `transport/`, `models/`): rejected
  because one feature would be spread over several folders and the rule "features
  never import each other" would have no folder to attach to.

## Consequences

More packages and folders for the same number of classes; `catalog` is the reference
shape so a new module does not interpret the rule. The Known gaps tables in both
architecture pages list the current deviations; each is closed by its own change.
Tests mirror the new packages and folders; the frontend gate glob becomes recursive
when tests move. Layer privacy (api and infrastructure) is documented but not
machine-checked until a rule is added; if that rule brings ArchUnit it needs its own
ADR. CONTRIBUTING carries the step lists for a new module and a new feature.
