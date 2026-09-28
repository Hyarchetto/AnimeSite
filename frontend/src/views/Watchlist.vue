<!-- src/views/Watchlist.vue -->
<template>
  <div class="watchlist">
    <h2 class="page-title">我的追番</h2>

    <p v-if="watchlist.loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!watchlist.items.length" class="page-hint">
      还没有追番。去<router-link to="/anime">番剧</router-link>页，把鼠标移到封面上点「追番」
    </p>
    <div v-else class="card-grid">
      <!-- 卡片上的按钮此时显示「已追番」，点它就是取消，取消后这张卡会从这个列表里消失 -->
      <AnimeCard v-for="anime in watchlist.items" :key="anime.id" :anime="anime" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AnimeCard from '@/components/AnimeCard.vue'
import { useWatchlistStore } from '@/stores/watchlist'

const watchlist = useWatchlistStore()
const error = ref('')

// store 的 watcher 在登录时已经拉过一次，但那时可能还没有 token 或者刚失败过，
// 进这个页面时再确认一次，保证看到的是最新的
onMounted(async () => {
  error.value = ''
  try {
    await watchlist.load()
  } catch (e) {
    error.value = e.message
  }
})
</script>

<style scoped>
.watchlist {
  padding: 4px 0;
}
</style>
