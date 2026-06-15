<template>
  <div class="register-container">
    <div class="register-card">
      <h1>注册账号</h1>

      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'user']" class="input-icon" />
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              placeholder="请设置用户名" 
              required
            />
          </div>
        </div>

        <div class="form-group">
          <label for="email">电子邮箱</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'envelope']" class="input-icon" />
            <input 
              type="email" 
              id="email" 
              v-model="email" 
              placeholder="请输入邮箱地址" 
              required
            />
          </div>
        </div>

        <div class="form-group">
          <label for="emailCode">邮箱验证码</label>
          <div class="email-code-row">
            <div class="input-wrapper email-code-input">
              <font-awesome-icon :icon="['fas', 'shield-alt']" class="input-icon" />
              <input
                type="text"
                id="emailCode"
                v-model="emailCode"
                placeholder="6位验证码"
                inputmode="numeric"
                autocomplete="one-time-code"
                maxlength="6"
                required
              />
            </div>
            <button
              type="button"
              class="btn-code"
              :disabled="sendingEmailCode || emailCodeCountdown > 0"
              @click="handleSendEmailCode"
            >
              <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="sendingEmailCode" />
              <span>{{ emailCodeButtonText }}</span>
            </button>
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
              placeholder="请设置密码" 
              required
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showPassword = !showPassword"
            >
              <font-awesome-icon :icon="['fas', showPassword ? 'eye-slash' : 'eye']" />
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'lock']" class="input-icon" />
            <input 
              :type="showConfirmPassword ? 'text' : 'password'" 
              id="confirmPassword" 
              v-model="confirmPassword" 
              placeholder="请再次输入密码" 
              required
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <font-awesome-icon :icon="['fas', showConfirmPassword ? 'eye-slash' : 'eye']" />
            </button>
          </div>
        </div>
        
        <div class="agreement">
          <input type="checkbox" id="agreement" v-model="agreeTerms" required />
          <label for="agreement">我已阅读并同意<a href="#">服务条款</a>和<a href="#">隐私政策</a></label>
        </div>

        <TacCaptcha ref="captchaRef" v-model="captchaValue" />

        <button type="submit" class="btn-register" :disabled="loading">
          <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="loading" />
          {{ loading ? '注册中...' : '立即注册' }}
        </button>
      </form>
      
      <div class="divider">
        <span>或</span>
      </div>
      
      <div class="social-register">
        <button class="btn-social github">
          <font-awesome-icon :icon="['fab', 'github']" />
          GitHub账号注册
        </button>
        <button class="btn-social google">
          <font-awesome-icon :icon="['fab', 'google']" />
          Google账号注册
        </button>
      </div>
      
      <div class="login-link">
        已有账号? 
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, defineComponent, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { userApi } from '../api/userApi'
import TacCaptcha from '../components/TacCaptcha.vue'

export default defineComponent({
  name: 'RegisterView',
  components: { TacCaptcha },
  setup() {
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
    let emailCodeTimer = null
    const error = ref('')
    const captchaValue = ref(null)
    const captchaRef = ref(null)
    const emailCodeButtonText = computed(() => {
      if (sendingEmailCode.value) {
        return '发送中'
      }
      return emailCodeCountdown.value > 0 ? `${emailCodeCountdown.value}s` : '发送验证码'
    })

    const startEmailCodeCountdown = () => {
      emailCodeCountdown.value = 60
      if (emailCodeTimer) {
        clearInterval(emailCodeTimer)
      }
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

    onUnmounted(() => {
      if (emailCodeTimer) {
        clearInterval(emailCodeTimer)
      }
    })

    const handleRegister = async () => {
      // 表单验证
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
        // 调用注册API
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
    
    return {
      username,
      email,
      emailCode,
      password,
      confirmPassword,
      showPassword,
      showConfirmPassword,
      agreeTerms,
      loading,
      sendingEmailCode,
      emailCodeCountdown,
      emailCodeButtonText,
      error,
      captchaValue,
      captchaRef,
      handleSendEmailCode,
      handleRegister
    }
  }
})
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem 1rem;
}

.register-card {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

h1 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
  font-size: 1.75rem;
}

.error-message {
  background-color: #fff2f0;
  border-left: 4px solid #ff4d4f;
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: 4px;
  color: #cf1322;
}

.form-group {
  margin-bottom: 1.25rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 1rem;
  color: #999;
}

input[type="text"],
input[type="email"],
input[type="password"] {
  width: 100%;
  padding: 0.75rem 1rem 0.75rem 2.5rem;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 1rem;
  transition: all 0.3s;
}

input[type="text"]:focus,
input[type="email"]:focus,
input[type="password"]:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
  outline: none;
}

.toggle-password {
  position: absolute;
  right: 1rem;
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
}

.email-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 0.75rem;
  align-items: stretch;
}

.email-code-input {
  min-width: 0;
}

.btn-code {
  border: 1px solid #1890ff;
  border-radius: 4px;
  background: #fff;
  color: #1890ff;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  min-height: 45px;
  white-space: nowrap;
}

.btn-code:hover:not(:disabled) {
  background: #e6f4ff;
}

.btn-code:disabled {
  border-color: #91caff;
  color: #91caff;
  cursor: not-allowed;
}

.agreement {
  display: flex;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.agreement input {
  margin-top: 0.25rem;
  margin-right: 0.5rem;
}

.agreement a {
  color: #1890ff;
  text-decoration: none;
}

.btn-register {
  width: 100%;
  padding: 0.75rem;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
  display: flex;
  justify-content: center;
  align-items: center;
}

.btn-register:hover {
  background-color: #096dd9;
}

.btn-register:disabled {
  background-color: #91caff;
  cursor: not-allowed;
}

.btn-register svg {
  margin-right: 0.5rem;
}

.divider {
  position: relative;
  text-align: center;
  margin: 1.5rem 0;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 45%;
  height: 1px;
  background-color: #e8e8e8;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.divider span {
  display: inline-block;
  padding: 0 1rem;
  background-color: white;
  position: relative;
  color: #999;
}

.social-register {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.btn-social {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border: none;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.3s;
}

.btn-social:hover {
  opacity: 0.9;
}

.github {
  background-color: #24292e;
  color: white;
}

.google {
  background-color: #db4437;
  color: white;
}

.login-link {
  text-align: center;
  font-size: 0.875rem;
  color: #666;
}

.login-link a {
  color: #1890ff;
  text-decoration: none;
  font-weight: 500;
}

@media (max-width: 480px) {
  .register-card {
    padding: 1.5rem;
  }
  
  .social-register {
    flex-direction: column;
  }

  .email-code-row {
    grid-template-columns: 1fr;
  }
}
</style>
