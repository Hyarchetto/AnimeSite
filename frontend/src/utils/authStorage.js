// 登录态的本地持久化
//
// 单独一个文件是为了让存储键只出现在一个地方。request 和 auth store 都要读 token，
// 如果两边各写一份 'anime_token'，改名字时漏改一处就会变成「登录了但请求不带 token」
//
// localStorage 对 XSS 不设防，页面里任何一段注入脚本都能读走 token。
// 更稳的是 httpOnly Cookie，但那要处理 CSRF，复杂度上一个台阶。
// 这个项目选 localStorage，代价要知道

const TOKEN_KEY = 'anime_token'
const USER_KEY = 'anime_user'

export function readToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function readUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    // 存坏了就当没有，不能让一段脏数据把整个应用卡死
    return null
  }
}

export function saveSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function saveUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
