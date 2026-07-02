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
  bgTheme: { type: String, default: 'ice' }
})
const emit = defineEmits(['toggle-theme', 'set-bg'])

// 背景主题预设（色卡）
const bgPresets = [
  { key: 'ice', label: 'Paper', swatch: 'linear-gradient(135deg, #ffffff, #f6f6f4)' },
  { key: 'mint', label: 'Sage', swatch: 'linear-gradient(135deg, #eef3ee, #f6f6f4)' },
  { key: 'gold', label: 'Sand', swatch: 'linear-gradient(135deg, #f4efe2, #f6f6f4)' },
  { key: 'violet', label: 'Slate', swatch: 'linear-gradient(135deg, #edf1f6, #f6f6f4)' },
  { key: 'gray', label: 'Mono', swatch: 'linear-gradient(135deg, #f2f1ee, #e4e2dd)' }
]
const unreadNotificationCount = ref(0)
const recentNotifications = ref([])
let clockTimer = null
let notificationTimer = null

// 从localStorage获取用户信息
const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}');
  } catch (error) {
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
    router.push('/settings');
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
  <div class="admin-nav-bar" :class="{ dark: props.isDark }">
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
  --admin-nav-surface: #ffffff;
  --admin-nav-bg: #f6f6f4;
  --admin-nav-muted: #53514d;
  --admin-nav-soft: #7d7972;
  --admin-nav-border: #e4e2dd;
  --admin-nav-border-strong: #cfcac2;
  --admin-nav-ink: #171717;
  --admin-nav-hover: #f2f1ee;
  --admin-nav-info: #596f8f;
  --admin-nav-danger: #9d4a3f;

  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  color: var(--admin-nav-ink);
  border-bottom: 1px solid var(--admin-nav-border);
  background: color-mix(in srgb, var(--admin-nav-surface) 94%, var(--admin-nav-bg));
  box-shadow: 0 1px 2px rgba(23, 23, 23, 0.04), 0 10px 24px rgba(23, 23, 23, 0.04);
}

:global(html[data-mode='dark']) .admin-nav-bar,
.admin-nav-bar.dark {
  --admin-nav-surface: #242019;
  --admin-nav-bg: #15130f;
  --admin-nav-muted: #c7bdae;
  --admin-nav-soft: #958a7b;
  --admin-nav-border: rgba(255, 244, 232, 0.12);
  --admin-nav-border-strong: rgba(255, 244, 232, 0.22);
  --admin-nav-ink: #f5efe6;
  --admin-nav-hover: #1c1914;
  --admin-nav-info: #ff9b69;
  --admin-nav-danger: #ee8d80;

  background: color-mix(in srgb, var(--admin-nav-surface) 88%, transparent);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.26), 0 16px 36px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(14px);
}

.admin-nav-left,
.admin-nav-right,
.nav-time,
.user-info,
.notification-panel-header,
.dropdown-item {
  display: flex;
  align-items: center;
}

.admin-nav-left {
  gap: 1.5rem;
}

.admin-nav-right {
  gap: 1.25rem;
}

.toggle-sidebar,
.nav-theme-toggle,
.nav-notifications,
.nav-time,
.nav-greeting,
.user-info,
.nav-button {
  border: 1px solid var(--admin-nav-border);
  background: var(--admin-nav-surface);
  color: var(--admin-nav-muted);
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    background-color var(--kumo-transition),
    color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.toggle-sidebar,
.nav-theme-toggle,
.nav-notifications {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
}

.toggle-sidebar {
  font-size: 1.25rem;
}

.toggle-sidebar:hover,
.nav-theme-toggle:hover,
.nav-notifications:hover,
.user-info:hover,
.nav-button:hover {
  transform: translateY(-1px);
  color: var(--admin-nav-ink);
  border-color: var(--admin-nav-border-strong);
  background: var(--admin-nav-hover);
  box-shadow: 0 1px 2px rgba(23, 23, 23, 0.06);
}

.nav-greeting {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 1rem;
  border-radius: 10px;
}

.greeting {
  color: var(--admin-nav-ink);
  font-size: 1rem;
  font-weight: 650;
}

.date,
.user-role,
.notification-panel-header span,
.notification-content,
.notification-time,
.bg-picker-tip,
.bg-swatch-label {
  color: var(--admin-nav-muted);
}

.date {
  font-size: 0.75rem;
}

.nav-time {
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 650;
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
  justify-content: space-between;
  padding-bottom: 8px;
  color: var(--admin-nav-ink);
  border-bottom: 1px solid var(--admin-nav-border);
}

.notification-panel-header span,
.notification-time {
  font-size: 12px;
}

.notification-empty {
  padding: 24px 0;
  color: var(--admin-nav-soft);
  text-align: center;
}

.notification-row,
.notification-footer {
  border: 1px solid var(--admin-nav-border);
  background: var(--admin-nav-surface);
  cursor: pointer;
  transition:
    background-color var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition);
}

.notification-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  border-radius: var(--kumo-radius-sm);
  text-align: left;
}

.notification-row:hover,
.notification-row.unread,
.notification-footer:hover {
  border-color: var(--admin-nav-border-strong);
  background: var(--admin-nav-hover);
}

.notification-title {
  color: var(--admin-nav-ink);
  font-weight: 650;
}

.notification-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-footer {
  padding: 8px 10px;
  border-radius: var(--kumo-radius-sm);
  color: var(--admin-nav-info);
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 650;
}

.user-dropdown {
  position: relative;
  cursor: pointer;
}

.user-info {
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  border-radius: var(--kumo-radius-md);
}

.user-avatar {
  border: 2px solid var(--admin-nav-border-strong);
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  color: var(--admin-nav-ink);
  font-size: 14px;
  font-weight: 650;
}

.user-role {
  font-size: 12px;
}

.dropdown-icon {
  margin-left: 5px;
  font-size: 12px;
  color: var(--admin-nav-soft);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 1000;
  width: 200px;
  overflow: hidden;
  border: 1px solid var(--admin-nav-border);
  border-radius: 12px;
  background: var(--admin-nav-surface);
  box-shadow: 0 1px 2px rgba(23, 23, 23, 0.05), 0 18px 44px rgba(23, 23, 23, 0.08);
  animation: dropdown-fade 0.2s ease;
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
  background: var(--admin-nav-hover);
  color: var(--admin-nav-ink);
  font-size: 14px;
  font-weight: 650;
}

.dropdown-item {
  gap: 12px;
  padding: 12px 15px;
  color: var(--admin-nav-ink);
  transition:
    background-color var(--kumo-transition),
    color var(--kumo-transition);
}

.dropdown-item:hover {
  color: var(--admin-nav-ink);
  background: var(--admin-nav-hover);
}

.dropdown-divider {
  height: 1px;
  margin: 5px 0;
  background: var(--admin-nav-border);
}

.bg-picker-title {
  margin-bottom: 10px;
  color: var(--admin-nav-ink);
  font-size: 13px;
  font-weight: 650;
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
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition:
    background-color var(--kumo-transition),
    border-color var(--kumo-transition);
}

.bg-swatch:hover,
.bg-swatch.active {
  background: var(--admin-nav-hover);
}

.bg-swatch.active {
  border-color: var(--admin-nav-ink);
}

.bg-swatch-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px var(--admin-nav-border);
}

.bg-swatch-label,
.bg-picker-tip {
  font-size: 11px;
}

.bg-picker-tip {
  margin-top: 10px;
}

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

  .toggle-sidebar,
  .nav-theme-toggle,
  .nav-notifications {
    width: 36px;
    height: 36px;
  }

  .greeting {
    font-size: 14px;
  }

  .notification-badge {
    font-size: 16px;
  }
}
</style>
