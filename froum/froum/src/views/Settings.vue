<template>
  <div class="settings-page">
    <div class="settings-container">
      <div class="settings-sidebar">
        <h2 class="settings-title">账号设置</h2>
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
      
      <div class="settings-content">
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
                <img v-if="form.avatar" :src="form.avatar" alt="用户头像">
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
                <button type="button" class="upload-btn" :disabled="isUploadingAvatar" @click="openAvatarPicker">
                  <font-awesome-icon :icon="['fas', 'camera']" />
                  <span>{{ isUploadingAvatar ? '上传中...' : '更换头像' }}</span>
                </button>
                <button 
                  type="button" 
                  class="delete-btn"
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
                class="save-btn"
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
              class="save-btn"
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
            <button class="logout-btn" @click="handleLogout">
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
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import PasswordUpdate from '../components/PasswordUpdate.vue';
import { userService } from '../services/userService';

const store = useStore();
const router = useRouter();

// 菜单配置
const menuItems = [
  { key: 'profile', label: '个人资料', icon: ['fas', 'user'] },
  { key: 'password', label: '密码设置', icon: ['fas', 'lock'] },
  { key: 'privacy', label: '隐私设置', icon: ['fas', 'shield-alt'] },
  { key: 'logout', label: '退出登录', icon: ['fas', 'sign-out-alt'] }
];

// 状态
const activeMenu = ref('profile');
const isLoading = ref(true);
const isSaving = ref(false);
const isSavingPrivacy = ref(false);
const isUploadingAvatar = ref(false);
const successMessage = ref('');
const errorMessage = ref('');
const avatarInput = ref(null);

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
    console.error('Failed to load user data:', error);
    errorMessage.value = error.message || '加载用户资料失败';
  } finally {
    isLoading.value = false;
  }
};

// 加载隐私设置
const loadPrivacySettings = async () => {
  privacySettings.value = {
    publicProfile: true,
    showFollowing: true,
    showFollowers: true,
    showActivity: true
  };
};

// 保存个人资料
const saveProfile = async () => {
  try {
    isSaving.value = true;
    successMessage.value = '';
    errorMessage.value = '';

    const updatedUser = await store.dispatch('updateUserProfile', {
      nickname: form.value.name.trim(),
      email: form.value.email.trim(),
      bio: form.value.bio,
      avatarUrl: form.value.avatar || null
    });

    fillForm(updatedUser);
    
    successMessage.value = '个人资料已更新';
    
    // 3秒后清除成功消息
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to save profile:', error);
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
    console.error('Failed to upload avatar:', error);
    errorMessage.value = error.message || '上传头像失败';
  } finally {
    isUploadingAvatar.value = false;
    event.target.value = '';
  }
};

const removeAvatar = () => {
  form.value.avatar = '';
};

// 保存隐私设置
const savePrivacySettings = async () => {
  try {
    isSavingPrivacy.value = true;
    errorMessage.value = '';
    await store.dispatch('updatePrivacySettings', privacySettings.value);
  } catch (error) {
    console.error('Failed to save privacy settings:', error);
    errorMessage.value = error.message || '隐私设置保存失败';
  } finally {
    isSavingPrivacy.value = false;
  }
};

// 退出登录
const handleLogout = async () => {
  try {
    await store.dispatch('logout');
    router.push('/login');
  } catch (error) {
    console.error('Failed to logout:', error);
  }
};

// 初始化
onMounted(() => {
  loadUserData();
  loadPrivacySettings();
});
</script>

<style scoped>
.settings-page {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 1rem;
}

.settings-container {
  display: flex;
  gap: 2rem;
}

.settings-sidebar {
  width: 250px;
  flex-shrink: 0;
}

.settings-title {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.settings-menu {
  list-style: none;
  padding: 0;
  margin: 0;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.settings-menu li {
  border-bottom: 1px solid var(--border-color);
}

.settings-menu li:last-child {
  border-bottom: none;
}

.settings-menu li.active .menu-item {
  background-color: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  font-weight: 500;
  border-left: 3px solid var(--primary-color);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 1rem 1.25rem;
  border: none;
  background: none;
  text-align: left;
  color: var(--text-color);
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  background-color: var(--bg-light);
  color: var(--primary-color);
}

.settings-content {
  flex: 1;
  min-width: 0;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 2rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 0;
  color: var(--text-light);
}

.spinner {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.success-message {
  padding: 0.75rem 1rem;
  border-radius: var(--radius);
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: rgba(var(--success-rgb), 0.1);
  color: var(--success-color);
  border: 1px solid var(--success-color);
}

.error-message {
  padding: 0.75rem 1rem;
  border-radius: var(--radius);
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: rgba(var(--error-rgb), 0.1);
  color: var(--error-color);
  border: 1px solid var(--error-color);
}

/* 头像部分 */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.current-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.current-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: 600;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.avatar-input {
  display: none;
}

.upload-btn, .delete-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-btn {
  background-color: var(--primary-color);
  color: white;
  border: none;
}

.upload-btn:hover {
  background-color: var(--primary-dark);
}

.delete-btn {
  background-color: transparent;
  color: var(--error-color);
  border: 1px solid var(--error-color);
}

.delete-btn:hover {
  background-color: rgba(var(--error-rgb), 0.1);
}

.delete-btn:disabled {
  background-color: transparent;
  color: var(--text-lighter);
  border-color: var(--text-lighter);
  cursor: not-allowed;
}

/* 表单样式 */
.profile-form {
  max-width: 600px;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 1rem;
  transition: all 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.form-hint {
  font-size: 0.85rem;
  color: var(--text-light);
  margin-top: 0.35rem;
}

.input-with-prefix {
  display: flex;
  align-items: center;
}

.input-prefix {
  background-color: var(--bg-light);
  color: var(--text-light);
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);
  border-right: none;
  border-radius: var(--radius) 0 0 var(--radius);
  font-size: 1rem;
}

.input-with-prefix input {
  border-radius: 0 var(--radius) var(--radius) 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 2rem;
}

.save-btn {
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

.save-btn:hover {
  background-color: var(--primary-dark);
}

.save-btn:disabled {
  background-color: var(--text-lighter);
  cursor: not-allowed;
}

/* 隐私设置样式 */
.privacy-settings {
  margin-bottom: 2rem;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
  border-bottom: 1px solid var(--border-color);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info h3 {
  font-size: 1.1rem;
  font-weight: 500;
  margin-bottom: 0.25rem;
  color: var(--text-color);
}

.setting-info p {
  color: var(--text-light);
  font-size: 0.9rem;
  margin: 0;
}

/* 开关样式 */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--text-lighter);
  transition: .4s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: var(--primary-color);
}

input:checked + .toggle-slider:before {
  transform: translateX(26px);
}

/* 退出登录样式 */
.logout-container {
  text-align: center;
  padding: 2rem 0;
}

.logout-container p {
  color: var(--text-light);
  margin-bottom: 1.5rem;
}

.logout-btn {
  background-color: var(--error-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  padding: 0.75rem 1.5rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.logout-btn:hover {
  background-color: var(--error-dark);
}

@media (max-width: 768px) {
  .settings-container {
    flex-direction: column;
    gap: 1rem;
  }
  
  .settings-sidebar {
    width: 100%;
  }
  
  .settings-menu {
    display: flex;
    flex-wrap: wrap;
  }
  
  .settings-menu li {
    border: none;
    flex: 1;
    min-width: 120px;
  }
  
  .menu-item {
    flex-direction: column;
    gap: 0.5rem;
    padding: 0.75rem;
  }
  
  .avatar-section {
    flex-direction: column;
    text-align: center;
  }
  
  .current-avatar {
    margin: 0 auto;
  }
  
  .form-actions {
    justify-content: center;
  }
}
</style> 
