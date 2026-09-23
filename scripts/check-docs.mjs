import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const failures=[];
function walk(dir) {
  return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>{
    const p=path.join(dir,e.name);return e.isDirectory()?walk(p):p.endsWith('.md')?[p]:[];
  });
}
function anchors(file) {
  const found=new Set(), counts=new Map();
  const text=fs.readFileSync(file,'utf8').replace(/(^```[\s\S]*?^```|^~~~[\s\S]*?^~~~)/gm,'');
  for(const heading of text.matchAll(/^#{1,6}\s+(.+)$/gm)) {
    const slug=heading[1].toLowerCase().replace(/[^\p{L}\p{N}_\s-]/gu,'').trim().replace(/\s/g,'-');
    const count=counts.get(slug)??0;found.add(count?slug+'-'+count:slug);counts.set(slug,count+1);
  }
  return found;
}
const files=[...walk(path.join(root,'docs')),path.join(root,'README.md'),path.join(root,'CONTRIBUTING.md')];
let links=0;
for(const file of files) {
  const text=fs.readFileSync(file,'utf8');
  for(const match of text.matchAll(/\[[^\]\n]+\]\(([^)\s]+)\)/g)) {
    const url=match[1];if(/^[a-z]+:/i.test(url))continue;
    const [relative,fragment]=url.split('#');
    const target=relative?path.resolve(path.dirname(file),decodeURIComponent(relative)):file;
    links++;
    if(!fs.existsSync(target))failures.push(path.relative(root,file)+': missing '+url);
    else if(fragment && target.endsWith('.md') && !anchors(target).has(decodeURIComponent(fragment)))
      failures.push(path.relative(root,file)+': missing anchor '+url);
  }
  if(file.includes(path.sep+'architecture'+path.sep) && /\{"type"\s*:/.test(text))
    failures.push(path.relative(root,file)+': wire examples belong in contracts');
}
// Documented package directions. This checks imports, not reflection or SQL ownership.
const allowed={bootstrap:['identity','catalog','shared'],identity:['shared'],catalog:['identity','shared'],gameplay:['identity','catalog','shared'],archive:['identity','gameplay','shared'],shared:[]};
const source=path.join(root,'backend/src/main/java/dev/sample/quiz');
function javaFiles(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>{const p=path.join(dir,e.name);return e.isDirectory()?javaFiles(p):p.endsWith('.java')?[p]:[];});}
for(const file of javaFiles(source)) {
  const own=path.relative(source,file).split(path.sep)[0];if(!allowed[own])continue;
  for(const match of fs.readFileSync(file,'utf8').matchAll(/import\s+dev\.sample\.quiz\.(\w+)\./g)) {
    if(match[1]!==own && !allowed[own].includes(match[1]))failures.push(path.relative(root,file)+': forbidden module dependency '+match[1]);
  }
}
if(failures.length){console.error(failures.join('\n'));process.exitCode=1;}
else console.log(`PASS ${files.length} documents, ${links} local links/anchors, architecture wire-example boundary and Java import directions`);
