<!-- src/views/admin/AdminBanners.vue -->
<template>
  <div class="admin-banners">
    <div class="page-head">
      <h2 class="page-title">轮播管理</h2>
      <button class="primary" @click="openCreate">添加轮播</button>
    </div>

    <p class="tip">
      首页轮播是「哪部番 + 一张**横版**大图」。横版图和番剧的竖版封面不是同一张，要单独指定。
      顺序用每行右侧的箭头调。**番剧被下架时它的轮播会自动从首页消失**，重新上架又回来。
    </p>

    <p v-if="loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!items.length" class="page-hint">还没有轮播</p>

    <table v-else>
      <thead>
        <tr>
          <th class="col-image">轮播图</th>
          <th>关联番剧</th>
          <th class="col-cover">竖版封面</th>
          <th class="col-active">状态</th>
          <th class="col-actions">操作</th>
          <th class="col-order">顺序</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(banner, index) in items" :key="banner.id" :class="{ off: !isShowing(banner) }">
          <td>
            <img class="banner-thumb" :src="resolveImageUrl(banner.imageUrl)" alt="轮播图" />
          </td>
          <td>
            <span class="title">{{ banner.animeTitle }}</span>
            <!-- 番剧下架时轮播不显示，但这条轮播本身还是启用的。
                 标出来免得管理员以为开关坏了 -->
            <span v-if="banner.animeVisible === false" class="tag">番剧已下架</span>
          </td>
          <td>
            <img class="cover-thumb" :src="resolveImageUrl(banner.animeCover)" alt="封面" />
          </td>
          <td>
            <button
              class="switch"
              :class="{ on: banner.active === true }"
              :disabled="togglingId === banner.id"
              @click="toggleActive(banner)"
            >
              {{ banner.active === true ? '启用中' : '已停用' }}
            </button>
          </td>
          <td class="col-actions">
            <div class="row-actions">
              <button @click="openEdit(banner)">编辑</button>
              <button class="danger" @click="deleting = banner">移除</button>
            </div>
          </td>
          <td class="col-order">
            <!-- 只给箭头不显示序号。序号是要人工维护的，箭头不用 -->
            <div class="order-btns">
              <button :disabled="index === 0 || moving" title="上移" @click="move(index, -1)">↑</button>
              <button :disabled="index === items.length - 1 || moving" title="下移" @click="move(index, 1)">↓</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <ModalDialog
      :open="formOpen"
      :title="editing ? '编辑轮播' : '添加轮播'"
      width="520px"
      @close="formOpen = false"
    >
      <div class="field">
        <span class="field-name">轮播哪部番</span>

        <!-- 选中之后就收起来，只显示选中的那部 -->
        <div v-if="pickedAnime" class="picked">
          <img :src="resolveImageUrl(pickedAnime.cover)" alt="" />
          <span class="picked-name">{{ pickedAnime.title }}</span>
          <button type="button" @click="clearPicked">换一个</button>
        </div>

        <!-- 番剧可能很多，用搜索而不是下拉。下拉要一行行找 -->
        <template v-else>
          <input
            ref="keywordInput"
            v-model.trim="animeKeyword"
            type="text"
            placeholder="输入番剧名搜索"
            @input="onAnimeSearch"
          />

          <!-- 结果浮在输入框下面，不占弹窗内部的位置，弹窗高度不会变。
               Teleport 到 body 是必须的：弹窗的 body 有 overflow-y: auto，
               留在里面的话这块浮层会被裁掉一半。代价是要自己算坐标 -->
          <Teleport to="body">
            <ul v-if="resultsOpen" class="search-float" :style="resultsStyle" @click.stop>
              <li v-if="searching" class="search-state">搜索中...</li>
              <li v-else-if="!animeResults.length" class="search-state">没搜到</li>
              <li v-for="a in animeResults" v-else :key="a.id" @click="pickAnime(a)">
                <img :src="resolveImageUrl(a.cover)" alt="" />
                <span class="result-name">{{ a.title }}</span>
                <span v-if="a.visible === false" class="tag">已下架</span>
              </li>
            </ul>
          </Teleport>
        </template>
      </div>

      <ImageUpload
        v-model="form.imageUrl"
        label="轮播图"
        aspect="16 / 9"
        hint="横版大图，和番剧的竖版封面不是同一张。首页那条轮播展示的就是它"
      />

      <label class="checkbox-row">
        <input v-model="form.active" type="checkbox" />
        <span>启用（不勾选就不显示在首页）</span>
      </label>

      <template #footer>
        <button @click="formOpen = false">取消</button>
        <button class="primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </ModalDialog>

    <ModalDialog :open="!!deleting" title="移除轮播" @close="deleting = null">
      <p class="confirm">要移除《<b>{{ deleting?.animeTitle }}</b>》的首页轮播吗？</p>
      <p class="confirm-warn">
        只移除这一条轮播，<b>番剧本身和它的图片都不受影响</b>。
        如果只是暂时不想显示，改成「停用」更合适。
      </p>
      <template #footer>
        <button @click="deleting = null">取消</button>
        <button class="danger" :disabled="deleting2" @click="confirmDelete">
          {{ deleting2 ? '移除中...' : '确认移除' }}
        </button>
      </template>
    </ModalDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import ModalDialog from '@/components/ModalDialog.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { request } from '@/utils/request'
import { resolveImageUrl } from '@/utils/image'
import { useToastStore } from '@/stores/toast'

const toasts = useToastStore()

const items = ref([])
const loading = ref(true)
const error = ref('')

const formOpen = ref(false)
const editing = ref(null)
const saving = ref(false)

const deleting = ref(null)
const deleting2 = ref(false)

const togglingId = ref(null)
const moving = ref(false)

// 番剧搜索
const keywordInput = ref(null)
const animeKeyword = ref('')
const animeResults = ref([])
const pickedAnime = ref(null)
const searching = ref(false)
const searchActive = ref(false)
/** 浮层的定位。Teleport 出去之后没有祖先给定位，只能自己算 */
const resultsStyle = ref({})
let searchToken = 0

/** 输入框里有内容并且还没选定时才显示结果浮层 */
const resultsOpen = computed(() => searchActive.value && !!animeKeyword.value)

const emptyForm = () => ({ imageUrl: '', active: true })
const form = reactive(emptyForm())

/** 这条轮播此刻在首页显示吗。番剧下架的即使启用了也不显示 */
function isShowing(banner) {
  return banner.active === true && banner.animeVisible !== false
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await request('/api/admin/banners')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, emptyForm())
  resetAnimeSearch()
  formOpen.value = true
}

async function openEdit(banner) {
  editing.value = banner
  Object.assign(form, {
    imageUrl: banner.imageUrl || '',
    active: banner.active === true
  })
  resetAnimeSearch()
  // 编辑时要把当前关联的那部番显示成「已选中」，得拿到它的封面和标题，
  // 列表里那条已经带了，直接用
  pickedAnime.value = {
    id: banner.animeId,
    title: banner.animeTitle,
    cover: banner.animeCover,
    visible: banner.animeVisible
  }
  formOpen.value = true
}

function resetAnimeSearch() {
  animeKeyword.value = ''
  animeResults.value = []
  pickedAnime.value = null
  searchActive.value = false
  resultsStyle.value = {}
}

/**
 * 把浮层贴到输入框正下方
 *
 * <p>每次搜索都重算一次。打字必然会触发搜索，所以位置跟着输入框走；
 * 弹窗本身不滚动，不用额外监听 scroll
 */
function positionResults() {
  const el = keywordInput.value
  if (!el) return
  const r = el.getBoundingClientRect()
  resultsStyle.value = {
    top: `${r.bottom + 6}px`,
    left: `${r.left}px`,
    width: `${r.width}px`
  }
}

async function onAnimeSearch() {
  const keyword = animeKeyword.value
  if (!keyword) {
    animeResults.value = []
    searchActive.value = false
    return
  }
  searchActive.value = true
  positionResults()

  // 每次搜索发一个递增的号，回来时对不上就丢掉。
  // 打字快的时候请求会并发，不丢的话先发的可能后到，结果就串了
  const token = ++searchToken
  searching.value = true
  try {
    const page = await request(`/api/admin/anime?q=${encodeURIComponent(keyword)}&size=20`)
    if (token === searchToken) {
      animeResults.value = page.items
    }
  } catch (e) {
    if (token === searchToken) {
      animeResults.value = []
      toasts.show(e.message)
    }
  } finally {
    if (token === searchToken) {
      searching.value = false
    }
  }
}

function pickAnime(anime) {
  pickedAnime.value = anime
  animeResults.value = []
  animeKeyword.value = ''
  searchActive.value = false
}

// 点浮层外面收起来。不给 Document 加的话，用户点了别处这块会一直飘着
function onDocumentClick(event) {
  if (!searchActive.value) return
  const inInput = keywordInput.value?.contains(event.target)
  const inPanel = event.target.closest?.('.search-float')
  if (!inInput && !inPanel) {
    searchActive.value = false
  }
}

onMounted(load)
onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))

function clearPicked() {
  pickedAnime.value = null
}

async function save() {
  if (!pickedAnime.value) {
    toasts.show('请先选一部番剧')
    return
  }

  saving.value = true
  try {
    const body = {
      animeId: pickedAnime.value.id,
      imageUrl: form.imageUrl,
      active: form.active
    }
    if (editing.value) {
      await request(`/api/admin/banners/${editing.value.id}`, { method: 'PUT', body })
    } else {
      await request('/api/admin/banners', { method: 'POST', body })
    }
    formOpen.value = false
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    saving.value = false
  }
}

async function toggleActive(banner) {
  togglingId.value = banner.id
  try {
    // 只传要改的字段，后端会把没传的沿用原值
    await request(`/api/admin/banners/${banner.id}`, {
      method: 'PUT',
      body: { active: banner.active !== true }
    })
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    togglingId.value = null
  }
}

/**
 * 和相邻的那条交换顺序
 *
 * <p>交换要发两次请求，中间可能失败。失败时页面重新拉一次，
 * 让界面回到库里的真实状态，不要停在一个「一半换了一半没换」的样子
 */
async function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= items.value.length) return

  moving.value = true
  try {
    const a = items.value[index]
    const b = items.value[target]
    await request(`/api/admin/banners/${a.id}`, { method: 'PUT', body: { sortOrder: b.sortOrder } })
    await request(`/api/admin/banners/${b.id}`, { method: 'PUT', body: { sortOrder: a.sortOrder } })
  } catch (e) {
    toasts.show(e.message)
  } finally {
    moving.value = false
    await load()
  }
}

async function confirmDelete() {
  deleting2.value = true
  try {
    await request(`/api/admin/banners/${deleting.value.id}`, { method: 'DELETE' })
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
.admin-banners {
  padding: 0;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.page-head .page-title {
  margin: 0;
}

.tip {
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
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
.col-image {
  width: 180px;
}

.col-cover {
  width: 100px;
}

.col-active {
  width: 110px;
}

.col-actions {
  width: 170px;
}

.col-order {
  width: 90px;
}

tr.off td:not(.col-actions, .col-order) {
  opacity: 0.55;
}

/* 轮播图是横版，按 16:9 显示 */
.banner-thumb {
  width: 140px;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  border-radius: var(--radius-sm);
  display: block;
  background: #f5f5f5;
}

.cover-thumb {
  width: 36px;
  aspect-ratio: 2 / 3;
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
  white-space: nowrap;
}

.order-btns {
  display: flex;
  gap: 6px;
}

.order-btns button {
  width: 30px;
  padding: 4px 0;
  text-align: center;
  line-height: 1;
}

.switch {
  min-width: 70px;
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

/* 弹窗里的表单 */
label,
.field {
  display: block;
  margin-bottom: 16px;
}

label span,
.field-name {
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
  line-height: 1.6;
}

input[type='text'] {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: #fff;
  font-size: 14px;
  font-family: inherit;
  color: #333;
  transition: all 0.2s;
}

input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.field-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: #999;
}

/* 搜索结果浮层。
   Teleport 到 body 之后没有祖先给定位，left/top/width 都是 JS 传进来的。
   scoped 样式对 Teleport 出去的内容仍然生效——元素身上带着本组件的
   data-v 属性，只是不在原来的 DOM 位置而已 */
.search-float {
  position: fixed;
  /* 比弹窗高。弹窗里触发的浮层，被弹窗盖住就没意义了 */
  z-index: 1500;
  max-height: 240px;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: var(--radius-md);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.search-float li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.search-float li:hover {
  background: var(--primary-soft);
}

.search-float img {
  width: 26px;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  border-radius: 3px;
  flex: none;
  background: #f5f5f5;
}

.result-name {
  flex: 1;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 「搜索中」和「没搜到」那两行不该有悬停高亮，它们点不了 */
.search-state {
  color: #999;
  font-size: 13px;
  cursor: default;
}

.search-state:hover {
  background: none;
}


/* 选中之后显示的那一条 */
.picked {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid var(--primary);
  border-radius: var(--radius-md);
  background: var(--primary-soft);
}

.picked img {
  width: 30px;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  border-radius: 3px;
  flex: none;
  background: #fff;
}

.picked-name {
  flex: 1;
  margin: 0;
  color: var(--primary);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picked button {
  flex: none;
  padding: 4px 10px;
  font-size: 12px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.checkbox-row span {
  margin: 0;
}

.checkbox-row input {
  width: auto;
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
