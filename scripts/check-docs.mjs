import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Documentation lint. Rule modes: 'fail' breaks the gate, 'warn' only prints, 'off' skips.
// Flip a rule to 'fail' only when the tree is clean for it; docs/development.md lists the rules.
const MODE = { links: 'fail', wire: 'fail', imports: 'fail', frontend: 'fail', L1: 'fail', L2: 'fail', L3: 'fail', L4: 'fail', L5: 'fail' };

// CHECK_DOCS_ROOT points the lint at another tree; scripts/check-docs.test.mjs uses it for fixture trees.
const root = process.env.CHECK_DOCS_ROOT ? path.resolve(process.env.CHECK_DOCS_ROOT) : path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const docsRoot = path.join(root, 'docs');
const failures = [], warnings = [];
const report = (rule, message) => { if (MODE[rule] === 'off') return; (MODE[rule] === 'fail' ? failures : warnings).push(`[${rule}] ${message}`); };
const rel = file => path.relative(root, file);
const read = file => fs.readFileSync(file, 'utf8');
const walk = (dir, keep = f => f.endsWith('.md')) => !fs.existsSync(dir) ? [] : fs.readdirSync(dir, { withFileTypes: true })
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
const extra = ['README.md', 'CONTRIBUTING.md', 'AGENTS.md', 'backend/AGENTS.md', 'frontend/AGENTS.md', 'e2e/AGENTS.md'].map(f => path.join(root, f)).filter(fs.existsSync);
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

// L2: acceptance IDs (PREFIX-NN bullets in docs/features and docs/domain.md) are defined exactly once and retired IDs are never redefined.
const definitions = new Map();
for (const file of [...walk(path.join(docsRoot, 'features')), path.join(docsRoot, 'domain.md')].filter(fs.existsSync)) {
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

// L3: every defined ID has a row in the development.md traceability table; covered rows must be greppable in test sources.
const rows = new Map();
const testingDoc = path.join(docsRoot, 'development.md');
if (fs.existsSync(testingDoc)) {
  const section = read(testingDoc).split(/^#{2,3} Traceability\b.*$/m)[1] ?? '';
  for (const m of section.matchAll(/^\|\s*([A-Z]{2,}-\d{2})\s*\|\s*([^|]*?)\s*\|/gm)) rows.set(m[1], m[2]);
}
const testSources = [['backend/src/test'], ['backend/src/integrationTest'], ['frontend/tests'], ['frontend/src/app', f => f.endsWith('.spec.ts')], ['scripts/test-room.lua']]
  .map(([p, keep]) => [path.join(root, p), keep ?? (() => true)]).filter(([p]) => fs.existsSync(p))
  .flatMap(([p, keep]) => fs.statSync(p).isDirectory() ? walk(p, keep) : [p]).map(read).join('\n');
for (const id of definitions.keys()) {
  const row = rows.get(id);
  if (row === undefined) { report('L3', `${id}: no row in the development.md traceability table`); continue; }
  if (!/NOT COVERED/.test(row) && !testSources.includes(id)) report('L3', `${id}: marked covered but no test source mentions it`);
}
for (const id of rows.keys()) if (!definitions.has(id)) report('L3', `${id}: traceability row for an undefined or retired ID`);

// L4: a numeric limit appears only in its owner document; link text is exempt. Owners follow the precedence in docs/README.md.
// Word boundaries are Unicode-aware so Vietnamese words such as "ký tự" terminate a match.
const literal = (source, flags = '') => new RegExp(`(?<![\\p{L}\\p{N}])(?:${source})(?![\\p{L}\\p{N}])`, 'u' + flags);
const DOMAIN = ['docs/domain.md'], LIMITS = ['docs/architecture/README.md'];
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

// Documented package directions. This checks imports, not reflection or SQL ownership. A package that is not in the
// map fails, so a new module registers in docs/architecture/backend.md and here before it enters the gate; the root
// package holds the composition root and is not a module.
const allowed = { bootstrap: ['identity', 'catalog', 'shared'], identity: ['shared'], catalog: ['identity', 'shared'], gameplay: ['identity', 'catalog', 'shared'], archive: ['identity', 'shared'], shared: [] };
const source = path.join(root, 'backend/src/main/java/dev/sample/quiz');
const LAYERS = ['api', 'application', 'domain', 'infrastructure'];
const PRIVATE_TO = { api: ['infrastructure'], application: ['api', 'infrastructure'], domain: ['api', 'application', 'infrastructure'], infrastructure: ['api'] };
const unknown = new Set();
for (const file of walk(source, f => f.endsWith('.java'))) {
  const parts = path.relative(source, file).split(path.sep); if (parts.length === 1) continue;
  const own = parts[0]; if (!allowed[own]) { unknown.add(own); continue; }
  // Layers (docs/architecture/backend.md): api and infrastructure are private to their module; inside a module
  // api -> application -> domain and infrastructure -> application, domain; domain imports the JDK and domain types only.
  const layer = parts.length > 2 && LAYERS.includes(parts[1]) ? parts[1] : null;
  for (const match of read(file).matchAll(/import\s+(?:static\s+)?([\w.]+)\s*;/g)) {
    const target = match[1];
    if (layer === 'domain' && !/^java\./.test(target) && !/^dev\.sample\.quiz\.\w+\.domain\./.test(target)) report('imports', `${rel(file)}: domain imports ${target}; a domain type depends on the JDK and other domain types only`);
    const module = /^dev\.sample\.quiz\.(\w+)\.(?:(\w+)\.)?/.exec(target); if (!module) continue;
    const [, other, sub] = module;
    if (other !== own && !allowed[own].includes(other)) report('imports', `${rel(file)}: forbidden module dependency ${other}`);
    if (other !== own && (sub === 'api' || sub === 'infrastructure')) report('imports', `${rel(file)}: private layer ${other}.${sub}; another module imports only application and domain`);
    if (other === own && layer && PRIVATE_TO[layer]?.includes(sub)) report('imports', `${rel(file)}: ${layer} imports ${own}.${sub}; the direction is api -> application -> domain, infrastructure -> application and domain`);
  }
}
for (const own of unknown) report('imports', `${rel(path.join(source, own))}: package ${own} is not in the module matrix; register it in docs/architecture/backend.md and in the allowed map`);

// Frontend folders (docs/architecture/frontend.md): features -> core and shared; core -> shared; shared -> nothing in the
// application; features never import each other or the app root; a policy module (.mjs) imports only other .mjs files.
const appRoot = path.join(root, 'frontend/src/app');
const zone = file => {
  const parts = path.relative(appRoot, file).split(path.sep);
  return parts[0] === 'features' ? `features/${parts[1]}` : parts.length > 1 ? parts[0] : 'app';
};
const allowedZones = { app: null, core: ['core', 'shared'], shared: ['shared'] };
for (const file of walk(appRoot, f => /\.(ts|mts|mjs)$/.test(f) && !f.endsWith('.spec.ts'))) {
  const own = zone(file), text = stripCode(read(file));
  for (const match of text.matchAll(/^\s*(?:import|export)\b[^;]*?\bfrom\s+['"]([^'"]+)['"]/gm)) {
    const target = match[1];
    if (file.endsWith('.mjs')) {
      if (!(target.startsWith('.') && target.endsWith('.mjs'))) report('frontend', `${rel(file)}: policy module imports ${target}; a policy module imports only other .mjs modules, never Angular, the DOM or a TypeScript file`);
      continue;
    }
    if (!target.startsWith('.')) continue;
    const resolved = path.resolve(path.dirname(file), target);
    if (!resolved.startsWith(appRoot)) continue;
    const other = zone(resolved.endsWith('.ts') || resolved.endsWith('.mjs') || resolved.endsWith('.mts') ? resolved : resolved + '.ts');
    if (other === own) continue;
    const allowedFrom = own.startsWith('features/') ? ['core', 'shared'] : allowedZones[own];
    if (allowedFrom === null) continue;
    if (!allowedFrom.includes(other)) {
      const label = other === 'app' ? path.basename(resolved) + (resolved.endsWith('.ts') ? '' : '.ts') : other;
      report('frontend', `${rel(file)}: ${own} imports ${label}; features import only core and shared, core imports only shared, shared imports nothing in the application`);
    }
  }
}

for (const warning of warnings) console.warn('WARN ' + warning);
if (failures.length) { console.error(failures.join('\n')); process.exitCode = 1; }
else console.log(`PASS ${files.length} documents, ${linkCount} local links/anchors, wire-example boundary, Java import directions, frontend folder boundaries; rules ${Object.entries(MODE).map(([k, v]) => k + '=' + v).join(' ')}; ${warnings.length} warning(s)`);
