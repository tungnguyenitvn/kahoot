// Self-test of the pull request shape check. The cases use the repository's own template so a
// template change that breaks the check is caught in the gate, not on the next pull request.
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { checkPullRequest, parseTemplate } from './check-pr.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const script = path.join(root, 'scripts/check-pr.mjs');
const template = fs.readFileSync(path.join(root, '.github/pull_request_template.md'), 'utf8');

const good = `## Related issue / ADR
No issue; ADR 0007.

## Goal / non-goals / acceptance IDs
Goal: the check exists. Non-goals: none. Acceptance IDs: none.

## Required context and affected boundaries
Feature / module / contracts / ADR links:
docs/development.md#delivery. No boundary touched.

## What changed
A script and a workflow.

## Behavior and docs
- [ ] Current feature/domain document updated if behavior changed
- [x] No behavior change

## Verification
- [x] \`./scripts/verify\` passed
- [ ] Other relevant checks (describe below)

Revision abc1234, macOS, Docker Desktop.

## Change status
Implemented; Verified once CI is green.

## Notes / unverified areas
None.
`;

test('template: the headings and the boilerplate come from the file', () => {
  const { headings, boilerplate } = parseTemplate(template);
  assert.equal(headings.length, 8);
  assert.ok(headings[0].startsWith('## Related issue'));
  assert.ok(boilerplate.has('- [ ] No behavior change'));
});

test('a filled template with a conventional title passes', () => {
  assert.deepEqual(checkPullRequest('chore(ci): check the pull request shape', good, template), []);
});

test('title: a non-conventional subject, a bad type and a trailing period fail', () => {
  assert.match(checkPullRequest('Update stuff', good, template).join('\n'), /not a conventional commit subject/);
  assert.match(checkPullRequest('feature(ci): add check', good, template).join('\n'), /not a conventional commit subject/);
  assert.match(checkPullRequest('fix(ci): add check.', good, template).join('\n'), /ends with a period/);
  assert.deepEqual(checkPullRequest('feat(gameplay)!: breaking round change', good, template), []);
});

test('body: an untouched template fails on every section', () => {
  const failures = checkPullRequest('docs: x', template, template);
  assert.ok(failures.some(f => /"## What changed" is empty or still the template text/.test(f)), failures.join('\n'));
  assert.ok(failures.some(f => /tick the box that is true/.test(f)));
  assert.ok(failures.some(f => /tick a check that ran, or write NOT RUN/.test(f)));
  assert.ok(failures.some(f => /"Change status": name one of/.test(f)));
});

test('body: a missing section, an empty body and a comment-only section fail', () => {
  assert.match(checkPullRequest('docs: x', good.replace('## Notes / unverified areas\nNone.\n', ''), template).join('\n'), /missing section "## Notes/);
  assert.match(checkPullRequest('docs: x', '', template).join('\n'), /missing section "## Related issue/);
  assert.match(checkPullRequest('docs: x', good.replace('A script and a workflow.', '<!-- later -->'), template).join('\n'), /"## What changed" is empty/);
});

test('body: NOT RUN satisfies verification when no box is ticked', () => {
  const body = good.replace('- [x] `./scripts/verify` passed', '- [ ] `./scripts/verify` passed').replace('Revision abc1234', 'NOT RUN: no Docker on this machine. Revision abc1234');
  assert.deepEqual(checkPullRequest('docs: x', body, template), []);
});

test('body: sections out of the template order fail', () => {
  const swapped = good.replace('## What changed\nA script and a workflow.\n\n', '').replace('## Notes / unverified areas\nNone.\n', '## Notes / unverified areas\nNone.\n\n## What changed\nA script and a workflow.\n');
  assert.match(checkPullRequest('docs: x', swapped, template).join('\n'), /not in the template order/);
});

test('cli: reads PR_TITLE and PR_BODY and exits 1 on a failure', () => {
  const pass = spawnSync(process.execPath, [script], { env: { ...process.env, PR_TITLE: 'docs: x', PR_BODY: good }, encoding: 'utf8' });
  assert.equal(pass.status, 0, pass.stdout + pass.stderr);
  assert.match(pass.stdout, /^PASS/);
  const fail = spawnSync(process.execPath, [script], { env: { ...process.env, PR_TITLE: 'bad title', PR_BODY: '' }, encoding: 'utf8' });
  assert.equal(fail.status, 1);
  assert.match(fail.stderr, /FAIL title/);
});
