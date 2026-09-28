<!-- src/views/UserProfile.vue -->
<template>
  <div class="profile">
    <p v-if="loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>

    <template v-else>
      <section class="head">
        <img class="avatar" :src="resolveAvatar(profile.avatar)" alt="头像" />
        <div class="head-info">
          <h2>
            {{ profile.nickname }}
            <span v-if="isSelf" class="self">这是你</span>
          </h2>
          <p class="signature">{{ profile.signature || '这个人很懒，什么也没写' }}</p>
          <p class="meta">加入于 {{ formatDate(profile.createdAt) }}</p>
          <router-link v-if="isSelf" to="/settings" class="edit">去设置里改资料</router-link>
        </div>
      </section>

      <!-- 观看记录只给自己看。后端也没有「看别人观看记录」的接口 -->
      <section v-if="isSelf">
        <h3>我的观看记录<span class="sub">共 {{ history.length }} 部</span></h3>
        <p v-if="!history.length" class="empty">
          还没有看过。去<router-link to="/anime">番剧</router-link>页看看吧
        </p>
        <ul v-else class="list">
          <li v-for="item in history" :key="item.id">
            <router-link :to="`/anime/${item.id}`" class="list-link">
              <img :src="resolveImageUrl(item.cover)" alt="" />
              <div>
                <p class="t">{{ item.title }}</p>
                <p class="d">看到第 {{ item.episodeNo }} 集</p>
              </div>
            </router-link>
          </li>
        </ul>
      </section>

      <section>
        <h3>{{ isSelf ? '我发过的评论' : 'TA 发过的评论' }}<span class="sub">共 {{ comments.length }} 条</span></h3>
        <p v-if="!comments.length" class="empty">还没有发过评论</p>
        <ul v-else class="list comments">
          <li v-for="c in comments" :key="c.id">
            <router-link :to="`/anime/${c.animeId}`" class="list-link">
              <img :src="resolveImageUrl(c.animeCover)" alt="" />
              <div>
                <p class="t">{{ c.animeTitle }}</p>
                <p class="d">{{ formatTime(c.createdAt) }}</p>
              </div>
            </router-link>
            <p class="content">{{ c.content }}</p>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { request } from '@/utils/request'
import { resolveAvatar, resolveImageUrl } from '@/utils/image'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => auth.user?.id === userId.value)

const profile = ref(null)
const comments = ref([])
const history = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [p, c] = await Promise.all([
      request(`/api/users/${userId.value}`),
      request(`/api/users/${userId.value}/comments`)
    ])
    profile.value = p
    comments.value = c

    // 观看记录只有自己的才拉。后端也没有查别人观看记录的接口，
    // 这一条是前端跟着后端的可见性规则走
    if (isSelf.value) {
      history.value = await request('/api/history')
    } else {
      history.value = []
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
// 自己的昵称在设置页改过之后回到这一页要能看到新的，所以跟着刷新一次
watch(() => [userId.value, auth.user?.id], load)

const formatDate = (v) => (v ? v.slice(0, 10) : '')
const formatTime = (v) => (v ? v.replace('T', ' ').slice(0, 16) : '')
</script>

<style scoped>
.profile {
  max-width: 760px;
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

.head {
  display: flex;
  gap: 22px;
}

.avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.head-info {
  flex: 1;
  min-width: 0;
}

.head-info h2 {
  margin: 4px 0 10px;
  font-size: 20px;
  color: #333;
}

.self {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: normal;
  vertical-align: middle;
}

.signature {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #555;
  word-break: break-word;
}

.meta {
  margin: 0 0 10px;
  font-size: 12px;
  color: #999;
}

.edit {
  display: inline-block;
  padding: 6px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #666;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}

.edit:hover {
  background: #eee;
  color: #333;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.list li {
  padding: 12px 0;
  border-top: 1px solid #f5f5f5;
}

.list-link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.list-link img {
  width: 40px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  flex: none;
  background: #f5f5f5;
}

.t {
  margin: 0 0 4px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.list-link:hover .t {
  color: var(--primary);
}

.d {
  margin: 0;
  font-size: 12px;
  color: #999;
}

/* 评论那部分的封面窄一点，配的是评论不是番剧卡 */
.comments .list-link img {
  height: 40px;
}

.content {
  margin: 10px 0 0;
  padding-left: 52px;
  font-size: 14px;
  line-height: 1.7;
  color: #555;
  word-break: break-word;
}

.empty {
  margin: 0;
  padding: 12px 0;
  font-size: 13px;
  color: #999;
}

.empty a {
  color: var(--primary);
  text-decoration: none;
}
</style>
