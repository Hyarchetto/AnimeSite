<!-- src/views/admin/AdminUsers.vue -->
<template>
  <div class="admin-users">
    <h2 class="page-title">用户管理</h2>

    <p class="tip">
      用户只封禁不删除
    </p>

    <div class="toolbar">
      <input
        v-model.trim="q"
        type="text"
        placeholder="按账号或用户名搜索"
        @keydown.enter="reloadFromFirstPage"
      />
      <button @click="reloadFromFirstPage">搜索</button>
      <button v-if="q" @click="resetSearch">重置</button>
    </div>

    <p v-if="loading && !items.length" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!items.length" class="page-hint">{{ q ? '没有匹配的用户' : '还没有用户' }}</p>

    <template v-else>
      <table>
        <thead>
          <tr>
            <th>用户</th>
            <th class="col-account">账号</th>
            <th class="col-role">角色</th>
            <th class="col-enabled">状态</th>
            <th class="col-data">数据</th>
            <th class="col-date">注册时间</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in items" :key="user.id" :class="{ off: user.enabled === false }">
            <td>
              <div class="user-cell">
                <img :src="resolveAvatar(user.avatar)" alt="" />
                <span class="nickname">{{ user.nickname }}</span>
                <span v-if="user.owner" class="tag owner">站长</span>
                <span v-if="user.id === auth.user?.id" class="tag me">这是你</span>
              </div>
            </td>
            <td class="muted">{{ user.account }}</td>
            <td>
              <span class="role" :class="{ admin: user.role === 'ADMIN' }">
                {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td>
              <span class="state" :class="{ disabled: user.enabled === false }">
                {{ user.enabled === false ? '已封禁' : '正常' }}
              </span>
            </td>
            <td class="muted small">
              追番 {{ user.watchlistCount }} · 记录 {{ user.historyCount }}
            </td>
            <td class="muted small">{{ formatDate(user.createdAt) }}</td>
            <td class="col-actions">
              <!-- 三种情况都不给按钮。后端也会拦，这里只是不让人白点一次。
                   站长那条排在最前：站长就是你自己时也先按「动不了」显示 -->
              <div v-if="user.owner" class="self-hint">站长不可修改</div>
              <div v-else-if="user.id === auth.user?.id" class="self-hint">不能操作自己</div>
              <div v-else class="row-actions">
                <button v-if="isOwner" @click="askRole(user)">
                  {{ user.role === 'ADMIN' ? '降为普通' : '设为管理员' }}
                </button>
                <button class="danger" @click="askEnabled(user)">
                  {{ user.enabled === false ? '恢复' : '封禁' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pager">
        <span class="pager-info">共 {{ page.total }} 个用户，第 {{ page.page }} / {{ page.totalPages }} 页</span>
        <template v-if="page.totalPages > 1">
          <button :disabled="page.page <= 1" @click="goPage(page.page - 1)">上一页</button>
          <button
            v-for="n in pageNumbers"
            :key="n"
            class="page-num"
            :class="{ on: n === page.page }"
            @click="goPage(n)"
          >
            {{ n }}
          </button>
          <button :disabled="page.page >= page.totalPages" @click="goPage(page.page + 1)">下一页</button>
        </template>
      </div>
    </template>

    <ModalDialog :open="!!acting" :title="actingTitle" @close="acting = null">
      <p class="confirm">{{ actingMessage }}</p>
      <p v-if="acting?.kind === 'enabled' && acting.enabled" class="confirm-warn">
        对方<b>正在登录的会话会立刻失效</b>，下一个请求就会被登出。
        他的收藏和观看记录都保留，以后重新启用还能接着用。
      </p>
      <p v-if="acting?.kind === 'role' && acting.role === 'ADMIN'" class="confirm-warn">
        管理员能改动全站的番剧、标签、轮播和所有用户。<b>只给信任的人。</b>
      </p>
      <template #footer>
        <button @click="acting = null">取消</button>
        <button :class="isDestructive ? 'danger' : 'primary'" :disabled="acting2" @click="confirmAct">
          {{ acting2 ? '处理中...' : '确认' }}
        </button>
      </template>
    </ModalDialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import ModalDialog from '@/components/ModalDialog.vue'
import { request } from '@/utils/request'
import { resolveAvatar } from '@/utils/image'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toasts = useToastStore()

const items = ref([])
const loading = ref(true)
const error = ref('')
const q = ref('')
const page = ref({ total: 0, page: 1, totalPages: 1 })

/** 待确认的操作。role 或 enabled，带上目标用户 */
const acting = ref(null)
const acting2 = ref(false)

/** 当前登录的人是不是站长。只有站长能改别人的角色，其他人只有封禁那一个按钮 */
const isOwner = computed(() => auth.user?.owner === true)

const pageNumbers = computed(() => {
  const total = page.value.totalPages
  const current = page.value.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const start = Math.max(1, Math.min(current - 2, total - 4))
  return Array.from({ length: 5 }, (_, i) => start + i)
})

const actingTitle = computed(() => {
  if (!acting.value) return ''
  return acting.value.kind === 'role' ? '修改角色' : '修改状态'
})

const isDestructive = computed(() => {
  if (!acting.value) return false
  // 降级和封禁都是「收回权限」，用红色按钮
  return acting.value.kind === 'role' ? acting.value.role === 'USER' : acting.value.enabled
})

const actingMessage = computed(() => {
  const a = acting.value
  if (!a) return ''
  if (a.kind === 'role') {
    return a.role === 'ADMIN'
      ? `要把「${a.nickname}」设为管理员吗？`
      : `要把「${a.nickname}」降为普通用户吗？`
  }
  return a.enabled ? `要封禁「${a.nickname}」吗？` : `要恢复「${a.nickname}」吗？`
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (q.value) params.set('q', q.value)
    params.set('page', page.value.page)
    const data = await request(`/api/admin/users?${params}`)
    items.value = data.items
    page.value = { total: data.total, page: data.page, totalPages: data.totalPages }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function reloadFromFirstPage() {
  page.value.page = 1
  load()
}

function resetSearch() {
  q.value = ''
  reloadFromFirstPage()
}

function goPage(n) {
  if (n < 1 || n > page.value.totalPages || n === page.value.page) return
  page.value.page = n
  load()
}

function askRole(user) {
  acting.value = {
    kind: 'role',
    id: user.id,
    nickname: user.nickname,
    role: user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  }
}

function askEnabled(user) {
  acting.value = {
    kind: 'enabled',
    id: user.id,
    nickname: user.nickname,
    enabled: user.enabled !== false
  }
}

async function confirmAct() {
  const a = acting.value
  acting2.value = true
  try {
    if (a.kind === 'role') {
      await request(`/api/admin/users/${a.id}/role`, { method: 'PUT', body: { role: a.role } })
    } else {
      await request(`/api/admin/users/${a.id}/enabled`, { method: 'PUT', body: { enabled: a.enabled } })
    }
    acting.value = null
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    acting2.value = false
  }
}

const formatDate = (v) => (v ? v.slice(0, 10) : '')
</script>

<style scoped>
.admin-users {
  padding: 0;
}

.page-title {
  margin: 0 0 12px;
}

.tip {
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.toolbar input {
  flex: 1;
  max-width: 320px;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: inherit;
  color: #333;
  transition: all 0.2s;
}

.toolbar input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

button {
  padding: 7px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

button:hover:not(:disabled) {
  background: #eee;
  color: #333;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

.primary:hover:not(:disabled) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

.danger {
  color: var(--primary);
}

.danger:hover:not(:disabled) {
  background: var(--primary-soft);
  border-color: var(--primary);
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  table-layout: fixed;
}

th,
td {
  padding: 12px 14px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  vertical-align: middle;
  overflow: hidden;
  text-overflow: ellipsis;
}

th {
  background: #fafafa;
  color: #888;
  font-weight: normal;
  white-space: nowrap;
}

/* 固定布局下必须给出列宽，没设宽度的那列吃掉剩余空间 */
.col-account {
  width: 150px;
}

.col-role {
  width: 110px;
}

.col-enabled {
  width: 90px;
}

.col-data {
  width: 150px;
}

.col-date {
  width: 120px;
}

.col-actions {
  width: 230px;
}

tr.off td:not(.col-actions) {
  opacity: 0.5;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
  background: #f5f5f5;
}

.nickname {
  color: #333;
  font-weight: 500;
}

.tag {
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 11px;
  white-space: nowrap;
}

/* 站长填色，和「这是你」区分开。同一个人身上可能两个都有 */
.tag.owner {
  background: var(--primary);
  color: #fff;
}

.role,
.state {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  white-space: nowrap;
}

.role {
  background: #f5f5f5;
  color: #888;
}

.role.admin {
  background: var(--primary-soft);
  color: var(--primary);
}

.state {
  background: #eef8f2;
  color: #2e9e5b;
}

.state.disabled {
  background: #f5f5f5;
  color: #999;
}

.muted {
  color: #888;
}

.small {
  font-size: 12px;
}

.row-actions {
  display: flex;
  gap: 8px;
}

/* 按钮定宽。文案跟着对方的当前角色翻转，不锁死的话点一下整行都跟着挪一截 */
.row-actions button {
  width: 96px;
}

.self-hint {
  color: #bbb;
  font-size: 12px;
}

/* 分页 */
.pager {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
}

.pager-info {
  margin-right: auto;
  font-size: 13px;
  color: #999;
}

.page-num {
  min-width: 34px;
}

.page-num.on {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

.page-num.on:hover {
  background: var(--primary-hover);
  color: #fff;
}

.confirm {
  margin: 0 0 12px;
  font-size: 14px;
  color: #333;
}

.confirm-warn {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #888;
}
</style>
