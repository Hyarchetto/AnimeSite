// 用 CDP 驱动无头 Edge 打开一个页面，等它渲染完，然后在页面里跑一段 JS 并把结果打出来
//
// 用法
//   node scripts/browser.mjs <url> <js文件> [等待的选择器]
//
// js 文件里的内容会被当成一个表达式，在页面上下文里求值，可以写 async IIFE。
// 返回值用 JSON 打印到标准输出，所以写验证片段时 return 一个对象最方便。
//
// 不依赖任何 npm 包。Node 22 内置了 WebSocket 和 fetch，CDP 要的两样都够
//
// 用它的理由：光靠 curl 拿 HTML 再 grep 只能证明「模块编译过了」，
// 证明不了「点了按钮真的登录了」。而且 Vue 会插入 scoped 属性、Vue Router
// 会把激活类插在 class 最前面，正则很容易看走眼——这个教训踩过两次

import { spawn, execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { mkdirSync, mkdtempSync, rmSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const EDGE_PATHS = [
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Google/Chrome/Application/chrome.exe'
]

const url = process.argv[2]
const jsFile = process.argv[3]
const waitSelector = process.argv[4]

if (!url || !jsFile) {
  console.error('用法: node scripts/browser.mjs <url> <js文件> [等待的选择器]')
  process.exit(2)
}

const port = 9333 + (process.pid % 500)
// 默认每次用全新 profile，localStorage 是干净的。
// 设了 CDP_PROFILE 就复用同一个目录，需要跨两次启动保留登录态时用它
const reuseProfile = Boolean(process.env.CDP_PROFILE)
const profileDir = reuseProfile ? process.env.CDP_PROFILE : createScratchProfile()

/**
 * 在项目内的 .tmp 下建一个一次性 profile 目录
 *
 * 不建在系统临时目录，%TEMP% 在 C 盘而 Edge 的 profile 有几十 MB，
 * 跑一次留一个，攒到九十多个就是三百多 MB
 */
function createScratchProfile() {
  const root = join(dirname(fileURLToPath(import.meta.url)), '..', '.tmp')
  mkdirSync(root, { recursive: true })
  return mkdtempSync(join(root, 'cdp-'))
}

// 无头模式默认窗口只有 754px 宽，会踩中项目里 max-width: 768px 那条移动端媒体查询。
// 不指定的话测的其实是移动端布局，而项目是照着桌面做的
const windowWidth = process.env.CDP_WIDTH || '1440'
const windowHeight = process.env.CDP_HEIGHT || '900'

const edge = spawn(findEdge(), [
  '--headless=new',
  '--disable-gpu',
  '--no-sandbox',
  '--no-first-run',
  '--no-default-browser-check',
  `--window-size=${windowWidth},${windowHeight}`,
  `--remote-debugging-port=${port}`,
  `--user-data-dir=${profileDir}`,
  'about:blank'
], { stdio: 'ignore' })

let socket
let nextId = 1
const pending = new Map()

function findEdge() {
  for (const p of EDGE_PATHS) {
    try {
      readFileSync(p)
      return p
    } catch {
      // 继续找下一个
    }
  }
  console.error('找不到 Edge 或 Chrome')
  process.exit(2)
}

/** 发一条 CDP 命令并等它的回应 */
function send(method, params = {}) {
  const id = nextId++
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject })
    socket.send(JSON.stringify({ id, method, params }))
    // 页面里那段 JS 是整段等它跑完才回结果的，跨两次登录加多次跳转很容易超过 20 秒。
    // 设短了会出现「脚本其实跑完了、副作用也生效了，但拿不到返回值」这种最难查的情况
    setTimeout(() => {
      if (pending.has(id)) {
        pending.delete(id)
        reject(new Error(`CDP 超时: ${method}`))
      }
    }, 90000)
  })
}

async function waitForTarget() {
  for (let i = 0; i < 60; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${port}/json/list`)
      const targets = await res.json()
      const page = targets.find((t) => t.type === 'page' && t.webSocketDebuggerUrl)
      if (page) return page.webSocketDebuggerUrl
    } catch {
      // 浏览器还没起来
    }
    await sleep(300)
  }
  throw new Error('等不到浏览器调试目标')
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function main() {
  const wsUrl = await waitForTarget()
  socket = new WebSocket(wsUrl)
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true })
    socket.addEventListener('error', reject, { once: true })
  })

  socket.addEventListener('message', (event) => {
    const text = typeof event.data === 'string' ? event.data : Buffer.from(event.data).toString('utf8')
    let msg
    try {
      msg = JSON.parse(text)
    } catch {
      return
    }
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id)
      pending.delete(msg.id)
      if (msg.error) reject(new Error(msg.error.message))
      else resolve(msg.result)
    }
  })

  await send('Page.enable')
  await send('Runtime.enable')
  await send('Page.navigate', { url })

  // 等 Vue 挂载。先等出现目标选择器，没指定就等文档就绪再宽限一会儿
  if (waitSelector) {
    await waitForSelector(waitSelector)
  } else {
    await sleep(2500)
  }

  const expression = readFileSync(jsFile, 'utf8')
  const result = await send('Runtime.evaluate', {
    expression: `(async () => { ${expression} })()`,
    awaitPromise: true,
    returnByValue: true
  })

  if (result.exceptionDetails) {
    console.error('页面里抛异常:')
    console.error(JSON.stringify(result.exceptionDetails, null, 2))
    process.exitCode = 1
  } else {
    console.log(JSON.stringify(result.result.value, null, 2))
  }
}

async function waitForSelector(selector) {
  const deadline = Date.now() + 15000
  while (Date.now() < deadline) {
    try {
      const r = await send('Runtime.evaluate', {
        expression: `!!document.querySelector(${JSON.stringify(selector)})`,
        returnByValue: true
      })
      if (r.result.value) return
    } catch {
      // 导航过程中执行上下文会短暂不可用
    }
    await sleep(200)
  }
  // 等不到也继续，让页面里的片段自己报告看到了什么
}

/**
 * 杀掉整个浏览器进程树
 *
 * 只调 edge.kill() 是不够的——它杀的是启动器，Edge 会派生一堆渲染进程留在那。
 * 那些页面还活着，带着有效的登录态，延迟触发的事件会打到后端上，
 * 造成「没人碰却多出一条数据」这种最难查的现象。攒到十几个之后尤其明显
 */
function killTree(pid) {
  try {
    if (process.platform === 'win32') {
      execFileSync('taskkill', ['/PID', String(pid), '/T', '/F'], { stdio: 'ignore' })
    } else {
      process.kill(pid, 'SIGKILL')
    }
  } catch {
    // 已经退干净了，taskkill 会报错，忽略
  }
}

main()
  .catch((err) => {
    console.error('驱动失败:', err.message)
    process.exitCode = 1
  })
  .finally(() => {
    try {
      socket?.close()
    } catch {
      // 已经关了
    }
    killTree(edge.pid)
    // 复用模式下的 profile 是调用方给的，留着下次接着用
    if (!reuseProfile) {
      try {
        rmSync(profileDir, { recursive: true, force: true })
      } catch {
        // Edge 刚被杀掉时文件可能还被占着，删不掉就留在 .tmp 里，不值得让验证失败
      }
    }
  })
