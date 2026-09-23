# Archive design

One scheduled platform-thread worker runs on a fixed delay AFTER the previous pass
(value in the [limits table](../architecture/quality-and-risks.md#limits-and-timings)).
Each active room has its own Stream and postgres consumer group; the stable
consumer name is single-instance. This is not a global one-second persistence SLA.

## Processing and failure order

1. Drain this consumer's pending entries before reading new entries for that room.
2. Lock game_room in a PostgreSQL transaction.
3. Ignore version <= archived_version; require exactly the next version otherwise.
4. Write event and participant/answer/phase projection, then commit.
5. XACK only after commit. Crash before ACK replays a no-op SQL effect.
6. On FINISHED, one Redis script ACKs, starts the retention TTL and removes active entry.

This gives idempotent database effects under at-least-once delivery, not universal
exactly-once execution. The terminal event can only commit after preceding versions.

## Boundaries and limitations

Archive never recomputes score from UI input and cannot invent lost Redis events.
History queries only expose host-owned rooms and completed results.
A sequence gap blocks that room; errors are logged and later retried.
No poison-event quarantine or multi-consumer claiming is implemented.

Retention starts after final SQL commit, not merely after UI FINISHED. A long DB
outage retains backlog and consumes Redis memory. Cleanup combines global ACTIVE
with room keys, so this path is Redis-standalone only.

Schema: [Redis events](../contracts/redis-room.md);
SQL: [migration](../../backend/src/main/resources/db/migration/V1__schema.sql).
