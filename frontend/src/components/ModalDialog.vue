<!-- src/components/ModalDialog.vue -->
<template>
  <!-- Teleport 到 body：弹窗固定在视口中央，留在原位置会被祖先的
       overflow 裁掉或者被层叠上下文困住，侧边栏的 z-index 是 100 会压住它 -->
  <Teleport to="body">
    <!-- 遮罩不响应点击。弹窗里常是填了一半的表单，手滑点到边缘外就关掉的话
         东西全丢，而且丢掉的过程看不出来。要关只能点右上角那个 × -->
    <div v-if="open" class="mask">
      <div class="dialog" :style="{ width }">
        <header>
          <h3>{{ title }}</h3>
          <button class="close" type="button" @click="$emit('close')">×</button>
        </header>
        <div class="body">
          <slot></slot>
        </div>
        <footer v-if="$slots.footer">
          <slot name="footer"></slot>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '440px' }
})

defineEmits(['close'])
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1000;
}

.dialog {
  max-width: 100%;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}

h3 {
  margin: 0;
  font-size: 15px;
  color: #333;
}

.close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-md);
  background: none;
  color: #999;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
}

.close:hover {
  background: #f5f5f5;
  color: #333;
}

.body {
  padding: 20px;
  overflow-y: auto;
}

footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
}

/* 底部按钮的基础样式放这里，各个弹窗不用自己写一遍。
   用 :deep 是因为插槽内容带的是**调用方**的 scoped 属性，不是本组件的，
   不穿透的话这些规则匹配不上，按钮会退回浏览器默认样式 */
/* 尺寸跟着弹窗正文走，不要比标题还显眼。
   定得比标题（15px）小一号，宽度也收窄——底部那一排太厚重的话，
   整个弹窗会显得头轻脚重 */
footer :deep(button) {
  min-width: 72px;
  padding: 7px 16px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: #fff;
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

footer :deep(button:hover:not(:disabled)) {
  background: #f5f5f5;
  color: #333;
}

footer :deep(button:disabled) {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 破坏性操作。填色而不是只改字色——确认删除和取消长得一样的话，
   手快点错就是删掉一条数据 */
footer :deep(button.danger) {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
}

footer :deep(button.danger:hover:not(:disabled)) {
  border-color: var(--primary-hover);
  background: var(--primary-hover);
  color: #fff;
}
</style>
