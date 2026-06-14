<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { notificationService } from './services/notificationService'
import { resolveAvatarFrom } from './utils/avatar'

const route = useRoute()
const router = useRouter()
const store = useStore()

const searchQuery = ref('')
const showUserMenu = ref(false)
const unreadNotificationCount = ref(0)

// 从Vuex获取状态
const isAuthenticated = computed(() => store.state.isAuthenticated)
const user = computed(() => store.state.user)
const error = computed(() => store.state.error)

// 判断是否为管理员
const isAdmin = computed(() => {
  const role = user.value?.role
  return role === 'admin' || role === 'ADMIN' || role === 'SUPER_ADMIN'
})

// 顶栏头像（统一解析，兼容 avatarUrl/avatar 字段与 /uploads 路径）
const userAvatarUrl = computed(() => resolveAvatarFrom(user.value))

// 判断是否在管理端页面
const isAdminPage = computed(() => route.path.startsWith('/admin'))

onMounted(async () => {
  // 初始化应用状态
  await initializeApp()
  await loadUnreadNotificationCount()

  // 监听点击事件，关闭用户菜单
  document.addEventListener('click', closeUserMenu)
  // 消息中心读取消息后刷新顶栏未读红点
  window.addEventListener('notifications-updated', loadUnreadNotificationCount)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeUserMenu)
  window.removeEventListener('notifications-updated', loadUnreadNotificationCount)
})

watch(isAuthenticated, (authenticated) => {
  if (authenticated) {
    loadUnreadNotificationCount()
  } else {
    unreadNotificationCount.value = 0
  }
})

// 初始化应用
const initializeApp = async () => {
  try {
    // 检查并恢复用户登录状态
    await store.dispatch('checkAuth')
  } catch (error) {
    console.error('应用初始化失败:', error)
  }
}

const loadUnreadNotificationCount = async () => {
  if (!isAuthenticated.value) {
    unreadNotificationCount.value = 0
    return
  }

  try {
    unreadNotificationCount.value = await notificationService.getUnreadCount()
  } catch (error) {
    unreadNotificationCount.value = 0
  }
}

const handleSearch = () => {
  if (!searchQuery.value.trim()) return
  router.push({
    name: 'Home',
    query: { search: searchQuery.value.trim() }
  })
}

const toggleUserMenu = (e) => {
  e.stopPropagation()
  showUserMenu.value = !showUserMenu.value
}

const navigateToUserProfile = (e) => {
  e.stopPropagation()

  if (user.value?.id) {
    // 关闭用户菜单
    showUserMenu.value = false

    // 确保ID是字符串类型并且有效
    const userId = String(user.value.id);

    // 使用Vue Router进行单页面应用路由跳转
    router.push(`/user/${userId}`).catch(err => {
      console.error('Navigation error:', err);
      // 如果路由跳转失败，回退到直接URL跳转
      window.location.href = `/user/${userId}`;
    });
  } else {
    console.error('无法导航到个人中心：用户ID不存在')
    // 尝试从localStorage恢复用户信息
    const userInfoJson = localStorage.getItem('userInfo')
    if (userInfoJson) {
      try {
        const userInfo = JSON.parse(userInfoJson)
        if (userInfo.id) {
          router.push(`/user/${userInfo.id}`)
        }
      } catch (e) {
        console.error('解析localStorage用户信息失败:', e)
      }
    }
  }
}

const closeUserMenu = (e) => {
  // 检查点击事件是否在用户菜单外部
  if (!e.target.closest('.user-menu')) {
    showUserMenu.value = false
  }
}

const getUserInitials = () => {
  if (!user.value?.username) return '用';
  return user.value.username.substring(0, 1).toUpperCase();
}

const logout = () => {
  store.dispatch('logout')
  router.push('/login')
}

const clearError = () => {
  store.dispatch('clearError')
}

// 导航到管理后台
const goToAdmin = () => {
  router.push('/admin/dashboard')
}
</script>

<template>
  <div class="app-container">
    <!-- 顶部导航栏 - 只在非管理端页面显示 -->
    <header class="header" v-if="!isAdminPage">
      <div class="header-container">
        <nav class="nav">
          <router-link to="/" class="logo">
            <div class="logo-icon">TF</div>
            <span class="logo-text">科技论坛</span>
          </router-link>
          
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchQuery" 
              placeholder="搜索文章、问题或标签..." 
              @keyup.enter="handleSearch"
            />
            <button class="search-btn" @click="handleSearch" aria-label="搜索">
              <font-awesome-icon :icon="['fas', 'search']" />
            </button>
          </div>
          
          <div class="nav-links">
            <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">
              <font-awesome-icon :icon="['fas', 'home']" />
              <span>首页</span>
            </router-link>
            <router-link to="/categories" class="nav-link" :class="{ active: route.path === '/categories' }">
              <font-awesome-icon :icon="['fas', 'th-large']" />
              <span>分类</span>
            </router-link>
            <router-link to="/tags" class="nav-link" :class="{ active: route.path === '/tags' }">
              <font-awesome-icon :icon="['fas', 'tags']" />
              <span>标签</span>
            </router-link>
            <router-link to="/questions" class="nav-link" :class="{ active: route.path === '/questions' }">
              <font-awesome-icon :icon="['fas', 'question-circle']" />
              <span>问答</span>
            </router-link>
            
            <template v-if="!isAuthenticated">
              <router-link to="/login" class="nav-link login-link">
                <font-awesome-icon :icon="['fas', 'sign-in-alt']" />
                <span>登录</span>
              </router-link>
              <router-link to="/register" class="btn-register">
                <font-awesome-icon :icon="['fas', 'user-plus']" />
                <span>注册</span>
              </router-link>
            </template>
            <template v-else>
              <router-link to="/article/new" class="btn-post">
                <font-awesome-icon :icon="['fas', 'edit']" />
                <span>发布文章</span>
              </router-link>
              
              <router-link to="/messages" class="notification-icon">
                <div class="icon-wrapper">
                  <font-awesome-icon :icon="['fas', 'bell']" />
                  <span v-if="unreadNotificationCount > 0" class="notification-badge">
                    {{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}
                  </span>
                </div>
              </router-link>
              
              <div class="user-menu">
                <div class="avatar" @click="navigateToUserProfile" :class="{ 'admin-avatar': isAdmin }">
                  <img v-if="userAvatarUrl" :src="userAvatarUrl" :alt="user?.username || '用户'" />
                  <span v-else>{{ getUserInitials() }}</span>
                  <div v-if="isAdmin" class="admin-badge" title="管理员">
                    <font-awesome-icon :icon="['fas', 'shield-alt']" />
                  </div>
                </div>
                <span class="username" @click="navigateToUserProfile">
                  {{ user?.username || '用户' }}
                  <span v-if="isAdmin" class="admin-tag">管理员</span>
                </span>
                <div class="dropdown-trigger" @click.stop="toggleUserMenu">
                  <font-awesome-icon :icon="['fas', showUserMenu ? 'chevron-up' : 'chevron-down']" class="dropdown-icon" />
                </div>
                
                <div class="user-menu-dropdown" v-show="showUserMenu">
                  <router-link
                    :to="'/user/' + (user?.id || '1')"
                    class="menu-item"
                    @click="showUserMenu = false"
                  >
                    <font-awesome-icon :icon="['fas', 'user']" />
                    <span>个人主页</span>
                  </router-link>
                  <router-link to="/following" class="menu-item">
                    <font-awesome-icon :icon="['fas', 'heart']" />
                    <span>我的关注</span>
                  </router-link>
                  <router-link to="/settings" class="menu-item">
                    <font-awesome-icon :icon="['fas', 'cog']" />
                    <span>设置</span>
                  </router-link>
                  
                  <!-- 管理员专属选项 -->
                  <template v-if="isAdmin">
                    <div class="menu-divider"></div>
                    <a href="#" class="menu-item admin-menu-item" @click.prevent="goToAdmin">
                      <font-awesome-icon :icon="['fas', 'shield-alt']" />
                      <span>管理后台</span>
                    </a>
                  </template>
                  
                  <div class="menu-divider"></div>
                  <a href="#" class="menu-item" @click.prevent="logout">
                    <font-awesome-icon :icon="['fas', 'sign-out-alt']" />
                    <span>退出</span>
                  </a>
                </div>
              </div>
            </template>
          </div>
        </nav>
      </div>
    </header>

    <!-- 全局错误提示 -->
    <div class="global-error" v-if="error">
      <div class="error-content">
        <font-awesome-icon :icon="['fas', 'exclamation-triangle']" class="error-icon" />
        <span>{{ error }}</span>
        <button @click="clearError" class="close-btn" aria-label="关闭">
          <font-awesome-icon :icon="['fas', 'times']" />
        </button>
      </div>
    </div>

    <!-- 主要内容区 -->
    <main class="main" :class="{ 'admin-main': isAdminPage }">
      <div class="container" v-if="!isAdminPage">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="$route.fullPath" />
        </router-view>
      </div>
      <!-- 管理端页面不使用container限制 -->
      <router-view v-else v-slot="{ Component }">
        <component :is="Component" :key="$route.fullPath" />
      </router-view>
    </main>

    <!-- 管理员快捷工具栏 -->
    <div class="admin-toolbar" v-if="isAdmin && !route.path.includes('/admin')">
      <div class="admin-toolbar-content">
        <div class="admin-toolbar-title">
          <font-awesome-icon :icon="['fas', 'shield-alt']" />
          <span>管理员工具</span>
        </div>
        <div class="admin-toolbar-actions">
          <button class="admin-action-btn" @click="goToAdmin">
            <font-awesome-icon :icon="['fas', 'tachometer-alt']" />
            <span>管理后台</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 页脚 - 只在非管理端页面显示 -->
    <footer class="footer" v-if="!isAdminPage">
      <div class="container">
        <div class="footer-content">
          <div class="footer-section">
            <h3>关于我们</h3>
            <p>科技论坛是一个面向开发者的技术社区，致力于分享前沿技术和最佳实践。</p>
            <div class="social-links">
              <a href="#" class="social-link" aria-label="GitHub">
                <font-awesome-icon :icon="['fab', 'github']" />
              </a>
              <a href="#" class="social-link" aria-label="Twitter">
                <font-awesome-icon :icon="['fab', 'twitter']" />
              </a>
              <a href="#" class="social-link" aria-label="微信">
                <font-awesome-icon :icon="['fab', 'weixin']" />
              </a>
            </div>
          </div>
          <div class="footer-section">
            <h3>快速链接</h3>
            <ul class="footer-links">
              <li><router-link to="/">首页</router-link></li>
              <li><router-link to="/categories">分类</router-link></li>
              <li><router-link to="/tags">标签</router-link></li>
              <li><router-link to="/questions">问答</router-link></li>
            </ul>
          </div>
          <div class="footer-section">
            <h3>联系我们</h3>
            <p><font-awesome-icon :icon="['fas', 'envelope']" /> contact@techforum.com</p>
            <p><font-awesome-icon :icon="['fas', 'phone']" /> +86 123 4567 8901</p>
            <p><font-awesome-icon :icon="['fas', 'map-marker-alt']" /> 北京市海淀区中关村</p>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; {{ new Date().getFullYear() }} 科技论坛. 保留所有权利.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  /* 主题色 - 更现代的蓝色调 */
  --primary-color: #2563eb;
  --primary-light: #dbeafe;
  --primary-dark: #1e40af;
  --accent-color: #06b6d4;
  
  /* 成功/警告/错误颜色 */
  --success-color: #10b981;
  --success-rgb: 16, 185, 129;
  --warning-color: #f59e0b;
  --error-color: #ef4444;
  --error-rgb: 239, 68, 68;
  
  /* 文本颜色 */
  --text-color: #1f2937;
  --text-light: #4b5563;
  --text-lighter: #9ca3af;
  
  /* 背景颜色 */
  --bg-color: #f9fafb;
  --bg-gray: #f3f4f6;
  --bg-white: #ffffff;
  
  /* 边框颜色 */
  --border-color: #e5e7eb;
  
  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  
  /* 圆角 */
  --radius-sm: 4px;
  --radius: 8px;
  --radius-lg: 12px;
  
  /* 过渡效果 */
  --transition: all 0.3s ease;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: var(--bg-color);
  color: var(--text-color);
  line-height: 1.6;
  overflow-x: hidden;
}

a {
  text-decoration: none;
  color: inherit;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
}

/* 顶部导航栏 */
.header {
  background-color: var(--bg-white);
  box-shadow: var(--shadow);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid var(--border-color);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  width: 100%;
}

.nav {
  display: flex;
  align-items: center;
  height: 64px;
  flex-wrap: nowrap;
}

.logo {
  display: flex;
  align-items: center;
  margin-right: 2rem;
  font-weight: 600;
  color: var(--primary-color);
  text-decoration: none;
  white-space: nowrap;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: white;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 0.5rem;
  font-weight: 700;
  font-size: 16px;
  box-shadow: var(--shadow-sm);
}

.logo-text {
  font-size: 18px;
}

.search-box {
  flex: 1;
  max-width: 460px;
  position: relative;
  margin-right: 1.5rem;
}

.search-box input {
  width: 100%;
  padding: 0.6rem 1rem;
  padding-right: 2.5rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 14px;
  transition: var(--transition);
  background-color: var(--bg-gray);
}

.search-box input:focus {
  border-color: var(--primary-color);
  background-color: var(--bg-white);
  outline: none;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
}

.search-btn {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-lighter);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  transition: var(--transition);
}

.search-btn:hover {
  color: var(--primary-color);
  background-color: var(--primary-light);
}

.nav-links {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 0.5rem;
  overflow-x: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.nav-links::-webkit-scrollbar {
  display: none;
}

.nav-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0.5rem 0.75rem;
  color: var(--text-light);
  font-size: 14px;
  border-radius: var(--radius);
  transition: var(--transition);
  white-space: nowrap;
}

.nav-link svg {
  font-size: 16px;
}

.nav-link.active,
.nav-link:hover {
  color: var(--primary-color);
  background-color: var(--primary-light);
}

.login-link {
  border: 1px solid transparent;
}

.login-link:hover {
  border-color: var(--primary-color);
}

.btn-register, .btn-post {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: white;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition);
  white-space: nowrap;
}

.btn-register:hover, .btn-post:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  filter: brightness(1.1);
}

.notification-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  transition: var(--transition);
}

.notification-icon:hover {
  background-color: var(--bg-gray);
}

.icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-light);
}

.notification-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background-color: var(--error-color);
  color: white;
  font-size: 10px;
  font-weight: 600;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  margin-left: 1rem;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: var(--primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--primary-color);
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.username {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  cursor: pointer;
  border-radius: 50%;
  transition: var(--transition);
}

.dropdown-trigger:hover {
  background-color: var(--primary-light);
  color: var(--primary-color);
}

.dropdown-icon {
  font-size: 0.8rem;
  opacity: 0.8;
}

.user-menu-dropdown {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  width: 180px;
  z-index: 100;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  color: var(--text-color);
  transition: var(--transition);
  text-decoration: none;
  cursor: pointer;
  position: relative;
  z-index: 20;
}

.menu-item:hover {
  background-color: var(--bg-gray);
  color: var(--primary-color);
}

.menu-item svg {
  color: var(--text-light);
  width: 16px;
}

.menu-divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 0.5rem 0;
}

/* 全局错误提示 */
.global-error {
  position: fixed;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  width: auto;
  max-width: 90%;
}

.error-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background-color: rgba(var(--error-rgb), 0.05);
  border: 1px solid var(--error-color);
  border-radius: var(--radius);
  color: var(--error-color);
  box-shadow: var(--shadow);
}

.error-icon {
  font-size: 1.2rem;
}

.close-btn {
  background: none;
  border: none;
  color: var(--text-lighter);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  transition: var(--transition);
  margin-left: 0.5rem;
}

.close-btn:hover {
  background-color: rgba(var(--error-rgb), 0.1);
  color: var(--error-color);
}

/* 主要内容区 */
.main {
  flex: 1;
  padding: 2rem 0;
}

/* 管理端主要内容区 */
.admin-main {
  padding: 0;
  width: 100%;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  width: 100%;
}

/* 页脚 */
.footer {
  background-color: var(--bg-white);
  border-top: 1px solid var(--border-color);
  padding: 3rem 0 1.5rem;
  margin-top: 2rem;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
  margin-bottom: 2rem;
}

.footer-section h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--text-color);
  position: relative;
  padding-bottom: 0.5rem;
}

.footer-section h3::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 2px;
  background-color: var(--primary-color);
}

.footer-section p {
  color: var(--text-light);
  line-height: 1.6;
  margin-bottom: 1rem;
}

.social-links {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: var(--bg-gray);
  color: var(--text-light);
  transition: var(--transition);
}

.social-link:hover {
  background-color: var(--primary-light);
  color: var(--primary-color);
  transform: translateY(-2px);
}

.footer-links {
  list-style: none;
  padding: 0;
}

.footer-links li {
  margin-bottom: 0.75rem;
}

.footer-links a {
  color: var(--text-light);
  transition: var(--transition);
  display: inline-block;
}

.footer-links a:hover {
  color: var(--primary-color);
  transform: translateX(5px);
}

.footer-bottom {
  border-top: 1px solid var(--border-color);
  padding-top: 1.5rem;
  text-align: center;
  color: var(--text-lighter);
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .search-box {
    max-width: 300px;
  }
}

@media (max-width: 768px) {
  .nav {
    flex-wrap: wrap;
    height: auto;
    padding: 1rem 0;
  }
  
  .logo {
    margin-right: 1rem;
  }
  
  .search-box {
    order: 3;
    width: 100%;
    max-width: none;
    margin: 1rem 0;
  }
  
  .nav-links {
    flex-wrap: nowrap;
    overflow-x: auto;
    width: 100%;
    padding-bottom: 0.5rem;
    margin-top: 0.5rem;
    justify-content: flex-start;
  }

  .user-menu {
    margin-left: auto;
  }
  
  .footer-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .nav-link span {
    font-size: 12px;
  }
  
  .username {
    max-width: 60px;
  }
  
  .btn-register, .btn-post {
    padding: 0.4rem 0.75rem;
    font-size: 12px;
  }
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 管理员相关样式 */
.admin-avatar {
  position: relative;
  border: 2px solid #5e72e4 !important;
}

.admin-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background-color: #5e72e4;
  color: white;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  border: 2px solid white;
}

.admin-tag {
  display: inline-block;
  background-color: #5e72e4;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 5px;
  vertical-align: middle;
  font-weight: 600;
}

.admin-menu-item {
  color: #5e72e4 !important;
  font-weight: 600;
}

.admin-menu-item:hover {
  background-color: rgba(94, 114, 228, 0.1) !important;
}

.admin-toolbar {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  animation: slide-up 0.3s ease;
}

@keyframes slide-up {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.admin-toolbar-content {
  background: linear-gradient(135deg, #5e72e4 0%, #825ee4 100%);
  border-radius: 12px;
  padding: 12px 15px;
  box-shadow: 0 5px 15px rgba(94, 114, 228, 0.3);
  color: white;
  display: flex;
  align-items: center;
  gap: 15px;
}

.admin-toolbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.admin-toolbar-actions {
  display: flex;
  gap: 10px;
}

.admin-action-btn {
  background-color: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background-color 0.3s ease;
}

.admin-action-btn:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

@media (max-width: 768px) {
  .admin-toolbar {
    bottom: 10px;
    right: 10px;
  }
  
  .admin-toolbar-title span {
    display: none;
  }
}
</style>
