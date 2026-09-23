# Product scope

Host authors a bounded multiple-choice quiz, publishes it, opens a room and
controls rounds. Guests join by PIN/nickname and receive tiered scores based on
the order in which correct answers are accepted by the server.

## Actors and journeys

- Host: login → author/publish quiz → open room → host rounds → view archive.
- Player: join → wait → answer → reveal → final ranking.
- Developer/operator: start isolated development services, verify changes, inspect
  archive failures and stop/recover affected rooms.

## Constraints and non-goals

Java 24, Gradle, Spring Boot, Angular 22, Redis standalone, PostgreSQL and Docker
Compose. One backend instance and one scheduled archive consumer are supported.
Exact score/capacity/deadline rules belong to [domain](../domain/game.md).

No HA, multi-region fairness, offline submission, production SSO, payment,
automatic disaster recovery or full quiz CMS. Network speed is not equalized:
fairness means consistent acceptance order at Redis, not browser click order.

## Quality priorities

Correctness/privacy before notification freshness; bounded notification work;
explicit retry semantics; durable archive only after PostgreSQL commit.
No measured latency/capacity SLO is claimed. See
[quality and risks](../architecture/quality-and-risks.md).
