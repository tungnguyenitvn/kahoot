# E2E scope

Playwright against the published stack, run by hand before a release through e2e/run;
never added to scripts/verify or CI (owner decision: the gate stays fast, the browser
check stays a release step). Selectors use roles and visible text; the UI strings are
the ones in docs/features. Credentials come from the environment (E2E_HOST_PASSWORD),
never from the tree. A failure here is reported in the gate status row "Published
stack in a browser" with the revision and the images used, not fixed by loosening
the check.
