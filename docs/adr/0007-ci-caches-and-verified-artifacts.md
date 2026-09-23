# ADR 0007: CI caches and release images from verified artifacts

Status: Accepted.
Date: 2026-09-23.

## Context

With [ADR 0006](0006-ci-cd-release-images.md) every push downloaded the Gradle
distribution, the Maven dependencies and the npm packages again, the development
image bootstrapped a Gradle wrapper that is already committed, and the release-image
job compiled the jar and the Angular bundle a second time on another runner. Roughly
two thirds of the runner time went to work whose inputs had not changed.

## Decision

The test compose file bind-mounts `.cache/gradle` and `.cache/npm` into the test
containers instead of anonymous named volumes, so GitHub Actions restores and saves
them with `actions/cache` keyed on the build files and the lockfile; the same
directories serve local runs and are ignored by git. The development image no longer
carries a Gradle distribution or bootstraps a wrapper; the committed wrapper downloads
into the cache. Release images no longer compile anything: `backend/Dockerfile.release`
packages `backend/build/libs/quiz-room.jar` and `frontend/Dockerfile.release` packages
`frontend/dist/quiz-room-ui/browser`, both produced by the verify gate. In `ci` the
`release-images` job depends on `verify` and receives those files as a workflow
artifact; in `release` the same runner verifies, packages, smokes and publishes, so the
shipped jar is byte for byte the tested one.

## Alternatives

Running Gradle and npm directly on the runner with the vendor setup actions
(rejected: the gates would differ between CI and local Docker runs). Docker layer
caching through a registry or the Actions cache backend (deferred: the remaining image
work is base-image pulls of well under a minute, and the setup is fragile). Keeping
self-contained multi-stage release Dockerfiles (rejected: they rebuild what the gate
already built and can drift from it).

## Consequences

`./scripts/smoke-release` needs the artifacts from `./scripts/verify` or
`BUILD_ARTIFACTS=1`, which builds them without running tests. Cache entries are per
operating system and expire after seven days without use, so the first run after a
quiet week pays the full download again. Files written by the containers are owned by
root on Linux runners; the workflows change ownership before the cache is saved.
