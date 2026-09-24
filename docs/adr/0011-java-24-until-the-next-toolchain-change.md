# ADR 0011: Java 24 stays pinned until the next toolchain change; the target is the LTS

Status: Superseded by [ADR 0012](0012-java-25-lts-and-gradle-9.md).
Date: 2026-09-24.

## Context

The build, both Dockerfiles and the documentation pin Java 24, a feature release
without long-term support: its public updates stopped when Java 25, the current
long-term-support release, shipped in September 2025. The quality register listed
this as a residual risk with no recorded decision, so every reader had to guess
whether the pin was deliberate. The sample runs one container image built from a
pinned base; nothing in it depends on a Java 24 feature that 25 removed, and nothing
in it depends on a 25 feature either.

## Decision

Java 24 stays pinned until the next change that touches the toolchain anyway: a
Spring Boot upgrade, a base-image refresh, or the first change that needs a newer
language feature. That change moves the sample to Java 25, the current LTS, in one
pull request that edits `backend/build.gradle.kts` (toolchain), `backend/Dockerfile`,
`backend/Dockerfile.release`, the scope statement in the architecture page and
`backend/AGENTS.md`, reruns the full gate and records the run in the gate status.
No separate upgrade pull request is opened for the version alone: the sample gains
nothing from it today, and a toolchain move without a functional reason is a change
with cost and no acceptance criterion.

## Alternatives

- Move to Java 25 now: rejected for now, as above; it becomes the decision the moment
  a toolchain change is needed.
- Track the latest feature release (Java 26 when it ships): rejected; the point of
  recording a target is to stop chasing feature releases, and an LTS gives a base
  image with updates for years.
- Leave the risk row without a decision: rejected; that is what this record replaces.

## Consequences

The risk row in the quality register stays, now pointing here, and closes with the
upgrade. Until then the base image `eclipse-temurin:24-jdk` receives no further
updates, which is acceptable for a local sample and unacceptable for anything
exposed; the deployment page already says no production deployment is implied.
`backend/AGENTS.md` keeps its rule that the Java version stays fixed unless the task
authorizes a change; this record names the authorized change.
