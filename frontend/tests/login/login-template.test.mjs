import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Static guard: the login screen must not ship the seed account (LOGIN-05); README and .env.example own it.
const source = fs.readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), '../../src/app/features/login/login.page.ts'), 'utf8');
test('LOGIN-05 login template embeds no demo credentials', () => {
  for (const secret of ['local-quiz-only', 'host@example.test', 'Tài khoản demo']) assert.equal(source.includes(secret), false, secret);
});
