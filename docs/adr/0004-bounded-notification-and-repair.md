# ADR 0004: bounded notification and provisioning retry repair

Status: Accepted.
Date: 2026-09-23.

## Context

Per-answer per-client task submission amplified Redis snapshot work. Initial
browser load failure could prevent connection recovery. SQL failure after Redis
init could incorrectly remove a usable room from the timer/archive registry.

## Decision

Coalesce dirty snapshots per connection, limit global in-flight work before virtual
thread submission, and retain periodic full-state reconciliation. Keep scoring in
the synchronous Lua command. Extract a framework-independent browser connection
lifecycle so failure transitions can be tested without a browser.

Register rooms after init; retry repairs existing live registration and SQL phase.
Never remove a live registration merely because the following SQL update failed.
Do not automatically rebuild lost room state from incomplete SQL projections.

Boot HTTP virtual threads are explicitly enabled; the custom scheduled worker pool
remains platform based. Runtime behavior must still be verified by integration tests.

## Alternatives

A broker/actor ownership model would expand scope; deferred. Room-wide public
projection caching could reduce reads further but needs careful private-overlay
versioning; deferred. A durable provisioning saga is stronger for abandoned retries,
but not required for this bounded sample.

## Consequences

Coalescing can skip intermediate versions and add notification delay; full snapshots
make that acceptable, answer receipts remain independent. Limits are protective
settings, not latency benchmarks; their values are canonical in the
[limits table](../architecture/quality-and-risks.md#limits-and-timings).

## Verification

Regression tests for burst coalescing, expired sessions, provisioning retry and
browser initial-failure/reconnect/disposal. See [evidence at revision 91658f5](https://github.com/tungnguyenitvn/kahoot/blob/91658f5733c2bd611ca52d452fa6e94675f180a3/docs/verification/refactor-review.md).
