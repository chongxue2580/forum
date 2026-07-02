<template>
  <div class="admin-login-container">
    <div class="admin-login-card">
      <div class="admin-login-header">
        <h1>管理员登录</h1>
        <p>请输入管理员账号和密码</p>
      </div>
      
      <div v-if="error" class="error-message">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
        <span>{{ error }}</span>
      </div>

      <div v-if="infoMessage" class="info-message">
        <font-awesome-icon :icon="['fas', 'shield-alt']" class="info-icon" />
        <span>{{ infoMessage }}</span>
      </div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">管理员账号</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'user-shield']" class="input-icon" />
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              placeholder="请输入管理员账号" 
              required
              autocomplete="username"
            />
          </div>
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'lock']" class="input-icon" />
            <input 
              :type="showPassword ? 'text' : 'password'" 
              id="password" 
              v-model="password" 
              placeholder="请输入密码" 
              required
              autocomplete="current-password"
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showPassword = !showPassword"
              aria-label="显示密码"
            >
              <font-awesome-icon :icon="['fas', showPassword ? 'eye-slash' : 'eye']" />
            </button>
          </div>
        </div>

        <div v-if="requiresTwoFactorSetup" class="setup-panel">
          <div class="setup-title">
            <font-awesome-icon :icon="['fas', 'shield-alt']" />
            <span>绑定管理员两步验证</span>
          </div>
          <p>请在 Google Authenticator、Microsoft Authenticator 或其它 TOTP 验证器中添加以下密钥。</p>
          <img v-if="setupQrCode" class="qr-code" :src="setupQrCode" alt="两步验证二维码">
          <div class="secret-box">{{ setupSecret }}</div>
          <a v-if="setupOtpAuthUrl" class="otp-link" :href="setupOtpAuthUrl">打开验证器添加账号</a>
        </div>

        <div v-if="requiresTwoFactor && !requiresTwoFactorSetup" class="verification-methods">
          <button
            type="button"
            class="method-button"
            :class="{ active: twoFactorMethod === 'totp' }"
            @click="selectTwoFactorMethod('totp')"
          >
            验证器
          </button>
          <button
            type="button"
            class="method-button"
            :class="{ active: twoFactorMethod === 'email' }"
            @click="selectTwoFactorMethod('email')"
          >
            邮箱验证码
          </button>
        </div>

        <div v-if="requiresTwoFactor || requiresTwoFactorSetup" class="form-group">
          <label for="twoFactorCode">{{ twoFactorCodeLabel }}</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', twoFactorMethod === 'email' && !requiresTwoFactorSetup ? 'envelope' : 'shield-alt']" class="input-icon" />
            <input
              type="text"
              id="twoFactorCode"
              v-model="twoFactorCode"
              :placeholder="twoFactorCodePlaceholder"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              required
            />
          </div>
          <button
            v-if="requiresTwoFactor && twoFactorMethod === 'email'"
            type="button"
            class="btn-secondary"
            :disabled="loading || emailCodeCooldown > 0"
            @click="sendEmailCode"
          >
            <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="sendingEmailCode" />
            <font-awesome-icon :icon="['fas', 'paper-plane']" v-else />
            <span>{{ emailCodeButtonText }}</span>
          </button>
        </div>
        
        <button type="submit" class="btn-login" :disabled="loading">
          <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="loading" />
          <span>{{ loading ? '处理中...' : submitButtonText }}</span>
        </button>
      </form>
      
      <div class="back-link">
        <router-link to="/">
          <font-awesome-icon :icon="['fas', 'arrow-left']" />
          返回首页
        </router-link>
      </div>
    </div>
    
    <div class="admin-login-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
    </div>
  </div>
</template>

<script>
import { computed, defineComponent, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import QRCode from 'qrcode'

export default defineComponent({
  name: 'AdminLoginView',
  setup() {
    const router = useRouter()
    const store = useStore()
    const route = useRoute()
    
    const username = ref('')
    const password = ref('')
    const showPassword = ref(false)
    const loading = ref(false)
    const error = ref('')
    const infoMessage = ref('')
    const requiresTwoFactor = ref(false)
    const twoFactorToken = ref('')
    const twoFactorCode = ref('')
    const twoFactorMethod = ref('totp')
    const sendingEmailCode = ref(false)
    const emailCodeCooldown = ref(0)
    let emailCodeTimer = null
    const requiresTwoFactorSetup = ref(false)
    const setupToken = ref('')
    const setupSecret = ref('')
    const setupOtpAuthUrl = ref('')
    const setupQrCode = ref('')

    const isAdminRole = (role) => ['admin', 'ADMIN', 'SUPER_ADMIN'].includes(role)

    const submitButtonText = computed(() => {
      if (requiresTwoFactorSetup.value) return '绑定并登录'
      if (requiresTwoFactor.value) return '验证并登录'
      return '管理员登录'
    })

    const twoFactorCodeLabel = computed(() => {
      if (requiresTwoFactorSetup.value || twoFactorMethod.value === 'totp') return '两步验证码'
      return '邮箱验证码'
    })

    const twoFactorCodePlaceholder = computed(() => {
      if (requiresTwoFactorSetup.value || twoFactorMethod.value === 'totp') return '请输入验证器中的6位数字'
      return '请输入邮箱收到的6位数字'
    })

    const emailCodeButtonText = computed(() => {
      return emailCodeCooldown.value > 0 ? `${emailCodeCooldown.value}秒后重新发送` : '发送邮箱验证码'
    })

    const generateQrCode = async (otpAuthUrl) => {
      if (!otpAuthUrl) {
        setupQrCode.value = ''
        return
      }
      setupQrCode.value = await QRCode.toDataURL(otpAuthUrl, {
        width: 180,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
    }
    
    // 在组件挂载时检查是否已经登录
    onMounted(async () => {
      const token = localStorage.getItem('token')
      if (!token) {
        return
      }

      await store.dispatch('checkAuth')
      if (isAdminRole(store.state.user?.role)) {
        const redirectPath = route.query.redirect || '/admin/dashboard'
        router.replace(redirectPath)
      }
    })

    onBeforeUnmount(() => {
      if (emailCodeTimer) {
        clearInterval(emailCodeTimer)
      }
    })

    const selectTwoFactorMethod = (method) => {
      twoFactorMethod.value = method
      twoFactorCode.value = ''
      error.value = ''
      infoMessage.value = method === 'email'
        ? '发送邮箱验证码后，输入邮箱中的6位数字完成管理员登录'
        : '请输入验证器中的6位数字完成管理员登录'
    }

    const startEmailCodeCooldown = () => {
      emailCodeCooldown.value = 60
      if (emailCodeTimer) {
        clearInterval(emailCodeTimer)
      }
      emailCodeTimer = setInterval(() => {
        emailCodeCooldown.value -= 1
        if (emailCodeCooldown.value <= 0) {
          clearInterval(emailCodeTimer)
          emailCodeTimer = null
        }
      }, 1000)
    }

    const sendEmailCode = async () => {
      if (!twoFactorToken.value) {
        error.value = '两步验证已过期，请重新输入账号密码'
        return
      }

      sendingEmailCode.value = true
      error.value = ''
      try {
        await store.dispatch('sendAdminTwoFactorEmailCode', {
          twoFactorToken: twoFactorToken.value
        })
        infoMessage.value = '邮箱验证码已发送，请查收管理员邮箱'
        startEmailCodeCooldown()
      } catch (err) {
        error.value = err.message || '邮箱验证码发送失败'
      } finally {
        sendingEmailCode.value = false
      }
    }
    
    const handleLogin = async () => {
      if (!username.value || !password.value) {
        error.value = '请输入管理员账号和密码'
        return
      }

      if ((requiresTwoFactor.value || requiresTwoFactorSetup.value) && !twoFactorCode.value) {
        error.value = twoFactorMethod.value === 'email' && !requiresTwoFactorSetup.value
          ? '请输入邮箱验证码'
          : '请输入两步验证码'
        return
      }
      
      loading.value = true
      error.value = ''
      
      try {
        let result

        if (requiresTwoFactorSetup.value) {
          result = await store.dispatch('confirmAdminTwoFactorSetup', {
            setupToken: setupToken.value,
            code: twoFactorCode.value
          })
        } else {
          result = await store.dispatch('adminLogin', {
            username: username.value,
            password: password.value,
            twoFactorCode: twoFactorCode.value,
            twoFactorToken: twoFactorToken.value,
            twoFactorMethod: requiresTwoFactor.value ? twoFactorMethod.value : undefined
          })

          if (result?.requiresTwoFactorSetup) {
            requiresTwoFactorSetup.value = true
            requiresTwoFactor.value = false
            setupToken.value = result.setupToken || ''
            setupSecret.value = result.twoFactorSecret || ''
            setupOtpAuthUrl.value = result.twoFactorOtpAuthUrl || ''
            await generateQrCode(setupOtpAuthUrl.value)
            twoFactorCode.value = ''
            infoMessage.value = '管理员账号首次使用必须先绑定两步验证'
            return
          }

          if (result?.requiresTwoFactor) {
            requiresTwoFactor.value = true
            requiresTwoFactorSetup.value = false
            twoFactorToken.value = result.twoFactorToken || ''
            twoFactorMethod.value = 'totp'
            twoFactorCode.value = ''
            infoMessage.value = '请输入验证器中的6位数字，或切换为邮箱验证码完成管理员登录'
            return
          }
        }
        
        // 登录成功，重定向到管理后台
        const redirectPath = route.query.redirect || '/admin/dashboard'
        router.push(redirectPath)
      } catch (err) {
        error.value = err.message || '登录失败，请检查管理员账号和密码'
      } finally {
        loading.value = false
      }
    }
    
    return {
      username,
      password,
      showPassword,
      loading,
      error,
      infoMessage,
      requiresTwoFactor,
      requiresTwoFactorSetup,
      twoFactorCode,
      twoFactorMethod,
      twoFactorCodeLabel,
      twoFactorCodePlaceholder,
      sendingEmailCode,
      emailCodeCooldown,
      emailCodeButtonText,
      setupSecret,
      setupOtpAuthUrl,
      setupQrCode,
      submitButtonText,
      selectTwoFactorMethod,
      sendEmailCode,
      handleLogin
    }
  }
})
</script>

<style scoped>
.admin-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: clamp(2rem, 6vw, 4rem) 1rem;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 18%, var(--kumo-bg-brand-soft), transparent 28rem),
    radial-gradient(circle at 84% 10%, var(--kumo-bg-accent-soft), transparent 24rem),
    linear-gradient(180deg, var(--kumo-bg-base), var(--kumo-bg-subtle));
}

.admin-login-card {
  width: 100%;
  max-width: 28rem;
  background: var(--kumo-bg-elevated);
  border-radius: var(--kumo-radius-lg);
  box-shadow: var(--kumo-shadow-lg);
  padding: clamp(1.5rem, 5vw, 2.5rem);
  position: relative;
  z-index: 2;
  border: 1px solid var(--kumo-hairline);
  backdrop-filter: var(--kumo-blur);
  animation: reveal-rise 520ms cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.admin-login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.admin-login-header h1 {
  margin: 0;
  margin-bottom: 0.5rem;
  color: var(--kumo-text-default);
  font-size: 1.75rem;
  font-weight: 820;
}

.admin-login-header p {
  color: var(--kumo-text-muted);
  margin: 0;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: var(--kumo-status-danger-tint);
  border: 1px solid var(--kumo-status-danger);
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: var(--kumo-radius-md);
  color: var(--kumo-status-danger);
}

.error-icon {
  font-size: 1rem;
}

.info-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: var(--kumo-status-info-tint);
  border: 1px solid var(--kumo-status-info);
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: var(--kumo-radius-md);
  color: var(--kumo-status-info);
}

.info-icon {
  font-size: 1rem;
}

.setup-panel {
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  padding: 1rem;
  margin-bottom: 1.5rem;
  background-color: var(--kumo-bg-subtle);
}

.setup-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--kumo-bg-brand-strong);
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.setup-panel p {
  margin: 0 0 0.75rem;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  line-height: 1.5;
}

.secret-box {
  word-break: break-all;
  background-color: var(--kumo-bg-elevated);
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  padding: 0.75rem;
  font-family: monospace;
  letter-spacing: 0;
  color: var(--kumo-text-default);
}

.qr-code {
  display: block;
  width: 180px;
  height: 180px;
  margin: 0.75rem auto;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background-color: var(--kumo-bg-elevated);
  padding: 0.5rem;
}

.otp-link {
  display: inline-flex;
  margin-top: 0.75rem;
  color: var(--kumo-bg-brand-strong);
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
}

.otp-link:hover {
  text-decoration: underline;
}

.verification-methods {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.method-button {
  border: 1px solid var(--kumo-hairline);
  background-color: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  border-radius: var(--kumo-radius-md);
  padding: 0.65rem 0.75rem;
  font-size: 0.9rem;
  cursor: pointer;
  transition: var(--transition);
}

.method-button.active {
  border-color: var(--kumo-bg-accent);
  background-color: var(--kumo-bg-accent-soft);
  color: var(--kumo-bg-accent);
  font-weight: 600;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--kumo-text-default);
  font-size: 0.9rem;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 1rem;
  color: var(--kumo-text-subtle);
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 0.75rem 1rem 0.75rem 2.5rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  font-size: 0.95rem;
  transition: var(--kumo-transition);
  background-color: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
}

input[type="text"]:focus,
input[type="password"]:focus {
  border-color: var(--kumo-bg-accent);
  outline: none;
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
}

.toggle-password {
  position: absolute;
  right: 1rem;
  background: none;
  border: none;
  color: var(--kumo-text-subtle);
  cursor: pointer;
  padding: 0;
  font-size: 0.9rem;
}

.btn-login {
  width: 100%;
  padding: 0.75rem 1rem;
  background: var(--kumo-text-default);
  color: var(--kumo-text-inverse);
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  transition: var(--kumo-transition);
  margin-top: 1rem;
  box-shadow: 0 10px 22px rgba(var(--accent-rgb), 0.2);
}

.btn-login:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(var(--accent-rgb), 0.24);
  filter: none;
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-secondary {
  width: 100%;
  margin-top: 0.75rem;
  padding: 0.65rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 12px;
  background-color: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  font-size: 0.9rem;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  transition: var(--kumo-transition);
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--kumo-bg-accent);
  color: var(--kumo-bg-accent);
}

.btn-secondary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.back-link {
  margin-top: 1.5rem;
  text-align: center;
}

.back-link a {
  color: var(--kumo-text-muted);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  transition: var(--kumo-transition);
}

.back-link a:hover {
  color: var(--kumo-bg-accent);
}

.admin-login-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(var(--primary-rgb), 0.1), rgba(var(--accent-rgb), 0.06));
  opacity: 0.5;
  animation: soft-pulse 7s ease-in-out infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  right: -50px;
}

.circle-3 {
  width: 150px;
  height: 150px;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
}

@media (max-width: 576px) {
  .admin-login-card {
    padding: 2rem 1.5rem;
  }
}
</style> 
