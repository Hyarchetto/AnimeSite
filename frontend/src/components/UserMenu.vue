<!-- src/components/UserMenu.vue -->
<template>
  <div ref="root" class="user-menu">
    <router-link v-if="!auth.isLoggedIn" to="/login" class="avatar login">登录</router-link>

    <template v-else>
      <button class="avatar" :title="auth.displayName" @click="open = !open">
        <img :src="resolveAvatar(auth.user?.avatar)" alt="头像" />
      </button>

      <div v-if="open" class="dropdown">
        <!-- 用户名。点进自己的资料页 -->
        <router-link :to="`/user/${auth.user.id}`" class="who" @click="open = false">
          <img class="who-avatar" :src="resolveAvatar(auth.user?.avatar)" alt="" />
          <span class="who-name">{{ auth.displayName }}</span>
        </router-link>

        <div class="divider"></div>

        <router-link :to="`/user/${auth.user.id}`" class="item" @click="open = false">
          个人资料
        </router-link>
        <router-link to="/settings" class="item" @click="open = false">个人设置</router-link>

        <!-- 管理是管理员才有的，鼠标移上去展开二级。
             只列已经在的路由，没做的先不放，免得点了是空白页 -->
        <div v-if="auth.isAdmin" class="group" @mouseenter="adminOpen = true" @mouseleave="adminOpen = false">
          <span class="item group-title">
            管理<span class="arrow" :class="{ down: adminOpen }">›</span>
          </span>
          <div v-if="adminOpen" class="submenu">
            <router-link to="/admin/anime" class="item sub" @click="closeAll">番剧管理</router-link>
            <router-link to="/admin/tags" class="item sub" @click="closeAll">标签管理</router-link>
            <router-link to="/admin/banners" class="item sub" @click="closeAll">轮播管理</router-link>
            <router-link to="/admin/users" class="item sub" @click="closeAll">用户管理</router-link>
          </div>
        </div>

        <div class="divider"></div>

        <button class="item" @click="onLogout">退出</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveAvatar } from '@/utils/image'

const auth = useAuthStore()
const router = useRouter()

const root = ref(null)
const open = ref(false)
const adminOpen = ref(false)

function closeAll() {
  open.value = false
  adminOpen.value = false
}

function onLogout() {
  closeAll()
  auth.logout()
  router.push('/')
}

// 点面板外面就关掉。不用 @blur，因为点面板里的链接也会触发失焦，
// 面板会在跳转之前先消失，看着像闪了一下
function onDocumentClick(event) {
  if (open.value && root.value && !root.value.contains(event.target)) {
    closeAll()
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))
</script>

<style scoped>
/* 右边距靠 topbar 自己的 padding 留，这里不用再加 */
.user-menu {
  position: relative;
  margin-right: 12px;
}

.avatar {
  display: block;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 50%;
  overflow: hidden;
  background: #f5f5f5;
  cursor: pointer;
  transition: all 0.2s;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar:hover {
  box-shadow: 0 0 0 3px var(--primary-soft);
}

/* 未登录时头像框里直接写「登录」两个字 */
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 500;
  text-decoration: none;
}

.login:hover {
  background: var(--primary-soft-hover);
}

.dropdown {
  position: absolute;
  top: 44px;
  right: 0;
  min-width: 168px;
  padding: 6px 0;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  z-index: 120;
}

/* 用户名那一块，做得像个可点的头部 */
.who {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  text-decoration: none;
  transition: all 0.2s;
}

.who:hover {
  background: #fafafa;
}

.who-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
}

.who-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.divider {
  height: 1px;
  margin: 6px 0;
  background: #f0f0f0;
}

.item {
  display: block;
  width: 100%;
  box-sizing: border-box;
  padding: 9px 14px;
  border: none;
  background: none;
  color: #555;
  font-size: 13px;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}

.item:hover {
  background: var(--primary-soft);
  color: var(--primary);
}

/* 管理那一组。相对定位是为了让二级菜单挂到它下面 */
.group {
  position: relative;
}

.group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: default;
}

.arrow {
  color: #bbb;
  transition: transform 0.2s;
}

.arrow.down {
  transform: rotate(90deg);
}

.submenu {
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.sub {
  padding-left: 26px;
  font-size: 13px;
  color: #666;
}
</style>
