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

        <div v-if="requiresTwoFactor || requiresTwoFactorSetup" class="form-group">
          <label for="twoFactorCode">两步验证码</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'shield-alt']" class="input-icon" />
            <input
              type="text"
              id="twoFactorCode"
              v-model="twoFactorCode"
              placeholder="请输入验证器中的6位数字"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              required
            />
          </div>
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
import { computed, defineComponent, ref, onMounted } from 'vue'
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
    const requiresTwoFactorSetup = ref(false)
    const setupToken = ref('')
    const setupSecret = ref('')
    const setupOtpAuthUrl = ref('')
    const setupQrCode = ref('')

    const submitButtonText = computed(() => {
      if (requiresTwoFactorSetup.value) return '绑定并登录'
      if (requiresTwoFactor.value) return '验证并登录'
      return '管理员登录'
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
    onMounted(() => {
      const token = localStorage.getItem('token')
      if (token) {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        if (userInfo.role === 'admin' || userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN') {
          // 如果已经是管理员登录状态，直接跳转到管理后台
          const redirectPath = route.query.redirect || '/admin/dashboard'
          router.replace(redirectPath)
        }
      }
    })
    
    const handleLogin = async () => {
      if (!username.value || !password.value) {
        error.value = '请输入管理员账号和密码'
        return
      }

      if ((requiresTwoFactor.value || requiresTwoFactorSetup.value) && !twoFactorCode.value) {
        error.value = '请输入两步验证码'
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
            twoFactorToken: twoFactorToken.value
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
            twoFactorCode.value = ''
            infoMessage.value = '请输入验证器中的6位数字完成管理员登录'
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
      setupSecret,
      setupOtpAuthUrl,
      setupQrCode,
      submitButtonText,
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
  padding: 2rem 1rem;
  position: relative;
  overflow: hidden;
  background-color: var(--bg-color);
}

.admin-login-card {
  width: 100%;
  max-width: 420px;
  background-color: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 2.5rem;
  position: relative;
  z-index: 2;
  border: 1px solid var(--border-color);
}

.admin-login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.admin-login-header h1 {
  margin: 0;
  margin-bottom: 0.5rem;
  color: var(--primary-color);
  font-size: 1.75rem;
  font-weight: 700;
}

.admin-login-header p {
  color: var(--text-light);
  margin: 0;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: rgba(var(--error-rgb), 0.05);
  border: 1px solid var(--error-color);
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: var(--radius);
  color: var(--error-color);
}

.error-icon {
  font-size: 1rem;
}

.info-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: rgba(var(--primary-rgb), 0.08);
  border: 1px solid rgba(var(--primary-rgb), 0.25);
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: var(--radius);
  color: var(--primary-color);
}

.info-icon {
  font-size: 1rem;
}

.setup-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 1rem;
  margin-bottom: 1.5rem;
  background-color: var(--bg-light);
}

.setup-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary-color);
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.setup-panel p {
  margin: 0 0 0.75rem;
  color: var(--text-light);
  font-size: 0.9rem;
  line-height: 1.5;
}

.secret-box {
  word-break: break-all;
  background-color: var(--bg-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 0.75rem;
  font-family: monospace;
  letter-spacing: 0;
  color: var(--text-color);
}

.qr-code {
  display: block;
  width: 180px;
  height: 180px;
  margin: 0.75rem auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background-color: #fff;
  padding: 0.5rem;
}

.otp-link {
  display: inline-flex;
  margin-top: 0.75rem;
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  font-size: 0.9rem;
}

.otp-link:hover {
  text-decoration: underline;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--text-color);
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
  color: var(--text-lighter);
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 0.75rem 1rem 0.75rem 2.5rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 0.95rem;
  transition: var(--transition);
  background-color: var(--bg-white);
}

input[type="text"]:focus,
input[type="password"]:focus {
  border-color: var(--primary-color);
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.toggle-password {
  position: absolute;
  right: 1rem;
  background: none;
  border: none;
  color: var(--text-lighter);
  cursor: pointer;
  padding: 0;
  font-size: 0.9rem;
}

.btn-login {
  width: 100%;
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  transition: var(--transition);
  margin-top: 1rem;
  box-shadow: var(--shadow);
}

.btn-login:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  filter: brightness(1.1);
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.back-link {
  margin-top: 1.5rem;
  text-align: center;
}

.back-link a {
  color: var(--text-light);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  transition: var(--transition);
}

.back-link a:hover {
  color: var(--primary-color);
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
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  opacity: 0.1;
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
