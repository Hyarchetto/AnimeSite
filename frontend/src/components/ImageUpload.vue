<!-- src/components/ImageUpload.vue -->
<template>
  <div class="image-upload">
    <span class="field-name">{{ label }}</span>

    <div class="row">
      <!-- 预览。没有图时给个和实际比例一致的虚线框，管理员能看出该传什么形状的 -->
      <div class="preview" :style="{ aspectRatio: aspect }">
        <img v-if="modelValue" :src="resolveImageUrl(modelValue)" alt="预览" />
        <span v-else class="placeholder">暂无图片</span>
      </div>

      <div class="ops">
        <label class="pick" :class="{ busy: uploading }">
          {{ uploading ? '上传中...' : '选择图片' }}
          <!-- 原生 file 输入框没法改样式，藏起来用 label 触发。
               上传中禁用，免得连点传上去好几张 -->
          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            :disabled="uploading"
            @change="onPick"
          />
        </label>
        <button v-if="modelValue" type="button" @click="$emit('update:modelValue', '')">清除</button>
        <p class="path">{{ modelValue || '未设置' }}</p>
      </div>
    </div>

    <small v-if="hint">{{ hint }}</small>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { request } from '@/utils/request'
import { resolveImageUrl } from '@/utils/image'
import { useToastStore } from '@/stores/toast'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '图片' },
  /** 预览框的宽高比。竖版封面是 2/3，轮播横图是 16/9 */
  aspect: { type: String, default: '2 / 3' },
  hint: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const toasts = useToastStore()
const fileInput = ref(null)
const uploading = ref(false)

async function onPick(event) {
  const file = event.target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    // 上传和应用分开：这里只拿到地址，写进表单，等表单整体保存时才生效
    const result = await request('/api/admin/upload', { method: 'POST', body: form })
    emit('update:modelValue', result.url)
  } catch (e) {
    // 用浮层提示，不要在表单里插一行字——那会把弹窗撑高
    toasts.show(e.message)
  } finally {
    uploading.value = false
    // 清掉选择，否则连着选同一个文件不会再触发 change
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>

<style scoped>
.image-upload {
  display: block;
  margin-bottom: 16px;
}

.field-name {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #555;
}

.row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.preview {
  width: 90px;
  flex: none;
  border: 1px dashed #e0e0e0;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.placeholder {
  font-size: 11px;
  color: #bbb;
}

.ops {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pick {
  display: inline-block;
  padding: 7px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.pick:hover:not(.busy) {
  background: #eee;
  color: #333;
}

.pick.busy {
  opacity: 0.6;
  cursor: not-allowed;
}

.pick input {
  display: none;
}

.ops button {
  padding: 7px 14px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: var(--bg-page);
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.ops button:hover {
  background: #eee;
  color: #333;
}

/* 现用路径完整显示。截断的话管理员没法核对传对没有 */
.path {
  flex-basis: 100%;
  margin: 0;
  font-size: 12px;
  color: #aaa;
  word-break: break-all;
}

small {
  display: block;
  margin-top: 8px;
  color: #aaa;
  font-size: 12px;
  line-height: 1.6;
}
</style>
