<template>
  <div class="settings-page">
    <ui-page-hero
      title="账号设置"
      description="管理资料、安全、隐私和登录状态，让账号配置保持清晰可控。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'cog']" />
          Settings
        </span>
      </template>
    </ui-page-hero>

    <div class="settings-container">
      <div class="settings-sidebar kumo-surface">
        <h2 class="settings-title">设置导航</h2>
        <ul class="settings-menu">
          <li 
            v-for="item in menuItems" 
            :key="item.key"
            :class="{ active: activeMenu === item.key }"
          >
            <button @click="activeMenu = item.key" class="menu-item">
              <font-awesome-icon :icon="item.icon" />
              <span>{{ item.label }}</span>
            </button>
          </li>
        </ul>
      </div>
      
      <div class="settings-content kumo-surface">
        <!-- 个人资料设置 -->
        <div v-if="activeMenu === 'profile'" class="settings-section">
          <h2 class="section-title">个人资料</h2>
          
          <div v-if="isLoading" class="loading-state">
            <font-awesome-icon :icon="['fas', 'spinner']" spin class="spinner" />
            <p>加载中...</p>
          </div>
          
          <form v-else @submit.prevent="saveProfile" class="profile-form">
            <div v-if="successMessage" class="success-message">
              <font-awesome-icon :icon="['fas', 'check-circle']" />
              {{ successMessage }}
            </div>

            <div v-if="errorMessage" class="error-message">
              <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
              {{ errorMessage }}
            </div>
            
            <div class="avatar-section">
              <div class="current-avatar">
                <img v-if="form.avatar" :src="resolveAvatarUrl(form.avatar)" alt="用户头像">
                <div v-else class="avatar-placeholder">
                  {{ getInitials(form.name) }}
                </div>
              </div>
              <div class="avatar-actions">
                <input
                  ref="avatarInput"
                  type="file"
                  accept="image/jpeg,image/png"
                  class="avatar-input"
                  @change="handleAvatarUpload"
                >
                <button type="button" class="kumo-button kumo-button--brand upload-btn" :disabled="isUploadingAvatar" @click="openAvatarPicker">
                  <font-awesome-icon :icon="['fas', 'camera']" />
                  <span>{{ isUploadingAvatar ? '上传中...' : '更换头像' }}</span>
                </button>
                <button 
                  type="button" 
                  class="kumo-button delete-btn"
                  :disabled="!form.avatar"
                  @click="removeAvatar"
                >
                  <font-awesome-icon :icon="['fas', 'trash']" />
                  <span>移除头像</span>
                </button>
              </div>
            </div>
            
            <div class="form-group">
              <label for="username">用户名</label>
              <input 
                type="text" 
                id="username" 
                v-model="form.username"
                required
                maxlength="20"
                disabled
              >
              <p class="form-hint">用户名将作为您的唯一标识，创建后不可修改</p>
            </div>
            
            <div class="form-group">
              <label for="name">昵称</label>
              <input 
                type="text" 
                id="name" 
                v-model="form.name"
                required
                maxlength="30"
              >
            </div>

            <div class="form-group">
              <label for="email">邮箱</label>
              <input
                type="email"
                id="email"
                v-model="form.email"
                required
                maxlength="100"
              >
              <div v-if="isEmailChanged" class="verification-row">
                <input
                  v-model="emailVerificationCode"
                  type="text"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  maxlength="6"
                  placeholder="请输入新邮箱验证码"
                >
                <button
                  type="button"
                  class="kumo-button code-btn"
                  :disabled="isSendingEmailCode || emailCodeCountdown > 0"
                  @click="sendEmailChangeCode"
                >
                  <font-awesome-icon v-if="isSendingEmailCode" :icon="['fas', 'spinner']" spin />
                  <span v-else>{{ emailCodeButtonText }}</span>
                </button>
              </div>
              <p v-if="isEmailChanged" class="form-hint">更换邮箱需要先验证新邮箱，验证码5分钟内有效。</p>
            </div>
            
            <div class="form-group">
              <label for="bio">个人简介</label>
              <textarea 
                id="bio" 
                v-model="form.bio"
                rows="3"
                maxlength="500"
              ></textarea>
              <p class="form-hint">{{ form.bio.length }}/500</p>
            </div>
            
            <div class="form-actions">
              <button 
                type="submit" 
                class="kumo-button kumo-button--brand save-btn"
                :disabled="isSaving"
              >
                <font-awesome-icon v-if="isSaving" :icon="['fas', 'spinner']" spin />
                <span v-else>保存修改</span>
              </button>
            </div>
          </form>
        </div>
        
        <!-- 密码设置 -->
        <div v-else-if="activeMenu === 'password'" class="settings-section">
          <password-update />
        </div>

        <!-- 安全设置 -->
        <div v-else-if="activeMenu === 'security'" class="settings-section">
          <h2 class="section-title">安全设置</h2>

          <div v-if="successMessage" class="success-message">
            <font-awesome-icon :icon="['fas', 'check-circle']" />
            {{ successMessage }}
          </div>

          <div v-if="errorMessage" class="error-message">
            <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
            {{ errorMessage }}
          </div>

          <div class="security-settings">
            <div class="setting-item">
              <div class="setting-info">
                <h3>两步验证</h3>
                <p>使用 Google Authenticator、Microsoft Authenticator 或其它 TOTP 验证器保护登录。</p>
              </div>
              <span class="status-badge" :class="{ enabled: twoFactorEnabled }">
                {{ twoFactorEnabled ? '已开启' : '未开启' }}
              </span>
            </div>

            <div v-if="!twoFactorEnabled" class="two-factor-panel">
              <button class="kumo-button kumo-button--brand save-btn" type="button" :disabled="isTwoFactorLoading" @click="startTwoFactorSetup">
                <font-awesome-icon v-if="isTwoFactorLoading" :icon="['fas', 'spinner']" spin />
                <span v-else>生成绑定密钥</span>
              </button>

              <div v-if="twoFactorSetup.secret" class="setup-details">
                <p class="form-hint">在验证器中添加以下密钥，然后输入生成的6位验证码启用。</p>
                <img v-if="twoFactorQrCode" class="qr-code" :src="twoFactorQrCode" alt="两步验证二维码">
                <div class="secret-box">{{ twoFactorSetup.secret }}</div>
                <a v-if="twoFactorSetup.otpAuthUrl" class="otp-link" :href="twoFactorSetup.otpAuthUrl">打开验证器添加账号</a>
                <div class="form-group code-group">
                  <label for="enableTwoFactorCode">验证码</label>
                  <input
                    id="enableTwoFactorCode"
                    v-model="enableTwoFactorCode"
                    type="text"
                    inputmode="numeric"
                    autocomplete="one-time-code"
                    maxlength="6"
                    placeholder="请输入6位数字"
                  >
                </div>
                <button class="kumo-button kumo-button--brand save-btn" type="button" :disabled="isTwoFactorLoading" @click="enableTwoFactor">
                  启用两步验证
                </button>
              </div>
            </div>

            <div v-else class="two-factor-panel">
              <div class="form-group code-group">
                <label for="disableTwoFactorCode">关闭前请输入当前验证码</label>
                <input
                  id="disableTwoFactorCode"
                  v-model="disableTwoFactorCode"
                  type="text"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  maxlength="6"
                  placeholder="请输入6位数字"
                >
              </div>
              <button class="kumo-button delete-btn inline-action" type="button" :disabled="isTwoFactorLoading" @click="disableTwoFactor">
                关闭两步验证
              </button>
            </div>
          </div>
        </div>
        
        <!-- 隐私设置 -->
        <div v-else-if="activeMenu === 'privacy'" class="settings-section">
          <h2 class="section-title">隐私设置</h2>
          
          <div class="privacy-settings">
            <div v-if="errorMessage" class="error-message">
              <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
              {{ errorMessage }}
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <h3>公开个人资料</h3>
                <p>允许其他用户查看您的个人资料、文章和问题</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="privacySettings.publicProfile">
                <span class="toggle-slider"></span>
              </label>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h3>显示关注列表</h3>
                <p>允许其他用户查看您关注的人</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="privacySettings.showFollowing">
                <span class="toggle-slider"></span>
              </label>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h3>显示粉丝列表</h3>
                <p>允许其他用户查看关注您的人</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="privacySettings.showFollowers">
                <span class="toggle-slider"></span>
              </label>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h3>活动状态</h3>
                <p>显示您最近的活跃时间</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="privacySettings.showActivity">
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
          
          <div class="form-actions">
            <button 
              class="kumo-button kumo-button--brand save-btn"
              @click="savePrivacySettings"
              :disabled="isSavingPrivacy"
            >
              <font-awesome-icon v-if="isSavingPrivacy" :icon="['fas', 'spinner']" spin />
              <span v-else>保存设置</span>
            </button>
          </div>
        </div>
        
        <!-- 退出登录 -->
        <div v-else-if="activeMenu === 'logout'" class="settings-section">
          <h2 class="section-title">退出登录</h2>
          
          <div class="logout-container">
            <p>确定要退出当前账号吗？</p>
            <button class="kumo-button logout-btn" @click="handleLogout">
              <font-awesome-icon :icon="['fas', 'sign-out-alt']" />
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import PasswordUpdate from '../components/PasswordUpdate.vue';
import UiPageHero from '../components/ui/UiPageHero.vue';
import { userService } from '../services/userService';
import { userApi } from '../api/userApi';
import { resolveAvatarUrl } from '../utils/avatar';
import QRCode from 'qrcode';

const store = useStore();
const router = useRouter();

// 菜单配置
const menuItems = [
  { key: 'profile', label: '个人资料', icon: ['fas', 'user'] },
  { key: 'password', label: '密码设置', icon: ['fas', 'lock'] },
  { key: 'security', label: '安全设置', icon: ['fas', 'shield-alt'] },
  { key: 'privacy', label: '隐私设置', icon: ['fas', 'shield-alt'] },
  { key: 'logout', label: '退出登录', icon: ['fas', 'sign-out-alt'] }
];

// 状态
const activeMenu = ref('profile');
const isLoading = ref(true);
const isSaving = ref(false);
const isSavingPrivacy = ref(false);
const isUploadingAvatar = ref(false);
const isTwoFactorLoading = ref(false);
const successMessage = ref('');
const errorMessage = ref('');
const avatarInput = ref(null);
const originalEmail = ref('');
const emailVerificationCode = ref('');
const isSendingEmailCode = ref(false);
const emailCodeCountdown = ref(0);
let emailCodeTimer = null;

// 表单数据
const form = ref({
  username: '',
  name: '',
  avatar: '',
  bio: '',
  email: ''
});

// 隐私设置
const privacySettings = ref({
  publicProfile: true,
  showFollowing: true,
  showFollowers: true,
  showActivity: true
});

const twoFactorEnabled = ref(false);
const twoFactorSetup = ref({
  secret: '',
  otpAuthUrl: ''
});
const twoFactorQrCode = ref('');
const enableTwoFactorCode = ref('');
const disableTwoFactorCode = ref('');

const normalizeEmail = (email) => (email || '').trim().toLowerCase();
const isEmailChanged = computed(() => normalizeEmail(form.value.email) !== normalizeEmail(originalEmail.value));
const emailCodeButtonText = computed(() => (
  emailCodeCountdown.value > 0 ? `${emailCodeCountdown.value}s后重发` : '发送验证码'
));

// 获取用户名首字母
const getInitials = (name) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

const getCurrentUserId = () => {
  const storeUserId = store.state.user?.id;
  if (storeUserId) return storeUserId;

  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    return userInfo.id || null;
  } catch (error) {
    return null;
  }
};

const fillForm = (userData) => {
  form.value = {
    username: userData.username || '',
    name: userData.name || userData.nickname || userData.username || '',
    avatar: userData.avatar || userData.avatarUrl || '',
    bio: userData.bio || '',
    email: userData.email || ''
  };
  originalEmail.value = userData.email || '';
  emailVerificationCode.value = '';
};

// 加载用户数据
const loadUserData = async () => {
  try {
    isLoading.value = true;
    errorMessage.value = '';

    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error('请先登录后再修改账号设置');
    }

    const userData = await store.dispatch('fetchUserProfile', { userId });
    fillForm(userData);
  } catch (error) {
    errorMessage.value = error.message || '加载用户资料失败';
  } finally {
    isLoading.value = false;
  }
};

// 加载隐私设置（后端暂无隐私表，作为客户端展示偏好持久化在本地，按用户隔离）
const privacyStorageKey = () => `privacySettings_${getCurrentUserId() || 'guest'}`;

const loadPrivacySettings = async () => {
  const defaults = {
    publicProfile: true,
    showFollowing: true,
    showFollowers: true,
    showActivity: true
  };
  try {
    const saved = JSON.parse(localStorage.getItem(privacyStorageKey()) || '{}');
    privacySettings.value = { ...defaults, ...saved };
  } catch (error) {
    privacySettings.value = defaults;
  }
};

const startEmailCodeCountdown = () => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer);
  }
  emailCodeCountdown.value = 60;
  emailCodeTimer = setInterval(() => {
    emailCodeCountdown.value -= 1;
    if (emailCodeCountdown.value <= 0) {
      clearInterval(emailCodeTimer);
      emailCodeTimer = null;
    }
  }, 1000);
};

const sendEmailChangeCode = async () => {
  const email = form.value.email.trim();
  if (!isEmailChanged.value) {
    errorMessage.value = '请先输入新的邮箱地址';
    return;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errorMessage.value = '请输入正确的邮箱地址';
    return;
  }

  try {
    isSendingEmailCode.value = true;
    successMessage.value = '';
    errorMessage.value = '';
    await userApi.sendEmailChangeCode(email);
    startEmailCodeCountdown();
    successMessage.value = '验证码已发送，请查收新邮箱';
  } catch (error) {
    errorMessage.value = error.message || '发送邮箱验证码失败';
  } finally {
    isSendingEmailCode.value = false;
  }
};

// 保存个人资料
const saveProfile = async () => {
  try {
    isSaving.value = true;
    successMessage.value = '';
    errorMessage.value = '';

    if (isEmailChanged.value && !/^\d{6}$/.test(emailVerificationCode.value.trim())) {
      errorMessage.value = '更换邮箱请输入6位邮箱验证码';
      return;
    }

    const profilePayload = {
      nickname: form.value.name.trim(),
      email: form.value.email.trim(),
      bio: form.value.bio,
      avatarUrl: form.value.avatar || null
    };
    if (isEmailChanged.value) {
      profilePayload.verificationCode = emailVerificationCode.value.trim();
    }

    const updatedUser = await store.dispatch('updateUserProfile', profilePayload);

    fillForm(updatedUser);
    originalEmail.value = updatedUser.email || form.value.email.trim();
    emailVerificationCode.value = '';
    
    successMessage.value = '个人资料已更新';
    
    // 3秒后清除成功消息
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (error) {
    errorMessage.value = error.message || '保存个人资料失败';
  } finally {
    isSaving.value = false;
  }
};

const openAvatarPicker = () => {
  avatarInput.value?.click();
};

const handleAvatarUpload = async (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  try {
    isUploadingAvatar.value = true;
    successMessage.value = '';
    errorMessage.value = '';

    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error('请先登录后再上传头像');
    }

    const response = await userService.uploadAvatar(userId, file);
    const avatarUrl = response?.data?.url || response?.url;
    if (!avatarUrl) {
      throw new Error('头像上传成功但未返回访问地址');
    }

    form.value.avatar = avatarUrl;
    successMessage.value = '头像已上传，请保存资料使其生效';
  } catch (error) {
    errorMessage.value = error.message || '上传头像失败';
  } finally {
    isUploadingAvatar.value = false;
    event.target.value = '';
  }
};

const removeAvatar = () => {
  form.value.avatar = '';
};

// 保存隐私设置（本地持久化）
const savePrivacySettings = async () => {
  try {
    isSavingPrivacy.value = true;
    errorMessage.value = '';
    successMessage.value = '';
    localStorage.setItem(privacyStorageKey(), JSON.stringify(privacySettings.value));
    successMessage.value = '隐私设置已保存';
    setTimeout(() => { successMessage.value = ''; }, 3000);
  } catch (error) {
    errorMessage.value = error.message || '隐私设置保存失败';
  } finally {
    isSavingPrivacy.value = false;
  }
};

const updateStoredTwoFactorState = (enabled) => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    if (userInfo?.id) {
      const updated = { ...userInfo, twoFactorEnabled: enabled };
      localStorage.setItem('userInfo', JSON.stringify(updated));
      store.commit('SET_USER', updated);
    }
  } catch (error) {
    return;
  }
};

const loadTwoFactorStatus = async () => {
  try {
    const response = await userApi.getTwoFactorStatus();
    twoFactorEnabled.value = !!response?.data?.enabled;
    updateStoredTwoFactorState(twoFactorEnabled.value);
  } catch (error) {
    return;
  }
};

const generateTwoFactorQrCode = async (otpAuthUrl) => {
  if (!otpAuthUrl) {
    twoFactorQrCode.value = '';
    return;
  }
  twoFactorQrCode.value = await QRCode.toDataURL(otpAuthUrl, {
    width: 180,
    margin: 1,
    errorCorrectionLevel: 'M'
  });
};

const startTwoFactorSetup = async () => {
  try {
    isTwoFactorLoading.value = true;
    successMessage.value = '';
    errorMessage.value = '';
    const response = await userApi.setupTwoFactor();
    twoFactorSetup.value = {
      secret: response?.data?.secret || '',
      otpAuthUrl: response?.data?.otpAuthUrl || ''
    };
    await generateTwoFactorQrCode(twoFactorSetup.value.otpAuthUrl);
    twoFactorEnabled.value = !!response?.data?.enabled;
    if (twoFactorEnabled.value) {
      successMessage.value = '两步验证已开启';
    }
  } catch (error) {
    errorMessage.value = error.message || '生成两步验证密钥失败';
  } finally {
    isTwoFactorLoading.value = false;
  }
};

const enableTwoFactor = async () => {
  if (!enableTwoFactorCode.value) {
    errorMessage.value = '请输入两步验证码';
    return;
  }

  try {
    isTwoFactorLoading.value = true;
    successMessage.value = '';
    errorMessage.value = '';
    const response = await userApi.enableTwoFactor(enableTwoFactorCode.value);
    twoFactorEnabled.value = !!response?.data?.enabled;
    twoFactorSetup.value = { secret: '', otpAuthUrl: '' };
    twoFactorQrCode.value = '';
    enableTwoFactorCode.value = '';
    updateStoredTwoFactorState(twoFactorEnabled.value);
    successMessage.value = '两步验证已启用';
  } catch (error) {
    errorMessage.value = error.message || '启用两步验证失败';
  } finally {
    isTwoFactorLoading.value = false;
  }
};

const disableTwoFactor = async () => {
  if (!disableTwoFactorCode.value) {
    errorMessage.value = '请输入两步验证码';
    return;
  }

  try {
    isTwoFactorLoading.value = true;
    successMessage.value = '';
    errorMessage.value = '';
    const response = await userApi.disableTwoFactor(disableTwoFactorCode.value);
    twoFactorEnabled.value = !!response?.data?.enabled;
    disableTwoFactorCode.value = '';
    updateStoredTwoFactorState(twoFactorEnabled.value);
    successMessage.value = '两步验证已关闭';
  } catch (error) {
    errorMessage.value = error.message || '关闭两步验证失败';
  } finally {
    isTwoFactorLoading.value = false;
  }
};

// 退出登录
const handleLogout = async () => {
  try {
    await store.dispatch('logout');
    router.push('/login');
  } catch (error) {
    errorMessage.value = error.message || '退出登录失败';
  }
};

// 初始化
onMounted(() => {
  loadUserData();
  loadPrivacySettings();
  loadTwoFactorStatus();
});

onUnmounted(() => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer);
  }
});
</script>

<style scoped>
.settings-page {
  display: grid;
  gap: 1.25rem;
}

.settings-container {
  display: grid;
  grid-template-columns: 17rem minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.settings-sidebar {
  position: sticky;
  top: 6rem;
  padding: 1rem;
}

.settings-title {
  margin: 0 0 1rem;
  color: var(--kumo-text-default);
  font-size: 1.2rem;
  font-weight: 850;
}

.settings-menu {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.settings-menu li {
  margin: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  min-height: 2.8rem;
  padding: 0.65rem 0.75rem;
  border: 1px solid transparent;
  border-radius: var(--kumo-radius-md);
  background: transparent;
  color: var(--kumo-text-muted);
  font-weight: 760;
  text-align: left;
  cursor: pointer;
  transition: var(--transition);
}

.settings-menu li.active .menu-item,
.menu-item:hover {
  border-color: var(--kumo-hairline);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.settings-content {
  min-width: 0;
  padding: clamp(1rem, 3vw, 2rem);
}

.settings-section {
  display: grid;
  gap: 1rem;
}

.section-title {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(1.45rem, 3vw, 2.1rem);
  font-weight: 870;
}

.loading-state {
  display: grid;
  place-items: center;
  gap: 0.75rem;
  min-height: 16rem;
  color: var(--kumo-text-muted);
}

.spinner,
.loading-state svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}

.success-message,
.error-message {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--kumo-radius-md);
  font-weight: 720;
}

.success-message {
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.error-message {
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.profile-form,
.security-settings,
.privacy-settings {
  display: grid;
  gap: 1rem;
  max-width: 46rem;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.current-avatar {
  width: 6rem;
  height: 6rem;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
}

.current-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--kumo-bg-brand-strong);
  font-size: 2rem;
  font-weight: 900;
}

.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
}

.avatar-input {
  display: none;
}

.form-group {
  display: grid;
  gap: 0.45rem;
}

.form-group label {
  color: var(--kumo-text-default);
  font-weight: 780;
}

.form-group input,
.form-group textarea,
.verification-row input {
  width: 100%;
  padding: 0.78rem 0.9rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font: inherit;
  transition: var(--transition);
}

.form-group input:focus,
.form-group textarea:focus,
.verification-row input:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.form-group input:disabled {
  color: var(--kumo-text-subtle);
  cursor: not-allowed;
}

.form-hint {
  margin: 0;
  color: var(--kumo-text-muted);
  font-size: 0.86rem;
}

.verification-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.65rem;
  margin-top: 0.35rem;
}

.code-btn {
  min-width: 8rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.save-btn,
.upload-btn,
.delete-btn,
.logout-btn {
  width: fit-content;
}

.delete-btn,
.logout-btn {
  color: var(--kumo-status-danger);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 4.5rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
  font-size: 0.85rem;
  font-weight: 780;
}

.status-badge.enabled {
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.setting-info h3 {
  margin: 0 0 0.25rem;
  color: var(--kumo-text-default);
  font-size: 1rem;
  font-weight: 820;
}

.setting-info p {
  margin: 0;
  color: var(--kumo-text-muted);
  line-height: 1.55;
}

.two-factor-panel {
  display: grid;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.setup-details {
  display: grid;
  gap: 0.75rem;
}

.secret-box {
  padding: 0.75rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-default);
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
}

.qr-code {
  width: 11.25rem;
  height: 11.25rem;
  padding: 0.5rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
}

.otp-link {
  color: var(--kumo-bg-brand-strong);
  font-weight: 760;
  text-decoration: none;
}

.code-group {
  max-width: 18rem;
}

.inline-action {
  display: inline-flex;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 3.1rem;
  height: 1.55rem;
  flex: 0 0 auto;
}

.toggle-switch input {
  width: 0;
  height: 0;
  opacity: 0;
}

.toggle-slider {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: var(--kumo-bg-recessed);
  cursor: pointer;
  transition: var(--transition);
}

.toggle-slider::before {
  content: '';
  position: absolute;
  left: 0.25rem;
  bottom: 0.25rem;
  width: 1.05rem;
  height: 1.05rem;
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-sm);
  transition: var(--transition);
}

input:checked + .toggle-slider {
  background: var(--kumo-bg-brand);
}

input:checked + .toggle-slider::before {
  transform: translateX(1.55rem);
}

.logout-container {
  display: grid;
  place-items: center;
  gap: 1rem;
  min-height: 16rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 860px) {
  .settings-container {
    grid-template-columns: 1fr;
  }

  .settings-sidebar {
    position: static;
  }

  .settings-menu {
    grid-template-columns: repeat(auto-fit, minmax(8rem, 1fr));
  }
}

@media (max-width: 560px) {
  .avatar-section,
  .setting-item,
  .verification-row {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .form-actions {
    justify-content: stretch;
  }

  .save-btn,
  .upload-btn,
  .delete-btn,
  .logout-btn {
    width: 100%;
  }
}
</style> 
