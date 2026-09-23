# ADR 0006: CI/CD on GitHub Actions with release images

Status: Accepted.
Date: 2026-09-23.

## Context

The repository had one workflow that ran scripts/verify and nothing that produced a
runnable artifact; the only images were development images that run Gradle and the
Angular dev server. There is no deployment target, so "CD" can only mean delivering
verified artifacts that an operator can run.

## Decision

Two workflows. `ci` runs on pull requests and pushes to main with two jobs: `verify`
(scripts/verify) and `release-images` (scripts/smoke-release, which builds the
multi-stage release images and boots compose.release.yaml to check the SPA, the API
proxy and CSRF enforcement). `release` runs on `v*` tags: it repeats scripts/verify on
the tagged tree, builds and smokes the same images, pushes them to GHCR with the tag
and `latest`, and creates a GitHub Release with the boot jar. Only GITHUB_TOKEN is
used. The release stack has no TLS and keeps the demo seed as the only account
provisioning; both are documented as operator responsibilities in
[deployment](../architecture/deployment.md).

## Alternatives

Deploying to a host from the workflow (rejected: no target, and a deployment needs
secrets and TLS the sample does not own). Publishing the development images (rejected:
they run build tooling, not the application). Skipping the second verify on tags
(rejected: a tag must never publish an unverified tree). Caching Gradle and npm on the
runner (deferred: named volumes keep the gates identical locally and in CI, and runner
time is acceptable for a sample).

## Consequences

A tag costs two image builds and one full verify. GHCR package visibility is set by
the repository owner, not by the workflow. Image names and the compose variables are
part of the deployment document, so changing them is a documented change. The smoke
proves that the images boot and route; application behavior stays with the verify
gate and the traceability matrix.
