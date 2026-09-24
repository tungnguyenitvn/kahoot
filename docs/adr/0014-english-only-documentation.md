# ADR 0014: English for every document

Status: Accepted.
Date: 2026-09-24.

## Context

The documentation map's language policy split the tree by audience: English for the
domain, architecture, contracts, ADRs, development, operations and the AGENTS files;
Vietnamese for README.md and docs/features. In practice the two halves are read
together. A feature document links to the domain invariant it depends on, the
traceability table quotes the acceptance criteria that feature documents define, and
an agent working on one task loads a Vietnamese feature page next to an English
contract and answers in a mix of both. The same concept carried two names ("Mục
tiêu" and "Goal", "Cổng" and "gate"), which a search cannot bridge, and the review
of this repository against the AI-native SDLC playbook listed the split as a
synchronization problem rather than a translation one.

The application's user interface is Vietnamese. That is a product decision about the
players and hosts, not a documentation decision, and UI strings appear in the
documents and in the tests exactly as the screen shows them.

## Decision

Every document in the repository is English: README.md, CONTRIBUTING.md, everything
under docs/, the AGENTS files and the GitHub templates. UI strings, error codes,
identifiers and acceptance IDs are quoted verbatim in the language they carry, never
translated; the feature documents keep the Vietnamese labels the screens show, such as
**Mở phòng** or `Đang đăng nhập…`, because tests and the browser check select by
those strings. The language policy in the documentation map is rewritten to say so;
README.md and the six feature documents are translated in the same change, and the
Vietnamese unit words in lint rule L4 are dropped because no document uses them.

## Alternatives

- Keep the split and add a glossary: rejected; the glossary is a third document to
  keep in sync and does not help a search or an agent that loads both halves.
- Bilingual documents, each section in both languages: rejected; two copies of every
  requirement drift, and the lint cannot tell which one is the requirement.
- Vietnamese for everything: rejected; the contracts, the ADRs and the AGENTS files
  are read by tools and agents whose instructions and the codebase's identifiers are
  English, and the domain vocabulary (receipt, reveal, round, snapshot) is already
  English in the code.

## Consequences

One language to search and to lint. A contributor who writes in Vietnamese in the
issue or the pull request discussion is fine; the documents and the acceptance
criteria they cite are English. The UI language stays a product decision; changing
it would change the strings the documents quote, not the policy. The translation is
one pull request with no behavior change; the acceptance IDs and their tests are
unchanged, which the L3 rule confirms.
