import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Home from '@/views/Home.vue'
import AnimeList from '@/views/AnimeList.vue'
import Watchlist from '@/views/Watchlist.vue'
import History from '@/views/History.vue'
import Login from '@/views/Login.vue'
import Settings from '@/views/Settings.vue'
import AnimeDetail from '@/views/AnimeDetail.vue'
import AdminAnime from '@/views/admin/AdminAnime.vue'
import AdminEpisodes from '@/views/admin/AdminEpisodes.vue'
import AdminTags from '@/views/admin/AdminTags.vue'
import AdminBanners from '@/views/admin/AdminBanners.vue'
import AdminUsers from '@/views/admin/AdminUsers.vue'
import UserProfile from '@/views/UserProfile.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/anime', component: AnimeList },
  // 放在 /anime 后面，两条不冲突：一条没有参数，一条必须有
  { path: '/anime/:id', component: AnimeDetail },
  { path: '/watchlist', component: Watchlist, meta: { requiresAuth: true } },
  { path: '/history', component: History, meta: { requiresAuth: true } },
  { path: '/settings', component: Settings, meta: { requiresAuth: true } },
  { path: '/login', component: Login },
  // 公开的用户资料，谁都看得到。不需要登录，所以没有 meta
  { path: '/user/:id', component: UserProfile },

  // 管理后台。/admin 直接进番剧管理，以后加了别的再改成后台首页
  { path: '/admin', redirect: '/admin/anime' },
  {
    path: '/admin/anime',
    component: AdminAnime,
    // 两个都写。只写 requiresAdmin 的话，未登录的人会被当成「不是管理员」
    // 打发回首页，而不是引导去登录
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/anime/:id/episodes',
    component: AdminEpisodes,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/tags',
    component: AdminTags,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/banners',
    component: AdminBanners,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/users',
    component: AdminUsers,
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 守卫只做「要不要放行页面」这一件事。真正的权限在后端，
// 前端把状态改掉最多是看到一个点不动的界面
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    // 带上原地址，登录后跳回去，而不是一律回首页
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { path: '/' }
  }

  if (to.path === '/login' && auth.isLoggedIn) {
    return { path: '/' }
  }
})

export default router
