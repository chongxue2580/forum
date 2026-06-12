<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { toggleSidebar } from './AdminSidebar.js'

const router = useRouter()
const store = useStore()

// 从localStorage获取用户信息
const userInfo = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}');
    console.log('Admin userInfo:', info);
    return info;
  } catch (e) {
    console.error('Error parsing userInfo:', e);
    return {};
  }
})

// 判断是否为管理员
const isAdmin = computed(() => {
  const role = userInfo.value.role
  return role === 'admin' || role === 'ADMIN'
})

// 导航到前台页面
const goToHome = () => {
  router.push('/')
}

// 显示用户下拉菜单
const showUserDropdown = ref(false)

// 退出登录
const logout = () => {
  // 使用Vuex的logout action
  store.dispatch('logout')
  // 重定向到管理员登录页
  router.push('/admin/login')
}

// 获取当前时间
const currentTime = ref(new Date())

// 更新当前时间
setInterval(() => {
  currentTime.value = new Date()
}, 1000)

// 格式化时间
const formattedTime = computed(() => {
  const hours = currentTime.value.getHours().toString().padStart(2, '0')
  const minutes = currentTime.value.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
})

// 格式化日期
const formattedDate = computed(() => {
  const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }
  return currentTime.value.toLocaleDateString('zh-CN', options)
})

// 获取问候语
const greeting = computed(() => {
  const hour = currentTime.value.getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 17) return '下午好'
  if (hour < 19) return '傍晚好'
  return '晚上好'
})

// 导航到个人主页
const goToUserProfile = () => {
  console.log('Navigating to user profile with ID:', userInfo.value.id);
  if (userInfo.value && userInfo.value.id) {
    router.push('/user/' + userInfo.value.id);
  } else {
    console.error('No user ID available for profile navigation');
    // 使用默认管理员ID
    router.push('/user/admin1');
  }
}

// 处理侧边栏切换
const handleToggleSidebar = () => {
  toggleSidebar();
}
</script>

<template>
  <div class="admin-nav-bar">
    <div class="admin-nav-left">
      <div class="toggle-sidebar" @click="handleToggleSidebar">
        <font-awesome-icon :icon="['fas', 'bars']" />
      </div>
      
      <div class="nav-greeting">
        <div class="greeting-text">
          <span class="greeting">{{ greeting }}，{{ userInfo.name || '管理员' }}</span>
          <span class="date">{{ formattedDate }}</span>
        </div>
      </div>
    </div>
    
    <div class="admin-nav-right">
      <div class="nav-time">
        <font-awesome-icon :icon="['fas', 'clock']" />
        <span>{{ formattedTime }}</span>
      </div>

      <div class="nav-notifications">
        <el-badge :value="3" class="notification-badge">
          <font-awesome-icon :icon="['fas', 'bell']" />
        </el-badge>
      </div>
      
      <el-tooltip content="返回前台" placement="bottom">
        <el-button 
          type="primary" 
          @click="goToHome"
          class="nav-button"
          icon="home"
          size="small"
          round
        >
          <font-awesome-icon :icon="['fas', 'home']" />
          <span>返回前台</span>
        </el-button>
      </el-tooltip>
      
      <div class="user-dropdown" @mouseenter="showUserDropdown = true" @mouseleave="showUserDropdown = false">
        <div class="user-info">
          <el-avatar :size="36" :src="userInfo.avatar" class="user-avatar">
            {{ userInfo.name ? userInfo.name.charAt(0) : 'A' }}
          </el-avatar>
          <div class="user-details">
            <span class="user-name">{{ userInfo.name || '管理员' }}</span>
            <span class="user-role">超级管理员</span>
          </div>
          <font-awesome-icon icon="chevron-down" class="dropdown-icon" />
        </div>
        
        <div class="dropdown-menu" v-show="showUserDropdown">
          <div class="dropdown-header">
            <span>管理员选项</span>
          </div>
          
          <div class="dropdown-item" @click="goToUserProfile">
            <font-awesome-icon icon="user" />
            <span>个人主页</span>
          </div>
          
          <div class="dropdown-item" @click="router.push('/settings')">
            <font-awesome-icon icon="cog" />
            <span>个人设置</span>
          </div>
          
          <div class="dropdown-divider"></div>
          
          <div class="dropdown-item" @click="logout">
            <font-awesome-icon icon="sign-out-alt" />
            <span>退出登录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-nav-bar {
  height: 70px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  box-shadow: var(--shadow-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 2rem;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  color: white;
  border-bottom: 2px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.admin-nav-bar::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--accent-color) 0%, #ff6b9d 50%, var(--primary-light) 100%);
}

.admin-nav-left {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.toggle-sidebar {
  font-size: 1.25rem;
  cursor: pointer;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.toggle-sidebar:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.nav-greeting {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.greeting {
  font-weight: 600;
  font-size: 1rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.date {
  font-size: 0.75rem;
  opacity: 0.8;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.admin-nav-right {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.nav-time {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.15);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.2);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.nav-time:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.02);
}

.nav-notifications {
  position: relative;
  cursor: pointer;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.nav-notifications:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.notification-badge {
  font-size: 1.125rem;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.8) 100%);
  color: var(--primary-color);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  transition: all 0.3s ease;
  font-size: 0.875rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-button:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 0.95) 100%);
  color: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.user-dropdown {
  position: relative;
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.5);
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
}

.user-role {
  font-size: 12px;
  opacity: 0.7;
}

.dropdown-icon {
  margin-left: 5px;
  font-size: 12px;
  opacity: 0.7;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 200px;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  z-index: 1000;
  animation: dropdown-fade 0.2s ease;
  border: 1px solid var(--border-color);
}

@keyframes dropdown-fade {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-header {
  padding: 12px 15px;
  background-color: var(--bg-gray);
  font-weight: 600;
  color: var(--text-color);
  font-size: 14px;
}

.dropdown-item {
  padding: 12px 15px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-color);
  transition: var(--transition);
}

.dropdown-item:hover {
  background-color: var(--bg-gray);
  color: var(--primary-color);
}

.dropdown-divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 5px 0;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .admin-nav-bar {
    padding: 0 20px;
  }
  
  .nav-button {
    padding: 6px 12px;
  }
}

@media (max-width: 768px) {
  .nav-greeting .date,
  .nav-time,
  .nav-button span,
  .user-details {
    display: none;
  }
  
  .admin-nav-bar {
    padding: 0 15px;
  }
  
  .admin-nav-right {
    gap: 10px;
  }
  
  .user-info {
    padding: 6px;
  }
}

@media (max-width: 576px) {
  .admin-nav-bar {
    height: 60px;
    padding: 0 10px;
  }
  
  .toggle-sidebar {
    width: 36px;
    height: 36px;
  }
  
  .greeting {
    font-size: 14px;
  }
  
  .nav-notifications {
    width: 36px;
    height: 36px;
  }
  
  .notification-badge {
    font-size: 16px;
  }
}

/* 后台顶栏降噪 */
.admin-nav-bar {
  background: #111827;
  box-shadow: none;
  border-bottom: 1px solid #1f2937;
  backdrop-filter: none;
}

.admin-nav-bar::before {
  display: none;
}

.toggle-sidebar,
.nav-greeting,
.nav-time,
.nav-notifications,
.user-info {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  box-shadow: none;
}

.toggle-sidebar:hover,
.nav-time:hover,
.nav-notifications:hover,
.user-info:hover,
.nav-button:hover {
  transform: none;
  box-shadow: none;
}

.toggle-sidebar:hover,
.nav-notifications:hover,
.user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}

.nav-button {
  background: #ffffff;
  color: #1f2937;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  box-shadow: none;
}

.nav-button:hover {
  background: #f3f4f6;
  color: #111827;
}

.dropdown-menu {
  border-radius: 6px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
}
</style> 
