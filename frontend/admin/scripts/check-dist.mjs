// 构建产物自检：在 vite build 之后运行，拦截坏产物进入部署。
// 1) 递归搜索 dist，命中 "Program Files" / "Program%20Files" 即失败
//    （Git Bash MSYS 路径转换把以 / 开头的构建参数污染成 C:/Program Files/Git/... 的特征串）；
// 2) 校验 dist/index.html 的资源引用必须是 EXPECTED_BASE + assets/ 形态。
// EXPECTED_BASE 需与 vite.config.ts 的 base 保持一致。
import { readdir, readFile } from 'node:fs/promises'
import { join, resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const EXPECTED_BASE = '/petshop-admin-7x9k2/'
const FORBIDDEN = ['Program Files', 'Program%20Files']

// 可选参数：指定待检目录（默认 dist），供验证自检本身时复用
const distDir = process.argv[2]
  ? resolve(process.argv[2])
  : resolve(dirname(fileURLToPath(import.meta.url)), '../dist')

async function* walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const p = join(dir, entry.name)
    if (entry.isDirectory()) yield* walk(p)
    else yield p
  }
}

const hits = []
for await (const file of walk(distDir)) {
  const buf = await readFile(file)
  for (const needle of FORBIDDEN) {
    if (buf.includes(Buffer.from(needle, 'latin1'))) {
      hits.push(`${file} → "${needle}"`)
    }
  }
}

if (hits.length > 0) {
  console.error('[check-dist] 构建产物被污染，禁止部署！')
  console.error('[check-dist] 污染特征：MSYS 把以 / 开头的构建传参转换成了 C:/Program Files/Git/...')
  for (const h of hits) console.error('  - ' + h)
  console.error('[check-dist] 修复方式：base 只能写死在 vite.config.ts，禁止用 VITE_BASE_URL 环境变量或 --base 传参。')
  process.exit(1)
}

const indexHtml = await readFile(join(distDir, 'index.html'), 'utf8')
if (!indexHtml.includes(EXPECTED_BASE + 'assets/')) {
  console.error('[check-dist] dist/index.html 资源引用不是 ' + EXPECTED_BASE + 'assets/... 形态，base 不正确，禁止部署！')
  process.exit(1)
}

console.log(`[check-dist] 自检通过：无污染特征串，index.html 资源引用为 ${EXPECTED_BASE}assets/...`)
