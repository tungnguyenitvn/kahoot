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
scripts/verify runs the Lua smoke, backend tests/build and frontend tests/build, with
cleanup.

## Release images and stack

The release images package what the verify gate built, they compile nothing:
backend/Dockerfile.release puts backend/build/libs/quiz-room.jar on a JRE running as a
non-root user; frontend/Dockerfile.release serves frontend/dist/quiz-room-ui/browser
from nginx, which proxies /api and /ws to the backend service
(frontend/nginx.release.conf). compose.release.yaml runs both images with PostgreSQL and
Redis on one host: DB_PASSWORD and DEMO_PASSWORD have no default, PUBLIC_ORIGIN must be
the browser-facing origin because the WebSocket Origin check uses it, and the demo seed
remains the only account provisioning. scripts/smoke-release packages the artifacts
(run ./scripts/verify first, or set BUILD_ARTIFACTS=1 to build them without tests),
boots the stack and checks that the SPA, the API proxy and CSRF enforcement answer; CI
runs it after the verify job on every push and pull request, and the release workflow
runs it before publishing images to GHCR ([delivery pipeline](../development/delivery.md),
[ADR 0006](../adr/0006-ci-cd-release-images.md),
[ADR 0007](../adr/0007-ci-caches-and-verified-artifacts.md)).

Still not implemented for production: TLS termination in front of nginx and the
secure-cookie flag behind it, idle timeouts, secret management, resource budgets,
backup/restore exercises and operational monitoring. Publishing an image is delivery,
not deployment; do not expose the development server as a production web server.

Redis Cluster is NOT supported: although room keys share a hash tag, registration and archive
cleanup touch a global active-room key in the same scripts. Multiple backend
instances also need notification fan-out and archive/provisioning coordination.
See [runbook](../operations/runbook.md).
