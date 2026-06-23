import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const scriptsDir = path.resolve(process.cwd(), 'scripts')
const allowedPrefixes = new Set(['env', 'global', 'local', 'private', 'script', 'using'])
const errors = []

for (const fileName of fs.readdirSync(scriptsDir).filter((name) => name.endsWith('.ps1'))) {
  const filePath = path.join(scriptsDir, fileName)
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)

  lines.forEach((line, index) => {
    // PowerShell treats `$Name:` as a drive-qualified variable. Ordinary variables
    // followed by a colon must be written as `${Name}:` inside double-quoted strings.
    const strings = line.match(/"(?:`.|[^"`])*"/g) ?? []
    for (const value of strings) {
      for (const match of value.matchAll(/\$([A-Za-z_][A-Za-z0-9_]*):/g)) {
        if (!allowedPrefixes.has(match[1].toLowerCase())) {
          errors.push(`${fileName}:${index + 1}: 非法变量插值 ${match[0]}，请改用 \${${match[1]}}:`)
        }
      }
    }

    if (/\(&\s+java\s+-version\s+2>&1/.test(line)) {
      errors.push(
        `${fileName}:${index + 1}: Windows PowerShell 会把 java -version 的 stderr 当作 NativeCommandError，请通过 cmd 合并输出`,
      )
    }
  })
}

if (errors.length > 0) {
  throw new Error(errors.join('\n'))
}

console.log('windows powershell interpolation ok')
