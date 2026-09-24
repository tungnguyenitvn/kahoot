// Self-test of the documentation lint against fixture trees. CHECK_DOCS_ROOT points the
// lint at the fixture, so these cases never depend on the repository's own content.
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const script = path.resolve(path.dirname(fileURLToPath(import.meta.url)), 'check-docs.mjs');
const javaRoot = 'backend/src/main/java/dev/sample/quiz';

function fixture(files) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'check-docs-'));
  for (const dir of ['docs/adr', javaRoot]) fs.mkdirSync(path.join(root, dir), { recursive: true });
  fs.writeFileSync(path.join(root, 'docs/README.md'), '# Documentation map\n');
  for (const [relative, content] of Object.entries(files)) {
    const file = path.join(root, relative);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, content);
  }
  return root;
}
function lint(root) {
  const run = spawnSync(process.execPath, [script], { env: { ...process.env, CHECK_DOCS_ROOT: root }, encoding: 'utf8' });
  return { status: run.status, out: run.stdout + run.stderr };
}
const java = (pkg, imports) => `package dev.sample.quiz${pkg ? '.' + pkg : ''};\n${imports.map(i => `import dev.sample.quiz.${i};`).join('\n')}\npublic class X {}\n`;

test('imports: a package outside the module matrix fails until it is registered', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/billing/X.java`]: java('billing', ['shared.ApiException']) }));
  assert.equal(status, 1, out);
  assert.match(out, /\[imports\] .*billing.* not in the module matrix/);
});

test('imports: a registered package with allowed imports passes', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/catalog/X.java`]: java('catalog', ['identity.Identity', 'shared.ApiException']) }));
  assert.equal(status, 0, out);
});

test('imports: a registered package with a forbidden import fails', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/catalog/X.java`]: java('catalog', ['gameplay.RedisRooms']) }));
  assert.equal(status, 1, out);
  assert.match(out, /\[imports\] .*forbidden module dependency gameplay/);
});

test('imports: the root package is the composition root, not a module', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/QuizApplication.java`]: java('', []) }));
  assert.equal(status, 0, out);
});
