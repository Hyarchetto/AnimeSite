<template>
  <nav class="navbar">
    <!-- Logo -->
    <div class="logo">
      <img :src="logoSrc" alt="芝士动漫" />
    </div>

    <!-- 新版菜单 -->
    <ul class="menu">
      <li><router-link to="/" class="nav-item">首页</router-link></li>
      <li><router-link to="/anime" class="nav-item">番剧</router-link></li>
      <li><router-link to="/watchlist" class="nav-item">我的追番</router-link></li>
      <li><router-link to="/history" class="nav-item">观看记录</router-link></li>
      <li v-if="auth.isAdmin"><router-link to="/admin" class="nav-item">管理后台</router-link></li>
    </ul>
  </nav>
</template>

<script setup>
// 导入 SVG 文件
import cheeseAnimeLogo from '@/assets/images/cheese-anime-logo.svg';
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const logoSrc = cheeseAnimeLogo;
</script>

<style scoped>
.navbar {
  width: 200px;
  background: white;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  padding: 20px 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 0 20px 20px; /* 上下留空 */
  text-align: center;
  flex: none; /* 不参与伸缩 */
  margin: 0 auto; /* 水平居中 */
}

.logo img {
  width: 180px; /* 留点边距 */
  height: auto;
  display: block; /* 避免底部空白 */
  margin: 0 auto; /* 再次确保居中 */
}

.menu {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1; /* 让菜单占满中间空间 */
}

.menu li {
  padding: 12px 20px;
}

.nav-item {
  text-decoration: none;
  color: #555;
  font-size: 16px;
  display: block;
  border-radius: var(--radius-md);
  transition: all 0.2s;
  padding: 8px 12px;
}

.nav-item:hover,
.nav-item.router-link-exact-active {
  color: var(--primary);
  background-color: var(--primary-soft);
  font-weight: bold;
}
</style>
