<template>
  <div class="reset-page">
    <section class="reset-card kumo-surface reveal-rise">
      <router-link to="/login" class="back-link" aria-label="返回登录">
        <font-awesome-icon :icon="['fas', 'arrow-left']" />
        返回登录
      </router-link>

      <div class="auth-header">
        <span class="brand-icon">TF</span>
        <div>
          <h1 class="kumo-heading">重置密码</h1>
          <p>验证码会发送到账号绑定邮箱</p>
        </div>
      </div>

      <div v-if="error" class="notice notice-error">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
        <span>{{ error }}</span>
      </div>

      <div v-if="successMessage" class="notice notice-success">
        <font-awesome-icon :icon="['fas', 'check-circle']" />
        <span>{{ successMessage }}</span>
      </div>

      <form class="auth-form" @submit.prevent="handleResetPassword">
        <label class="form-field" for="account">
          <span>用户名或邮箱</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'user']" />
            <input id="account" v-model="account" type="text" placeholder="请输入用户名或邮箱" autocomplete="username" required>
          </span>
        </label>

        <label class="form-field" for="verificationCode">
          <span>邮箱验证码</span>
          <span class="code-row">
            <span class="input-shell">
              <font-awesome-icon :icon="['fas', 'shield-alt']" />
              <input id="verificationCode" v-model="verificationCode" type="text" placeholder="6位验证码" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required>
            </span>
            <button type="button" class="kumo-button" :disabled="sendingCode || codeCountdown > 0" @click="handleSendCode">
              <font-awesome-icon v-if="sendingCode" :icon="['fas', 'spinner']" spin />
              <span>{{ codeButtonText }}</span>
            </button>
          </span>
        </label>

        <label class="form-field" for="newPassword">
          <span>新密码</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'lock']" />
            <input id="newPassword" v-model="newPassword" :type="showPassword ? 'text' : 'password'" placeholder="请设置新密码" autocomplete="new-password" required>
            <button type="button" class="field-icon-button" aria-label="切换密码显示" @click="showPassword = !showPassword">
              <font-awesome-icon :icon="['fas', showPassword ? 'eye-slash' : 'eye']" />
            </button>
          </span>
        </label>

        <label class="form-field" for="confirmPassword">
          <span>确认新密码</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'lock']" />
            <input id="confirmPassword" v-model="confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" placeholder="请再次输入新密码" autocomplete="new-password" required>
            <button type="button" class="field-icon-button" aria-label="切换密码显示" @click="showConfirmPassword = !showConfirmPassword">
              <font-awesome-icon :icon="['fas', showConfirmPassword ? 'eye-slash' : 'eye']" />
            </button>
          </span>
        </label>

        <button type="submit" class="kumo-button kumo-button--brand submit-button" :disabled="loading">
          <font-awesome-icon v-if="loading" :icon="['fas', 'spinner']" spin />
          <span>{{ loading ? '提交中...' : '重置密码' }}</span>
        </button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import { userApi } from '../api/userApi'

const account = ref('')
const verificationCode = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const sendingCode = ref(false)
const loading = ref(false)
const error = ref('')
const successMessage = ref('')
const codeCountdown = ref(0)
let codeTimer = null

const codeButtonText = computed(() => {
  if (sendingCode.value) return '发送中'
  return codeCountdown.value > 0 ? `${codeCountdown.value}s` : '发送验证码'
})

const startCountdown = () => {
  codeCountdown.value = 60
  if (codeTimer) clearInterval(codeTimer)
  codeTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      clearInterval(codeTimer)
      codeTimer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  const normalizedAccount = account.value.trim()
  if (!normalizedAccount) {
    error.value = '请先填写用户名或邮箱'
    return
  }

  sendingCode.value = true
  error.value = ''
  successMessage.value = ''

  try {
    await userApi.sendForgotPasswordCode(normalizedAccount)
    successMessage.value = '验证码已发送，请查收绑定邮箱'
    startCountdown()
  } catch (err) {
    error.value = err.message || '验证码发送失败，请稍后重试'
  } finally {
    sendingCode.value = false
  }
}

const handleResetPassword = async () => {
  if (!account.value || !verificationCode.value || !newPassword.value || !confirmPassword.value) {
    error.value = '请填写所有必填项'
    return
  }
  if (!/^\d{6}$/.test(verificationCode.value)) {
    error.value = '邮箱验证码必须是6位数字'
    return
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 20) {
    error.value = '新密码长度必须在6-20个字符之间'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的新密码不一致'
    return
  }

  loading.value = true
  error.value = ''
  successMessage.value = ''

  try {
    await userApi.resetForgotPassword({
      account: account.value.trim(),
      verificationCode: verificationCode.value,
      newPassword: newPassword.value
    })
    successMessage.value = '密码重置成功，请返回登录'
    verificationCode.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (err) {
    error.value = err.message || '密码重置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (codeTimer) clearInterval(codeTimer)
})
</script>

<style scoped>
.reset-page {
  display: grid;
  place-items: center;
  min-height: calc(100vh - 12rem);
}

.reset-card {
  display: grid;
  gap: 1rem;
  width: min(100%, 30rem);
  padding: clamp(1.25rem, 3vw, 2rem);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  width: fit-content;
  color: var(--kumo-text-muted);
  font-weight: 760;
  text-decoration: none;
}

.back-link:hover {
  color: var(--kumo-bg-brand-strong);
}

.auth-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-accent));
  color: var(--kumo-text-inverse);
  font-weight: 900;
}

.auth-header h1,
.auth-header p {
  margin: 0;
}

.auth-header h1 {
  font-size: 1.9rem;
}

.auth-header p {
  color: var(--kumo-text-muted);
}

.auth-form {
  display: grid;
  gap: 1rem;
}

.form-field {
  display: grid;
  gap: 0.45rem;
  color: var(--kumo-text-default);
  font-weight: 780;
}

.input-shell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
  min-height: 3rem;
  padding: 0 0.55rem 0 0.9rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-subtle);
}

.input-shell input {
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--kumo-text-default);
  outline: none;
}

.field-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--kumo-text-subtle);
  cursor: pointer;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.65rem;
}

.code-row .kumo-button {
  min-width: 7.6rem;
}

.submit-button {
  width: 100%;
}

.notice {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--kumo-radius-md);
  font-weight: 720;
}

.notice-error {
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.notice-success {
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 560px) {
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
