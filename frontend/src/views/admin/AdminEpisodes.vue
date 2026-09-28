<!-- src/views/admin/AdminEpisodes.vue -->
<template>
  <div class="admin-episodes">
    <div class="page-head">
      <div>
        <router-link to="/admin/anime" class="back">← 番剧管理</router-link>
        <h2 class="page-title">{{ animeTitle || '剧集管理' }}</h2>
      </div>
      <button class="primary" @click="openCreate">新增剧集</button>
    </div>

    <p v-if="loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!items.length" class="page-hint">
      还没有录入剧集。点右上角新增，集号从 1 开始
    </p>

    <table v-else>
      <thead>
        <tr>
          <th class="col-no">集号</th>
          <th>标题</th>
          <th class="col-date">播出日期</th>
          <th class="col-url">观看地址</th>
          <th class="col-actions">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ep in items" :key="ep.id">
          <td class="no">第 {{ ep.episodeNo }} 集</td>
          <td>{{ ep.title || '—' }}</td>
          <td class="muted">{{ ep.airDate || '—' }}</td>
          <td class="url" :title="ep.watchUrl || ''">{{ ep.watchUrl || '—' }}</td>
          <td class="col-actions">
            <!-- 按钮包一层 div，不要直接把 td 设成 flex，那样它就不是表格单元格了，
                 会脱离垂直对齐，按钮跑到格子顶部 -->
            <div class="row-actions">
              <button @click="openEdit(ep)">编辑</button>
              <button class="danger" @click="deleting = ep">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 新建 / 编辑 -->
    <ModalDialog
      :open="formOpen"
      :title="editing ? `编辑第 ${form.episodeNo} 集` : '新增剧集'"
      width="480px"
      @close="formOpen = false"
    >
      <label>
        <span>集号</span>
        <input v-model.number="form.episodeNo" type="number" min="1" placeholder="从 1 开始" />
        <small>同一部番里集号不能重复</small>
      </label>
      <label>
        <span>标题</span>
        <input v-model.trim="form.title" type="text" maxlength="200" placeholder="可留空" />
      </label>
      <label>
        <span>播出日期</span>
        <input v-model="form.airDate" type="date" />
      </label>
      <label>
        <span>观看地址</span>
        <input v-model.trim="form.watchUrl" type="text" placeholder="https://..." />
        <small>外链完整地址。详情页点这一集会跳过去</small>
      </label>

      <template #footer>
        <button @click="formOpen = false">取消</button>
        <button class="primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </ModalDialog>

    <!-- 标题说删什么，按钮说做什么 -->
    <ModalDialog :open="!!deleting" title="删除剧集" @close="deleting = null">
      <p class="confirm">要删除《{{ animeTitle }}》的第 <b>{{ deleting?.episodeNo }}</b> 集吗？</p>
      <p class="confirm-warn">
        用户的观看记录里如果记着这一集，那些记录会一起被删掉。
        <b>不可恢复。</b>
      </p>
      <template #footer>
        <button @click="deleting = null">取消</button>
        <button class="danger" :disabled="deleting2" @click="confirmDelete">
          {{ deleting2 ? '删除中...' : '确认删除' }}
        </button>
      </template>
    </ModalDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ModalDialog from '@/components/ModalDialog.vue'
import { request } from '@/utils/request'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const animeId = Number(route.params.id)

const toasts = useToastStore()

const animeTitle = ref('')
const items = ref([])
const loading = ref(true)
const error = ref('')

const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)

const deleting = ref(null)
const deleting2 = ref(false)

const emptyForm = () => ({ episodeNo: null, title: '', airDate: '', watchUrl: '' })
const form = reactive(emptyForm())

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 剧集的读取接口是公开的那个，管理员和普通用户看到的是同一份数据
    const [detail, list] = await Promise.all([
      request(`/api/anime/${animeId}`),
      request(`/api/anime/${animeId}/episodes`)
    ])
    animeTitle.value = detail.title
    items.value = list
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  // 新建时给个建议集号：接着最后一集往下排，省得每次都自己去数
  const max = items.value.reduce((m, e) => Math.max(m, e.episodeNo), 0)
  form.episodeNo = max + 1
  formOpen.value = true
}

function openEdit(ep) {
  editing.value = ep
  Object.assign(form, {
    episodeNo: ep.episodeNo,
    title: ep.title || '',
    airDate: ep.airDate || '',
    watchUrl: ep.watchUrl || ''
  })
  formOpen.value = true
}

async function save() {
  saving.value = true
  try {
    const body = {
      episodeNo: form.episodeNo,
      title: form.title,
      airDate: form.airDate || null,
      watchUrl: form.watchUrl
    }
    if (editing.value) {
      await request(`/api/admin/episodes/${editing.value.id}`, { method: 'PUT', body })
    } else {
      await request(`/api/admin/anime/${animeId}/episodes`, { method: 'POST', body })
    }
    formOpen.value = false
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  deleting2.value = true
  try {
    await request(`/api/admin/episodes/${deleting.value.id}`, { method: 'DELETE' })
    deleting.value = null
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    deleting2.value = false
  }
}
</script>

<style scoped>
/* 同番剧管理：占满可用宽度，右侧不留空 */
.admin-episodes {
  padding: 0;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.back {
  display: inline-block;
  margin-bottom: 6px;
  color: #999;
  font-size: 13px;
  text-decoration: none;
}

.back:hover {
  color: var(--primary);
}

.page-head .page-title {
  margin: 0;
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
  /* 固定布局，列宽不跟着内容变。自动布局下列宽取决于所有行里最宽的那条，
     一筛选或一改表头文字就会重新分配，看着像表格被拉伸 */
  table-layout: fixed;
}

th,
td {
  padding: 11px 14px;
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

/* 固定布局下必须给出列宽，名字没设宽度的那一列吃掉剩余空间 */
.col-no {
  width: 90px;
}

.col-date {
  width: 120px;
}

.col-url {
  width: 260px;
}

.col-actions {
  width: 150px;
}

.no {
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

.muted {
  color: #888;
  white-space: nowrap;
}

/* 观看地址可能很长，截断并靠 title 提示完整值 */
.url {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #999;
  font-size: 12px;
}

.row-actions {
  display: flex;
  gap: 8px;
}

label {
  display: block;
  margin-bottom: 16px;
}

label span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #555;
}

label small {
  display: block;
  margin-top: 6px;
  color: #aaa;
  font-size: 12px;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: #333;
  transition: all 0.2s;
}

input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.confirm {
  margin: 0 0 12px;
  font-size: 14px;
  color: #333;
}

.confirm-warn {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #888;
}
</style>
