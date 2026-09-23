# Contributing

Use a short-lived branch (`feature/...` or `fix/...`) and a focused PR. Reference the issue, update the current feature/domain document when behavior changes, run `./scripts/verify`, and describe the checks in the PR. Require passing CI and review before merge. Commit subjects follow conventional commits with the module as scope: `feat(gameplay): add a quiz round`, `fix(realtime): pace reconnect after capacity refusal`, `docs: ...`, `chore(ci): ...`; scopes are identity, catalog, gameplay, archive, realtime, frontend, docs and ci.

The day-to-day sequence lives in [workflow](docs/development/workflow.md); local setup and the test commands are in the [README](README.md).
