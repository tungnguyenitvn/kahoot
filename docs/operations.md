# Local operations

## Start and health

docker compose up runs the dev environment. UI http://localhost:4200;
backend health http://localhost:8080/actuator/health.
[Deployment](architecture/README.md#deployment) explains topology and local-only scope.
Do not expose demo credentials, insecure cookies or the Angular dev server publicly.

## Failure handling

| Symptom | Safe response |
|---|---|
| Answer timeout | Preserve original command/payload, reconcile and retry supported operation |
| Redis unavailable | Fail closed; restore service, then reconcile; do not score from SQL |
| Live meta exists but creation response failed | Retry original create command; registration/SQL phase repair is idempotent |
| ROOM_STATE_LOST / STATE_CORRUPT | Stop affected room; investigate state and archive; no automated reconstruction |
| Archive warning / backlog | Restore PostgreSQL; retain Redis events; inspect archived_version before manual intervention |
| WS disconnected | REST fallback continues; inspect Origin/proxy/security before changing access rules |
| Redis memory pressure | Stop admitting new work, inspect archive backlog; noeviction can reject writes |

Logical room expiry is a [domain rule](domain.md); room keys receive the
retention TTL from the [limits table](architecture/README.md#limits-and-timings)
only AFTER final SQL archive commit. Pending archive must not be deleted to silence errors.
If provisioning was abandoned before active registration, retry/operator action is
needed; no autonomous repair scanner exists.

## Data and recovery limits

Dev volumes: postgres-data, redis-data. docker compose down stops services without
deleting these volumes. The optional command docker compose down -v destroys
dev data; use only when you explicitly intend to discard ALL that project's
database/session/game data and have a needed backup.

Test Compose uses isolated data; scripts/test and scripts/verify clean test containers.
No backup automation, restore RTO or no-loss RPO is promised. Redis AOF everysec
does not ensure every acknowledged answer survives a crash.

Do not log cookies/passwords/private answers. Changing seed environment variables
does not change an existing stored password; seed inserts only absent accounts.
