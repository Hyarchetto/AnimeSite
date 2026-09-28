<template>
  <div id="app">
    <Navbar />

    <!-- 顶部栏：搜索在左，用户区在右。观看记录已经在左侧导航里，这里不再重复放 -->
    <header class="topbar">
      <SearchBar @search="onSearch" />
      <UserMenu />
    </header>

    <!-- 浮层提示。放在这里一次，任何页面触发都显示 -->
    <ToastHost />

    <main class="main-content">
      <!-- key 用 path 而不是 fullPath。
           用 path 是为了让 /anime/4 跳到 /anime/2 时重建组件——不重建的话
           Vue Router 会复用实例，onMounted 不再执行，页面还是上一部番的数据，
           setup 里取到的 animeId 也是旧的，点选集会记到上一部番头上。

           用 fullPath 就过头了：搜索页的筛选条件在查询参数里，
           每点一个标签都会把整页重建一次，输入框和已选标签全部丢失 -->
      <router-view :key="$route.path" />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Navbar from '@/components/Navbar.vue'
import SearchBar from '@/components/SearchBar.vue'
import UserMenu from '@/components/UserMenu.vue'
import ToastHost from '@/components/ToastHost.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

// 搜索跳番剧页，那一页本身就是搜索页。
// 原来跳的是 /search，但那个路由从来没注册过，点了是一片空白
function onSearch(q) {
  router.push({ path: '/anime', query: q ? { q } : {} })
}

// 启动时用本地 token 换一次最新的用户信息。本地缓存里的 role 可能是旧的，
// 管理员把某人降级之后，对方浏览器里存的那份还是 ADMIN。
// token 已经失效的话 request 内部会清掉登录态并跳登录页，这里不用再处理
onMounted(() => {
  auth.refresh().catch(() => {})
})
</script>

<style>
/* 全局设计变量。
   App.vue 这段 style 没加 scoped，本来就是项目的全局样式表，
   所以变量定义也放这儿。
   定义在 :root 上而不是某个容器上，这样各个组件里的 scoped 样式也能直接用——
   CSS 变量是继承的，不受 scoped 属性影响。

   主色原来散在 11 个文件、46 处，改一次配色要全项目搜一遍 */
:root {
  /* 主色及其衍生。换配色只动这四行。
     这里是**唯一**该出现色值字面量的地方，其余全部用变量 */
  --primary: #e94560;
  --primary-hover: #d13a53;
  --primary-soft: #fff0f3;
  --primary-soft-hover: #ffe0e6;

  /* 背景 */
  --bg-page: #f7f8fb;

  /* 圆角三档：小控件 / 按钮输入框 / 卡片弹窗 */
  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 12px;

  /* 系统字体栈。中文交给系统挑，不硬写 Arial——
     硬写的话中文会回落到别的字体，字重和行高和其它页面就对不上 */
  --font: system-ui, -apple-system, 'Segoe UI', 'Microsoft YaHei', sans-serif;
}

/* 应用级布局 */
#app {
  min-height: 100vh;
  font-family: var(--font);
}

/* 浏览器默认给 body 8px 外边距，会在这套固定定位的布局外面留一圈白边。
   侧边栏和顶栏是 fixed 不受影响，主内容区却会整体偏移 8px */
body {
  margin: 0;
}

/* 顶部搜索栏 */
.topbar {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between; /* ← 关键：SearchBar 左对齐，用户区右对齐 */
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  z-index: 90;
}

/* 主内容区。
   顶栏是 fixed 的，会盖住内容区顶部，所以上面要留出它那 64px 再加一点间距。
   这个 padding-top 必须写在简写里，单独写一行会被后面的简写整个覆盖掉 */
.main-content {
  margin-left: 200px;
  /* box-sizing 不设的话，width 只是内容宽，再加左右 padding 就比可用宽度多出 48px */
  box-sizing: border-box;
  width: calc(100% - 200px);
  min-height: 100vh;
  padding: 84px 24px 24px; /* 上 = 顶栏 64 + 间距 20 */
  background: var(--bg-page);
}

/* 卡片网格：追番页、观看记录页、番剧页共用。
   App.vue 的 style 没加 scoped，实际就是全局样式表，所以这条放这里只有一份 */
.card-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

/* 页面通用的小标题和提示文案 */
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #333;
}

.page-hint {
  margin: 0;
  padding: 40px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.page-hint a {
  color: var(--primary);
  text-decoration: none;
}

.page-hint a:hover {
  text-decoration: underline;
}

/* 响应式：小屏隐藏左侧菜单，topbar 全宽 */
@media (max-width: 768px) {
  .topbar {
    left: 0;
    width: 100%;
  }
  .main-content {
    margin-left: 0;
    width: 100%;
  }
}
</style>