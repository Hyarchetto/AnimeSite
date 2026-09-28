<template>
  <div class="section">
    <div class="section-header">
      <div class="h">{{ title }}</div>
      <router-link :to="to" class="more-btn">更多</router-link>
    </div>
    <div class="anime-list">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  to: {
    type: String,
    required: true
  }
})
</script>

<style scoped>
.h {
  font-size: 28px;
  color: #333;
  margin: -30px 0 -20px 10px;
  font-weight: bold;
  font-style: normal; /* 或者 italic 来倾斜字体 */
  text-shadow: 2px 2px 4px rgba(0,0,0,0.3); /* 添加一些阴影 */
  letter-spacing: 0.5px; /* 增加字母间的间距 */
}
.section {
  margin: 30px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h2 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.more-btn {
  color: #666;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  background-color: #f5f5f5;
  transition: all 0.2s;
}

.more-btn:hover {
  background-color: #e0e0e0;
  color: #333;
}

.anime-list {
  display: flex;
  gap: 16px;
  padding: 8px 0;
  scroll-behavior: smooth;
  white-space: nowrap;
  /* 少了这一行，卡片放不下时会把整个页面撑宽，页面能左右拖动。
     卡片是 flex-shrink: 0 不让收缩的，所以必须由这一层来裁 */
  overflow-x: auto;
  /* 放不下时只差几十像素，原生滚动条又粗又显眼，为这点距离挂一整条不划算。
     藏掉外观，拖动、触控板和 shift+滚轮仍然能滚 */
  scrollbar-width: none;
}

.anime-list::-webkit-scrollbar {
  display: none;
}
</style>