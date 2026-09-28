<!-- src/views/History.vue -->
<template>
  <div class="history">
    <h2 class="page-title">观看记录</h2>

    <p v-if="loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!items.length" class="page-hint">
      还没有观看记录。去<router-link to="/anime">番剧</router-link>页看看吧
    </p>
    <div v-else class="card-grid">
      <AnimeCard
        v-for="item in items"
        :key="item.id"
        :anime="item"
        :note="`看到第 ${item.episodeNo} 集`"
      >
        <button class="remove" title="删除这条观看记录" @click.stop="askDelete(item)">删除</button>
      </AnimeCard>
    </div>

    <!-- 标题说删什么，按钮说做什么。两处都写「确认删除」的话同一个词出现两次，
         下面那个还是加宽的按钮，视觉上底部会压过顶部 -->
    <ModalDialog :open="!!deleting" title="删除观看记录" @close="deleting = null">
      <p class="confirm">要删除《<b>{{ deleting?.title }}</b>》的观看记录吗？</p>
      <p class="confirm-warn">
        只删这条记录，<b>番剧本身和你的追番都不受影响</b>。
        以后再看这部番时，记录会自动重新出现。
      </p>
      <template #footer>
        <button @click="deleting = null">取消</button>
        <button class="danger" :disabled="deleting2" @click="confirmDelete">
          {{ deleting2 ? '删除中...' : '确认删除' }}
        </button>
      </template>
    </ModalDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AnimeCard from '@/components/AnimeCard.vue'
import ModalDialog from '@/components/ModalDialog.vue'
import { request } from '@/utils/request'
import { useToastStore } from '@/stores/toast'

const toasts = useToastStore()

const items = ref([])
const loading = ref(true)
const error = ref('')

const deleting = ref(null)
const deleting2 = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await request('/api/history')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function askDelete(item) {
  deleting.value = item
}

async function confirmDelete() {
  deleting2.value = true
  try {
    await request(`/api/history/${deleting.value.id}`, { method: 'DELETE' })
    deleting.value = null
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    deleting2.value = false
  }
}
</script>

<style scoped>
.history {
  padding: 0;
}

/* 挂在卡片右上角。定位基准是 .anime-card，它已经设了 position: relative */
.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  padding: 3px 9px;
  border: none;
  border-radius: var(--radius-sm);
  background: rgba(233, 69, 96, 0.92);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.remove:hover {
  background: var(--primary-hover);
}

.confirm {
  margin: 0 0 12px;
  font-size: 14px;
  color: #333;
}

.confirm-warn {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #888;
}
</style>
