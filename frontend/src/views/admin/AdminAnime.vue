<!-- src/views/admin/AdminAnime.vue -->
<template>
  <div class="admin-anime">
    <div class="page-head">
      <h2 class="page-title">番剧管理</h2>
      <button class="primary" @click="openCreate">新建番剧</button>
    </div>

    <!-- 搜索 -->
    <div class="toolbar">
      <input
        v-model.trim="filters.q"
        type="text"
        placeholder="按名称搜索"
        @keydown.enter="applyKeyword"
      />
      <button @click="applyKeyword">搜索</button>
      <button v-if="hasFilter" @click="resetFilters">重置</button>
    </div>

    <p v-if="loading && !items.length" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!items.length" class="page-hint">
      {{ hasFilter ? '没有符合条件的番剧' : '还没有番剧' }}
    </p>

    <template v-else>
      <table>
        <thead>
          <tr>
            <th class="col-cover">封面</th>
            <!-- 点击表头循环切换：默认 → 升序 → 降序 → 默认 -->
            <th class="sortable" @click="cycleSort('title')">
              名称<span class="arrow">{{ sortMark('title') }}</span>
            </th>
            <th class="col-status">更新状态</th>
            <th class="col-date sortable" @click="cycleSort('release_date')">
              首播日期<span class="arrow">{{ sortMark('release_date') }}</span>
            </th>
            <!-- 上架状态是筛选不是排序：默认 → 只看上架 → 只看未上架 → 默认 -->
            <th class="col-visible sortable" @click="cycleVisible">
              上架状态<span class="arrow">{{ visibleMark }}</span>
            </th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="anime in items" :key="anime.id" :class="{ offline: anime.visible === false }">
            <td>
              <img class="thumb" :src="resolveImageUrl(anime.cover)" alt="封面" />
            </td>
            <td>
              <span class="title">{{ anime.title }}</span>
              <span v-if="anime.visible === false" class="tag">已下架</span>
            </td>
            <td class="muted">{{ anime.latest || '—' }}</td>
            <td class="muted">{{ anime.release_date || '—' }}</td>
            <td>
              <!-- 上下架是高频单点操作，直接一个开关，不裹进编辑表单 -->
              <button
                class="switch"
                :class="{ on: anime.visible === true }"
                :disabled="togglingId === anime.id"
                @click="onToggleVisible(anime)"
              >
                {{ anime.visible === true ? '已上架' : '已下架' }}
              </button>
            </td>
            <td class="col-actions">
              <!-- 按钮包一层 div，不要直接把 td 设成 flex。
                   td 一变成 flex 就不是表格单元格了，会脱离垂直对齐，按钮跑到格子顶部 -->
              <div class="row-actions">
                <button @click="openEdit(anime)">编辑</button>
                <router-link :to="`/admin/anime/${anime.id}/episodes`" class="link-btn">剧集</router-link>
                <button class="danger" @click="askDelete(anime)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="pager">
        <span class="pager-info">共 {{ page.total }} 部，第 {{ page.page }} / {{ page.totalPages }} 页</span>
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

    <!-- 新建 / 编辑 -->
    <ModalDialog
      :open="formOpen"
      :title="editing ? '编辑番剧' : '新建番剧'"
      width="520px"
      @close="formOpen = false"
    >
      <label>
        <span>名称</span>
        <input v-model.trim="form.title" type="text" maxlength="200" placeholder="番剧名称" />
      </label>
      <ImageUpload
        v-model="form.cover"
        label="封面"
        aspect="2 / 3"
        hint="竖版封面，正面比例 2:3。没传的话列表里会是空白格"
      />
      <label>
        <span>更新状态</span>
        <input v-model.trim="form.latest" type="text" maxlength="100" placeholder="已完结 / 更新至第 12 集" />
      </label>
      <label>
        <span>首播日期</span>
        <input v-model="form.release_date" type="date" />
      </label>
      <label>
        <span>简介</span>
        <textarea v-model="form.desc" rows="4" placeholder="不填就显示「暂无简介」"></textarea>
      </label>

      <!-- 标签是多选，结构和其他字段不一样，用 div 不用 label -->
      <div class="field">
        <span class="field-name">标签</span>
        <p v-if="!tags.length" class="field-hint">
          还没有标签，去<router-link to="/admin/tags">标签管理</router-link>先建几个
        </p>
        <div v-else class="tag-picker">
          <label
            v-for="t in tags"
            :key="t.id"
            class="tag-option"
            :class="{ on: form.tagIds.includes(t.id) }"
          >
            <input v-model="form.tagIds" type="checkbox" :value="t.id" />
            {{ t.name }}
          </label>
        </div>
      </div>

      <template #footer>
        <button @click="formOpen = false">取消</button>
        <button class="primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </ModalDialog>

    <!-- 删除确认。删番剧会连带清掉剧集、用户的追番和观看记录，不可恢复 -->
    <!-- 标题说删什么，按钮说做什么。两处都写「确认删除」的话同一个词出现两次，
         下面那个还是加宽的按钮，视觉上底部会压过顶部 -->
    <ModalDialog :open="!!deleting" title="删除番剧" @close="deleting = null">
      <p class="confirm">要删除《<b>{{ deleting?.title }}</b>》吗？</p>
      <p class="confirm-warn">
        这会同时删掉它的剧集、标签关联、轮播，以及所有用户对它的追番和观看记录。
        <b>不可恢复。</b>
      </p>
      <p class="confirm-warn">如果只是想让它从网页上消失，请改用「已下架」。</p>
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
import { ref, reactive, computed, onMounted } from 'vue'
import ModalDialog from '@/components/ModalDialog.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { request } from '@/utils/request'
import { resolveImageUrl } from '@/utils/image'
import { useToastStore } from '@/stores/toast'

const items = ref([])
/** 全部标签，给编辑表单的多选框用 */
const tags = ref([])
const loading = ref(true)
const error = ref('')

const toasts = useToastStore()

/** 分页信息由后端给，前端不自己算——算错了页码和内容就对不上 */
const page = ref({ total: 0, page: 1, size: 10, totalPages: 1 })

/**
 * 筛选和排序条件
 *
 * 这些都在后端生效，不是拿回来在前端过滤。前端只能筛已经取回来的那一页，
 * 第二页的数据根本不在手里
 */
const filters = reactive({ q: '', visible: null, sort: null, order: 'asc', size: 10 })

const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)

const deleting = ref(null)
const deleting2 = ref(false)

const togglingId = ref(null)

const hasFilter = computed(() => !!filters.q || filters.visible !== null || !!filters.sort)

/** 表头那个小箭头。空串表示这一列没参与排序 */
function sortMark(field) {
  if (filters.sort !== field) return ''
  return filters.order === 'asc' ? '↑' : '↓'
}

const visibleMark = computed(() => {
  if (filters.visible === true) return '上架'
  if (filters.visible === false) return '未上架'
  return ''
})

/** 页码只显示当前页附近的几个，否则页数多了会铺满一整行 */
const pageNumbers = computed(() => {
  const total = page.value.totalPages
  const current = page.value.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const start = Math.max(1, Math.min(current - 2, total - 4))
  return Array.from({ length: 5 }, (_, i) => start + i)
})

const emptyForm = () => ({ title: '', cover: '', latest: '', release_date: '', desc: '', tagIds: [] })
const form = reactive(emptyForm())

onMounted(async () => {
  await loadTags()
  await load()
})

async function loadTags() {
  try {
    tags.value = await request('/api/admin/tags')
  } catch (e) {
    error.value = e.message
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (filters.q) params.set('q', filters.q)
    // visible 的三态：null 不传（全都要），true/false 才带上
    if (filters.visible !== null) params.set('visible', filters.visible)
    if (filters.sort) {
      params.set('sort', filters.sort)
      params.set('order', filters.order)
    }
    params.set('page', page.value.page)
    params.set('size', filters.size)

    const data = await request(`/api/admin/anime?${params}`)
    items.value = data.items
    // 用后端纠正过的 page 回写。要删掉最后一页的最后一条时，
    // 后端会把页码收回到上一页，前端得跟着走
    page.value = { total: data.total, page: data.page, size: data.size, totalPages: data.totalPages }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

/** 条件变了要回到第 1 页，否则会停在一个空页上 */
function reloadFromFirstPage() {
  page.value.page = 1
  load()
}

function applyKeyword() {
  reloadFromFirstPage()
}

function resetFilters() {
  filters.q = ''
  filters.visible = null
  filters.sort = null
  filters.order = 'asc'
  reloadFromFirstPage()
}

/** 点击表头循环：换一列 → 从升序开始；同一列 → 升序转降序 → 再点回到默认 */
function cycleSort(field) {
  if (filters.sort !== field) {
    filters.sort = field
    filters.order = 'asc'
  } else if (filters.order === 'asc') {
    filters.order = 'desc'
  } else {
    filters.sort = null
    filters.order = 'asc'
  }
  reloadFromFirstPage()
}

/** 上架状态是筛选：默认 → 只看上架 → 只看未上架 → 默认 */
function cycleVisible() {
  filters.visible = filters.visible === null ? true : filters.visible === true ? false : null
  reloadFromFirstPage()
}

function goPage(n) {
  if (n < 1 || n > page.value.totalPages || n === page.value.page) return
  page.value.page = n
  load()
}

function openCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  formOpen.value = true
}

function openEdit(anime) {
  editing.value = anime
  Object.assign(form, {
    title: anime.title || '',
    cover: anime.cover || '',
    latest: anime.latest || '',
    release_date: anime.release_date || '',
    desc: anime.desc || '',
    // 复制一份。直接赋引用的话，取消编辑时勾选框的改动会留在原对象上
    tagIds: [...(anime.tagIds || [])]
  })
  formOpen.value = true
}

async function save() {
  saving.value = true
  try {
    const body = {
      title: form.title,
      cover: form.cover,
      latest: form.latest,
      release_date: form.release_date || null,
      desc: form.desc,
      // 整组替换：传什么就是最终有哪些
      tagIds: form.tagIds
    }
    if (editing.value) {
      await request(`/api/admin/anime/${editing.value.id}`, { method: 'PUT', body })
    } else {
      await request('/api/admin/anime', { method: 'POST', body })
    }
    formOpen.value = false
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    saving.value = false
  }
}

async function onToggleVisible(anime) {
  togglingId.value = anime.id
  try {
    await request(`/api/admin/anime/${anime.id}/visibility`, {
      method: 'PUT',
      body: { visible: anime.visible !== true }
    })
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    togglingId.value = null
  }
}

function askDelete(anime) {
  deleting.value = anime
}

async function confirmDelete() {
  deleting2.value = true
  try {
    await request(`/api/admin/anime/${deleting.value.id}`, { method: 'DELETE' })
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
/* 不设 max-width，占满主内容区的可用宽度。
   设了的话右边会空出一大块——侧边栏已经在左边占掉 200px，
   右边再空一块，整页看着就是歪的。表格本来就该越宽越好用 */
.admin-anime {
  padding: 0;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head .page-title {
  margin: 0;
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
  /* 固定布局，列宽由下面那组 width 决定，不跟着内容变。
     自动布局下列宽取决于「所有行里最宽的那条」，一筛选、一改表头文字，
     整张表的列宽就会重新分配，看着像表格被拉伸 */
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

/* 固定布局下必须给出列宽，不给的话所有列等宽。
   名称是唯一不设宽度的，它吃掉剩下的空间 */
.col-cover {
  width: 80px;
}

.col-status {
  width: 150px;
}

.col-date {
  width: 120px;
}

.col-visible {
  width: 108px;
}

.col-actions {
  width: 230px;
}

/* 可点的表头。做成手型光标，不然没人知道能点 */
.sortable {
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;
}

.sortable:hover {
  color: var(--primary);
}

.arrow {
  margin-left: 5px;
  font-size: 11px;
  color: var(--primary);
}

tr.offline {
  background: #fcfcfc;
}

tr.offline .title {
  color: #aaa;
}

.thumb {
  width: 40px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  display: block;
  background: #f5f5f5;
}

.title {
  color: #333;
  font-weight: 500;
}

.tag {
  margin-left: 8px;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: #f0f0f0;
  color: #999;
  font-size: 11px;
}

.muted {
  color: #888;
}

.switch {
  min-width: 68px;
  border-color: #e0e0e0;
  background: var(--bg-page);
  color: #999;
}

.switch.on {
  border-color: #b7e0c6;
  background: #eef8f2;
  color: #2e9e5b;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.link-btn {
  padding: 7px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #555;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}

.link-btn:hover {
  background: #eee;
  color: #333;
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

/* 弹窗里的表单 */
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

input,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: #333;
  transition: all 0.2s;
}

textarea {
  resize: vertical;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

/* 标签多选。结构和其他字段不同，单独一套样式 */
.field {
  margin-bottom: 16px;
}

.field-name {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #555;
}

.field-hint {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.field-hint a {
  color: var(--primary);
  text-decoration: none;
}

.tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-option {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  padding: 5px 11px;
  border: 1px solid #e0e0e0;
  border-radius: 999px;
  background: var(--bg-page);
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-option:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.tag-option.on {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary);
}

/* 原生复选框藏掉，靠整个胶囊的底色表示选中。
   藏而不用 display:none，那样键盘和读屏软件就够不到了 */
.tag-option input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
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
