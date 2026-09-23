# Contributing

Use a short-lived branch and a focused pull request into `main`. Reference the issue, update the current feature/domain document when behavior changes, run `./scripts/verify`, and describe the checks in the PR. Both `ci` jobs must be green and a reviewer must approve before merge; see [delivery](docs/development/delivery.md) for the pipeline, the release and the hotfix procedure.

## Branch naming

`type/short-description` in kebab-case; the type says what the change is and where it branches from.

| Prefix | For | Branch from | PR target |
|---|---|---|---|
| `feature/` | New behavior; needs a feature document update and acceptance IDs | `main` | `main` |
| `fix/` | Bug fix; the failing regression test comes first | `main` | `main` |
| `chore/` | Tooling, CI, dependencies, build | `main` | `main` |
| `docs/` | Documentation only | `main` | `main` |
| `hotfix/` | Urgent fix for a released tag while `main` holds unreleased work | the last release tag | `main`, after the patch tag is cut from the hotfix branch |

Examples: `feature/round-timer-display`, `fix/stale-round-rejection`, `chore/gradle-cache`, `docs/delivery-pipeline`.

## Commits

Commit subjects follow conventional commits with the module as scope: `feat(gameplay): add a quiz round`, `fix(realtime): pace reconnect after capacity refusal`, `docs: ...`, `chore(ci): ...`; scopes are identity, catalog, gameplay, archive, realtime, frontend, docs and ci. One coherent change per commit; do not mix a refactor with a behavior change.

The day-to-day sequence lives in [workflow](docs/development/workflow.md); local setup and the test commands are in the [README](README.md).
