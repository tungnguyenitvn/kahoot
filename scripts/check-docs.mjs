import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Documentation lint. Rule modes: 'fail' breaks the gate, 'warn' only prints, 'off' skips.
// Flip a rule to 'fail' only when the tree is clean for it; docs/development/testing.md lists the rules.
const MODE = { links: 'fail', wire: 'fail', imports: 'fail', L1: 'fail', L2: 'fail', L3: 'fail', L4: 'fail', L5: 'warn' };

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const docsRoot = path.join(root, 'docs');
const failures = [], warnings = [];
const report = (rule, message) => { if (MODE[rule] === 'off') return; (MODE[rule] === 'fail' ? failures : warnings).push(`[${rule}] ${message}`); };
const rel = file => path.relative(root, file);
const read = file => fs.readFileSync(file, 'utf8');
const walk = (dir, keep = f => f.endsWith('.md')) => fs.readdirSync(dir, { withFileTypes: true })
  .flatMap(e => { const p = path.join(dir, e.name); return e.isDirectory() ? walk(p, keep) : keep(p) ? [p] : []; });
const stripCode = text => text.replace(/(^```[\s\S]*?^```|^~~~[\s\S]*?^~~~)/gm, '');
const stripLinks = text => text.replace(/\[[^\]\n]+\]\([^)\s]+\)/g, '');

function anchors(file) {
  const found = new Set(), counts = new Map();
  for (const heading of stripCode(read(file)).matchAll(/^#{1,6}\s+(.+)$/gm)) {
    const slug = heading[1].toLowerCase().replace(/[^\p{L}\p{N}_\s-]/gu, '').trim().replace(/\s/g, '-');
    const count = counts.get(slug) ?? 0; found.add(count ? slug + '-' + count : slug); counts.set(slug, count + 1);
  }
  return found;
}
function links(file) {
  const out = [];
  for (const match of read(file).matchAll(/\[[^\]\n]+\]\(([^)\s]+)\)/g)) {
    const url = match[1]; if (/^[a-z]+:/i.test(url)) continue;
    const [relative, fragment] = url.split('#');
    out.push({ url, fragment, target: relative ? path.resolve(path.dirname(file), decodeURIComponent(relative)) : file });
  }
  return out;
}

// Files under lint: docs/, root documents, the AGENTS files and GitHub templates (L6).
const docs = walk(docsRoot);
const extra = ['README.md', 'CONTRIBUTING.md', 'AGENTS.md', 'backend/AGENTS.md', 'frontend/AGENTS.md'].map(f => path.join(root, f)).filter(fs.existsSync);
const github = fs.existsSync(path.join(root, '.github')) ? walk(path.join(root, '.github')) : [];
const files = [...docs, ...extra, ...github];

// Links, anchors and the architecture wire-example boundary.
let linkCount = 0;
for (const file of files) {
  for (const { url, target, fragment } of links(file)) {
    linkCount++;
    if (!fs.existsSync(target)) report('links', `${rel(file)}: missing ${url}`);
    else if (fragment && target.endsWith('.md') && !anchors(target).has(decodeURIComponent(fragment))) report('links', `${rel(file)}: missing anchor ${url}`);
  }
  if (file.includes(path.sep + 'architecture' + path.sep) && /\{"type"\s*:/.test(read(file))) report('wire', `${rel(file)}: wire examples belong in contracts`);
}

// L1: every document under docs/ is reachable from docs/README.md or from the README.md of its own directory.
const indexTargets = new Set(links(path.join(docsRoot, 'README.md')).map(l => l.target));
for (const file of docs) {
  if (file === path.join(docsRoot, 'README.md')) continue;
  const dirIndex = path.join(path.dirname(file), 'README.md');
  const viaDirectory = dirIndex !== file && fs.existsSync(dirIndex) && links(dirIndex).some(l => l.target === file);
  if (!indexTargets.has(file) && !viaDirectory) report('L1', `${rel(file)}: not linked from docs/README.md or its directory README.md`);
}

// L2: acceptance IDs (PREFIX-NN bullets in docs/features and docs/domain) are defined exactly once and retired IDs are never redefined.
const definitions = new Map();
for (const file of [...walk(path.join(docsRoot, 'features')), ...walk(path.join(docsRoot, 'domain'))]) {
  stripCode(read(file)).split('\n').forEach((line, i) => {
    const m = /^\s*-\s+([A-Z]{2,}-\d{2}):/.exec(line);
    if (m) definitions.set(m[1], [...(definitions.get(m[1]) ?? []), `${rel(file)}:${i + 1}`]);
  });
}
const retired = new Map();
const featuresIndex = path.join(docsRoot, 'features', 'README.md');
if (fs.existsSync(featuresIndex)) for (const m of read(featuresIndex).matchAll(/^\|\s*([A-Z]{2,}-\d{2})\s*\|\s*([^|]*?)\s*\|/gm)) retired.set(m[1], m[2]);
for (const [id, sites] of definitions) {
  if (sites.length > 1) report('L2', `${id} defined ${sites.length} times: ${sites.join(', ')}`);
  if (retired.has(id)) report('L2', `${id} is retired (${retired.get(id)}) but still defined at ${sites[0]}`);
}

// L3: every defined ID has a row in the testing.md traceability table; covered rows must be greppable in test sources.
const rows = new Map();
const testingDoc = path.join(docsRoot, 'development', 'testing.md');
if (fs.existsSync(testingDoc)) {
  const section = read(testingDoc).split(/^## Traceability\b.*$/m)[1] ?? '';
  for (const m of section.matchAll(/^\|\s*([A-Z]{2,}-\d{2})\s*\|\s*([^|]*?)\s*\|/gm)) rows.set(m[1], m[2]);
}
const testSources = ['backend/src/test', 'backend/src/integrationTest', 'frontend/tests', 'scripts/test-room.lua']
  .map(p => path.join(root, p)).filter(fs.existsSync)
  .flatMap(p => fs.statSync(p).isDirectory() ? walk(p, () => true) : [p]).map(read).join('\n');
for (const id of definitions.keys()) {
  const row = rows.get(id);
  if (row === undefined) { report('L3', `${id}: no row in the testing.md traceability table`); continue; }
  if (!/NOT COVERED/.test(row) && !testSources.includes(id)) report('L3', `${id}: marked covered but no test source mentions it`);
}
for (const id of rows.keys()) if (!definitions.has(id)) report('L3', `${id}: traceability row for an undefined or retired ID`);

// L4: a numeric limit appears only in its owner document; link text is exempt. Owners follow the precedence in docs/README.md.
// Word boundaries are Unicode-aware so Vietnamese words such as "ký tự" terminate a match.
const literal = (source, flags = '') => new RegExp(`(?<![\\p{L}\\p{N}])(?:${source})(?![\\p{L}\\p{N}])`, 'u' + flags);
const DOMAIN = ['docs/domain/game.md'], LIMITS = ['docs/architecture/quality-and-risks.md'];
const LITERALS = [
  { pattern: literal('1024'), owners: ['docs/contracts/websocket.md'] },
  { pattern: literal('top 10', 'i'), owners: LIMITS },
  { pattern: literal('5\\s*[–-]\\s*120'), owners: DOMAIN },
  { pattern: literal('(?:tối đa|maximum|up to) 20', 'i'), owners: DOMAIN },
  { pattern: literal('24 (?:ký tự|characters)', 'i'), owners: ['docs/contracts/rest-api.md'] },
  { pattern: literal('100 (?:participants|memberships|người chơi)', 'i'), owners: DOMAIN },
  { pattern: literal('2 (?:hours|giờ)', 'i'), owners: DOMAIN },
  { pattern: literal('2 h'), owners: LIMITS },
  { pattern: literal('24 h|24-hour|24 hours', 'i'), owners: LIMITS },
  { pattern: literal('5 (?:s|seconds|giây)', 'i'), owners: LIMITS },
  { pattern: literal('250 ms'), owners: LIMITS },
  { pattern: literal('10 s|10 seconds|10 giây', 'i'), owners: LIMITS },
  { pattern: literal('1,000 connections|150 per room|150/room', 'i'), owners: LIMITS },
  { pattern: literal('500 entries|500 (?:lệnh|commands)', 'i'), owners: LIMITS },
  { pattern: literal('100 active rooms', 'i'), owners: LIMITS },
  { pattern: literal('256 MB', 'i'), owners: LIMITS },
];
for (const file of [...docs, ...extra]) {
  const text = stripLinks(stripCode(read(file)));
  for (const { pattern, owners } of LITERALS) {
    if (owners.includes(rel(file))) continue;
    const m = pattern.exec(text);
    if (m) report('L4', `${rel(file)}: "${m[0]}" is owned by ${owners.join(' or ')}; link instead of repeating the value`);
  }
}

// L5: every ADR file has an index row in docs/adr/README.md whose Status matches the file's Status line.
const adrDir = path.join(docsRoot, 'adr');
const adrIndex = fs.existsSync(path.join(adrDir, 'README.md')) ? read(path.join(adrDir, 'README.md')) : '';
const statusKey = text => { const m = /(Proposed|Accepted|Deprecated|Superseded)(?:\s+by\D*?(\d{4}))?/.exec(text || ''); return m ? (m[2] ? `${m[1]} by ${m[2]}` : m[1]) : ''; };
for (const file of walk(adrDir)) {
  const number = /^(\d{4})-/.exec(path.basename(file))?.[1]; if (!number) continue;
  const fileStatus = statusKey(/^Status:\s*(.+)$/m.exec(read(file))?.[1]);
  const row = new RegExp(`^\\|\\s*\\[${number}\\][^\\n]*$`, 'm').exec(adrIndex)?.[0];
  if (!row) { report('L5', `${rel(file)}: no row in docs/adr/README.md`); continue; }
  const cells = row.split('|').map(c => c.trim()).filter(Boolean);
  if (!fileStatus) report('L5', `${rel(file)}: missing or unrecognized Status line`);
  else if (statusKey(cells[2]) !== fileStatus) report('L5', `${rel(file)}: Status "${fileStatus}" differs from index "${cells[2]}"`);
}

// Documented package directions. This checks imports, not reflection or SQL ownership.
const allowed = { bootstrap: ['identity', 'catalog', 'shared'], identity: ['shared'], catalog: ['identity', 'shared'], gameplay: ['identity', 'catalog', 'shared'], archive: ['identity', 'gameplay', 'shared'], shared: [] };
const source = path.join(root, 'backend/src/main/java/dev/sample/quiz');
for (const file of walk(source, f => f.endsWith('.java'))) {
  const own = path.relative(source, file).split(path.sep)[0]; if (!allowed[own]) continue;
  for (const match of read(file).matchAll(/import\s+dev\.sample\.quiz\.(\w+)\./g)) {
    if (match[1] !== own && !allowed[own].includes(match[1])) report('imports', `${rel(file)}: forbidden module dependency ${match[1]}`);
  }
}

for (const warning of warnings) console.warn('WARN ' + warning);
if (failures.length) { console.error(failures.join('\n')); process.exitCode = 1; }
else console.log(`PASS ${files.length} documents, ${linkCount} local links/anchors, wire-example boundary, Java import directions; rules ${Object.entries(MODE).map(([k, v]) => k + '=' + v).join(' ')}; ${warnings.length} warning(s)`);
