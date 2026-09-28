// 后端请求的统一入口
//
// 所有请求都要走这里，不要在各处直接 fetch。收口之后 token 注入、401 处理、
// 204 处理、错误消息提取都只有一份

import { readToken, clearSession } from './authStorage'

// 后端的源。image.js 也引这个常量，改端口只有这一处要动
export const BASE_URL = 'http://localhost:3001'

// 登录态存在两处：localStorage 和 Pinia store 的内存状态。
// 401 的时候只清一处会留下一个自相矛盾的状态——顶栏还显示着用户名，
// 但每个请求都失败。这里不直接去改 store，那会成循环依赖，
// 改成通知出去，由 store 自己订阅
const unauthorizedListeners = new Set()

/** 订阅 401 事件，返回取消订阅的函数 */
export function addUnauthorizedListener(listener) {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

/**
 * 发一个请求
 *
 * @param path   以 / 开头的路径，如 '/api/anime'
 * @param method 默认 GET
 * @param body   有值就按 JSON 发出去并自动带上 Content-Type
 * @returns      解析后的响应体，204 返回 null
 * @throws       Error，message 是后端给的提示文案，可直接显示给用户
 */
export async function request(path, { method = 'GET', body } = {}) {
  const headers = {}

  // 传 FormData 时**不能自己设 Content-Type**。multipart 的 Content-Type 里
  // 要带一段 boundary 分隔符，浏览器生成 FormData 时才知道它是什么，
  // 手写成 application/json 会让后端完全解析不出文件
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
  if (body !== undefined && !isFormData) {
    headers['Content-Type'] = 'application/json'
  }

  const token = readToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(BASE_URL + path, {
      method,
      headers,
      body: body === undefined ? undefined : (isFormData ? body : JSON.stringify(body))
    })
  } catch {
    // fetch 只在网络层失败时 reject，后端没起、地址写错都会走到这里。
    // 后端返回的 4xx 5xx 不会 reject，所以这两种情况要分开处理
    throw new Error('连不上服务器，请确认后端已启动')
  }

  if (response.status === 401) {
    handleUnauthorized()
    throw new Error(await readErrorMessage(response))
  }

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  // 写接口返回 204 没有响应体，直接 json() 会抛解析错误
  if (response.status === 204) {
    return null
  }
  return response.json()
}

/**
 * token 失效时清掉本地登录态并跳登录页，带上当前地址以便登录后跳回来
 *
 * 用 window.location.hash 而不是 import router，是为了避开循环依赖：
 * router 引入视图，视图引入 store，store 引入这里，这里再引入 router 就成环了。
 * 项目用的是 hash 模式，改 hash 就等于导航
 */
function handleUnauthorized() {
  clearSession()
  // 先让 store 把内存里的状态也清掉，再跳转。
  // 顺序反了的话，守卫会在跳转时看到 isLoggedIn 仍是 true，
  // 触发「已登录还去登录页就送回首页」，用户被弹回首页而看不到登录页
  unauthorizedListeners.forEach((listener) => listener())

  const current = window.location.hash.slice(1) || '/'
  if (!current.startsWith('/login')) {
    window.location.hash = `#/login?redirect=${encodeURIComponent(current)}`
  }
}

/** 后端的错误响应统一是 { message }，取不到就退回一个通用文案 */
async function readErrorMessage(response) {
  try {
    const data = await response.json()
    if (data && data.message) return data.message
  } catch {
    // 响应体不是 JSON，比如静态资源 404 返回的空体
  }
  return `请求失败（${response.status}）`
}
