# Frontend scope

Read root AGENTS plus docs/architecture/frontend.md and relevant feature/contracts.
Keep Angular standalone/Signals patterns. Components delegate transport to store
and connection adapter. Do not use localStorage for credentials.
Run npm test AND npm run build; Node tests do not compile Angular templates.
Any transport change needs initial-failure, close/retry, terminal access and dispose
tests. Label missing browser E2E coverage rather than claiming it exists.
