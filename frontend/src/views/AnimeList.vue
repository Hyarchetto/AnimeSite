<!-- src/views/AnimeList.vue -->
<template>
  <div class="search-page">
    <h2 class="page-title">番剧</h2>

    <!-- 标签选择 -->
    <section class="filter">
      <p class="label">
        按标签筛选
        <span v-if="selectedTags.length" class="sub">已选 {{ selectedTags.length }} 个，结果是同时满足它们的</span>
      </p>
      <div class="tags">
        <button
          v-for="tag in tagsWithAnime"
          :key="tag.id"
          class="tag"
          :class="{ on: selectedTags.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }}<span class="count">{{ tag.animeCount }}</span>
        </button>
      </div>
    </section>

    <!-- 搜索栏 -->
    <section class="filter">
      <p class="label">按名称搜索</p>
      <div class="search-row">
        <input
          v-model="keywordInput"
          type="text"
          placeholder="输入番剧名，支持部分匹配"
          @keydown.enter="applyKeyword"
        />
        <button class="primary" @click="applyKeyword">搜索</button>
        <button v-if="hasFilter" @click="clearAll">清除筛选</button>
      </div>
    </section>

    <!-- 结果 -->
    <p class="result-head">
      <span v-if="loading">搜索中...</span>
      <span v-else-if="error" class="err">{{ error }}</span>
      <span v-else>
        共 <b>{{ items.length }}</b> 部
        <template v-if="hasFilter">符合条件</template>
      </span>
    </p>

    <p v-if="!loading && !error && !items.length" class="page-hint">
      没有符合条件的番剧。<button class="link" @click="clearAll">清除筛选</button>
    </p>
    <div v-else-if="items.length" class="card-grid">
      <AnimeCard v-for="anime in items" :key="anime.id" :anime="anime" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AnimeCard from '@/components/AnimeCard.vue'
import { request } from '@/utils/request'

const route = useRoute()
const router = useRouter()

const tags = ref([])
const items = ref([])
const loading = ref(true)
const error = ref('')

const keywordInput = ref('')
const selectedTags = ref([])

/** 只列出有番剧在用的标签。没人用的标签点进去必然是空结果，列出来是噪音 */
const tagsWithAnime = computed(() => tags.value.filter((t) => t.animeCount > 0))

const hasFilter = computed(() => !!keywordInput.value || selectedTags.value.length > 0)

onMounted(async () => {
  try {
    tags.value = await request('/api/tags')
  } catch (e) {
    error.value = e.message
  }
})

/**
 * 地址栏是筛选条件的唯一来源
 *
 * 只在这里读条件并发起搜索，各个操作函数只负责改地址。
 * 分成两处的话会出现「点了标签但搜的还是上一次的条件」这类不一致。
 *
 * immediate 让首次进入也走这条路——顶栏搜索框带 ?q= 跳进来时靠的就是它
 */
watch(
  () => route.query,
  (query) => {
    keywordInput.value = typeof query.q === 'string' ? query.q : ''
    selectedTags.value = parseTags(query.tags)
    search()
  },
  { immediate: true }
)

function parseTags(raw) {
  if (!raw) return []
  const list = Array.isArray(raw) ? raw : String(raw).split(',')
  return list.map(Number).filter((n) => Number.isFinite(n))
}

async function search() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (keywordInput.value) params.set('q', keywordInput.value)
    selectedTags.value.forEach((id) => params.append('tags', id))
    const qs = params.toString()
    items.value = await request(`/api/search${qs ? '?' + qs : ''}`)
  } catch (e) {
    error.value = e.message
    items.value = []
  } finally {
    loading.value = false
  }
}

/** 只负责把条件写进地址。真正的搜索由上面那个 watch 触发 */
function syncUrl() {
  const query = {}
  if (keywordInput.value) query.q = keywordInput.value
  if (selectedTags.value.length) query.tags = selectedTags.value.join(',')
  router.replace({ path: '/anime', query })
}

function toggleTag(tagId) {
  // 先改本地状态再写地址。不先改的话，地址里的 tags 还是旧的，
  // 写进去就变成了「取消勾选又立刻勾上」
  selectedTags.value = selectedTags.value.includes(tagId)
    ? selectedTags.value.filter((id) => id !== tagId)
    : [...selectedTags.value, tagId]
  syncUrl()
}

function applyKeyword() {
  syncUrl()
}

function clearAll() {
  keywordInput.value = ''
  selectedTags.value = []
  syncUrl()
}
</script>

<style scoped>
.search-page {
  padding: 0;
}

.filter {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 18px 22px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.label {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.sub {
  margin-left: 8px;
  font-weight: normal;
  font-size: 12px;
  color: #999;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 13px;
  border: 1px solid #e0e0e0;
  border-radius: 999px;
  background: var(--bg-page);
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.tag.on {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 500;
}

.count {
  font-size: 11px;
  color: #bbb;
}

.tag.on .count {
  color: var(--primary);
}

.search-row {
  display: flex;
  gap: 10px;
}

input {
  flex: 1;
  min-width: 0;
  padding: 9px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
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

button {
  flex: none;
  padding: 9px 20px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

button:hover {
  background: #eee;
  color: #333;
}

.primary {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

.primary:hover {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

.result-head {
  margin: 20px 0 14px;
  font-size: 14px;
  color: #666;
}

.result-head b {
  color: var(--primary);
  font-size: 16px;
}

.result-head .err {
  color: var(--primary);
}

.link {
  padding: 0;
  border: none;
  background: none;
  color: var(--primary);
  font-size: inherit;
  cursor: pointer;
  text-decoration: underline;
}

.link:hover {
  background: none;
}
</style>
