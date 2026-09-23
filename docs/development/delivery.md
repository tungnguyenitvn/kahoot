# Delivery pipeline

What happens to a change after it leaves the developer's hands: which workflow runs
on which trigger, which checks must be green before a merge, how a release is cut and
how a hotfix ships. The developer-facing side (context, task brief, change lifecycle)
is [workflow](workflow.md); gate scopes and the traceability matrix are
[testing](testing.md); decisions are [ADR 0006](../adr/0006-ci-cd-release-images.md)
and [ADR 0007](../adr/0007-ci-caches-and-verified-artifacts.md).

## Branch model

Trunk-based: `main` is the only long-lived branch and every change reaches it through
a pull request from a short-lived branch (naming in [CONTRIBUTING](../../CONTRIBUTING.md)).
Releases are cut from tags, not from branches, and the same artifacts that passed the
gate are what gets published. No cherry-picking between branches; nothing is pushed
to `main` outside a pull request. Long-lived integration or staging branches are not
used: a staging environment, if one ever exists, receives the same image tag as
production (artifact promotion), not a separate branch.

## Workflows by trigger

| Trigger | Workflow and job | Runs | Publishes |
|---|---|---|---|
| Pull request, push to `main`, manual | `ci` / `verify gate` | scripts/verify with the Gradle and npm caches restored: Lua smoke, backend unit and integration tests plus bootJar, documentation lint, Node tests, Angular build | Workflow artifacts `verification-reports` and `build-artifacts` (boot jar, Angular bundle) |
| Same run, after `verify gate` | `ci` / `release images from verified artifacts` | scripts/smoke-release on the downloaded artifacts: package the release images, boot compose.release.yaml, check the SPA, the API proxy and CSRF | Nothing |
| Tag `v*`, manual | `release` / `verify, package, smoke, publish` | scripts/verify on the tagged tree, scripts/smoke-release on its artifacts, push to GHCR, GitHub Release | `ghcr.io/<owner>/kahoot-backend` and `kahoot-frontend` with the tag and `latest`, artifact `release-jar`, a GitHub Release carrying the jar |

A commit that only rewrites the [verification status](../verification/README.md)
after a run carries `[skip ci]` so it does not start another run.

## Required checks before a merge

Both `ci` jobs, `verify gate` and `release images from verified artifacts`, must be
green before a merge. The ruleset `main` (Settings, Rules, Rulesets) enforces it:
changes reach `main` through a pull request, both status checks must pass, force
pushes and branch deletion are blocked, and repository admins stay on the bypass list
so a broken pipeline can never lock the owner out; a bypassed push is recorded by
GitHub and should be the exception. Merge methods merge, squash and rebase are all
allowed. No review count is required yet; add one in the ruleset once the project has
more than one maintainer. The ruleset id and its state are recorded in the
[verification status](../verification/README.md).

## Cutting a release

1. Confirm `main` is green in `ci` and that the [verification status](../verification/README.md)
   cites the revision you release.
2. Tag from `main` with a semantic version and push the tag:

   ```bash
   git tag v0.2.0 && git push origin v0.2.0
   ```

3. The `release` workflow verifies the tagged tree again, packages and smokes the
   images, pushes them and creates the GitHub Release. Record the run in the
   verification status in the same change that bumps the version.

Version numbers follow the backend `version` in build.gradle.kts and the frontend
package.json; bump both in the pull request that prepares the release.

## Hotfix

- Normal case: fix forward. Branch `fix/<topic>` from `main`, write the failing
  regression test first, merge through a pull request, then tag the next patch
  version from `main`.
- `main` holds unreleased work that must not ship: branch `hotfix/<topic>` from the
  last release tag, open the pull request into `main` as usual, and cut the patch tag
  from the hotfix branch head once its `ci` run is green. The tag's `release` run
  verifies exactly that tree. Merge the hotfix branch into `main` so the fix is not
  lost; never cherry-pick it.

## Secrets and environment values

Workflows use only `GITHUB_TOKEN`; no repository secret exists today. The release
stack takes `DB_PASSWORD`, `DEMO_PASSWORD`, `PUBLIC_ORIGIN` and `COOKIE_SECURE` from
the operator's environment or a `.env` file that is never committed (`.env.example`
holds development-only values). A future deployment credential (SSH key, registry
token) belongs in GitHub Actions secrets or a GitHub Environment, never in the tree,
and its name is added here when it exists.

## When a deployment target exists

Add a `deploy` job that runs after the `release` job on the same tag, bound to a
GitHub Environment (`staging`, then `production`) with required reviewers. It deploys
the images already published for that tag, either by `docker compose pull` on the
host or, where the host cannot reach the registry, by `docker save` in the workflow,
transfer over SSH and `docker load` on the host, followed by
`docker compose -f compose.release.yaml up -d` with the host's `.env`. Promotion from
staging to production is the same tag deployed to the next environment, never a
rebuild and never a branch merge. TLS termination, secure cookies, backups and
monitoring remain prerequisites listed in [deployment](../architecture/deployment.md).
