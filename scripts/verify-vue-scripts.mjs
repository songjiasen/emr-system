import { execFileSync } from 'node:child_process';
import { mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, join, resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const tempDir = mkdtempSync(join(tmpdir(), 'emr-vue-check-'));

function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      return walk(path);
    }
    return entry.isFile() && entry.name.endsWith('.vue') ? [path] : [];
  });
}

try {
  for (const file of walk(join(root, 'frontend'))) {
    const text = readFileSync(file, 'utf8');
    const match = text.match(/<script(?:\s+setup)?[^>]*>([\s\S]*?)<\/script>/);
    if (!match) {
      continue;
    }

    const tempFile = join(tempDir, `${basename(file)}.mjs`);
    writeFileSync(tempFile, match[1]);
    execFileSync('node', ['--check', tempFile], { stdio: 'pipe' });
  }

  console.log('vue script syntax ok');
} finally {
  rmSync(tempDir, { recursive: true, force: true });
}
