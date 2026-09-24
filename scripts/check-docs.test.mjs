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

test('layers: another module may import only application and domain, never api or infrastructure', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/gameplay/application/X.java`]: java('gameplay.application', ['catalog.infrastructure.JdbcQuizRepository']) }));
  assert.equal(status, 1, out);
  assert.match(out, /\[imports\] .*private layer catalog\.infrastructure/);
});

test('layers: domain imports the JDK and domain types only', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/catalog/domain/X.java`]: 'package dev.sample.quiz.catalog.domain;\nimport org.springframework.stereotype.Service;\nimport dev.sample.quiz.shared.ApiException;\npublic class X {}\n' }));
  assert.equal(status, 1, out);
  assert.match(out, /\[imports\] .*domain imports org\.springframework\.stereotype\.Service/);
  assert.match(out, /\[imports\] .*domain imports dev\.sample\.quiz\.shared\.ApiException/);
});

test('layers: application never depends on its own api or infrastructure', () => {
  const { status, out } = lint(fixture({ [`${javaRoot}/catalog/application/X.java`]: java('catalog.application', ['catalog.infrastructure.JdbcQuizRepository', 'catalog.api.QuizController']) }));
  assert.equal(status, 1, out);
  assert.match(out, /\[imports\] .*application imports catalog\.infrastructure/);
  assert.match(out, /\[imports\] .*application imports catalog\.api/);
});

test('layers: a layered module with the documented directions passes', () => {
  const { status, out } = lint(fixture({
    [`${javaRoot}/catalog/api/C.java`]: java('catalog.api', ['catalog.application.QuizCatalog', 'catalog.domain.Quiz', 'identity.application.Identities']),
    [`${javaRoot}/catalog/application/S.java`]: java('catalog.application', ['catalog.domain.Quiz', 'shared.ApiException']),
    [`${javaRoot}/catalog/domain/Q.java`]: 'package dev.sample.quiz.catalog.domain;\nimport java.util.List;\nimport dev.sample.quiz.identity.domain.Identity;\npublic record Q() {}\n',
    [`${javaRoot}/catalog/infrastructure/R.java`]: java('catalog.infrastructure', ['catalog.application.QuizRepository', 'catalog.domain.Quiz']),
    [`${javaRoot}/gameplay/RoomService.java`]: java('gameplay', ['catalog.application.QuizCatalog', 'catalog.domain.QuizStatus']),
  }));
  assert.equal(status, 0, out);
});
