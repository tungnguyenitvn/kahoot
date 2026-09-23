# Transactions

Live commands open no PostgreSQL transaction. The Lua script is the Redis atomic
boundary for one room: it checks authority, phase, deadline, duplicate receipt,
correct order and the ZSET score in one execution. Atomic means no interleaving with
other commands; it is not rollback. A runtime error after a write leaves earlier
writes in place, so the script stays short and validates key types before any write.

Redis runs with noeviction for a separate reason: under memory pressure it must
reject writes with an error rather than silently evict room or session keys. Both a
Lua runtime error and a rejected write surface to the caller as a storage error, not
as a domain result; see the [Redis room contract](../contracts/redis-room.md) and
[quality and risks](../architecture/quality-and-risks.md).

The event Stream is archived separately. ArchiveTransactions.apply locks game_room,
requires version = archived_version + 1, and writes the event and projection in one
transaction; a replayed older event becomes a no-op. The Stream ACK and terminal key
cleanup run in one Lua script after the transaction commits. HTTP responses and
WebSocket sends are outside every transaction.
