<!-- src/views/Login.vue -->
<template>
  <div class="login-page">
    <div class="card">
      <h2>{{ isRegister ? '注册' : '登录' }}</h2>
      <p class="hint">
        {{ isRegister ? '注册后就是一个普通账号' : '登录后可以追番和记录观看进度' }}
      </p>

      <form @submit.prevent="submit">
        <label>
          <span>账号</span>
          <!-- autocomplete 的值保持 username 不动。它是 HTML 规范里的固定 token，
               浏览器和密码管理器靠它认出这是登录名输入框，改成 account 会失去自动填充 -->
          <input v-model.trim="form.account" type="text" autocomplete="username" placeholder="3 到 50 个字符" />
        </label>

        <label>
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            :autocomplete="isRegister ? 'new-password' : 'current-password'"
            placeholder="至少 6 位"
          />
        </label>

        <label v-if="isRegister">
          <span>用户名</span>
          <input v-model.trim="form.nickname" type="text" placeholder="选填" />
        </label>

        <button type="submit" :disabled="submitting">
          {{ submitting ? '处理中...' : (isRegister ? '注册并登录' : '登录') }}
        </button>
      </form>

      <p class="switch">
        {{ isRegister ? '已经有账号了？' : '还没有账号？' }}
        <a href="#" @click.prevent="toggle">{{ isRegister ? '去登录' : '去注册' }}</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toasts = useToastStore()
const route = useRoute()
const router = useRouter()

const isRegister = ref(false)
const submitting = ref(false)
const form = reactive({ account: '', password: '', nickname: '' })

function toggle() {
  isRegister.value = !isRegister.value
}

async function submit() {
  submitting.value = true
  try {
    if (isRegister.value) {
      await auth.register(form.account, form.password, form.nickname)
    } else {
      await auth.login(form.account, form.password)
    }
    // redirect 是守卫在拦下来时带上的原地址，没有就回首页
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect ? redirect : '/')
  } catch (e) {
    // 用浮层提示而不是写在表单里——写在表单里会把卡片撑高，
    // 按钮和下面的「还没有账号」全都往下跳一截
    toasts.show(e.message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  padding: 48px 16px;
}

.card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 32px 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #333;
}

.hint {
  margin: 0 0 24px;
  font-size: 13px;
  color: #999;
}

label {
  display: block;
  margin-bottom: 16px;
}

label span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #555;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
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

button {
  width: 100%;
  padding: 11px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

button:hover:not(:disabled) {
  background: var(--primary-hover);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.switch {
  margin: 20px 0 0;
  text-align: center;
  font-size: 13px;
  color: #666;
}

.switch a {
  color: var(--primary);
  text-decoration: none;
}

.switch a:hover {
  text-decoration: underline;
}
</style>
