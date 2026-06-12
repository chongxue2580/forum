<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>创建账户</h2>
        <p>欢迎加入科技论坛，请完成以下信息进行注册</p>
      </div>
      
      <form class="register-form" @submit.prevent="handleRegister">
        <div class="form-row">
          <div class="form-group">
            <label for="firstName">姓</label>
            <div class="input-wrapper">
              <font-awesome-icon :icon="['fas', 'user']" class="input-icon" />
              <input 
                type="text" 
                id="firstName" 
                v-model="firstName" 
                placeholder="请输入姓" 
                required
              />
            </div>
          </div>
          <div class="form-group">
            <label for="lastName">名</label>
            <div class="input-wrapper">
              <font-awesome-icon :icon="['fas', 'user']" class="input-icon" />
              <input 
                type="text" 
                id="lastName" 
                v-model="lastName" 
                placeholder="请输入名" 
                required
              />
            </div>
          </div>
        </div>
        
        <div class="form-group">
          <label for="username">用户名</label>
          <div class="input-wrapper">
            <font-awesome-icon :icon="['fas', 'id-badge']" class="input-icon" />
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              placeholder="请设置用户名" 
              required
            />
          </div>
          <div class="input-hint">用户名将显示在您发布的内容中</div>
        </div>
        
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
              placeholder="请设置密码" 
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
          <div class="password-strength" :class="passwordStrengthClass">
            <div class="strength-bar"></div>
            <span class="strength-text">{{ passwordStrengthText }}</span>
          </div>
          <div class="password-hint">
            密码至少包含8个字符，且包含数字、字母和特殊字符
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
              <font-awesome-icon 
                :icon="['fas', showConfirmPassword ? 'eye-slash' : 'eye']" 
              />
            </button>
          </div>
          <div class="input-error" v-if="passwordMismatch">
            两次输入的密码不一致
          </div>
        </div>
        
        <div class="form-group checkbox-group">
          <label class="checkbox-label">
            <input type="checkbox" v-model="agreeTerms" required />
            <span>我已阅读并同意 <a href="#" class="link">用户协议</a> 和 <a href="#" class="link">隐私政策</a></span>
          </label>
        </div>
        
        <button 
          type="submit" 
          class="register-button" 
          :disabled="isLoading || !isFormValid"
        >
          <span v-if="!isLoading">创建账户</span>
          <font-awesome-icon v-else :icon="['fas', 'spinner']" spin />
        </button>
      </form>
      
      <div class="login-link">
        已有账号？ <a href="#/login">立即登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const firstName = ref('');
const lastName = ref('');
const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const agreeTerms = ref(false);
const isLoading = ref(false);

// 密码强度检查
const passwordStrength = computed(() => {
  if (!password.value) return 0;
  
  let strength = 0;
  
  // 长度检查
  if (password.value.length >= 8) strength += 1;
  
  // 包含数字
  if (/\d/.test(password.value)) strength += 1;
  
  // 包含小写字母
  if (/[a-z]/.test(password.value)) strength += 1;
  
  // 包含大写字母
  if (/[A-Z]/.test(password.value)) strength += 1;
  
  // 包含特殊字符
  if (/[^A-Za-z0-9]/.test(password.value)) strength += 1;
  
  return strength;
});

const passwordStrengthClass = computed(() => {
  const strength = passwordStrength.value;
  if (strength <= 1) return 'weak';
  if (strength <= 3) return 'medium';
  return 'strong';
});

const passwordStrengthText = computed(() => {
  const strength = passwordStrength.value;
  if (strength <= 1) return '弱';
  if (strength <= 3) return '中';
  return '强';
});

const passwordMismatch = computed(() => {
  return confirmPassword.value && password.value !== confirmPassword.value;
});

const isFormValid = computed(() => {
  return (
    firstName.value &&
    lastName.value &&
    username.value &&
    email.value &&
    password.value &&
    confirmPassword.value &&
    !passwordMismatch.value &&
    agreeTerms.value
  );
});

const handleRegister = async () => {
  if (!isFormValid.value) return;
  
  try {
    isLoading.value = true;
    
    // 这里模拟注册请求
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    // 注册成功后，可以将用户信息存储到Vuex中
    // store.dispatch('auth/register', { 
    //   firstName: firstName.value,
    //   lastName: lastName.value,
    //   username: username.value,
    //   email: email.value,
    //   password: password.value
    // });
    
    // 注册成功后跳转到首页或登录页
    router.push('/login');
  } catch (error) {
    console.error('注册失败:', error);
    // 可以添加错误处理逻辑
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background-color: var(--bg-color);
}

.register-card {
  width: 100%;
  max-width: 600px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  padding: 40px;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  font-size: 24px;
  color: var(--text-color);
  margin-bottom: 10px;
}

.register-header p {
  color: var(--text-light);
  font-size: 14px;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  gap: 15px;
}

.form-row .form-group {
  flex: 1;
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

.input-hint,
.password-hint {
  font-size: 12px;
  color: var(--text-light);
}

.input-error {
  font-size: 12px;
  color: #ff4d4f;
}

.password-strength {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.strength-bar {
  height: 4px;
  border-radius: 2px;
  background-color: #eee;
  position: relative;
  overflow: hidden;
}

.strength-bar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  transition: width 0.3s, background-color 0.3s;
}

.weak .strength-bar::before {
  width: 33%;
  background-color: #ff4d4f;
}

.medium .strength-bar::before {
  width: 66%;
  background-color: #faad14;
}

.strong .strength-bar::before {
  width: 100%;
  background-color: #52c41a;
}

.strength-text {
  font-size: 12px;
  align-self: flex-end;
}

.weak .strength-text {
  color: #ff4d4f;
}

.medium .strength-text {
  color: #faad14;
}

.strong .strength-text {
  color: #52c41a;
}

.checkbox-group {
  margin-top: 10px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-color);
}

.checkbox-label input {
  cursor: pointer;
}

.link {
  color: var(--primary-color);
  text-decoration: none;
}

.register-button {
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
  margin-top: 10px;
}

.register-button:hover {
  background-color: #40a9ff;
}

.register-button:disabled {
  background-color: #b5dbff;
  cursor: not-allowed;
}

.login-link {
  margin-top: 30px;
  text-align: center;
  font-size: 14px;
  color: var(--text-light);
}

.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

@media (max-width: 600px) {
  .form-row {
    flex-direction: column;
    gap: 20px;
  }
  
  .register-card {
    padding: 30px 20px;
  }
}
</style> 