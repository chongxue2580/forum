<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>登录账户</h2>
        <p>欢迎回到科技论坛，请登录您的账户</p>
      </div>
      
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="email">邮箱</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'envelope']" class="input-icon" />
            <input 
              type="email" 
              id="email" 
              v-model="email" 
              placeholder="请输入邮箱" 
              required
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
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showPassword = !showPassword"
            >
              <font-awesome-icon 
                :icon="['fas', showPassword ? 'eye-slash' : 'eye']" 
              />
            </button>
          </div>
        </div>
        
        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe" />
            <span>记住我</span>
          </label>
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>
        
        <button 
          type="submit" 
          class="login-button" 
          :disabled="isLoading"
        >
          <span v-if="!isLoading">登录</span>
          <font-awesome-icon v-else :icon="['fas', 'spinner']" spin />
        </button>
      </form>
      
      <div class="social-login">
        <p>使用第三方账号登录</p>
        <div class="social-buttons">
          <button class="social-button github">
            <font-awesome-icon :icon="['fab', 'github']" />
          </button>
          <button class="social-button google">
            <font-awesome-icon :icon="['fab', 'google']" />
          </button>
          <button class="social-button weixin">
            <font-awesome-icon :icon="['fab', 'weixin']" />
          </button>
        </div>
      </div>
      
      <div class="register-link">
        还没有账号？ <a href="#/register">立即注册</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const email = ref('');
const password = ref('');
const rememberMe = ref(false);
const showPassword = ref(false);
const isLoading = ref(false);

const handleLogin = async () => {
  try {
    isLoading.value = true;
    
    // 这里模拟登录请求
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    // 登录成功后，可以将用户信息存储到Vuex中
    // store.dispatch('auth/login', { email: email.value, password: password.value });
    
    // 登录成功后跳转到首页
    router.push('/');
  } catch (error) {
    console.error('登录失败:', error);
    // 可以添加错误处理逻辑
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background-color: var(--bg-color);
}

.login-card {
  width: 100%;
  max-width: 420px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  padding: 40px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 24px;
  color: var(--text-color);
  margin-bottom: 10px;
}

.login-header p {
  color: var(--text-light);
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 15px;
  color: var(--text-light);
}

input[type="email"],
input[type="password"],
input[type="text"] {
  width: 100%;
  padding: 12px 15px 12px 45px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s;
}

input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.toggle-password {
  position: absolute;
  right: 15px;
  background: none;
  border: none;
  color: var(--text-light);
  cursor: pointer;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.remember-me input {
  cursor: pointer;
}

.forgot-password {
  color: var(--primary-color);
  text-decoration: none;
}

.login-button {
  width: 100%;
  padding: 12px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.login-button:hover {
  background-color: #40a9ff;
}

.login-button:disabled {
  background-color: #b5dbff;
  cursor: not-allowed;
}

.social-login {
  margin-top: 30px;
  text-align: center;
}

.social-login p {
  font-size: 14px;
  color: var(--text-light);
  margin-bottom: 15px;
  position: relative;
}

.social-login p::before,
.social-login p::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background-color: var(--border-color);
}

.social-login p::before {
  left: 0;
}

.social-login p::after {
  right: 0;
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.social-button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: transform 0.3s;
}

.social-button:hover {
  transform: translateY(-3px);
}

.github {
  background-color: #333;
}

.google {
  background-color: #DB4437;
}

.weixin {
  background-color: #07C160;
}

.register-link {
  margin-top: 30px;
  text-align: center;
  font-size: 14px;
  color: var(--text-light);
}

.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}
</style> 