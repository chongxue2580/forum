<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>登录</h1>
        <p>欢迎回到科技论坛，请登录您的账号</p>
      </div>
      
      <div v-if="error" class="error-message">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
        <span>{{ error }}</span>
      </div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'user']" class="input-icon" />
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              placeholder="请输入用户名" 
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
        
        <div class="form-footer">
          <div class="remember-me">
            <input type="checkbox" id="remember" v-model="remember" />
            <label for="remember">记住我</label>
          </div>
          <router-link to="/forgot-password" class="forgot-password">
            忘记密码?
          </router-link>
        </div>
        
        <button type="submit" class="btn-login" :disabled="loading">
          <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="loading" />
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
      </form>
      
      <div class="divider">
        <span>或</span>
      </div>
      
      <div class="social-login">
        <button class="btn-social github">
          <font-awesome-icon :icon="['fab', 'github']" />
          <span>GitHub登录</span>
        </button>
        <button class="btn-social google">
          <font-awesome-icon :icon="['fab', 'google']" />
          <span>Google登录</span>
        </button>
      </div>
      
      <div class="register-link">
        还没有账号? 
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
    
    <div class="login-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'

export default defineComponent({
  name: 'LoginView',
  setup() {
    const router = useRouter()
    const store = useStore()
    const route = useRoute()
    
    const username = ref('')
    const password = ref('')
    const remember = ref(false)
    const showPassword = ref(false)
    const loading = ref(false)
    const error = ref('')
    
    const handleLogin = async () => {
      if (!username.value || !password.value) {
        error.value = '请输入用户名和密码'
        return
      }
      
      loading.value = true
      error.value = ''
      
      try {
        const result = await store.dispatch('login', {
          username: username.value,
          password: password.value,
          remember: remember.value
        })

        console.log('登录成功，结果:', result)
        console.log('当前认证状态:', {
          isAuthenticated: store.state.isAuthenticated,
          user: store.state.user,
          localStorage: {
            token: localStorage.getItem('token'),
            userInfo: localStorage.getItem('userInfo')
          }
        })

        // 登录成功，重定向到原来的页面或首页
        const redirectPath = route.query.redirect || '/'
        console.log('准备重定向到:', redirectPath)

        await router.push(redirectPath)
        console.log('重定向完成')
      } catch (err) {
        console.error('登录失败:', err)
        error.value = err.message || '登录失败，请检查用户名和密码'
      } finally {
        loading.value = false
      }
    }
    
    return {
      username,
      password,
      remember,
      showPassword,
      loading,
      error,
      handleLogin
    }
  }
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem 1rem;
  position: relative;
  overflow: hidden;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 2.5rem;
  position: relative;
  z-index: 2;
  border: 1px solid var(--border-color);
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-header h1 {
  margin: 0;
  margin-bottom: 0.5rem;
  color: var(--text-color);
  font-size: 1.75rem;
  font-weight: 700;
}

.login-header p {
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

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  font-size: 0.9rem;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.remember-me input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--primary-color);
}

.forgot-password {
  color: var(--primary-color);
  text-decoration: none;
  transition: var(--transition);
}

.forgot-password:hover {
  text-decoration: underline;
}

.btn-login {
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

.btn-login:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  filter: brightness(1.1);
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.divider {
  display: flex;
  align-items: center;
  margin: 1.5rem 0;
  color: var(--text-lighter);
  font-size: 0.9rem;
}

.divider::before,
.divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background-color: var(--border-color);
}

.divider span {
  padding: 0 1rem;
}

.social-login {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.btn-social {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border-color);
  background-color: var(--bg-white);
}

.github {
  color: #333;
}

.github:hover {
  background-color: #333;
  color: white;
}

.google {
  color: #ea4335;
}

.google:hover {
  background-color: #ea4335;
  color: white;
}

.register-link {
  text-align: center;
  margin-top: 1rem;
  color: var(--text-light);
  font-size: 0.9rem;
}

.register-link a {
  color: var(--primary-color);
  font-weight: 500;
  text-decoration: none;
  transition: var(--transition);
}

.register-link a:hover {
  text-decoration: underline;
}

/* 装饰元素 */
.login-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.2;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  right: -50px;
  animation: float 6s ease-in-out infinite 1s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  bottom: 20%;
  left: 10%;
  animation: float 10s ease-in-out infinite 2s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.05);
  }
}

/* 响应式设计 */
@media (max-width: 576px) {
  .login-card {
    padding: 1.5rem;
  }
  
  .social-login {
    grid-template-columns: 1fr;
  }
}
</style> 