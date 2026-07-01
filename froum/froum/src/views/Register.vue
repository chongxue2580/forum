<template>
  <div class="auth-page">
    <section class="auth-visual kumo-surface-strong reveal-rise">
      <span class="kumo-eyebrow">
        <font-awesome-icon :icon="['fas', 'user-plus']" />
        Create Account
      </span>
      <h1 class="kumo-heading">加入社区</h1>
      <p>创建账号后即可发布文章、提出问题、关注作者并接收回复通知。</p>
      <div class="visual-stack" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </section>

    <section class="auth-card kumo-surface reveal-rise">
      <div class="auth-header">
        <span class="brand-icon">TF</span>
        <div>
          <h2 class="kumo-heading">注册账号</h2>
          <p>邮箱验证码用于确认账号归属</p>
        </div>
      </div>

      <div v-if="error" class="notice notice-error">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
        <span>{{ error }}</span>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <label class="form-field" for="username">
          <span>用户名</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'user']" />
            <input id="username" v-model="username" type="text" placeholder="请设置用户名" autocomplete="username" required>
          </span>
        </label>

        <label class="form-field" for="email">
          <span>电子邮箱</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'envelope']" />
            <input id="email" v-model="email" type="email" placeholder="请输入邮箱地址" autocomplete="email" required>
          </span>
        </label>

        <label class="form-field" for="emailCode">
          <span>邮箱验证码</span>
          <span class="code-row">
            <span class="input-shell">
              <font-awesome-icon :icon="['fas', 'shield-alt']" />
              <input id="emailCode" v-model="emailCode" type="text" placeholder="6位验证码" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required>
            </span>
            <button type="button" class="kumo-button" :disabled="sendingEmailCode || emailCodeCountdown > 0" @click="handleSendEmailCode">
              <font-awesome-icon v-if="sendingEmailCode" :icon="['fas', 'spinner']" spin />
              <span>{{ emailCodeButtonText }}</span>
            </button>
          </span>
        </label>

        <label class="form-field" for="password">
          <span>密码</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'lock']" />
            <input id="password" v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="请设置密码" autocomplete="new-password" required>
            <button type="button" class="field-icon-button" aria-label="切换密码显示" @click="showPassword = !showPassword">
              <font-awesome-icon :icon="['fas', showPassword ? 'eye-slash' : 'eye']" />
            </button>
          </span>
        </label>

        <label class="form-field" for="confirmPassword">
          <span>确认密码</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'lock']" />
            <input id="confirmPassword" v-model="confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" placeholder="请再次输入密码" autocomplete="new-password" required>
            <button type="button" class="field-icon-button" aria-label="切换密码显示" @click="showConfirmPassword = !showConfirmPassword">
              <font-awesome-icon :icon="['fas', showConfirmPassword ? 'eye-slash' : 'eye']" />
            </button>
          </span>
        </label>

        <label class="check-row" for="agreement">
          <input id="agreement" v-model="agreeTerms" type="checkbox" required>
          <span>我已阅读并同意<a href="#">服务条款</a>和<a href="#">隐私政策</a></span>
        </label>

        <TacCaptcha ref="captchaRef" v-model="captchaValue" />

        <button type="submit" class="kumo-button kumo-button--brand submit-button" :disabled="loading">
          <font-awesome-icon v-if="loading" :icon="['fas', 'spinner']" spin />
          <span>{{ loading ? '注册中...' : '立即注册' }}</span>
        </button>
      </form>

      <div class="divider"><span>或</span></div>

      <div class="social-actions">
        <button class="kumo-button" type="button" :disabled="!!oauthLoadingProvider" @click="handleOAuthRegister('github')">
          <font-awesome-icon :icon="['fab', 'github']" />
          <span>{{ oauthLoadingProvider === 'github' ? '跳转中...' : 'GitHub' }}</span>
        </button>
        <button class="kumo-button" type="button" :disabled="!!oauthLoadingProvider" @click="handleOAuthRegister('google')">
          <font-awesome-icon :icon="['fab', 'google']" />
          <span>{{ oauthLoadingProvider === 'google' ? '跳转中...' : 'Google' }}</span>
        </button>
      </div>

      <p class="switch-link">
        已有账号?
        <router-link to="/login">立即登录</router-link>
      </p>
    </section>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '../api/userApi'
import TacCaptcha from '../components/TacCaptcha.vue'

const router = useRouter()
const store = useStore()

const username = ref('')
const email = ref('')
const emailCode = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const agreeTerms = ref(false)
const loading = ref(false)
const sendingEmailCode = ref(false)
const emailCodeCountdown = ref(0)
const error = ref('')
const captchaValue = ref(null)
const captchaRef = ref(null)
const oauthLoadingProvider = ref('')
let emailCodeTimer = null

const emailCodeButtonText = computed(() => {
  if (sendingEmailCode.value) return '发送中'
  return emailCodeCountdown.value > 0 ? `${emailCodeCountdown.value}s` : '发送验证码'
})

const startEmailCodeCountdown = () => {
  emailCodeCountdown.value = 60
  if (emailCodeTimer) clearInterval(emailCodeTimer)
  emailCodeTimer = setInterval(() => {
    emailCodeCountdown.value -= 1
    if (emailCodeCountdown.value <= 0) {
      clearInterval(emailCodeTimer)
      emailCodeTimer = null
    }
  }, 1000)
}

const handleSendEmailCode = async () => {
  const normalizedEmail = email.value.trim()
  if (!normalizedEmail) {
    error.value = '请先填写邮箱地址'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
    error.value = '邮箱格式不正确'
    return
  }

  sendingEmailCode.value = true
  error.value = ''

  try {
    await userApi.sendRegistrationEmailCode(normalizedEmail)
    startEmailCodeCountdown()
  } catch (err) {
    error.value = err.message || '验证码发送失败，请稍后重试'
  } finally {
    sendingEmailCode.value = false
  }
}

const handleRegister = async () => {
  if (!username.value || !email.value || !emailCode.value || !password.value || !confirmPassword.value) {
    error.value = '请填写所有必填项'
    return
  }
  if (!/^\d{6}$/.test(emailCode.value)) {
    error.value = '邮箱验证码必须是6位数字'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (!agreeTerms.value) {
    error.value = '请阅读并同意服务条款和隐私政策'
    return
  }
  if (!captchaValue.value?.captchaId) {
    error.value = '请先完成验证码验证'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await store.dispatch('register', {
      username: username.value,
      email: email.value,
      password: password.value,
      verificationCode: emailCode.value,
      captchaId: captchaValue.value.captchaId,
      captchaPercentage: captchaValue.value.captchaPercentage
    })

    sessionStorage.setItem('postRegisterPassword', password.value)
    router.push({
      path: '/login',
      query: {
        registered: '1',
        account: email.value.trim()
      }
    })
  } catch (err) {
    error.value = err.message || '注册失败，请稍后重试'
    captchaValue.value = null
    captchaRef.value?.refresh()
  } finally {
    loading.value = false
  }
}

const handleOAuthRegister = async (provider) => {
  oauthLoadingProvider.value = provider
  error.value = ''

  try {
    sessionStorage.setItem('oauthRedirect', '/')
    sessionStorage.setItem('oauthProvider', provider)
    const response = await userApi.getOAuthAuthorizeUrl(provider)
    const authorizationUrl = response?.data?.authorizationUrl
    if (!authorizationUrl) {
      throw new Error(response?.message || '第三方注册暂不可用')
    }
    window.location.href = authorizationUrl
  } catch (err) {
    error.value = err.message || '第三方注册暂不可用'
    oauthLoadingProvider.value = ''
  }
}

onUnmounted(() => {
  if (emailCodeTimer) clearInterval(emailCodeTimer)
})
</script>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(24rem, 30rem);
  gap: 1.25rem;
  align-items: stretch;
  min-height: calc(100vh - 12rem);
}

.auth-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
  min-height: 38rem;
  padding: clamp(1.5rem, 5vw, 4rem);
  overflow: hidden;
}

.auth-visual::after {
  content: '';
  position: absolute;
  right: -12rem;
  bottom: -14rem;
  width: 34rem;
  height: 34rem;
  border-radius: 999px;
  background: radial-gradient(circle, var(--kumo-bg-brand-soft), transparent 68%);
  animation: soft-pulse 8s ease-in-out infinite;
}

.auth-visual h1 {
  max-width: 7ch;
  margin: 0;
  font-size: clamp(3rem, 9vw, 7rem);
}

.auth-visual p {
  position: relative;
  z-index: 1;
  max-width: 36rem;
  margin: 0;
  color: var(--kumo-text-muted);
  font-size: 1.08rem;
}

.visual-stack {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.8rem;
  width: min(100%, 26rem);
  margin-top: 1rem;
}

.visual-stack span {
  height: 4.5rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-elevated);
}

.visual-stack span:nth-child(2) {
  margin-left: 2rem;
}

.visual-stack span:nth-child(3) {
  width: 72%;
}

.auth-card {
  align-self: center;
  padding: clamp(1.25rem, 3vw, 2rem);
}

.auth-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1.35rem;
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

.auth-header h2,
.auth-header p {
  margin: 0;
}

.auth-header h2 {
  font-size: 1.8rem;
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

.check-row {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
}

.check-row input {
  margin-top: 0.25rem;
  accent-color: var(--kumo-bg-brand);
}

.check-row a,
.switch-link a {
  color: var(--kumo-bg-brand-strong);
  font-weight: 780;
  text-decoration: none;
}

.submit-button {
  width: 100%;
}

.notice {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-bottom: 1rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
  font-weight: 720;
}

.divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 1.25rem 0;
  color: var(--kumo-text-subtle);
  font-size: 0.88rem;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--kumo-hairline);
}

.social-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.switch-link {
  margin: 1.25rem 0 0;
  color: var(--kumo-text-muted);
  text-align: center;
}

@media (max-width: 940px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 22rem;
  }
}

@media (max-width: 560px) {
  .social-actions,
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
