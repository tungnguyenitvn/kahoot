# ADR 0013: contracts are reviewed Markdown, not generated schemas

Status: Accepted.
Date: 2026-09-24.

## Context

The four boundary contracts under docs/contracts (REST, room state, WebSocket, Redis
room) are hand-written Markdown, and the [documentation map](../README.md#contracts) has
said so since [ADR 0003](0003-documentation-and-evidence.md) without recording why. An
AI-native process as described in the playbook this repository follows expects
contract-first work: an OpenAPI or AsyncAPI file changed before the code, with mocks,
clients and contract tests generated from it so CI fails when the code drifts. The
question came up in a compliance review of the repository against that process and
deserves a decision rather than a sentence in the map.

The sample has one consumer of every boundary: its own Angular SPA over REST and
WebSocket, its own archive worker over the Redis stream. No public compatibility
guarantee exists ([REST contract](../contracts/rest-api.md)). Two of the four
boundaries cannot be expressed by an HTTP schema at all: the Redis key space and the
Lua command interface, and the WebSocket close codes and delivery rules. The parts of
a contract that decide correctness here are ordering, idempotency, privacy before
reveal and error codes, which a schema describes only as free text.

## Decision

The contracts stay Markdown, one file per boundary, and remain the authority over the
code: a contradiction is a defect to review, never a reason to rewrite the contract
([status and authority](../README.md#status-and-authority)). Their evidence is review
and executed tests, not generation:

- A contract change and the implementation and tests it affects are one pull request;
  the pull request template asks for the affected boundaries, and `.github/CODEOWNERS`
  requests the owner's review on every change under docs/contracts.
- Wire examples live only in the contracts; lint rule `wire` fails an architecture page
  that carries one, so there is no second copy to drift.
- Wire-visible validation bounds are owned by the contract that carries them and
  linked from everywhere else (lint rule L4).
- Behavior that the contract promises is proven by integration tests and the Lua smoke
  named in the [traceability table](../development.md#traceability), by acceptance ID.
- An accepted deviation from HTTP semantics is listed in the
  [REST contract](../contracts/rest-api.md#known-deviations) until the code changes.

What this does not give: CI cannot fail on a drift between the Markdown and the code
by itself; a reviewer reads the contract next to the diff. That is the accepted cost.

The decision is revisited, and an OpenAPI document for REST becomes the source with
generated contract tests, when the first of these happens: a second consumer of the
REST API (another client, a partner, a generated SDK), a public compatibility guarantee
on any endpoint, or a versioned API. AsyncAPI for the WebSocket follows the same
trigger. The Redis room contract stays Markdown in every case; it is a private
protocol between two modules of one process.

## Alternatives

- OpenAPI and AsyncAPI now, hand-written, with contract tests generated from them:
  rejected. With one consumer the schema is a third copy of every endpoint next to the
  Markdown and the code; the generated tests would prove field shapes, which the
  integration tests already exercise, and could not prove ordering, idempotency or
  privacy, which the contracts are about. The Redis and WebSocket rules would still need
  the Markdown.
- OpenAPI generated from the Spring controllers (springdoc) and published as the
  contract: rejected. The code would become the contract, which inverts the authority
  order the map fixes, and a bug in a controller would document itself as the rule.
- Markdown plus a schema check of the JSON examples in the contracts against the
  integration test responses: deferred. It is cheap and stays inside the current
  layout; it is worth adding when a contract page gains more than a handful of
  examples or the first drift is found in review.

## Consequences

No new file format and no generator in the gate. The reviewer's checklist in
[CONTRIBUTING](../../CONTRIBUTING.md) keeps "contracts and documents match the code" as a
human step, and the pull request template keeps its affected-boundaries section. The
map's sentence "hand-maintained Markdown, not generated OpenAPI or AsyncAPI" now
links here. The triggers above are the only conditions under which the format changes;
a change of format is a new ADR that supersedes this one.
