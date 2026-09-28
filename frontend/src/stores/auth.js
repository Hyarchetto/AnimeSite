// 登录态
//
// token 落到 localStorage，刷新页面不会掉登录。store 里存一份是为了让界面能响应式地
// 跟着变——Navbar 要立刻从「登录」变成昵称

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { request, addUnauthorizedListener } from '@/utils/request'
import { readToken, readUser, saveSession, saveUser, clearSession } from '@/utils/authStorage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(readToken())
  const user = ref(readUser())

  // token 失效时把内存里的状态一起清掉。localStorage 那边由 request 负责，
  // 两处必须同时清，否则界面会停在「看起来还登录着」的状态
  addUnauthorizedListener(() => {
    token.value = ''
    user.value = null
  })

  const isLoggedIn = computed(() => !!token.value)
  // 这个只决定要不要显示管理入口。真正拦住越权的是后端的 AdminInterceptor，
  // 前端把 isAdmin 改成 true 只会看到一个点不动的菜单
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const displayName = computed(() => user.value?.nickname || user.value?.account || '')

  async function login(account, password) {
    const data = await request('/api/auth/login', {
      method: 'POST',
      body: { account, password }
    })
    applySession(data)
  }

  async function register(account, password, nickname) {
    const data = await request('/api/auth/register', {
      method: 'POST',
      body: { account, password, nickname }
    })
    applySession(data)
  }

  /**
   * 用本地 token 换一次最新的用户信息
   *
   * 启动时调一次。本地缓存的 role 可能是旧的——管理员把某人降级之后，
   * 对方浏览器里存的那份还是 ADMIN，不刷新的话管理入口会一直显示着
   */
  async function refresh() {
    if (!token.value) return
    const info = await request('/api/auth/me')
    user.value = info
    saveUser(info)
  }

  /**
   * 改昵称和签名
   *
   * 只传想改的那个字段就行，后端会把没传的保持原值。
   * 这样设置页不用为了改签名先把昵称读出来带上
   */
  async function updateProfile({ nickname, signature } = {}) {
    const body = {}
    if (nickname !== undefined) body.nickname = nickname
    if (signature !== undefined) body.signature = signature

    const info = await request('/api/auth/profile', { method: 'PUT', body })
    user.value = info
    saveUser(info)
    return info
  }

  /** 换头像。上传和应用是同一个请求，后端存完图直接写进用户记录 */
  async function updateAvatar(file) {
    const form = new FormData()
    form.append('file', file)
    const info = await request('/api/auth/avatar', { method: 'POST', body: form })
    user.value = info
    saveUser(info)
    return info
  }

  async function changePassword(oldPassword, newPassword) {
    await request('/api/auth/password', {
      method: 'PUT',
      body: { oldPassword, newPassword }
    })
  }

  /**
   * 注销账号。**不可逆**
   *
   * 要验证密码——这是整套功能里唯一删数据且撤不回来的操作
   */
  async function deleteAccount(password) {
    await request('/api/auth/account', { method: 'DELETE', body: { password } })
    logout()
  }

  function logout() {
    clearSession()
    token.value = ''
    user.value = null
  }

  function applySession(data) {
    token.value = data.token
    user.value = data.user
    saveSession(data.token, data.user)
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    displayName,
    login,
    register,
    refresh,
    updateProfile,
    updateAvatar,
    changePassword,
    deleteAccount,
    logout
  }
})
