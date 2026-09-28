<!-- src/views/AnimeDetail.vue -->
<template>
  <div class="detail">
    <p v-if="loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>

    <template v-else>
      <!-- 头部：封面 + 信息。追番按钮在标题那一行的最右端 -->
      <section class="head">
        <img class="cover" :src="resolveImageUrl(anime.cover)" alt="封面" />

        <div class="head-info">
          <div class="title-row">
            <h2>
              {{ anime.title }}
              <span v-if="anime.visible === false" class="offline">已下架</span>
            </h2>
            <button class="collect" :class="{ on: collected }" :disabled="working" @click="onCollect">
              {{ collected ? '已追番' : '追番' }}
            </button>
          </div>

          <p class="meta">{{ anime.latest }}<span v-if="anime.release_date"> · {{ anime.release_date }} 首播</span></p>
          <p v-if="watchedEpisodeNo" class="meta progress">看到第 {{ watchedEpisodeNo }} 集</p>
          <p class="desc">{{ anime.desc || '暂无简介' }}</p>
          <p v-if="!auth.isLoggedIn" class="meta login-tip">
            <router-link to="/login">登录</router-link>后可以追番、记进度和评论
          </p>
        </div>
      </section>

      <!-- 标签。点一个跳到搜索页筛出同类 -->
      <section>
        <h3>标签</h3>
        <div v-if="anime.tags.length" class="tags">
          <router-link
            v-for="t in anime.tags"
            :key="t.id"
            :to="`/anime?tags=${t.id}`"
            class="tag"
          >{{ t.name }}</router-link>
        </div>
        <p v-else class="empty">还没有标签</p>
      </section>

      <!-- 选集 -->
      <section>
        <h3>选集<span class="sub">共 {{ episodes.length }} 集</span></h3>
        <p v-if="!episodes.length" class="empty">还没有录入剧集</p>
        <div v-else class="episodes">
          <button
            v-for="ep in episodes"
            :key="ep.id"
            class="episode"
            :class="{ current: ep.episodeNo === watchedEpisodeNo }"
            :title="ep.title || ''"
            @click="onEpisode(ep)"
          >
            {{ ep.episodeNo }}
          </button>
        </div>
        <p class="sub">点一集会记进观看记录，并在新标签页打开观看地址</p>
      </section>

      <!-- 评论 -->
      <section>
        <h3>评论<span class="sub">{{ comments.length }} 条</span></h3>

        <div v-if="auth.isLoggedIn" class="post">
          <textarea
            v-model.trim="draft"
            maxlength="500"
            rows="3"
            placeholder="说点什么..."
            @keydown.ctrl.enter="onPost"
          ></textarea>
          <div class="post-bar">
            <span class="sub">{{ draft.length }} / 500</span>
            <button :disabled="posting || !draft" @click="onPost">
              {{ posting ? '发表中...' : '发表' }}
            </button>
          </div>
        </div>
        <p v-else class="empty">
          <router-link to="/login">登录</router-link>后可以发表评论
        </p>

        <p v-if="!comments.length" class="empty">还没有评论，来说第一句</p>
        <ul v-else class="comments">
          <li v-for="c in comments" :key="c.id">
            <!-- 点头像或昵称去看这个人的资料页。
                 作者注销之后 userId 是空的，这时不给链接——点进去只会是 404 -->
            <router-link v-if="c.userId" :to="`/user/${c.userId}`" class="author">
              <img class="avatar" :src="resolveAvatar(c.avatar)" alt="头像" />
            </router-link>
            <span v-else class="author">
              <img class="avatar" :src="resolveAvatar(c.avatar)" alt="头像" />
            </span>
            <div class="body">
              <div class="line">
                <router-link v-if="c.userId" :to="`/user/${c.userId}`" class="name">{{ c.nickname }}</router-link>
                <span v-else class="name gone">{{ c.nickname }}</span>
                <span class="time">{{ formatTime(c.createdAt) }}</span>
                <button v-if="canDelete(c)" class="del" @click="onDelete(c)">删除</button>
              </div>
              <p class="text">{{ c.content }}</p>
            </div>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { request } from '@/utils/request'
import { resolveImageUrl, resolveAvatar } from '@/utils/image'
import { useAuthStore } from '@/stores/auth'
import { useWatchlistStore } from '@/stores/watchlist'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const watchlist = useWatchlistStore()
const toasts = useToastStore()

const animeId = Number(route.params.id)

const anime = ref(null)
const episodes = ref([])
const comments = ref([])
const watchedEpisodeNo = ref(null)

const loading = ref(true)
const error = ref('')

const working = ref(false)
const draft = ref('')
const posting = ref(false)

const collected = computed(() => watchlist.isCollected(animeId))

onMounted(async () => {
  try {
    // 三个接口互不依赖，一起发
    const [detail, eps, list] = await Promise.all([
      request(`/api/anime/${animeId}`),
      request(`/api/anime/${animeId}/episodes`),
      request(`/api/anime/${animeId}/comments`)
    ])
    anime.value = detail
    episodes.value = eps
    comments.value = list

    // 已登录的话再查一下看到哪了，用来高亮那一集。
    // 单独查是因为它依赖登录态，失败也不该影响页面主体
    if (auth.isLoggedIn) {
      const history = await request('/api/history')
      watchedEpisodeNo.value = history.find((item) => item.id === animeId)?.episodeNo ?? null
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

/**
 * 追番按钮一直显示，未登录时点了跳登录并带上回来的地址
 *
 * 不像卡片那样藏起来，是因为它是这个页面的主操作，藏了会空一块。
 * 卡片上藏是因为卡片本身很小，放不下「点了会跳走」这层解释
 */
async function onCollect() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  working.value = true
  try {
    await watchlist.toggle(anime.value)
  } catch (e) {
    error.value = e.message
  } finally {
    working.value = false
  }
}

/**
 * 点一集：先记进观看记录，再开新标签页跳外链
 *
 * 顺序不能反。先跳转的话，用户可能在新标签页里待很久才回来，
 * 这段时间记录是缺的；而且新标签页的弹窗拦截也更容易触发
 */
async function onEpisode(ep) {
  if (auth.isLoggedIn) {
    try {
      await request('/api/history', { method: 'POST', body: { episodeId: ep.id } })
      watchedEpisodeNo.value = ep.episodeNo
    } catch (e) {
      error.value = e.message
      return
    }
  }
  if (ep.watchUrl) {
    // noopener 让新页面拿不到本页的 window 句柄，避免外站通过 window.opener 改本页地址
    window.open(ep.watchUrl, '_blank', 'noopener')
  }
}

async function onPost() {
  if (!draft.value) return
  posting.value = true
  try {
    const created = await request('/api/comments', {
      method: 'POST',
      body: { animeId, content: draft.value }
    })
    // 接口返回的是完整的一条，直接插到最前面，不用重拉整页
    comments.value = [created, ...comments.value]
    draft.value = ''
  } catch (e) {
    toasts.show(e.message)
  } finally {
    posting.value = false
  }
}

/** 自己的能删，管理员谁的都能删。判断和后端一致，不是各写一套 */
function canDelete(comment) {
  return auth.user?.id === comment.userId || auth.isAdmin
}

async function onDelete(comment) {
  try {
    await request(`/api/comments/${comment.id}`, { method: 'DELETE' })
    comments.value = comments.value.filter((c) => c.id !== comment.id)
  } catch (e) {
    toasts.show(e.message)
  }
}

/** 后端给的是 ISO 串，这里只取到分钟 */
function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.detail {
  max-width: 900px;
}

section {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

h3 {
  margin: 0 0 14px;
  font-size: 15px;
  color: #333;
}

.sub {
  margin-left: 8px;
  font-size: 12px;
  font-weight: normal;
  color: #999;
}

/* 头部 */
.head {
  display: flex;
  gap: 24px;
}

.cover {
  width: 180px;
  aspect-ratio: 2 / 3;
  object-fit: cover;
  border-radius: var(--radius-lg);
  flex: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.head-info {
  flex: 1;
  min-width: 0;
}

/* 标题和追番按钮同一行，按钮靠最右。
   这块要撑满 head-info 的宽度，只写 justify-content 是不够的——
   容器宽度仍是内容决定的，按钮会紧贴在标题右边而不是贴到右上角，
   标题长的时候就更明显 */
.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.head-info h2 {
  margin: 0;
  font-size: 22px;
  color: #333;
}

.offline {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: #f0f0f0;
  color: #999;
  font-size: 12px;
  font-weight: normal;
  vertical-align: middle;
}

.meta {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
}

.progress {
  color: var(--primary);
}

.login-tip {
  margin-top: 10px;
}

.login-tip a {
  color: var(--primary);
  text-decoration: none;
}

.collect {
  /* 别让长标题把按钮挤窄 */
  flex: none;
  padding: 9px 28px;
  border: 1px solid var(--primary);
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.collect:hover:not(:disabled) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

.collect.on {
  background: #fff;
  color: var(--primary);
}

.collect.on:hover:not(:disabled) {
  background: var(--primary-soft);
}

.collect:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 简介 */
/* 简介挪进了头部，跟在「看到第 N 集」下面，所以要自己顶上留出间距 */
.desc {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 5px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 999px;
  background: var(--bg-page);
  color: #666;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}

.tag:hover {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary);
}

/* 选集 */
.episodes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.episode {
  min-width: 44px;
  padding: 7px 10px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.episode:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}

.episode.current {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

/* 评论 */
.post textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: #333;
  resize: vertical;
  transition: all 0.2s;
}

.post textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.post-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 16px;
}

.post-bar button {
  padding: 7px 22px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.post-bar button:hover:not(:disabled) {
  background: var(--primary-hover);
}

.post-bar button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comments {
  list-style: none;
  margin: 0;
  padding: 0;
}

.comments li {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid #f0f0f0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
}

.body {
  flex: 1;
  min-width: 0;
}

.line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.author {
  flex: none;
  text-decoration: none;
}

.name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  text-decoration: none;
}

.name:hover {
  color: var(--primary);
}

/* 已注销用户的名字。不做得可点，颜色也淡一些 */
.name.gone {
  color: #aaa;
  font-weight: normal;
}

.time {
  font-size: 12px;
  color: #aaa;
}

.del {
  margin-left: auto;
  padding: 2px 8px;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  color: #aaa;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.del:hover {
  background: var(--primary-soft);
  color: var(--primary);
}

.text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #555;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty {
  margin: 0;
  padding: 16px 0;
  font-size: 13px;
  color: #999;
}

.empty a {
  color: var(--primary);
  text-decoration: none;
}
</style>
