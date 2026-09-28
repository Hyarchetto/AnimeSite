// 追番状态
//
// 每张 AnimeCard 都要知道「这部番我收了没有」。让卡片各自去问后端就是 N 次请求，
// 所以收在一个 store 里：列表拿一次，id 集合供卡片查
//
// ids 和 items 是同一份数据的两种视图。ids 给卡片做 O(1) 判断，
// items 给追番页渲染，切换时两个一起更新，不会出现「卡片说收了但列表里没有」

import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { request } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

export const useWatchlistStore = defineStore('watchlist', () => {
  const auth = useAuthStore()

  const ids = ref(new Set())
  const items = ref([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      items.value = await request('/api/watchlist')
      ids.value = new Set(items.value.map((anime) => anime.id))
    } finally {
      // 失败时也要复位，否则页面会永远停在「加载中」
      loading.value = false
    }
  }

  /**
   * 收藏状态取反
   *
   * 传整个番剧对象而不只是 id，是因为加入时要把它塞进 items。
   * 卡片手上那份数据的字段和 /api/watchlist 返回的完全一致，可以直接用
   *
   * 两个接口都是幂等的，所以这里不做「先查再改」，直接发请求
   */
  async function toggle(anime) {
    if (ids.value.has(anime.id)) {
      await request(`/api/watchlist/${anime.id}`, { method: 'DELETE' })
      ids.value.delete(anime.id)
      items.value = items.value.filter((item) => item.id !== anime.id)
      return false
    }

    await request('/api/watchlist', { method: 'POST', body: { animeId: anime.id } })
    ids.value.add(anime.id)
    // 新收的排在最前，和后端按收藏时间倒序的排法一致
    items.value = [anime, ...items.value]
    return true
  }

  function isCollected(animeId) {
    return ids.value.has(animeId)
  }

  function clear() {
    ids.value = new Set()
    items.value = []
  }

  // 跟着登录状态走。换用户和退出登录都在这里一次处理掉，
  // 不用指望每个页面都记得清缓存——漏一个就会把上一个用户的收藏显示给下一个人
  watch(
    () => auth.user?.id,
    (userId) => {
      if (userId) {
        load().catch(() => clear())
      } else {
        clear()
      }
    },
    { immediate: true }
  )

  return { ids, items, loading, load, toggle, isCollected, clear }
})
