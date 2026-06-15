<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <div class="forgot-header">
        <router-link to="/login" class="back-link" aria-label="返回登录">
          <font-awesome-icon :icon="['fas', 'arrow-left']" />
        </router-link>
        <h1>重置密码</h1>
      </div>

      <div v-if="error" class="error-message">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
        <span>{{ error }}</span>
      </div>

      <div v-if="successMessage" class="success-message">
        <font-awesome-icon :icon="['fas', 'check-circle']" />
        <span>{{ successMessage }}</span>
      </div>

      <form @submit.prevent="handleResetPassword">
        <div class="form-group">
          <label for="account">用户名或邮箱</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'user']" class="input-icon" />
            <input
              type="text"
              id="account"
              v-model="account"
              placeholder="请输入用户名或邮箱"
              autocomplete="username"
              required
            />
          </div>
        </div>

        <div class="form-group">
          <label for="verificationCode">邮箱验证码</label>
          <div class="code-row">
            <div class="input-wrapper code-input">
              <font-awesome-icon :icon="['fas', 'shield-alt']" class="input-icon" />
              <input
                type="text"
                id="verificationCode"
                v-model="verificationCode"
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
              :disabled="sendingCode || codeCountdown > 0"
              @click="handleSendCode"
            >
              <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="sendingCode" />
              <span>{{ codeButtonText }}</span>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label for="newPassword">新密码</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'lock']" class="input-icon" />
            <input
              :type="showPassword ? 'text' : 'password'"
              id="newPassword"
              v-model="newPassword"
              placeholder="请设置新密码"
              autocomplete="new-password"
              required
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

        <div class="form-group">
          <label for="confirmPassword">确认新密码</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'lock']" class="input-icon" />
            <input
              :type="showConfirmPassword ? 'text' : 'password'"
              id="confirmPassword"
              v-model="confirmPassword"
              placeholder="请再次输入新密码"
              autocomplete="new-password"
              required
            />
            <button
              type="button"
              class="toggle-password"
              @click="showConfirmPassword = !showConfirmPassword"
              aria-label="显示密码"
            >
              <font-awesome-icon :icon="['fas', showConfirmPassword ? 'eye-slash' : 'eye']" />
            </button>
          </div>
        </div>

        <button type="submit" class="btn-reset" :disabled="loading">
          <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="loading" />
          <span>{{ loading ? '提交中...' : '重置密码' }}</span>
        </button>
      </form>

      <div class="login-link">
        想起密码?
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, defineComponent, onUnmounted, ref } from 'vue'
import { userApi } from '../api/userApi'

export default defineComponent({
  name: 'ForgotPasswordView',
  setup() {
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
      if (sendingCode.value) {
        return '发送中'
      }
      return codeCountdown.value > 0 ? `${codeCountdown.value}s` : '发送验证码'
    })

    const startCountdown = () => {
      codeCountdown.value = 60
      if (codeTimer) {
        clearInterval(codeTimer)
      }
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
      if (codeTimer) {
        clearInterval(codeTimer)
      }
    })

    return {
      account,
      verificationCode,
      newPassword,
      confirmPassword,
      showPassword,
      showConfirmPassword,
      sendingCode,
      loading,
      error,
      successMessage,
      codeCountdown,
      codeButtonText,
      handleSendCode,
      handleResetPassword
    }
  }
})
</script>

<style scoped>
.forgot-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem 1rem;
}

.forgot-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-white);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 2.5rem;
}

.forgot-header {
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
  margin-bottom: 2rem;
}

.forgot-header h1 {
  margin: 0;
  color: var(--text-color);
  font-size: 1.75rem;
  font-weight: 700;
  text-align: center;
}

.back-link {
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  color: var(--text-light);
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  transition: var(--transition);
}

.back-link:hover {
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.08);
}

.error-message,
.success-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: var(--radius);
}

.error-message {
  background-color: #fff1f0;
  border: 1px solid #ff7875;
  color: #a8071a;
}

.success-message {
  background-color: rgba(var(--success-rgb), 0.08);
  border: 1px solid rgba(var(--success-rgb), 0.28);
  color: var(--success-color);
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
  transition: var(--transition);
}

.toggle-password:hover {
  color: var(--primary-color);
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 0.75rem;
  align-items: stretch;
}

.code-input {
  min-width: 0;
}

.btn-code {
  min-height: 45px;
  border: 1px solid var(--primary-color);
  border-radius: var(--radius);
  background: var(--bg-white);
  color: var(--primary-color);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  white-space: nowrap;
}

.btn-code:hover:not(:disabled) {
  background: rgba(var(--primary-rgb), 0.08);
}

.btn-code:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-reset {
  width: 100%;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-reset:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  filter: brightness(1.1);
}

.btn-reset:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-link {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 0.9rem;
  color: var(--text-light);
}

.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .forgot-card {
    padding: 1.5rem;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
