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

Releases are semantic versions on annotated tags `vX.Y.Z`; the GitHub Release notes
are generated from the merged pull requests since the previous tag, so pull request
titles are the changelog.

1. Prepare the version in the last pull request before the tag: `version` in
   backend/build.gradle.kts, and `version` in frontend/package.json together with the
   two `version` fields of frontend/package-lock.json (running
   `npm version --no-git-tag-version X.Y.Z` inside frontend/ updates both JSON files).
   Bump the patch number for fixes, the minor number for features.
2. Confirm the merge commit on `main` is green in `ci` and that its message does not
   contain `[skip ci]` (see below).
3. Tag that commit and push the tag:

   ```bash
   git tag -a v0.2.2 -m "v0.2.2: what this release changes" && git push origin v0.2.2
   ```

4. Watch the `release` workflow (Actions, workflow `release`); it takes a few minutes.
   Green means: the tagged tree passed scripts/verify again, the images passed
   scripts/smoke-release, `ghcr.io/<owner>/kahoot-backend` and `kahoot-frontend`
   carry the new tag and `latest`, and the GitHub Release exists with
   `quiz-room-vX.Y.Z.jar` attached.
5. Record the run in the [verification status](../verification/README.md): result,
   tag, revision, environment, run URL. That commit carries `[skip ci]`.
6. Try the published stack once before announcing it: pull the images by tag, start
   compose.release.yaml as described in [deployment](../architecture/deployment.md),
   and open a room in a browser. The gate proves the images boot and route; a browser
   session is the only check of the UI today.

If the tag push started no `release` run, the tagged commit's message carries
`[skip ci]`: GitHub applies the marker to the push event of a tag as well, and to
pull request events, and it matches the marker anywhere in the message, including the
body. Never write the marker in prose inside a commit message. Either tag a commit
without the marker or start the workflow on the existing tag by hand; the dispatched
run still sees a tag ref and publishes the same way:

```bash
gh workflow run release.yml --ref v0.2.2
```

Rollback is a redeploy of the previous tag: every version stays on GHCR, only
`latest` moves. Delete a GitHub Release only when its images were never used. Images
are built on the `ubuntu-latest` runner and are linux/amd64 only; Apple Silicon and
other arm64 hosts run them under emulation until a multi-architecture build exists.

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
