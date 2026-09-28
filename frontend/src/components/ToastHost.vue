<!-- src/components/ToastHost.vue -->
<template>
  <!-- 挂在屏幕顶部中间，不占任何页面的布局位置。
       Teleport 到 body 是为了不受任何祖先的 overflow 和层叠上下文影响 -->
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="toast-host">
      <div v-for="m in toasts.messages" :key="m.id" class="toast" :class="m.type">
        {{ m.text }}
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { useToastStore } from '@/stores/toast'

const toasts = useToastStore()
</script>

<style scoped>
.toast-host {
  position: fixed;
  top: 78px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  /* 比弹窗（1000）还高，弹窗里触发的提示也要看得见 */
  z-index: 2000;
  /* 这一层不接收点击，免得挡住下面的按钮 */
  pointer-events: none;
}

.toast {
  max-width: 420px;
  padding: 10px 18px;
  border-radius: var(--radius-md);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.toast.error {
  background: var(--primary);
  color: #fff;
}

.toast.success {
  background: #2e9e5b;
  color: #fff;
}

/* 从上方滑入淡出，位置感清楚：是从顶上掉下来的提示 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
