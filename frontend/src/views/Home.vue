<!-- src/views/Home.vue -->
<template>
  <div class="home">
    <!-- 轮播海报 -->
    <Banner />

    <!-- 番剧 -->
    <Section title="番剧" to="/anime">
      <p v-if="loading" class="page-hint">加载中...</p>
      <p v-else-if="!animeList.length" class="page-hint">还没有番剧</p>
      <template v-else>
        <AnimeCard v-for="anime in animeList" :key="anime.id" :anime="anime" />
      </template>
    </Section>

    <!-- 我的追番 -->
    <Section title="我的追番" to="/watchlist">
      <!-- 未登录时给一句话，不留空白。空一块比写着「登录后可见」更让人困惑 -->
      <p v-if="!auth.isLoggedIn" class="page-hint">
        <router-link to="/login">登录</router-link>后可以追番
      </p>
      <p v-else-if="watchlist.loading" class="page-hint">加载中...</p>
      <p v-else-if="!watchlist.items.length" class="page-hint">
        还没有追番，去<router-link to="/anime">番剧</router-link>页看看
      </p>
      <template v-else>
        <AnimeCard v-for="anime in watchlistPreview" :key="anime.id" :anime="anime" />
      </template>
    </Section>

    <!-- 最近观看 -->
    <Section title="最近观看" to="/history">
      <p v-if="!auth.isLoggedIn" class="page-hint">
        <router-link to="/login">登录</router-link>后可以记录观看进度
      </p>
      <p v-else-if="!history.length" class="page-hint">还没有观看记录</p>
      <template v-else>
        <AnimeCard
          v-for="item in history"
          :key="item.id"
          :anime="item"
          :note="`看到第 ${item.episodeNo} 集`"
        />
      </template>
    </Section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import Banner from '@/components/Banner.vue'
import Section from '@/components/Section.vue'
import AnimeCard from '@/components/AnimeCard.vue'
import { request } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useWatchlistStore } from '@/stores/watchlist'

/**
 * 首页是预览，每个分区只放前几个，完整的点「更多」进各自的页面
 *
 * <p>三个分区共用这一个上限。数字不一样的话，同样的「更多」按钮点进去，
 * 有的区少一截有的区一条不少，看着像坏了
 */
const PREVIEW_COUNT = 6

const auth = useAuthStore()
const watchlist = useWatchlistStore()

const animeList = ref([])
const history = ref([])
const loading = ref(true)

const watchlistPreview = computed(() => watchlist.items.slice(0, PREVIEW_COUNT))

onMounted(async () => {
  try {
    // 不再自己排序。后端已经按首播日期倒序返回了，前端再排一遍是重复实现
    animeList.value = (await request('/api/anime')).slice(0, PREVIEW_COUNT)
  } catch (e) {
    console.error('获取番剧失败:', e.message)
  } finally {
    loading.value = false
  }
})

/**
 * 观看记录单独拉，因为只有登录了才有
 *
 * 跟着登录状态走：换用户要重新拉，退出要清掉。
 * 追番那份的监听在 store 里，这里只管历史
 */
watch(
  () => auth.user?.id,
  async (userId) => {
    if (!userId) {
      history.value = []
      return
    }
    try {
      history.value = (await request('/api/history')).slice(0, PREVIEW_COUNT)
    } catch (e) {
      console.error('获取观看记录失败:', e.message)
      history.value = []
    }
  },
  { immediate: true }
)
</script>

<style scoped>
/* 主内容区已经有 24px 的 padding 了，这里不再重复加缩进，
   否则首页会比别的页面多凹进去一块 */
.home {
  padding: 0;
}

/* 分区里那句提示和卡片是同级元素，卡片行是 flex 布局，
   不让它撑开的话会缩成一小块贴在左边 */
.home .page-hint {
  flex: 1;
  padding: 20px 0 24px;
}
</style>
