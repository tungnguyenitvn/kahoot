# Deployment boundary

Supported environment: local development/test with Docker Compose. No production
deployment recipe or HA claim is implied.

| Process | Access / dependency | Persistence |
|---|---|---|
| Browser → Angular dev server | localhost:4200 | No client session tokens stored |
| Angular dev proxy → backend | /api and /ws upgrade to backend:8080 | None |
| Backend → Redis | Internal redis:6379 | AOF everysec on dev volume |
| Backend → PostgreSQL | Internal postgres:5432 | Dev volume |

Host-published ports bind loopback. Session cookie belongs to the browser-facing
origin; REST uses CSRF cookie/header, WS validates Origin and session/membership.
Local HTTP uses insecure cookies only for local development.

compose.test.yaml uses isolated PostgreSQL temporary storage and separate Redis.
scripts/verify runs backend tests/build and frontend tests/build, with cleanup.

Production prerequisites (not implemented): TLS/reverse proxy with WS upgrade and
idle timeouts, secure cookies, disabled demo seed, secret management, resource
budgets, backup/restore exercises and operational monitoring. Do not expose the
development server as a production web server.

Redis Cluster is NOT supported: although room keys share a hash tag, registration and archive
cleanup touch a global active-room key in the same scripts. Multiple backend
instances also need notification fan-out and archive/provisioning coordination.
See [runbook](../operations/runbook.md).
