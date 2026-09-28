<!-- src/views/Settings.vue -->
<template>
  <div class="settings">
    <h2 class="page-title">个人设置</h2>

    <!-- 头像 -->
    <section>
      <h3>头像</h3>
      <div class="avatar-row">
        <img class="avatar-preview" :src="resolveAvatar(auth.user?.avatar)" alt="头像" />
        <div>
          <label class="file-btn">
            选择图片
            <!-- 原生 file 输入框没法改样式，藏起来用 label 触发 -->
            <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" @change="onPickFile" />
          </label>
          <p class="tip">支持 jpg png webp，不超过 5MB。选完立即上传</p>
        </div>
      </div>
    </section>

    <!-- 用户名与签名 -->
    <section>
      <h3>资料</h3>
      <p class="tip">
        登录用的账号是 <b>{{ auth.user?.account }}</b>，用户名和签名是展示给别人看的，可以随时改。
        <router-link :to="`/user/${auth.user?.id}`" class="link">看看别人眼里的我</router-link>
      </p>
      <div class="field-row">
        <input v-model.trim="nickname" type="text" maxlength="50" placeholder="用户名" />
      </div>
      <div class="field-row">
        <textarea v-model="signature" maxlength="200" rows="2" placeholder="个性签名，留空就不显示"></textarea>
      </div>
      <div class="field-row">
        <span class="count">{{ signature.length }} / 200</span>
        <button :disabled="savingProfile" @click="saveProfile">
          {{ savingProfile ? '保存中...' : '保存' }}
        </button>
      </div>
    </section>

    <!-- 密码 -->
    <section>
      <h3>密码</h3>
      <p class="tip">改密码需要先验证原密码</p>
      <div class="field-row">
        <input v-model="oldPassword" type="password" autocomplete="current-password" placeholder="原密码" />
      </div>
      <div class="field-row">
        <input v-model="newPassword" type="password" autocomplete="new-password" placeholder="新密码，至少 6 位" />
        <button :disabled="savingPassword" @click="savePassword">
          {{ savingPassword ? '提交中...' : '修改密码' }}
        </button>
      </div>
    </section>

    <!-- 注销。放在最后单独一块，和上面那些「随时能改回来」的操作分开 -->
    <section class="danger-zone">
      <h3>注销账号</h3>
      <p class="tip">
        注销后<b>无法恢复</b>：账号、追番、观看记录都会消失，需要重新注册。
        你发过的评论会留下，但作者会显示成「已注销用户」。
      </p>
      <button class="danger" @click="openDelete">注销账号</button>
    </section>

    <ModalDialog :open="deleteOpen" title="注销账号" @close="closeDelete">
      <p class="confirm">这会永久删除你的账号。确定要继续吗？</p>
      <label>
        <span>输入密码确认</span>
        <input
          v-model="deletePassword"
          type="password"
          autocomplete="current-password"
          placeholder="当前密码"
          @keydown.enter="confirmDelete"
        />
      </label>
      <template #footer>
        <button @click="closeDelete">取消</button>
        <button class="danger" :disabled="deletingAccount" @click="confirmDelete">
          {{ deletingAccount ? '注销中...' : '确认注销' }}
        </button>
      </template>
    </ModalDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ModalDialog from '@/components/ModalDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { resolveAvatar } from '@/utils/image'

const auth = useAuthStore()
const toasts = useToastStore()
const router = useRouter()

const deleteOpen = ref(false)
const deletePassword = ref('')
const deletingAccount = ref(false)

const fileInput = ref(null)

const nickname = ref('')
const signature = ref('')
const savingProfile = ref(false)

const oldPassword = ref('')
const newPassword = ref('')
const savingPassword = ref(false)

onMounted(() => {
  nickname.value = auth.user?.nickname || ''
  signature.value = auth.user?.signature || ''
})

async function onPickFile(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    await auth.updateAvatar(file)
    toasts.show('头像已更新', 'success')
  } catch (e) {
    toasts.show(e.message)
  } finally {
    // 清掉选择，否则连选同一个文件不会再触发 change
    if (fileInput.value) fileInput.value.value = ''
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    // 两个字段一起提交，后端一个 UPDATE 写完，不会出现只有一半生效的中间状态
    await auth.updateProfile({ nickname: nickname.value, signature: signature.value })
    toasts.show('资料已更新', 'success')
  } catch (e) {
    toasts.show(e.message)
  } finally {
    savingProfile.value = false
  }
}

function openDelete() {
  deletePassword.value = ''
  deleteOpen.value = true
}

function closeDelete() {
  deleteOpen.value = false
  deletePassword.value = ''
}

async function confirmDelete() {
  deletingAccount.value = true
  try {
    await auth.deleteAccount(deletePassword.value)
    deleteOpen.value = false
    // 账号没了，回到首页。不跳登录页——用户刚注销，不该再被引导去登录
    router.replace('/')
  } catch (e) {
    toasts.show(e.message)
  } finally {
    deletingAccount.value = false
  }
}

async function savePassword() {
  savingPassword.value = true
  try {
    await auth.changePassword(oldPassword.value, newPassword.value)
    toasts.show('密码已修改。当前登录状态不受影响', 'success')
    oldPassword.value = ''
    newPassword.value = ''
  } catch (e) {
    toasts.show(e.message)
  } finally {
    savingPassword.value = false
  }
}
</script>

<style scoped>
.settings {
  max-width: 560px;
}

/* 标题用全局的 .page-title，这里不再重复一份一模一样的规则 */

section {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #333;
}

.tip {
  margin: 0 0 12px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.tip b {
  color: #555;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  flex: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.file-btn {
  display: inline-block;
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  background: #f5f5f5;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.file-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.file-btn input {
  display: none;
}

.field-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

input[type='text'],
input[type='password'],
textarea {
  flex: 1;
  min-width: 0;
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: #333;
  transition: all 0.2s;
}

textarea {
  resize: vertical;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}

.link {
  color: var(--primary);
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

.count {
  flex: 1;
  font-size: 12px;
  color: #bbb;
  align-self: center;
}

button {
  flex: none;
  padding: 9px 18px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  font-size: 14px;
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

/* 注销区单独一块，和上面「随时能改回来」的操作分开 */
.danger-zone {
  border: 1px solid var(--primary-soft);
  background: #fffafa;
}

.danger-zone h3 {
  color: var(--primary);
}

.danger-zone .tip b {
  color: var(--primary);
}

.danger-zone button.danger {
  padding: 9px 20px;
  border-color: var(--primary);
  background: #fff;
  color: var(--primary);
  font-size: 14px;
}

.danger-zone button.danger:hover:not(:disabled) {
  background: var(--primary);
  color: #fff;
}

.confirm {
  margin: 0 0 16px;
  font-size: 14px;
  color: #333;
}
</style>
