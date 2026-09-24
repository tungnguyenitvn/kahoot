import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Static guard: without <base href="/"> the bundle is requested relative to a deep route such as
// /room/<id>, nginx answers index.html for that request and a reload of the room shows a blank page (ROOM-04).
const html = fs.readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), '../../src/index.html'), 'utf8');
test('ROOM-04 index.html declares base href "/" so a reload on a deep route loads the bundle', () => {
  assert.match(html, /<base href="\/">/);
});
