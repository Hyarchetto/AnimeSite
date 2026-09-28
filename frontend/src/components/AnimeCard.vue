<template>
  <div class="anime-card">
    <!-- 封面和标题是可点的，进详情页。收藏按钮不放这里——
         卡片在首页是横排的一小格，放不下一个像样的操作区，
         而且「追番」这种事本来就要看完简介再决定 -->
    <router-link :to="`/anime/${anime.id}`" class="card-link">
      <div class="cover-container">
        <img :src="resolveImageUrl(anime.cover)" alt="封面" />
      </div>
      <h3>{{ anime.title }}</h3>
    </router-link>

    <p class="latest">{{ anime.latest }}</p>
    <!-- 额外说明，比如观看记录页的「看到第 3 集」 -->
    <p v-if="note" class="note">{{ note }}</p>

    <!-- 给调用方挂场景专属的操作。观看记录页用它放右上角那个删除按钮，
         管理后台以后可以放编辑和上下架。
         不塞成一堆 prop 开关，是因为「这个场景要显示哪些按钮」是调用方知道的事 -->
    <slot></slot>
  </div>
</template>

<script setup>
import { resolveImageUrl } from '@/utils/image'

defineProps({
  anime: {
    type: Object,
    required: true
  },
  /** 标题下面那行的补充说明，不传就不显示 */
  note: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.anime-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin: 0 2px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
  flex-shrink: 0;
  width: 190px;
  /* 插槽里的内容要能挂到卡片右上角。overflow: hidden 保证它不会溢出卡片边界 */
  position: relative;
}

.anime-card:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

/* 封面容器 */
.cover-container {
  width: 100%;
  height: auto;
  border-radius: var(--radius-lg);
  overflow: hidden;
  aspect-ratio: 2 / 3;
  position: relative;
}

.cover-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-link {
  display: block;
  text-decoration: none;
  color: inherit;
}

/* 标题 */
.anime-card h3 {
  font-size: 14px;
  color: #333;
  margin: 4px 0;
  line-height: 1.4;
  font-weight: 600;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.3s ease;
}

.anime-card:hover h3 {
  color: var(--primary); /* 悬停变色 */
}

/* 最新状态 */
.anime-card .latest {
  font-size: 12px;
  color: #666;
  margin: -2px 0 4px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 补充说明，比如看到第几集 */
.anime-card .note {
  font-size: 12px;
  color: var(--primary);
  margin: 0 0 4px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
