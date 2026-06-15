<template>
  <div class="password-update">
    <h2 class="section-title">修改密码</h2>
    
    <div class="form-container">
      <div v-if="successMessage" class="success-message">
        <font-awesome-icon :icon="['fas', 'check-circle']" />
        {{ successMessage }}
      </div>
      
      <div v-if="errorMessage" class="error-message">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
        {{ errorMessage }}
      </div>
      
      <form @submit.prevent="updatePassword">
        <div class="form-item">
          <label for="currentPassword">当前密码</label>
          <div class="password-input">
            <input 
              :type="showCurrentPassword ? 'text' : 'password'" 
              id="currentPassword" 
              v-model="form.currentPassword"
              required
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showCurrentPassword = !showCurrentPassword"
            >
              <font-awesome-icon :icon="['fas', showCurrentPassword ? 'eye-slash' : 'eye']" />
            </button>
          </div>
        </div>
        
        <div class="form-item">
          <label for="newPassword">新密码</label>
          <div class="password-input">
            <input 
              :type="showNewPassword ? 'text' : 'password'" 
              id="newPassword" 
              v-model="form.newPassword"
              required
            />
            <button 
              type="button" 
              class="toggle-password" 
              @click="showNewPassword = !showNewPassword"
            >
              <font-awesome-icon :icon="['fas', showNewPassword ? 'eye-slash' : 'eye']" />
            </button>
          </div>
          <div class="password-strength" v-if="form.newPassword">
            <div class="strength-meter">
              <div 
                class="strength-bar" 
                :style="{ width: passwordStrength.percent + '%' }"
                :class="passwordStrength.class"
              ></div>
            </div>
            <span class="strength-text" :class="passwordStrength.class">
              {{ passwordStrength.text }}
            </span>
          </div>
        </div>
        
        <div class="form-item">
          <label for="confirmPassword">确认新密码</label>
          <div class="password-input">
            <input 
              :type="showConfirmPassword ? 'text' : 'password'" 
              id="confirmPassword" 
              v-model="form.confirmPassword"
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
          <div class="confirm-message" v-if="form.newPassword && form.confirmPassword">
            <span v-if="form.newPassword === form.confirmPassword" class="match">
              <font-awesome-icon :icon="['fas', 'check']" /> 密码匹配
            </span>
            <span v-else class="not-match">
              <font-awesome-icon :icon="['fas', 'times']" /> 密码不匹配
            </span>
          </div>
        </div>
        
        <div class="password-rules">
          <h4>密码要求</h4>
          <ul>
            <li :class="{ valid: hasValidLength }">
              <font-awesome-icon :icon="['fas', hasValidLength ? 'check' : 'times']" />
              6-20个字符
            </li>
            <li :class="{ valid: form.newPassword && form.newPassword === form.confirmPassword }">
              <font-awesome-icon :icon="['fas', form.newPassword && form.newPassword === form.confirmPassword ? 'check' : 'times']" />
              两次输入一致
            </li>
          </ul>
        </div>

        <div class="form-item">
          <label for="passwordVerificationCode">邮箱验证码</label>
          <div class="verification-row">
            <input
              id="passwordVerificationCode"
              v-model="form.verificationCode"
              type="text"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              placeholder="请输入6位验证码"
              required
            />
            <button
              type="button"
              class="code-btn"
              :disabled="isSendingCode || codeCountdown > 0"
              @click="sendPasswordCode"
            >
              <font-awesome-icon v-if="isSendingCode" :icon="['fas', 'spinner']" spin />
              <span v-else>{{ codeButtonText }}</span>
            </button>
          </div>
          <p class="form-hint">验证码会发送到当前绑定邮箱，5分钟内有效。</p>
        </div>

        <div class="form-actions">
          <button 
            type="submit" 
            class="update-btn"
            :disabled="!isFormValid || isLoading"
          >
            <font-awesome-icon v-if="isLoading" :icon="['fas', 'spinner']" spin />
            <span v-else>更新密码</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue';
import { useStore } from 'vuex';
import { userApi } from '../api/userApi';

const store = useStore();

const form = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
  verificationCode: ''
});

const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);
const isLoading = ref(false);
const isSendingCode = ref(false);
const codeCountdown = ref(0);
const successMessage = ref('');
const errorMessage = ref('');
let codeTimer = null;

// 密码验证规则
const hasValidLength = computed(() => form.value.newPassword.length >= 6 && form.value.newPassword.length <= 20);
const hasUppercase = computed(() => /[A-Z]/.test(form.value.newPassword));
const hasLowercase = computed(() => /[a-z]/.test(form.value.newPassword));
const hasNumber = computed(() => /[0-9]/.test(form.value.newPassword));
const hasSpecial = computed(() => /[!@#$%^&*(),.?":{}|<>]/.test(form.value.newPassword));

// 密码强度计算
const passwordStrength = computed(() => {
  if (!form.value.newPassword) {
    return { percent: 0, class: '', text: '' };
  }
  
  let strength = 0;
  if (hasValidLength.value) strength += 20;
  if (hasUppercase.value) strength += 20;
  if (hasLowercase.value) strength += 20;
  if (hasNumber.value) strength += 20;
  if (hasSpecial.value) strength += 20;
  
  let strengthClass = '';
  let strengthText = '';
  
  if (strength < 40) {
    strengthClass = 'weak';
    strengthText = '弱';
  } else if (strength < 80) {
    strengthClass = 'medium';
    strengthText = '中';
  } else {
    strengthClass = 'strong';
    strengthText = '强';
  }
  
  return {
    percent: strength,
    class: strengthClass,
    text: strengthText
  };
});

const codeButtonText = computed(() => (
  codeCountdown.value > 0 ? `${codeCountdown.value}s后重发` : '发送验证码'
));

// 表单验证
const isFormValid = computed(() => {
  return (
    form.value.currentPassword && 
    form.value.newPassword && 
    form.value.confirmPassword && 
    form.value.newPassword === form.value.confirmPassword && 
    hasValidLength.value &&
    /^\d{6}$/.test(form.value.verificationCode.trim())
  );
});

const startCodeCountdown = () => {
  if (codeTimer) {
    clearInterval(codeTimer);
  }
  codeCountdown.value = 60;
  codeTimer = setInterval(() => {
    codeCountdown.value -= 1;
    if (codeCountdown.value <= 0) {
      clearInterval(codeTimer);
      codeTimer = null;
    }
  }, 1000);
};

const sendPasswordCode = async () => {
  try {
    isSendingCode.value = true;
    successMessage.value = '';
    errorMessage.value = '';
    await userApi.sendPasswordChangeCode();
    startCodeCountdown();
    successMessage.value = '验证码已发送，请查收当前绑定邮箱';
  } catch (error) {
    errorMessage.value = error.message || '发送验证码失败';
  } finally {
    isSendingCode.value = false;
  }
};

// 重置消息
watch(form, () => {
  successMessage.value = '';
  errorMessage.value = '';
}, { deep: true });

// 提交表单
const updatePassword = async () => {
  if (!isFormValid.value) return;
  
  try {
    isLoading.value = true;
    errorMessage.value = '';

    await store.dispatch('updatePassword', {
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
      verificationCode: form.value.verificationCode.trim()
    });

    successMessage.value = '密码已成功更新';
    form.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
      verificationCode: ''
    };
  } catch (error) {
    errorMessage.value = error.message || '更新密码时出错';
  } finally {
    isLoading.value = false;
  }
};

onUnmounted(() => {
  if (codeTimer) {
    clearInterval(codeTimer);
  }
});
</script>

<style scoped>
.password-update {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.form-container {
  background-color: #fff;
  border-radius: var(--radius);
  padding: 1.5rem;
  box-shadow: var(--shadow);
}

.success-message, .error-message {
  padding: 0.75rem 1rem;
  border-radius: var(--radius);
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.success-message {
  background-color: rgba(var(--success-rgb), 0.1);
  color: var(--success-color);
  border: 1px solid var(--success-color);
}

.error-message {
  background-color: #fff1f0;
  color: #a8071a;
  border: 1px solid #ff7875;
}

.form-item {
  margin-bottom: 1.5rem;
}

.form-item label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.password-input {
  position: relative;
}

.password-input input {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 1rem;
  transition: all 0.3s;
}

.password-input input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2);
}

.toggle-password {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-light);
  cursor: pointer;
  padding: 0.25rem;
}

.password-strength {
  margin-top: 0.5rem;
}

.strength-meter {
  height: 5px;
  background-color: var(--bg-light);
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 0.25rem;
}

.strength-bar {
  height: 100%;
  transition: width 0.3s;
}

.strength-bar.weak {
  background-color: var(--error-color);
}

.strength-bar.medium {
  background-color: var(--warning-color);
}

.strength-bar.strong {
  background-color: var(--success-color);
}

.strength-text {
  font-size: 0.85rem;
}

.strength-text.weak {
  color: var(--error-color);
}

.strength-text.medium {
  color: var(--warning-color);
}

.strength-text.strong {
  color: var(--success-color);
}

.confirm-message {
  margin-top: 0.5rem;
  font-size: 0.85rem;
}

.match {
  color: var(--success-color);
}

.not-match {
  color: var(--error-color);
}

.password-rules {
  background-color: var(--bg-light);
  border-radius: var(--radius);
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.password-rules h4 {
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.password-rules ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.password-rules li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--text-light);
  margin-bottom: 0.25rem;
}

.password-rules li.valid {
  color: var(--success-color);
}

.verification-row {
  display: flex;
  gap: 0.75rem;
}

.verification-row input {
  flex: 1;
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 1rem;
  transition: all 0.3s;
}

.verification-row input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2);
}

.code-btn {
  flex: 0 0 132px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.75rem 1rem;
  border: none;
  border-radius: var(--radius);
  background-color: var(--primary-color);
  color: #fff;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.code-btn:hover {
  background-color: var(--primary-dark);
}

.code-btn:disabled {
  background-color: var(--text-lighter);
  cursor: not-allowed;
}

.form-hint {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
  color: var(--text-light);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.update-btn {
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  padding: 0.75rem 1.5rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
}

.update-btn:hover {
  background-color: var(--primary-dark);
}

.update-btn:disabled {
  background-color: var(--text-lighter);
  cursor: not-allowed;
}
</style> 
