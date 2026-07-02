<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { notificationService } from './services/notificationService'
import { resolveAvatarFrom } from './utils/avatar'

const route = useRoute()
const router = useRouter()
const store = useStore()

const searchQuery = ref('')
const showUserMenu = ref(false)
const mobileMenuOpen = ref(false)
const unreadNotificationCount = ref(0)
const themeMode = ref(localStorage.getItem('themeMode') || 'light')

const navLinks = [
  { to: '/', label: '首页', icon: ['fas', 'home'], exact: true },
  { to: '/articles', label: '文章', icon: ['fas', 'file-alt'] },
  { to: '/questions', label: '问答', icon: ['fas', 'question-circle'] },
  { to: '/categories', label: '分类', icon: ['fas', 'th-large'] },
  { to: '/tags', label: '标签', icon: ['fas', 'tags'] }
]

const isAuthenticated = computed(() => store.state.isAuthenticated)
const user = computed(() => store.state.user)
const error = computed(() => store.state.error)
const userAvatarUrl = computed(() => resolveAvatarFrom(user.value))
const isAdminPage = computed(() => route.path.startsWith('/admin'))

const isAdmin = computed(() => {
  const role = user.value?.role
  return ['admin', 'ADMIN', 'SUPER_ADMIN'].includes(role)
})

const themeIcon = computed(() => themeMode.value === 'dark' ? ['fas', 'sun'] : ['fas', 'moon'])
const themeLabel = computed(() => themeMode.value === 'dark' ? '切换浅色模式' : '切换深色模式')

const setThemeMode = (mode) => {
  themeMode.value = mode
  document.documentElement.dataset.mode = mode
  localStorage.setItem('themeMode', mode)
}

const toggleThemeMode = () => {
  setThemeMode(themeMode.value === 'dark' ? 'light' : 'dark')
}

const initializeApp = async () => {
  try {
    await store.dispatch('checkAuth')
  } catch (err) {
    store.commit('SET_ERROR', err?.message || '应用初始化失败，请刷新页面重试')
  }
}

const loadUnreadNotificationCount = async () => {
  if (!isAuthenticated.value) {
    unreadNotificationCount.value = 0
    return
  }

  try {
    unreadNotificationCount.value = await notificationService.getUnreadCount()
  } catch (err) {
    unreadNotificationCount.value = 0
  }
}

const handleSearch = () => {
  const keyword = searchQuery.value.trim()
  if (!keyword) return

  mobileMenuOpen.value = false
  router.push({
    name: 'Home',
    query: { search: keyword }
  })
}

const toggleUserMenu = (event) => {
  event.stopPropagation()
  showUserMenu.value = !showUserMenu.value
}

const closeMenus = (event) => {
  if (!event.target.closest('.user-menu')) {
    showUserMenu.value = false
  }
  if (!event.target.closest('.site-nav') && !event.target.closest('.mobile-menu-button')) {
    mobileMenuOpen.value = false
  }
}

const navigateToUserProfile = (event) => {
  event.stopPropagation()

  const cachedUser = user.value || JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (!cachedUser?.id) {
    store.commit('SET_ERROR', '无法打开个人主页：当前用户信息缺失')
    return
  }

  showUserMenu.value = false
  mobileMenuOpen.value = false
  router.push(`/user/${cachedUser.id}`)
}

const getUserInitials = () => {
  if (!user.value?.username) return '用'
  return user.value.username.substring(0, 1).toUpperCase()
}

const logout = () => {
  showUserMenu.value = false
  mobileMenuOpen.value = false
  store.dispatch('logout')
  router.push('/login')
}

const clearError = () => {
  store.dispatch('clearError')
}

const goToAdmin = () => {
  showUserMenu.value = false
  router.push('/admin/dashboard')
}

onMounted(async () => {
  setThemeMode(themeMode.value)
  await initializeApp()
  await loadUnreadNotificationCount()

  document.addEventListener('click', closeMenus)
  window.addEventListener('notifications-updated', loadUnreadNotificationCount)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenus)
  window.removeEventListener('notifications-updated', loadUnreadNotificationCount)
})

watch(isAuthenticated, (authenticated) => {
  if (authenticated) {
    loadUnreadNotificationCount()
  } else {
    unreadNotificationCount.value = 0
  }
})

watch(() => route.fullPath, () => {
  showUserMenu.value = false
  mobileMenuOpen.value = false
})
</script>

<template>
  <div class="app-container kumo-page">
    <header v-if="!isAdminPage" class="site-header kumo-shell">
      <div class="header-container">
        <router-link to="/" class="brand-mark" aria-label="科技论坛首页">
          <span class="brand-icon">TF</span>
          <span class="brand-copy">
            <strong>科技论坛</strong>
            <small>Tech Forum</small>
          </span>
        </router-link>

        <form class="global-search" role="search" @submit.prevent="handleSearch">
          <font-awesome-icon :icon="['fas', 'search']" />
          <input
            v-model="searchQuery"
            type="search"
            placeholder="搜索文章、问题或标签"
            aria-label="搜索文章、问题或标签"
          >
          <button type="submit" aria-label="搜索">
            <font-awesome-icon :icon="['fas', 'arrow-right']" />
          </button>
        </form>

        <button
          class="mobile-menu-button kumo-button kumo-button--icon"
          type="button"
          :aria-expanded="mobileMenuOpen"
          aria-label="打开导航菜单"
          @click.stop="mobileMenuOpen = !mobileMenuOpen"
        >
          <font-awesome-icon :icon="['fas', 'bars']" />
        </button>

        <nav class="site-nav" :class="{ open: mobileMenuOpen }" aria-label="主导航">
          <router-link
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="nav-link"
            :class="{ active: link.exact ? route.path === link.to : route.path.startsWith(link.to) }"
          >
            <font-awesome-icon :icon="link.icon" />
            <span>{{ link.label }}</span>
          </router-link>
        </nav>

        <div class="header-actions">
          <button
            class="theme-toggle kumo-button kumo-button--icon"
            type="button"
            :aria-label="themeLabel"
            :title="themeLabel"
            @click="toggleThemeMode"
          >
            <font-awesome-icon :icon="themeIcon" />
          </button>

          <template v-if="!isAuthenticated">
            <router-link to="/login" class="kumo-button auth-link">
              <font-awesome-icon :icon="['fas', 'sign-in-alt']" />
              <span>登录</span>
            </router-link>
            <router-link to="/register" class="kumo-button kumo-button--brand auth-link">
              <font-awesome-icon :icon="['fas', 'user-plus']" />
              <span>注册</span>
            </router-link>
          </template>

          <template v-else>
            <router-link to="/article/new" class="kumo-button kumo-button--brand create-link">
              <font-awesome-icon :icon="['fas', 'edit']" />
              <span>发布</span>
            </router-link>

            <router-link to="/messages" class="notification-link kumo-button kumo-button--icon" aria-label="消息中心">
              <font-awesome-icon :icon="['fas', 'bell']" />
              <span v-if="unreadNotificationCount > 0" class="notification-badge">
                {{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}
              </span>
            </router-link>

            <div class="user-menu">
              <button
                class="user-trigger"
                type="button"
                :aria-expanded="showUserMenu"
                @click.stop="toggleUserMenu"
              >
                <span class="avatar" :class="{ 'admin-avatar': isAdmin }" @click.stop="navigateToUserProfile">
                  <img v-if="userAvatarUrl" :src="userAvatarUrl" :alt="user?.username || '用户'">
                  <span v-else>{{ getUserInitials() }}</span>
                  <span v-if="isAdmin" class="admin-badge" title="管理员">
                    <font-awesome-icon :icon="['fas', 'shield-alt']" />
                  </span>
                </span>
                <span class="user-name">
                  {{ user?.username || '用户' }}
                  <small v-if="isAdmin">Admin</small>
                </span>
                <font-awesome-icon :icon="['fas', showUserMenu ? 'chevron-up' : 'chevron-down']" />
              </button>

              <transition name="menu-pop">
                <div v-if="showUserMenu" class="user-menu-dropdown kumo-surface">
                  <router-link :to="'/user/' + (user?.id || '1')" class="menu-item">
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
                  <button v-if="isAdmin" type="button" class="menu-item admin-menu-item" @click="goToAdmin">
                    <font-awesome-icon :icon="['fas', 'shield-alt']" />
                    <span>管理后台</span>
                  </button>
                  <button type="button" class="menu-item" @click="logout">
                    <font-awesome-icon :icon="['fas', 'sign-out-alt']" />
                    <span>退出登录</span>
                  </button>
                </div>
              </transition>
            </div>
          </template>
        </div>
      </div>
    </header>

    <transition name="error-slide">
      <div v-if="error" class="global-error kumo-surface" role="alert">
        <font-awesome-icon :icon="['fas', 'exclamation-triangle']" />
        <span>{{ error }}</span>
        <button type="button" aria-label="关闭错误提示" @click="clearError">
          <font-awesome-icon :icon="['fas', 'times']" />
        </button>
      </div>
    </transition>

    <main class="main" :class="{ 'admin-main': isAdminPage }">
      <div v-if="!isAdminPage" class="main-container">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </div>
      <router-view v-else v-slot="{ Component }">
        <component :is="Component" :key="route.fullPath" />
      </router-view>
    </main>

    <aside v-if="isAdmin && !isAdminPage" class="admin-toolbar kumo-surface">
      <span>
        <font-awesome-icon :icon="['fas', 'shield-alt']" />
        管理员模式
      </span>
      <button type="button" class="kumo-button kumo-button--brand" @click="goToAdmin">
        <font-awesome-icon :icon="['fas', 'tachometer-alt']" />
        后台
      </button>
    </aside>

    <footer v-if="!isAdminPage" class="site-footer">
      <div class="footer-container">
        <div class="footer-brand">
          <span class="brand-icon">TF</span>
          <div>
            <strong>科技论坛</strong>
            <p>面向开发者的技术交流、问题协作和知识沉淀空间。</p>
          </div>
        </div>
        <div class="footer-links">
          <router-link to="/articles">文章</router-link>
          <router-link to="/questions">问答</router-link>
          <router-link to="/categories">分类</router-link>
          <router-link to="/tags">标签</router-link>
        </div>
        <div class="footer-social">
          <a href="#" aria-label="GitHub"><font-awesome-icon :icon="['fab', 'github']" /></a>
          <a href="#" aria-label="Twitter"><font-awesome-icon :icon="['fab', 'twitter']" /></a>
          <a href="#" aria-label="Weixin"><font-awesome-icon :icon="['fab', 'weixin']" /></a>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  border-width: 0 0 1px;
  border-radius: 0;
}

.header-container {
  display: grid;
  grid-template-columns: auto minmax(16rem, 28rem) 1fr auto;
  align-items: center;
  gap: 1rem;
  width: min(100% - 2rem, 1320px);
  min-height: 4.7rem;
  margin: 0 auto;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--kumo-text-default);
  text-decoration: none;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 12px;
  background:
    linear-gradient(180deg, var(--kumo-bg-elevated), var(--kumo-bg-brand-soft));
  color: var(--kumo-bg-brand-strong);
  font-weight: 900;
  box-shadow: var(--kumo-shadow-sm);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.brand-copy strong {
  font-size: 1rem;
  font-weight: 850;
}

.brand-copy small {
  color: var(--kumo-text-subtle);
  font-size: 0.74rem;
  font-weight: 760;
  text-transform: uppercase;
}

.global-search {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.6rem;
  min-height: 2.9rem;
  padding: 0 0.4rem 0 0.9rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-subtle);
  box-shadow: var(--kumo-shadow-sm);
}

.global-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--kumo-text-default);
  outline: none;
}

.global-search button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border: 0;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  cursor: pointer;
}

.site-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 2.5rem;
  padding: 0.5rem 0.78rem;
  border-radius: 999px;
  color: var(--kumo-text-muted);
  font-size: 0.92rem;
  font-weight: 760;
  text-decoration: none;
  transition: var(--transition);
}

.nav-link:hover,
.nav-link.active {
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.55rem;
}

.mobile-menu-button {
  display: none;
}

.auth-link,
.create-link {
  min-height: 2.55rem;
  padding-inline: 0.9rem;
}

.notification-link {
  position: relative;
}

.notification-badge {
  position: absolute;
  top: -0.25rem;
  right: -0.25rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.1rem;
  height: 1.1rem;
  padding: 0 0.25rem;
  border: 2px solid var(--kumo-bg-base);
  border-radius: 999px;
  background: var(--kumo-status-danger);
  color: var(--kumo-text-inverse);
  font-size: 0.64rem;
  font-weight: 850;
}

.user-menu {
  position: relative;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  min-height: 2.65rem;
  padding: 0.22rem 0.55rem 0.22rem 0.22rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  cursor: pointer;
  box-shadow: var(--kumo-shadow-sm);
}

.avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 850;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-avatar {
  box-shadow: 0 0 0 2px var(--kumo-bg-accent);
}

.admin-badge {
  position: absolute;
  right: -0.1rem;
  bottom: -0.1rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  border-radius: 999px;
  background: var(--kumo-bg-accent);
  color: var(--kumo-text-inverse);
  font-size: 0.55rem;
}

.user-name {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 7rem;
  overflow: hidden;
  color: var(--kumo-text-default);
  font-size: 0.86rem;
  font-weight: 790;
  line-height: 1.05;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name small {
  color: var(--kumo-bg-accent);
  font-size: 0.62rem;
  font-weight: 820;
  text-transform: uppercase;
}

.user-menu-dropdown {
  position: absolute;
  top: calc(100% + 0.6rem);
  right: 0;
  display: grid;
  gap: 0.25rem;
  min-width: 13rem;
  padding: 0.5rem;
  z-index: 120;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  min-height: 2.55rem;
  padding: 0.62rem 0.75rem;
  border: 0;
  border-radius: 0.8rem;
  background: transparent;
  color: var(--kumo-text-muted);
  font-weight: 720;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  transition: var(--transition);
}

.menu-item:hover {
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.admin-menu-item {
  color: var(--kumo-bg-accent);
}

.global-error {
  position: fixed;
  top: 5.5rem;
  left: 50%;
  z-index: 300;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: min(92vw, 42rem);
  padding: 0.8rem 1rem;
  color: var(--kumo-status-danger);
  transform: translateX(-50%);
}

.global-error button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  margin-left: auto;
  border: 0;
  border-radius: 999px;
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
  cursor: pointer;
}

.main {
  flex: 1;
  padding: clamp(1rem, 3vw, 2rem) 0 3rem;
}

.admin-main {
  min-height: 100vh;
  padding: 0;
}

.main-container,
.footer-container {
  width: min(100% - 2rem, 1320px);
  margin: 0 auto;
}

.admin-toolbar {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  z-index: 80;
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.6rem;
}

.admin-toolbar span {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding-left: 0.45rem;
  color: var(--kumo-text-muted);
  font-weight: 760;
}

.site-footer {
  margin-top: auto;
  border-top: 1px solid var(--kumo-hairline);
  background: color-mix(in srgb, var(--kumo-bg-base) 72%, transparent);
}

.footer-container {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 1.5rem;
  padding: 1.5rem 0;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.footer-brand strong {
  color: var(--kumo-text-default);
  font-weight: 850;
}

.footer-brand p {
  max-width: 36rem;
  margin: 0.15rem 0 0;
  color: var(--kumo-text-muted);
}

.footer-links,
.footer-social {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.footer-links a,
.footer-social a {
  color: var(--kumo-text-muted);
  font-weight: 740;
  text-decoration: none;
}

.footer-links a:hover,
.footer-social a:hover {
  color: var(--kumo-bg-brand-strong);
}

.page-fade-enter-active,
.page-fade-leave-active,
.menu-pop-enter-active,
.menu-pop-leave-active,
.error-slide-enter-active,
.error-slide-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.menu-pop-enter-from,
.menu-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

.error-slide-enter-from,
.error-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}

@media (max-width: 1180px) {
  .header-container {
    grid-template-columns: auto minmax(12rem, 1fr) auto auto;
  }

  .site-nav {
    position: absolute;
    top: calc(100% + 0.5rem);
    left: 1rem;
    right: 1rem;
    display: none;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    padding: 0.55rem;
    border: 1px solid var(--kumo-hairline);
    border-radius: var(--kumo-radius-lg);
    background: var(--kumo-bg-elevated);
    box-shadow: var(--kumo-shadow-md);
    backdrop-filter: var(--kumo-blur);
  }

  .site-nav.open {
    display: grid;
  }

  .nav-link {
    justify-content: center;
  }

  .mobile-menu-button {
    display: inline-flex;
  }
}

@media (max-width: 760px) {
  .header-container {
    grid-template-columns: auto auto 1fr auto;
    gap: 0.65rem;
    min-height: auto;
    padding: 0.75rem 0;
  }

  .brand-copy,
  .auth-link span,
  .create-link span,
  .user-name {
    display: none;
  }

  .global-search {
    order: 5;
    grid-column: 1 / -1;
  }

  .site-nav.open {
    grid-template-columns: 1fr;
  }

  .header-actions {
    gap: 0.35rem;
  }

  .footer-container {
    grid-template-columns: 1fr;
  }

  .footer-links,
  .footer-social {
    flex-wrap: wrap;
  }
}
</style>
