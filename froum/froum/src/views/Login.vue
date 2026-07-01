<template>
  <div class="auth-page">
    <section class="auth-visual kumo-surface-strong reveal-rise">
      <span class="kumo-eyebrow">
        <font-awesome-icon :icon="['fas', 'shield-alt']" />
        Secure Access
      </span>
      <h1 class="kumo-heading">欢迎回来</h1>
      <p>继续进入科技论坛，查看关注内容、问题回复和最新技术讨论。</p>
      <div class="visual-grid" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </div>
    </section>

    <section class="auth-card kumo-surface reveal-rise">
      <div class="auth-header">
        <span class="brand-icon">TF</span>
        <div>
          <h2 class="kumo-heading">登录账号</h2>
          <p>使用用户名或邮箱登录</p>
        </div>
      </div>

      <div v-if="error" class="notice notice-error">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
        <span>{{ error }}</span>
      </div>

      <div v-if="infoMessage" class="notice notice-info">
        <font-awesome-icon :icon="['fas', 'shield-alt']" />
        <span>{{ infoMessage }}</span>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <label class="form-field" for="username">
          <span>用户名或邮箱</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'user']" />
            <input
              id="username"
              v-model="username"
              type="text"
              placeholder="请输入用户名或邮箱"
              autocomplete="username"
              required
            >
          </span>
        </label>

        <label class="form-field" for="password">
          <span>密码</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'lock']" />
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
            >
            <button type="button" class="field-icon-button" aria-label="切换密码显示" @click="showPassword = !showPassword">
              <font-awesome-icon :icon="['fas', showPassword ? 'eye-slash' : 'eye']" />
            </button>
          </span>
        </label>

        <div class="form-row">
          <label class="check-row" for="remember">
            <input id="remember" v-model="remember" type="checkbox">
            <span>记住我</span>
          </label>
          <router-link to="/forgot-password">忘记密码?</router-link>
        </div>

        <TacCaptcha v-if="!requiresTwoFactor" ref="captchaRef" v-model="captchaValue" />

        <label v-if="requiresTwoFactor" class="form-field" for="twoFactorCode">
          <span>两步验证码</span>
          <span class="input-shell">
            <font-awesome-icon :icon="['fas', 'shield-alt']" />
            <input
              id="twoFactorCode"
              v-model="twoFactorCode"
              type="text"
              placeholder="请输入6位数字"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              required
            >
          </span>
        </label>

        <button type="submit" class="kumo-button kumo-button--brand submit-button" :disabled="loading">
          <font-awesome-icon v-if="loading" :icon="['fas', 'spinner']" spin />
          <span>{{ loading ? '登录中...' : loginButtonText }}</span>
        </button>
      </form>

      <div class="divider"><span>或</span></div>

      <div class="social-actions">
        <button class="kumo-button" type="button" :disabled="!!oauthLoadingProvider" @click="handleOAuthLogin('github')">
          <font-awesome-icon :icon="['fab', 'github']" />
          <span>{{ oauthLoadingProvider === 'github' ? '跳转中...' : 'GitHub' }}</span>
        </button>
        <button class="kumo-button" type="button" :disabled="!!oauthLoadingProvider" @click="handleOAuthLogin('google')">
          <font-awesome-icon :icon="['fab', 'google']" />
          <span>{{ oauthLoadingProvider === 'google' ? '跳转中...' : 'Google' }}</span>
        </button>
      </div>

      <p class="switch-link">
        还没有账号?
        <router-link to="/register">立即注册</router-link>
      </p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '../api/userApi'
import TacCaptcha from '../components/TacCaptcha.vue'

const router = useRouter()
const route = useRoute()
const store = useStore()

const username = ref('')
const password = ref('')
const remember = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')
const infoMessage = ref('')
const captchaValue = ref(null)
const captchaRef = ref(null)
const requiresTwoFactor = ref(false)
const twoFactorCode = ref('')
const twoFactorToken = ref('')
const oauthLoadingProvider = ref('')

const loginButtonText = computed(() => requiresTwoFactor.value ? '验证并登录' : '登录')

onMounted(() => {
  if (route.query.account || route.query.username) {
    username.value = String(route.query.account || route.query.username).trim()
  }

  const pendingPassword = sessionStorage.getItem('postRegisterPassword')
  if (pendingPassword) {
    password.value = pendingPassword
    remember.value = true
    sessionStorage.removeItem('postRegisterPassword')
  }

  if (route.query.registered === '1') {
    infoMessage.value = '注册成功，完成验证码后即可登录'
  }
  if (route.query.oauthError) {
    error.value = String(route.query.oauthError)
  }
})

const handleOAuthLogin = async (provider) => {
  oauthLoadingProvider.value = provider
  error.value = ''

  try {
    const redirectPath = route.query.redirect ? String(route.query.redirect) : '/'
    sessionStorage.setItem('oauthRedirect', redirectPath)
    sessionStorage.setItem('oauthProvider', provider)
    const response = await userApi.getOAuthAuthorizeUrl(provider)
    const authorizationUrl = response?.data?.authorizationUrl
    if (!authorizationUrl) {
      throw new Error(response?.message || '第三方登录暂不可用')
    }
    window.location.href = authorizationUrl
  } catch (err) {
    error.value = err.message || '第三方登录暂不可用'
    oauthLoadingProvider.value = ''
  }
}

const handleLogin = async () => {
  const account = username.value.trim()
  if (!account || !password.value) {
    error.value = '请输入用户名或邮箱和密码'
    return
  }

  if (requiresTwoFactor.value && !twoFactorCode.value) {
    error.value = '请输入两步验证码'
    return
  }

  if (!requiresTwoFactor.value && !captchaValue.value?.captchaId) {
    error.value = '请先完成验证码验证'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const result = await store.dispatch('login', {
      username: account,
      password: password.value,
      remember: remember.value,
      captchaId: captchaValue.value?.captchaId,
      captchaPercentage: captchaValue.value?.captchaPercentage,
      twoFactorCode: twoFactorCode.value,
      twoFactorToken: twoFactorToken.value
    })

    if (result?.requiresTwoFactor) {
      requiresTwoFactor.value = true
      twoFactorToken.value = result.twoFactorToken || ''
      twoFactorCode.value = ''
      infoMessage.value = '账号已开启两步验证，请输入验证器中的6位数字'
      return
    }

    await router.push(route.query.redirect || '/')
  } catch (err) {
    error.value = err.message || '登录失败，请检查用户名或邮箱和密码'
    if (!requiresTwoFactor.value) {
      captchaValue.value = null
      captchaRef.value?.refresh()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(22rem, 28rem);
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
  min-height: 34rem;
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

.visual-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
  max-width: 32rem;
  margin-top: 1rem;
}

.visual-grid span {
  aspect-ratio: 1;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-elevated);
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

.form-row,
.check-row,
.social-actions {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.form-row {
  justify-content: space-between;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
}

.form-row a,
.switch-link a {
  color: var(--kumo-bg-brand-strong);
  font-weight: 780;
  text-decoration: none;
}

.check-row input {
  accent-color: var(--kumo-bg-brand);
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
  font-weight: 720;
}

.notice-error {
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.notice-info {
  background: var(--kumo-status-info-tint);
  color: var(--kumo-status-info);
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
}

.social-actions button:disabled,
.submit-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.switch-link {
  margin: 1.25rem 0 0;
  color: var(--kumo-text-muted);
  text-align: center;
}

@media (max-width: 920px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 22rem;
  }
}

@media (max-width: 560px) {
  .social-actions {
    grid-template-columns: 1fr;
  }
}
</style>
