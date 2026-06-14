<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { toggleSidebar } from './AdminSidebar.js'
import { notificationService } from '../../services/notificationService'
import { formatRelativeTime, toDate } from '../../utils/dateUtils'

const router = useRouter()
const store = useStore()
const props = defineProps({
  isDark: { type: Boolean, default: false },
  bgTheme: { type: String, default: 'mint' }
})
const emit = defineEmits(['toggle-theme', 'set-bg'])

// 背景主题预设（色卡）
const bgPresets = [
  { key: 'mint', label: '薄荷', swatch: 'linear-gradient(135deg,#a8ded6,#bee3de)' },
  { key: 'ice', label: '冰蓝', swatch: 'linear-gradient(135deg,#96beff,#96dee8)' },
  { key: 'gold', label: '暖金', swatch: 'linear-gradient(135deg,#ffe0af,#ffd1c4)' },
  { key: 'violet', label: '暮光', swatch: 'linear-gradient(135deg,#c7c2ff,#e2c8f5)' },
  { key: 'gray', label: '极简', swatch: 'linear-gradient(135deg,#cdd4e0,#eef0f3)' }
]
const unreadNotificationCount = ref(0)
const recentNotifications = ref([])
let clockTimer = null
let notificationTimer = null

// 从localStorage获取用户信息
const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}');
  } catch (e) {
    console.error('Error parsing userInfo:', e);
    return {};
  }
})

// 判断是否为管理员
const isAdmin = computed(() => {
  const role = userInfo.value.role
  return role === 'admin' || role === 'ADMIN' || role === 'SUPER_ADMIN'
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
clockTimer = setInterval(() => {
  currentTime.value = new Date()
}, 1000)

const displayUnreadCount = computed(() => {
  return unreadNotificationCount.value > 99 ? '99+' : unreadNotificationCount.value
})

const loadNotifications = async () => {
  if (!localStorage.getItem('token')) {
    unreadNotificationCount.value = 0
    recentNotifications.value = []
    return
  }

  try {
    const [count, recent] = await Promise.all([
      notificationService.getUnreadCount(),
      notificationService.getRecentNotifications()
    ])
    unreadNotificationCount.value = Number(count || 0)
    recentNotifications.value = recent.slice(0, 5).map(notification => ({
      id: notification.id,
      title: notification.title || '系统通知',
      content: notification.content || '',
      createdAt: toDate(notification.createdAt),
      read: notification.isRead ?? notification.read ?? false
    }))
  } catch (error) {
    unreadNotificationCount.value = 0
    recentNotifications.value = []
  }
}

const formatNotificationTime = (date) => {
  return date ? formatRelativeTime(date) : ''
}

const goToMessages = () => {
  router.push('/messages')
}

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

onMounted(() => {
  loadNotifications()
  notificationTimer = setInterval(loadNotifications, 60000)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (notificationTimer) clearInterval(notificationTimer)
})
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
      <el-popover placement="bottom" :width="240" trigger="click">
        <template #reference>
          <div class="nav-theme-toggle" title="背景主题">
            <font-awesome-icon :icon="['fas', 'palette']" />
          </div>
        </template>
        <div class="bg-picker">
          <div class="bg-picker-title">背景主题</div>
          <div class="bg-picker-grid">
            <button
              v-for="preset in bgPresets"
              :key="preset.key"
              class="bg-swatch"
              :class="{ active: props.bgTheme === preset.key && !props.isDark }"
              :title="preset.label"
              @click="emit('set-bg', preset.key)"
            >
              <span class="bg-swatch-dot" :style="{ background: preset.swatch }"></span>
              <span class="bg-swatch-label">{{ preset.label }}</span>
            </button>
          </div>
          <div class="bg-picker-tip">夜间模式下使用专属深色背景</div>
        </div>
      </el-popover>

      <el-tooltip :content="props.isDark ? '切换到日间' : '切换到夜间'" placement="bottom">
        <div class="nav-theme-toggle" @click="emit('toggle-theme')">
          <font-awesome-icon :icon="['fas', props.isDark ? 'sun' : 'moon']" />
        </div>
      </el-tooltip>

      <div class="nav-time">
        <font-awesome-icon :icon="['fas', 'clock']" />
        <span>{{ formattedTime }}</span>
      </div>

      <el-popover placement="bottom" width="320" trigger="click">
        <template #reference>
          <div class="nav-notifications">
            <el-badge
              :value="displayUnreadCount"
              :hidden="unreadNotificationCount === 0"
              class="notification-badge"
            >
              <font-awesome-icon :icon="['fas', 'bell']" />
            </el-badge>
          </div>
        </template>
        <div class="notification-panel">
          <div class="notification-panel-header">
            <strong>最近通知</strong>
            <span>{{ unreadNotificationCount }} 未读</span>
          </div>
          <div v-if="recentNotifications.length === 0" class="notification-empty">
            暂无通知
          </div>
          <button
            v-for="notification in recentNotifications"
            :key="notification.id"
            class="notification-row"
            :class="{ unread: !notification.read }"
            @click="goToMessages"
          >
            <span class="notification-title">{{ notification.title }}</span>
            <span class="notification-content">{{ notification.content }}</span>
            <span class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</span>
          </button>
          <button class="notification-footer" @click="goToMessages">
            查看全部
          </button>
        </div>
      </el-popover>
      
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
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.01), 0 12px 30px rgba(100, 120, 160, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 2rem;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  color: #1d1d1f;
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(60px) saturate(230%);
  -webkit-backdrop-filter: blur(60px) saturate(230%);
}

.admin-nav-bar::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.6);
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
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.toggle-sidebar:hover {
  background: rgba(0, 0, 0, 0.06);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.nav-greeting {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 1rem;
  background: rgba(0, 0, 0, 0.04);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.greeting {
  font-weight: 600;
  font-size: 1rem;
  text-shadow: none;
}

.date {
  font-size: 0.75rem;
  opacity: 0.8;
  text-shadow: none;
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
  background: rgba(0, 0, 0, 0.05);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
  border: 1px solid rgba(0, 0, 0, 0.06);
  text-shadow: none;
  transition: all 0.3s ease;
}

.nav-time:hover {
  background: rgba(0, 0, 0, 0.06);
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
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-notifications:hover {
  background: rgba(0, 0, 0, 0.06);
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.notification-badge {
  font-size: 1.125rem;
}

.notification-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1f2937;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(127, 149, 176, 0.16);
}

.notification-panel-header span {
  color: #64748b;
  font-size: 12px;
}

.notification-empty {
  padding: 24px 0;
  color: #94a3b8;
  text-align: center;
}

.notification-row {
  border: none;
  background: #fff;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}

.notification-row:hover {
  background: #f8fafc;
}

.notification-row.unread {
  background: #f2f6ff;
}

.notification-title {
  color: #1f2937;
  font-weight: 600;
}

.notification-content {
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  color: #94a3b8;
  font-size: 12px;
}

.notification-footer {
  border: 1px solid rgba(65, 105, 216, 0.24);
  background: #fff;
  color: #4169d8;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
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
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.user-info:hover {
  background: rgba(0, 0, 0, 0.06);
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

/* 后台顶栏：磨砂白柔和风格，与仪表盘卡片统一 */
.admin-nav-bar {
  background: rgba(255, 255, 255, 0.7);
  color: #334155;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(22px);
}

.admin-nav-bar::before {
  display: none;
}

.greeting,
.date,
.nav-time {
  text-shadow: none;
}

.greeting {
  color: #1f2937;
}

.date {
  color: #8190a3;
  opacity: 1;
}

.toggle-sidebar,
.nav-greeting,
.nav-time,
.nav-notifications,
.user-info {
  background: rgba(244, 247, 251, 0.9);
  border: 1px solid rgba(127, 149, 176, 0.18);
  border-radius: 14px;
  box-shadow: none;
  color: #506176;
}

.toggle-sidebar:hover,
.nav-time:hover,
.nav-notifications:hover,
.user-info:hover,
.nav-button:hover {
  transform: none;
}

.toggle-sidebar:hover,
.nav-notifications:hover,
.user-info:hover {
  color: #4169d8;
  background: #f2f6ff;
  border-color: rgba(65, 105, 216, 0.26);
  box-shadow: 0 8px 22px rgba(82, 111, 154, 0.1);
}

.nav-time,
.nav-greeting {
  border-radius: 999px;
}

.user-name {
  color: #1f2937;
}

.user-role {
  color: #8190a3;
  opacity: 1;
}

.nav-button {
  background: #ffffff;
  color: #506176;
  border: 1px solid rgba(105, 126, 154, 0.22);
  border-radius: 999px;
  box-shadow: none;
}

.nav-button:hover {
  background: #f7fbff;
  color: #4169d8;
  border-color: rgba(65, 105, 216, 0.3);
  box-shadow: 0 8px 22px rgba(82, 111, 154, 0.1);
}

.user-avatar {
  border: 2px solid rgba(127, 149, 176, 0.24);
}

.dropdown-menu {
  border-radius: 16px;
  border: 1px solid rgba(127, 149, 176, 0.16);
  box-shadow: 0 20px 50px rgba(74, 96, 128, 0.16);
}

/* 夜间模式切换按钮 */
.nav-theme-toggle {
  cursor: pointer;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-theme-toggle:hover {
  background: rgba(0, 0, 0, 0.07);
  transform: scale(1.05);
}

/* 背景主题选择器 */
.bg-picker-title {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 10px;
}

.bg-picker-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.bg-swatch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 6px 2px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.bg-swatch:hover {
  background: rgba(0, 0, 0, 0.04);
}

.bg-swatch.active {
  border-color: #007aff;
  background: rgba(0, 122, 255, 0.08);
}

.bg-swatch-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}

.bg-swatch-label {
  font-size: 11px;
  color: #515154;
}

.bg-picker-tip {
  margin-top: 10px;
  font-size: 11px;
  color: #86868b;
}

/* ===== 夜间模式：顶栏深色玻璃 ===== */
.admin-dashboard-container.dark .admin-nav-bar {
  background: rgba(20, 24, 30, 0.55);
  color: #f5f5f7;
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

.admin-dashboard-container.dark .admin-nav-bar::before {
  background: rgba(255, 255, 255, 0.08);
}

.admin-dashboard-container.dark .toggle-sidebar,
.admin-dashboard-container.dark .nav-greeting,
.admin-dashboard-container.dark .nav-time,
.admin-dashboard-container.dark .nav-notifications,
.admin-dashboard-container.dark .nav-theme-toggle,
.admin-dashboard-container.dark .user-info {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
}

.admin-dashboard-container.dark .toggle-sidebar:hover,
.admin-dashboard-container.dark .nav-time:hover,
.admin-dashboard-container.dark .nav-notifications:hover,
.admin-dashboard-container.dark .nav-theme-toggle:hover,
.admin-dashboard-container.dark .user-info:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
