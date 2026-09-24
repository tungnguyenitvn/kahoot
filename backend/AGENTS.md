# Backend scope

Read root AGENTS plus docs/architecture/backend.md and the affected module section.
Java 24 / Gradle / Spring Boot versions stay fixed unless the task authorizes changes.
Tests: src/test for isolated policies; src/integrationTest for real services.
Use scripts/test and scripts/verify with isolated Compose, never dev volumes.
Preserve Lua score/privacy/receipt invariants. No network or DB work inside Lua.
Do not add a new microservice/framework to solve a sample-local problem.
