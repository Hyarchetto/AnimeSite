<!-- src/components/SearchBar.vue -->
<template>
  <div class="search-bar">
    <input
      v-model="query"
      type="text"
      placeholder="输入番剧关键词，请少字也别错字了…"
      @keyup.enter="handleSearch"
      class="search-input"
    />
    <button @click="handleSearch" class="search-btn">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M21 21L15 15M17 10C17 13.866 13.866 17 10 17C6.13401 17 3 13.866 3 10C3 6.13401 6.13401 3 10 3C13.866 3 17 6.13401 17 10Z" stroke="#666" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const query = ref('')

const emit = defineEmits(['search'])

function handleSearch() {
  if (!query.value.trim()) return
  emit('search', query.value)
  query.value = ''
}
</script>

<style scoped>
.search-bar {
  margin-left: 20px;
  display: flex;
  align-items: center;
  position: relative;
  width: 100%;
  max-width: 500px;
  border-radius: var(--radius-md);
  overflow: hidden; /* 关键！隐藏溢出 */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

/* 输入框：左圆角 */
.search-input {
  flex: 1;
  padding: 12px 16px;
  border: none; /* 移除边框 */
  background-color: #f9f9f9;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  color: #333;
}

.search-input:focus {
  box-shadow: 0 0 0 2px rgba(233, 69, 96, 0.1);
}

/* 搜索按钮：右圆角 */
.search-btn {
  padding: 10px 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}

.search-btn:hover {
  background-color: #f0f0f0;
}

.search-btn svg {
  color: #666;
  transition: color 0.2s;
}

.search-btn:hover svg {
  color: #333;
}
</style>