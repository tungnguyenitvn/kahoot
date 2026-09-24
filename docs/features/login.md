# Feature: host login

## Goal

A host signs in with a server-side session to author quizzes, open rooms and read the
history. A guest never logs in to play.

## Entry point and navigation

- Route: `/login`.
- The app shell shows the **Đăng nhập** link while there is no host identity.
- A successful login redirects to `/host`.
- The host logs out through the header action; the server clears the session and the
  app redirects to `/`.

## Main flow

1. The frontend calls `GET /api/auth/csrf` to receive the CSRF cookie.
2. The host enters a username or email and a password.
3. The frontend sends `POST /api/auth/login` with the form fields and the CSRF header.
4. The backend creates a Spring Security session; the frontend calls `GET /api/auth/me`
   again.
5. The route guard admits `/host` when `identity.host === true`.

## UI state

| State | Behavior |
|---|---|
| Idle | Username/password form, submit enabled when valid |
| Submitting | Button locked, `Đang đăng nhập…` shown |
| Invalid credentials | `INVALID_CREDENTIALS` shown; never reveals whether the account exists |
| Network/server error | Keep the username, clear the password, allow a retry |
| Authenticated | Clear the password from UI memory and navigate to `/host` |

## Security rules

- The session cookie belongs to the browser; it is never copied into localStorage or a URL.
- The CSRF token is never logged or rendered in the UI.
- `Auth.ready()` must be re-runnable after a reload to restore the identity.
- The host guard does not replace backend authorization; every catalog and history
  command still checks the role server-side.
- The screen shows no demo credential; the seeded account is described only in the
  README and `.env.example`.

## Acceptance criteria

- LOGIN-01: After a reload the host is still recognized through the session cookie.
- LOGIN-02: A guest requesting `/host` is redirected to `/login`.
- LOGIN-03: Logout returns `204`, clears the session and removes host rights from the
  next request.
- LOGIN-04: A failed login keeps the route and every entered value except the password.
- LOGIN-05: The login template embeds no demo credential.

## Contracts

See the [REST API identity contract](../contracts/rest-api.md#identity-and-security) and
the [engineering conventions](../development.md#conventions).
