import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Pull request shape check. The title is a conventional commit subject (it becomes the squash
// commit on main) and the body fills every section of .github/pull_request_template.md. The
// workflow pr-shape.yml runs it on every pull request event with PR_TITLE and PR_BODY in the
// environment; docs/development.md#delivery lists it among the checks before a merge. It checks
// the shape only: whether the evidence in the body is true is the reviewer's job.

const TYPES = ['feat', 'fix', 'refactor', 'test', 'docs', 'chore', 'ci', 'perf', 'build'];
const TITLE = new RegExp(`^(${TYPES.join('|')})(\\([a-z0-9-]+\\))?!?: \\S.*$`);
const STATUSES = ['Draft', 'Accepted', 'Implemented', 'Verified'];

const stripComments = text => text.replace(/<!--[\s\S]*?-->/g, '');
const lines = text => stripComments(text).split(/\r?\n/).map(l => l.trim()).filter(Boolean);

// The template defines the sections (its level-two headings) and the boilerplate (every other
// non-comment line, checkboxes included), so a section counts as filled only by text the author wrote.
export function parseTemplate(template) {
  const headings = [], boilerplate = new Set();
  for (const line of lines(template)) (line.startsWith('## ') ? headings : { push: l => boilerplate.add(l) }).push(line);
  return { headings, boilerplate };
}

export function checkPullRequest(title, body, template) {
  const failures = [];
  const subject = (title ?? '').trim();
  if (!TITLE.test(subject)) failures.push(`title "${subject}" is not a conventional commit subject: type(scope): summary, type one of ${TYPES.join(', ')}`);
  else if (/\.$/.test(subject)) failures.push('title ends with a period');

  const { headings, boilerplate } = parseTemplate(template);
  const sections = new Map();
  let current = null;
  for (const line of lines(body ?? '')) {
    if (line.startsWith('## ')) { current = line; if (!sections.has(line)) sections.set(line, []); continue; }
    if (current) sections.get(current).push(line);
  }
  const order = [...sections.keys()].filter(h => headings.includes(h)).map(h => headings.indexOf(h));
  if (order.some((index, i) => i > 0 && index < order[i - 1])) failures.push('sections are not in the template order');
  for (const heading of headings) {
    const content = sections.get(heading);
    if (!content) { failures.push(`missing section "${heading}"`); continue; }
    const written = content.filter(l => !boilerplate.has(l));
    if (!written.length) failures.push(`section "${heading}" is empty or still the template text`);
    if (/^## Behavior and docs/.test(heading) && !written.some(l => /^- \[x\]/i.test(l))) failures.push('"Behavior and docs": tick the box that is true');
    if (/^## Verification/.test(heading) && !written.some(l => /^- \[x\]/i.test(l) || /\bNOT RUN\b/.test(l))) failures.push('"Verification": tick a check that ran, or write NOT RUN for what did not');
    if (/^## Change status/.test(heading)) {
      const named = STATUSES.filter(s => written.some(l => l.includes(s)));
      if (!named.length) failures.push(`"Change status": name one of ${STATUSES.join(', ')} and justify it`);
    }
  }
  return failures;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
  const template = fs.readFileSync(path.join(root, '.github/pull_request_template.md'), 'utf8');
  const failures = checkPullRequest(process.env.PR_TITLE, process.env.PR_BODY, template);
  if (failures.length) { console.error(failures.map(f => 'FAIL ' + f).join('\n')); process.exitCode = 1; }
  else console.log('PASS pull request title and every template section');
}
