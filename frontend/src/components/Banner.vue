<template>
  <div class="banner">
    <!-- 轮播容器 -->
    <div class="slide-container">
      <div class="slides" ref="slides" :style="slideStyles">
        <div v-for="(item, index) in banners" :key="index" class="slide-item">
          <img :src="item.imageSrc" alt="番剧海报" @error="onImageError" />
          <div class="info-overlay">
            <div class="cover-thumb">
              <img :src="item.coverSrc" alt="小封面" @error="onImageError" />
            </div>
            <h2>{{ item.title }}</h2>
            <p class="status">{{ item.status }}</p>
            <p class="desc">{{ item.desc }}</p>
          </div>
        </div>
      </div>

      <!-- 左右切换按钮 -->
      <button class="prev" @click="prevSlide">&lt;</button>
      <button class="next" @click="nextSlide">&gt;</button>

      <!-- 底部指示器 -->
      <div class="indicators">
        <span
          v-for="(item, index) in banners"
          :key="index"
          class="indicator"
          :class="{ active: currentIndex === index }"
          @click="goToSlide(index)"
        ></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { request } from '@/utils/request'

const banners = ref([])
const currentIndex = ref(0)

// 计算样式
const slideStyles = computed(() => {
  const shift = (currentIndex.value * 100) / banners.value.length
  return {
    width: `${banners.value.length * 100}%`,
    transform: `translateX(-${shift}%)`,
    transition: 'transform 0.5s ease'
  }
})

// 轮播控制
const prevSlide = () => {
  if (banners.value.length === 0) return
  currentIndex.value = (currentIndex.value - 1 + banners.value.length) % banners.value.length
  startAutoPlay() 
}

const nextSlide = () => {
  if (banners.value.length === 0) return
  currentIndex.value = (currentIndex.value + 1) % banners.value.length
  startAutoPlay() 
}

const goToSlide = (index) => {
  currentIndex.value = index
  startAutoPlay() 
}

// 图片错误处理
const onImageError = (e) => {
  e.target.src = 'https://via.placeholder.com/1200x400?text=No+Image'
}

// 自动播放
let autoPlayTimer = null
const startAutoPlay = () => {
  stopAutoPlay()
  autoPlayTimer = setInterval(nextSlide, 5000)
}
const stopAutoPlay = () => {
  clearInterval(autoPlayTimer)
}

// 获取数据
async function fetchBanners() {
  try {
    banners.value = await request('/api/banner')
    currentIndex.value = 0
  }
  catch (err) {
    console.error('❌ 获取轮播数据失败:', err)
  }
}

onMounted(() => {
  fetchBanners()
})

onUnmounted(() => {
  stopAutoPlay()
})

watch(banners, (newVal) => {
  if (newVal.length > 0) {
    startAutoPlay()
  }
})
</script>

<style scoped>

.banner {
  width: 100%;
  max-width: 1400px;
  height: 430px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  /* margin-top 原本写的是 50px，用来躲开固定顶栏。
     顶栏的让位现在由 .main-content 的 padding-top 统一做了，这里不用再各写各的 */
  margin-bottom: 20px;
  position: relative;
}

.slide-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.slides {
  display: flex;
  transition: transform 0.5s ease;
  width: 100%;
  height: 100%;
}

.slide-item {
  flex: 100%;
  position: relative;
}

.slide-item img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover; 
  transition: transform 0.3s ease;
  user-select: none;
  -webkit-user-drag: none;
}

.info-overlay {
  position: absolute;
  top: 50%;
  right: 80px;
  transform: translateY(-50%);
  backdrop-filter: blur(2px);
  padding: 16px;
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 16px rgba(0,0,0,0.3);
  max-width: 180px;
  color: white;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.5);
}

.cover-thumb {
  width: 120px;
  height: 160px;
  margin: 0 auto 12px;
  overflow: hidden;
  border-radius: var(--radius-md);
}

.cover-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-overlay h2 {
  font-size: 1.6rem;
  margin: 0 0 8px;
  color: #fff;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-overlay .status {
  font-size: 1rem;
  color: var(--primary);
  margin: 4px 0;
  font-weight: bold;
}

.info-overlay .desc {
  font-size: 0.95rem;
  line-height: 1.4;
  margin: 4px 0;
  color: #ccc;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;

  display: -moz-box;
  display: -ms-flexbox;
  display: -webkit-flex;
  display: flex;
  flex-direction: column;
  max-height: 2.8em;
  line-clamp: 2; /* 标准属性，未来兼容 */
}

.prev,
.next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 20px;
  color: rgba(255, 255, 255, 0.9);
  z-index: 10;
  background: rgba(255, 255, 255, 0.1); /* 半透明背景 */
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1); /* 轻微阴影 */
  transition: all 0.3s ease;
}

/* 悬停效果 */
.prev:hover,
.next:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  transform: translateY(-50%) scale(1.1);
}

/* 点击时效果 */
.prev:active,
.next:active {
  transform: translateY(-50%) scale(0.95);
}

.prev {
  left: 20px;
}

.next {
  right: 20px;
}

.indicators {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 10;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.indicator.active {
  background: var(--primary);
  transform: scale(1.2);
}
</style>