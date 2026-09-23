# Identity and catalog

## Identity

Identity.current resolves host principal or existing guestId from Spring Session.
Identity.ensure bootstraps a guest identity when necessary; PIN and display name
are not credentials. Guest session IDs must never appear in public room state.

SecurityConfig owns CSRF, form login/logout and HTTP filter authorization.
REST room handlers and the WS handshake perform room authorization after the
filter. Catalog/history require HOST role. Ws transport does not replace CSRF for
HTTP mutations. Logout/session invalidation is rechecked before snapshot sending.

## Catalog

Host ownership is derived from identity, never client ownerId. QuizCatalog validates
a draft with bounded title/questions/options/duration, stores JSONB in PostgreSQL,
and publishes status. Publish may be repeated; published content has no edit API.

Creating a room freezes quiz content with generated round IDs. Catalog is not
consulted during answers. Draft POST has no idempotency key, unlike room creation.

Feature behavior: [login](../features/login.md), [studio](../features/host-studio.md).
Wire schema and validation limits: [REST](../contracts/rest-api.md).
