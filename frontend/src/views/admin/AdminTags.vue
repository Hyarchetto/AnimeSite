<!-- src/views/admin/AdminTags.vue -->
<template>
  <div class="admin-tags">
    <div class="page-head">
      <h2 class="page-title">标签管理</h2>
      <button class="primary" @click="openCreate">新建标签</button>
    </div>

    <p class="tip">
      标签由管理员统一创建。番剧打标签在「番剧管理」的编辑表单里选。
      删除标签**不会删番剧**，只是那些番不再有这个标签。
    </p>

    <p v-if="loading" class="page-hint">加载中...</p>
    <p v-else-if="error" class="page-hint">{{ error }}</p>
    <p v-else-if="!items.length" class="page-hint">还没有标签</p>

    <table v-else>
      <thead>
        <tr>
          <th>标签</th>
          <th class="col-count">用了几部番</th>
          <th class="col-actions">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="tag in items" :key="tag.id" :class="{ unused: !tag.animeCount }">
          <td class="name">{{ tag.name }}</td>
          <td class="muted">
            {{ tag.animeCount }} 部
            <span v-if="!tag.animeCount" class="tag">没人用</span>
          </td>
          <td class="col-actions">
            <!-- 按钮包一层 div，不要直接把 td 设成 flex，那样它就不是表格单元格了，
                 会脱离垂直对齐，按钮跑到格子顶部 -->
            <div class="row-actions">
              <button @click="openEdit(tag)">改名</button>
              <button class="danger" @click="deleting = tag">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <ModalDialog
      :open="formOpen"
      :title="editing ? '重命名标签' : '新建标签'"
      @close="formOpen = false"
    >
      <label>
        <span>标签名</span>
        <input
          v-model.trim="name"
          type="text"
          maxlength="50"
          placeholder="如：奇幻、恋爱"
          @keydown.enter="save"
        />
        <small>标签名不能重复，否则会出现「恋爱」和「爱情」指同一件事</small>
      </label>

      <template #footer>
        <button @click="formOpen = false">取消</button>
        <button class="primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </ModalDialog>

    <!-- 标题说删什么，按钮说做什么 -->
    <ModalDialog :open="!!deleting" title="删除标签" @close="deleting = null">
      <p class="confirm">要删除标签「<b>{{ deleting?.name }}</b>」吗？</p>
      <p class="confirm-warn">
        <template v-if="deleting?.animeCount">
          有 <b>{{ deleting.animeCount }}</b> 部番在用这个标签，删掉之后它们不再有这个标签，
          <b>番剧本身不会被删除</b>。
        </template>
        <template v-else>这个标签还没有被任何番剧使用。</template>
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
import ModalDialog from '@/components/ModalDialog.vue'
import { request } from '@/utils/request'
import { useToastStore } from '@/stores/toast'

const toasts = useToastStore()

const items = ref([])
const loading = ref(true)
const error = ref('')

const formOpen = ref(false)
const editing = ref(null)
const name = ref('')
const saving = ref(false)

const deleting = ref(null)
const deleting2 = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = await request('/api/admin/tags')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  name.value = ''
  formOpen.value = true
}

function openEdit(tag) {
  editing.value = tag
  name.value = tag.name
  formOpen.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await request(`/api/admin/tags/${editing.value.id}`, {
        method: 'PUT',
        body: { name: name.value }
      })
    } else {
      await request('/api/admin/tags', { method: 'POST', body: { name: name.value } })
    }
    formOpen.value = false
    await load()
  } catch (e) {
    toasts.show(e.message)
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  deleting2.value = true
  try {
    await request(`/api/admin/tags/${deleting.value.id}`, { method: 'DELETE' })
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
/* 同番剧管理：占满可用宽度，右侧不留空 */
.admin-tags {
  padding: 0;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.page-head .page-title {
  margin: 0;
}

.tip {
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

button {
  padding: 7px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

button:hover:not(:disabled) {
  background: #eee;
  color: #333;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

.primary:hover:not(:disabled) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}

.danger {
  color: var(--primary);
}

.danger:hover:not(:disabled) {
  background: var(--primary-soft);
  border-color: var(--primary);
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  /* 固定布局，列宽不跟着内容变。标签名长短差很多，
     自动布局下会被最长的那条把整张表撑歪 */
  table-layout: fixed;
}

th,
td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid #f5f5f5;
  vertical-align: middle;
}

th {
  background: #fafafa;
  color: #888;
  font-weight: normal;
  white-space: nowrap;
}

.col-count {
  width: 150px;
}

.col-actions {
  width: 160px;
}

tr.unused {
  background: #fcfcfc;
}

tr.unused .name {
  color: #aaa;
}

.name {
  font-weight: 500;
  color: #333;
}

.muted {
  color: #888;
}

.tag {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: #f0f0f0;
  color: #999;
  font-size: 11px;
}

.row-actions {
  display: flex;
  gap: 8px;
}

label {
  display: block;
}

label span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #555;
}

label small {
  display: block;
  margin-top: 6px;
  color: #aaa;
  font-size: 12px;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: #333;
  transition: all 0.2s;
}

input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
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
