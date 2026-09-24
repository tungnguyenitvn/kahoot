# ADR 0012: Java 25 LTS and Gradle 9 now

Status: Accepted.
Date: 2026-09-24.

## Context

[ADR 0011](0011-java-24-until-the-next-toolchain-change.md) kept Java 24 pinned until
the next change that touched the toolchain anyway, on the ground that the sample gained
nothing from an upgrade without a functional reason. The owner decided the same day
that the open risk (a feature release without updates behind every image) is reason
enough for a sample that is published as images on GHCR and read as a reference.
Running Gradle on Java 25 needs Gradle 9, so the wrapper moves with it.

## Decision

The build, both Dockerfiles and the documentation pin Java 25, the current
long-term-support release: `JavaLanguageVersion.of(25)` in `backend/build.gradle.kts`,
`eclipse-temurin:25-jdk` for development and test, `eclipse-temurin:25-jre` for the
release image. The Gradle wrapper moves to 9.7.1, the current release, because Gradle
8.14 cannot run on Java 25. Nothing else changes: no language feature of 25 is used,
no dependency is bumped, and the full gate is the proof. This supersedes ADR 0011; the
risk row it pointed to closes.

## Alternatives

- Wait for the next toolchain change (ADR 0011): superseded by the owner's decision.
- Java 25 on Gradle 8.14.3 with a JDK 21 runtime for Gradle and a toolchain download
  for 25: rejected; two JDKs in the image and a network download at build time for one
  version pin.
- Gradle 9.1, the first release that supports Java 25: rejected in favour of the
  current release; the build file uses no API that changed between them, and the gate
  decides.

## Consequences

The base images receive updates again. `backend/AGENTS.md` names Java 25 as the fixed
version; the next move is the next LTS, with the same trigger rule as before: a
toolchain change or a functional need, recorded in an ADR. Contributors' local Gradle
distribution is re-downloaded once into `.cache/gradle` by the wrapper; CI's cache key
changes with the build files and warms on the first run.
