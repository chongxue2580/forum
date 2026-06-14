<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveArticle,
  approveCategory,
  approveQuestion,
  approveTag,
  getAllArticles,
  getAllCategories,
  getAllQuestions,
  getAllTags,
  getDashboardOverview,
  getDataTrends,
  getRecentActivities,
  getRecentUsers,
  getSystemStatus,
  getTodayStatistics,
  rejectArticle,
  rejectCategory,
  rejectQuestion,
  rejectTag
} from '@/api/admin'
import UserManagement from './UserManagement.vue'
import OperationLog from './OperationLog.vue'
import ArticleManagement from './ArticleManagement.vue'
import CommentManagement from './CommentManagement.vue'
import CategoryManagement from './CategoryManagement.vue'
import QuestionManagement from './QuestionManagement.vue'
import TagManagement from './TagManagement.vue'
import SystemSettings from './SystemSettings.vue'
import AdminNavBar from './AdminNavBar.vue'
import { sidebarVisible, toggleSidebar, initSidebar, setupResizeListener, removeResizeListener } from './AdminSidebar.js'

const router = useRouter()

const activeTab = ref('dashboard')

// 夜间模式
const isDark = ref(localStorage.getItem('adminTheme') === 'dark')
const toggleTheme = () => {
  isDark.value = !isDark.value
  localStorage.setItem('adminTheme', isDark.value ? 'dark' : 'light')
}

// 背景主题（用户可自选，记忆到本地）
const bgTheme = ref(localStorage.getItem('adminBgTheme') || 'mint')
const setBgTheme = (theme) => {
  bgTheme.value = theme
  localStorage.setItem('adminBgTheme', theme)
}
const stats = ref({
  users: {
    total: 0,
    active: 0,
    admin: 0,
    disabled: 0,
    newToday: 0
  },
  articles: {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    official: 0,
    pinned: 0,
    featured: 0
  },
  questions: {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    solved: 0
  },
  categories: {
    total: 0,
    pending: 0
  },
  tags: {
    total: 0,
    pending: 0
  },
  comments: {
    total: 0,
    article: 0,
    question: 0
  },
  logs: {
    today: 0,
    thisWeek: 0
  }
})

const todayStats = ref({
  newUsers: 0,
  newArticles: 0,
  newQuestions: 0,
  newComments: 0,
  activeUsers: 0
})

const systemStatus = ref({
  memory: {
    usagePercent: 0
  },
  application: {
    status: 'running',
    uptime: '-',
    startTime: '-',
    version: '-',
    environment: '-'
  }
})

const recentActivities = ref({
  activities: []
})

const dataTrends = ref({
  userRegistrations: [],
  articlePublications: [],
  questionAsks: []
})

const dashboardUsers = ref([])
const loading = ref(false)
const usersLoading = ref(false)
const pendingLoading = ref(false)
const pendingType = ref('articles')
const pendingItems = ref({
  articles: [],
  questions: [],
  categories: [],
  tags: []
})
const pendingTotals = ref({
  articles: 0,
  questions: 0,
  categories: 0,
  tags: 0
})

const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
  { label: '超级管理员', value: 'SUPER_ADMIN' }
]

const statusOptions = [
  { label: '正常', value: 'ACTIVE' },
  { label: '已禁用', value: 'DISABLED' }
]

const navigationItems = computed(() => [
  { key: 'dashboard', label: '仪表盘', icon: 'tachometer-alt' },
  { key: 'users', label: '用户管理', icon: 'users' },
  { key: 'articles', label: '文章管理', icon: 'file-alt', count: stats.value.articles.pending },
  { key: 'questions', label: '问答管理', icon: 'question-circle', count: stats.value.questions.pending },
  { key: 'comments', label: '评论管理', icon: 'comment' },
  { key: 'categories', label: '分类管理', icon: 'folder', count: stats.value.categories.pending },
  { key: 'tags', label: '标签管理', icon: 'tags', count: stats.value.tags.pending },
  { key: 'logs', label: '操作日志', icon: 'clipboard-list' },
  { key: 'settings', label: '系统设置', icon: 'cog' }
])

const adminUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('adminUser') || localStorage.getItem('userInfo') || '{}')
  } catch (error) {
    return {}
  }
})

const adminName = computed(() => adminUser.value.nickname || adminUser.value.name || adminUser.value.username || '管理员')
const currentDate = computed(() => new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}))

const pendingTabs = computed(() => [
  { key: 'articles', label: '文章', count: pendingTotals.value.articles || stats.value.articles.pending || 0 },
  { key: 'questions', label: '问答', count: pendingTotals.value.questions || stats.value.questions.pending || 0 },
  { key: 'categories', label: '分类', count: pendingTotals.value.categories || stats.value.categories.pending || 0 },
  { key: 'tags', label: '标签', count: pendingTotals.value.tags || stats.value.tags.pending || 0 }
])

const currentPendingItems = computed(() => pendingItems.value[pendingType.value] || [])

const unwrapData = (response) => {
  if (response && typeof response === 'object' && Object.prototype.hasOwnProperty.call(response, 'success')) {
    return response.data
  }
  return response?.data || response
}

const unwrapPage = (response) => {
  const page = unwrapData(response) || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || page.total || 0
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0)
    : new Date(value)

  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getAuthorName = (author) => author?.nickname || author?.username || author?.name || '未知用户'

const normalizePendingItem = (type, item) => {
  const fieldMap = {
    articles: {
      title: item.title,
      owner: getAuthorName(item.author),
      time: item.createdAt || item.createTime || item.publishedAt
    },
    questions: {
      title: item.title,
      owner: getAuthorName(item.author),
      time: item.createdAt || item.createTime
    },
    categories: {
      title: item.name,
      owner: item.createdBy?.name || '系统',
      time: item.createdAt || item.createTime
    },
    tags: {
      title: item.name,
      owner: item.createdBy?.name || '系统',
      time: item.createdAt || item.createTime
    }
  }

  const mapped = fieldMap[type]
  return {
    id: item.id,
    type,
    title: mapped.title || '未命名',
    author: mapped.owner,
    time: formatDateTime(mapped.time)
  }
}

const normalizeUser = (user) => {
  const role = String(user.role || 'USER').toUpperCase()
  const status = String(user.status || 'ACTIVE').toUpperCase()

  return {
    ...user,
    name: user.nickname || user.username || `用户${user.id}`,
    role,
    status,
    roleLabel: roleOptions.find(option => option.value === role)?.label || role,
    statusLabel: statusOptions.find(option => option.value === status)?.label || status,
    email: user.email || '未设置邮箱',
    createdAtText: formatDateTime(user.createdAt),
    lastLoginText: user.lastLoginTime ? formatDateTime(user.lastLoginTime) : '从未登录'
  }
}

const getAvatarColor = (name) => {
  const colors = [
    '#6c8ff5', '#68c3a3', '#f1a766', '#a991f7',
    '#ee86b7', '#ef8c8c', '#7ac7d7', '#96b86d'
  ]
  const safeName = name || '用户'
  const index = safeName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
  return colors[index]
}

const getAvatarText = (name) => (name || '用').charAt(0)

const getTrendValues = (field, fallback) => {
  const values = dataTrends.value?.[field]
  if (Array.isArray(values) && values.length > 0) {
    return values.map(point => Number(point.count || 0))
  }
  return fallback
}

const createChartCoords = (values, width = 120, height = 42, padding = 5) => {
  const safeValues = values.length ? values.map(value => Number(value) || 0) : [0]
  const max = Math.max(...safeValues, 1)
  const min = Math.min(...safeValues, 0)
  const range = Math.max(max - min, 1)
  const step = safeValues.length > 1 ? (width - padding * 2) / (safeValues.length - 1) : 0

  return safeValues.map((value, index) => ({
    x: padding + index * step,
    y: height - padding - ((value - min) / range) * (height - padding * 2)
  }))
}

const sparklinePoints = (values, width = 120, height = 42) => {
  return createChartCoords(values, width, height).map(point => `${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' ')
}

const lineAreaPath = (values, width = 360, height = 160, padding = 12) => {
  const coords = createChartCoords(values, width, height, padding)
  if (!coords.length) return ''
  const line = coords.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(' ')
  return `${line} L ${coords[coords.length - 1].x.toFixed(1)} ${height - padding} L ${coords[0].x.toFixed(1)} ${height - padding} Z`
}

const mainTrendValues = computed(() => getTrendValues('userRegistrations', [3, 7, 5, 11, 9, 14, 18]))
const articleTrendValues = computed(() => getTrendValues('articlePublications', [8, 6, 12, 10, 16, 15, 21]))
const questionTrendValues = computed(() => getTrendValues('questionAsks', [5, 8, 7, 13, 10, 12, 16]))

const kpiCards = computed(() => [
  {
    key: 'users',
    label: '用户总数',
    value: stats.value.users.total,
    detail: `今日新增 ${todayStats.value.newUsers}`,
    icon: 'users',
    tone: 'blue',
    trend: mainTrendValues.value
  },
  {
    key: 'articles',
    label: '文章总数',
    value: stats.value.articles.total,
    detail: `${stats.value.articles.pending} 篇待审核`,
    icon: 'file-alt',
    tone: 'mint',
    trend: articleTrendValues.value
  },
  {
    key: 'questions',
    label: '问答总数',
    value: stats.value.questions.total,
    detail: `${stats.value.questions.solved} 个已解决`,
    icon: 'question-circle',
    tone: 'lavender',
    trend: questionTrendValues.value
  },
  {
    key: 'comments',
    label: '评论总数',
    value: stats.value.comments.total,
    detail: `今日新增 ${todayStats.value.newComments}`,
    icon: 'comment',
    tone: 'pink',
    trend: [4, 6, 5, 9, 8, 12, Math.max(todayStats.value.newComments, 10)]
  }
])

const trendAxisLabels = computed(() => {
  const points = dataTrends.value?.userRegistrations || []
  if (points.length > 0) {
    return points.map(point => point.date?.slice(5) || point.date).slice(-7)
  }
  return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
})

const approvalDonut = computed(() => {
  const approved = Number(stats.value.articles.approved || 0) + Number(stats.value.questions.approved || 0)
  const pending = Number(stats.value.articles.pending || 0) + Number(stats.value.questions.pending || 0)
  const rejected = Number(stats.value.articles.rejected || 0) + Number(stats.value.questions.rejected || 0)
  const total = Math.max(approved + pending + rejected, 1)
  const approvedPct = Math.round((approved / total) * 100)
  const pendingPct = Math.round((pending / total) * 100)

  return {
    approved,
    pending,
    rejected,
    approvedPct,
    pendingPct,
    style: {
      background: `conic-gradient(#68c3a3 0 ${approvedPct}%, #f3c46d ${approvedPct}% ${approvedPct + pendingPct}%, #ee8a9a ${approvedPct + pendingPct}% 100%)`
    }
  }
})

const memoryUsage = computed(() => Math.min(Number(systemStatus.value?.memory?.usagePercent || 0), 100))

const loadDashboardData = async () => {
  loading.value = true
  try {
    const [overviewRes, todayRes, statusRes, activitiesRes, trendsRes] = await Promise.all([
      getDashboardOverview(),
      getTodayStatistics(),
      getSystemStatus(),
      getRecentActivities(),
      getDataTrends()
    ])

    const data = unwrapData(overviewRes)
    if (data) {
      if (data.users) {
        stats.value.users.total = data.users.totalUsers || 0
        stats.value.users.active = data.users.activeUsers || 0
        stats.value.users.admin = data.users.adminUsers || 0
        stats.value.users.disabled = data.users.disabledUsers || 0
      }
      if (data.articles) {
        stats.value.articles.total = data.articles.totalArticles || 0
        stats.value.articles.pending = data.articles.pendingArticles || 0
        stats.value.articles.approved = data.articles.approvedArticles || 0
        stats.value.articles.rejected = data.articles.rejectedArticles || 0
        stats.value.articles.pinned = data.articles.pinnedArticles || 0
        stats.value.articles.featured = data.articles.featuredArticles || 0
      }
      if (data.questions) {
        stats.value.questions.total = data.questions.totalQuestions || 0
        stats.value.questions.pending = data.questions.pendingQuestions || 0
        stats.value.questions.approved = data.questions.approvedQuestions || 0
        stats.value.questions.rejected = data.questions.rejectedQuestions || 0
        stats.value.questions.solved = data.questions.solvedQuestions || 0
      }
      if (data.comments) {
        stats.value.comments.total = data.comments.totalComments || 0
        stats.value.comments.article = data.comments.articleComments || 0
        stats.value.comments.question = data.comments.questionComments || 0
      }
    }

    const today = unwrapData(todayRes) || todayStats.value
    todayStats.value = today
    stats.value.users.newToday = today.newUsers || 0
    stats.value.logs.today = today.newComments || 0
    systemStatus.value = unwrapData(statusRes) || systemStatus.value
    recentActivities.value = unwrapData(activitiesRes) || recentActivities.value
    dataTrends.value = unwrapData(trendsRes) || dataTrends.value
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadRecentUserRows = async () => {
  usersLoading.value = true
  try {
    const page = unwrapPage(await getRecentUsers({ page: 1, pageSize: 5 }))
    dashboardUsers.value = page.content.map(normalizeUser)
  } catch (error) {
    console.error('加载最近用户失败:', error)
    dashboardUsers.value = []
  } finally {
    usersLoading.value = false
  }
}

const loadPendingItems = async () => {
  pendingLoading.value = true
  const loaders = {
    articles: () => getAllArticles({ page: 1, pageSize: 5, status: 'PENDING' }),
    questions: () => getAllQuestions({ page: 1, pageSize: 5, status: 'PENDING' }),
    categories: () => getAllCategories({ page: 1, pageSize: 5, status: 'PENDING' }),
    tags: () => getAllTags({ page: 1, pageSize: 5, status: 'PENDING' })
  }

  try {
    const entries = await Promise.all(Object.entries(loaders).map(async ([type, loader]) => {
      try {
        const page = unwrapPage(await loader())
        return [type, page]
      } catch (error) {
        console.error(`加载待审核${type}失败:`, error)
        return [type, { content: [], totalElements: 0 }]
      }
    }))

    entries.forEach(([type, page]) => {
      pendingItems.value[type] = page.content.map(item => normalizePendingItem(type, item))
      pendingTotals.value[type] = page.totalElements
    })

    stats.value.articles.pending = pendingTotals.value.articles
    stats.value.questions.pending = pendingTotals.value.questions
    stats.value.categories.pending = pendingTotals.value.categories
    stats.value.tags.pending = pendingTotals.value.tags
  } finally {
    pendingLoading.value = false
  }
}

const approvePendingItem = async (item) => {
  const actionMap = {
    articles: approveArticle,
    questions: approveQuestion,
    categories: approveCategory,
    tags: approveTag
  }

  try {
    await actionMap[item.type](item.id)
    ElMessage.success('已通过审核')
    await Promise.all([loadDashboardData(), loadPendingItems()])
  } catch (error) {
    ElMessage.error(error.message || '审核通过失败')
  }
}

const rejectPendingItem = async (item) => {
  const actionMap = {
    articles: rejectArticle,
    questions: rejectQuestion,
    categories: rejectCategory,
    tags: rejectTag
  }

  try {
    let reason
    if (item.type === 'articles' || item.type === 'questions') {
      const result = await ElMessageBox.prompt(`请输入拒绝「${item.title}」的原因`, '拒绝审核', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea'
      })
      reason = result.value
    } else {
      await ElMessageBox.confirm(`确定要拒绝「${item.title}」吗？`, '拒绝审核', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    }

    await actionMap[item.type](item.id, reason)
    ElMessage.success('已拒绝审核')
    await Promise.all([loadDashboardData(), loadPendingItems()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核拒绝失败')
    }
  }
}

const openPendingManagement = (type) => {
  activeTab.value = type
}

const openUserManagement = () => {
  activeTab.value = 'users'
}

const viewUserProfile = (user) => {
  router.push(`/user/${user.id}`)
}

const sendMessage = (user) => {
  router.push({
    path: '/messages',
    query: { userId: user.id }
  })
}

const getActivityIcon = (type) => {
  const iconMap = {
    USER_REGISTER: 'user-plus',
    ARTICLE_PUBLISH: 'file-alt',
    QUESTION_ASK: 'question-circle',
    COMMENT_POST: 'comment'
  }
  return iconMap[type] || 'info-circle'
}

const getActivityTypeClass = (type) => {
  const classMap = {
    USER_REGISTER: 'success',
    ARTICLE_PUBLISH: 'primary',
    QUESTION_ASK: 'warning',
    COMMENT_POST: 'info'
  }
  return classMap[type] || 'default'
}

const getActivityTypeText = (type) => {
  const textMap = {
    USER_REGISTER: '用户注册',
    ARTICLE_PUBLISH: '文章发布',
    QUESTION_ASK: '问答提问',
    COMMENT_POST: '评论发布'
  }
  return textMap[type] || '系统活动'
}

onMounted(() => {
  initSidebar()
  setupResizeListener()
  loadDashboardData()
  loadRecentUserRows()
  loadPendingItems()
})

onBeforeUnmount(() => {
  removeResizeListener()
})
</script>

<template>
  <div class="admin-dashboard-container" :class="[{ dark: isDark }, 'bg-' + bgTheme]">
    <div class="glass-bg-blobs" aria-hidden="true">
      <span></span><span></span><span></span>
    </div>
    <AdminNavBar :is-dark="isDark" :bg-theme="bgTheme" @toggle-sidebar="toggleSidebar" @toggle-theme="toggleTheme" @set-bg="setBgTheme" />

    <div class="admin-dashboard-shell">
      <aside class="admin-sidebar" :class="{ show: sidebarVisible }">
        <div class="sidebar-menu">
          <div class="sidebar-header">
            <div class="admin-logo">
              <span class="logo-mark">
                <font-awesome-icon icon="chart-line" />
              </span>
              <div>
                <span class="logo-title">FroumX</span>
                <span class="logo-subtitle">Admin Console</span>
              </div>
            </div>
          </div>

          <el-menu
            :default-active="activeTab"
            class="menu-list"
            @select="activeTab = $event"
          >
            <el-menu-item
              v-for="item in navigationItems"
              :key="item.key"
              :index="item.key"
              class="menu-item"
            >
              <font-awesome-icon :icon="item.icon" class="menu-icon" />
              <span class="menu-text">{{ item.label }}</span>
              <span v-if="item.count" class="menu-badge">{{ item.count }}</span>
            </el-menu-item>
          </el-menu>

          <div class="sidebar-profile">
            <div
              class="sidebar-avatar"
              :style="{ backgroundColor: getAvatarColor(adminName) }"
            >
              {{ getAvatarText(adminName) }}
            </div>
            <div class="sidebar-profile-meta">
              <span>{{ adminName }}</span>
              <small>超级管理员</small>
            </div>
          </div>
        </div>
      </aside>

      <main class="admin-content" :class="{ 'full-width': !sidebarVisible }">
        <div v-if="activeTab === 'dashboard'" class="dashboard-content">
          <section class="dashboard-grid">
            <div class="dashboard-center">
              <header class="dashboard-hero">
                <div>
                  <span class="eyebrow">运营概览</span>
                  <h1>管理员仪表盘</h1>
                  <p>{{ currentDate }}，{{ adminName }}，关键数据已为你整理好。</p>
                </div>
                <div class="hero-status">
                  <span class="status-dot"></span>
                  <div>
                    <strong>{{ systemStatus.application?.status || 'running' }}</strong>
                    <small>系统运行 {{ systemStatus.application?.uptime || '-' }}</small>
                  </div>
                </div>
              </header>

              <div class="stats-cards" v-loading="loading">
                <article
                  v-for="card in kpiCards"
                  :key="card.key"
                  class="stat-card"
                  :class="card.tone"
                >
                  <div class="stat-card-top">
                    <span class="stat-icon">
                      <font-awesome-icon :icon="card.icon" />
                    </span>
                    <span class="stat-chip">+{{ Math.max(card.trend[card.trend.length - 1] || 0, 0) }}</span>
                  </div>
                  <div class="stat-value">{{ card.value }}</div>
                  <div class="stat-label">{{ card.label }}</div>
                  <div class="stat-detail">{{ card.detail }}</div>
                  <svg class="sparkline" viewBox="0 0 120 42" preserveAspectRatio="none" aria-hidden="true">
                    <polyline :points="sparklinePoints(card.trend)" />
                  </svg>
                </article>
              </div>

              <section class="dashboard-card user-table-card">
                <div class="card-header">
                  <div>
                    <span class="eyebrow">User Management</span>
                    <h2>最新用户</h2>
                  </div>
                  <button class="ghost-command" @click="openUserManagement">
                    <font-awesome-icon icon="users" />
                    全部用户
                  </button>
                </div>

                <div class="admin-user-table" v-loading="usersLoading">
                  <div class="table-head">
                    <span>用户</span>
                    <span>角色</span>
                    <span>状态</span>
                    <span>最近登录</span>
                    <span>操作</span>
                  </div>
                  <div
                    v-for="user in dashboardUsers"
                    :key="user.id"
                    class="table-row"
                  >
                    <div class="user-cell">
                      <div
                        class="user-avatar"
                        :style="{ backgroundColor: getAvatarColor(user.name) }"
                      >
                        {{ getAvatarText(user.name) }}
                      </div>
                      <div>
                        <strong>{{ user.name }}</strong>
                        <small>{{ user.email }}</small>
                      </div>
                    </div>
                    <span :class="['pill-badge', user.role === 'USER' ? 'role-user' : 'role-admin']">
                      {{ user.roleLabel }}
                    </span>
                    <span :class="['pill-badge', user.status === 'ACTIVE' ? 'status-active' : 'status-disabled']">
                      {{ user.statusLabel }}
                    </span>
                    <span class="muted-text">{{ user.lastLoginText }}</span>
                    <div class="icon-actions">
                      <el-tooltip content="发送消息" placement="top">
                        <button class="line-icon-btn" @click="sendMessage(user)">
                          <font-awesome-icon icon="comment-dots" />
                        </button>
                      </el-tooltip>
                      <el-tooltip content="查看资料" placement="top">
                        <button class="line-icon-btn" @click="viewUserProfile(user)">
                          <font-awesome-icon icon="edit" />
                        </button>
                      </el-tooltip>
                      <el-tooltip content="更多操作" placement="top">
                        <button class="line-icon-btn" @click="openUserManagement">
                          <font-awesome-icon icon="ellipsis-h" />
                        </button>
                      </el-tooltip>
                    </div>
                  </div>
                  <el-empty
                    v-if="!usersLoading && dashboardUsers.length === 0"
                    description="暂无用户数据"
                    :image-size="72"
                  />
                </div>
              </section>

              <section class="dashboard-card pending-card">
                <div class="card-header">
                  <div>
                    <span class="eyebrow">Review Queue</span>
                    <h2>待处理事项</h2>
                  </div>
                  <button class="ghost-command" :disabled="pendingLoading" @click="loadPendingItems">
                    <font-awesome-icon icon="sync" />
                    刷新
                  </button>
                </div>

                <div class="tab-header">
                  <button
                    v-for="tab in pendingTabs"
                    :key="tab.key"
                    class="tab-item"
                    :class="{ active: pendingType === tab.key }"
                    @click="pendingType = tab.key"
                  >
                    {{ tab.label }}
                    <span>{{ tab.count }}</span>
                  </button>
                </div>

                <div class="pending-list" v-loading="pendingLoading">
                  <el-empty
                    v-if="!pendingLoading && currentPendingItems.length === 0"
                    description="暂无待处理事项"
                    :image-size="72"
                  />
                  <div
                    v-for="item in currentPendingItems"
                    :key="`${item.type}-${item.id}`"
                    class="pending-item"
                  >
                    <div class="item-info">
                      <div class="item-title">{{ item.title }}</div>
                      <div class="item-meta">
                        <span><font-awesome-icon icon="user" /> {{ item.author }}</span>
                        <span><font-awesome-icon icon="clock" /> {{ item.time }}</span>
                      </div>
                    </div>
                    <div class="item-actions">
                      <button class="line-action" @click="openPendingManagement(item.type)">
                        <font-awesome-icon icon="list" />
                        管理
                      </button>
                      <button class="line-action success" @click="approvePendingItem(item)">
                        <font-awesome-icon icon="check" />
                        通过
                      </button>
                      <button class="line-action danger" @click="rejectPendingItem(item)">
                        <font-awesome-icon icon="times" />
                        拒绝
                      </button>
                    </div>
                  </div>
                </div>
              </section>
            </div>

            <aside class="dashboard-insights">
              <section class="dashboard-card chart-card">
                <div class="card-header compact">
                  <div>
                    <span class="eyebrow">Data Trend</span>
                    <h2>增长趋势</h2>
                  </div>
                </div>
                <svg class="line-chart" viewBox="0 0 360 160" preserveAspectRatio="none" aria-hidden="true">
                  <path :d="lineAreaPath(mainTrendValues, 360, 160, 14)" class="chart-area" />
                  <polyline :points="sparklinePoints(mainTrendValues, 360, 160)" class="chart-line users-line" />
                  <polyline :points="sparklinePoints(articleTrendValues, 360, 160)" class="chart-line articles-line" />
                  <polyline :points="sparklinePoints(questionTrendValues, 360, 160)" class="chart-line questions-line" />
                </svg>
                <div class="axis-labels">
                  <span v-for="label in trendAxisLabels" :key="label">{{ label }}</span>
                </div>
                <div class="chart-legend">
                  <span><i class="legend-dot users-dot"></i>用户</span>
                  <span><i class="legend-dot articles-dot"></i>文章</span>
                  <span><i class="legend-dot questions-dot"></i>问答</span>
                </div>
              </section>

              <section class="dashboard-card donut-card">
                <div class="card-header compact">
                  <div>
                    <span class="eyebrow">Audit Status</span>
                    <h2>审核分布</h2>
                  </div>
                </div>
                <div class="donut-layout">
                  <div class="donut-chart" :style="approvalDonut.style">
                    <div class="donut-hole">
                      <strong>{{ approvalDonut.approvedPct }}%</strong>
                      <small>通过</small>
                    </div>
                  </div>
                  <div class="donut-stats">
                    <span><i class="legend-dot approved-dot"></i>已通过 {{ approvalDonut.approved }}</span>
                    <span><i class="legend-dot pending-dot"></i>待审核 {{ approvalDonut.pending }}</span>
                    <span><i class="legend-dot rejected-dot"></i>已拒绝 {{ approvalDonut.rejected }}</span>
                  </div>
                </div>
              </section>

              <section class="dashboard-card system-card">
                <div class="card-header compact">
                  <div>
                    <span class="eyebrow">System</span>
                    <h2>系统状态</h2>
                  </div>
                </div>
                <div class="system-meter">
                  <div class="meter-label">
                    <span>内存使用</span>
                    <strong>{{ memoryUsage }}%</strong>
                  </div>
                  <div class="meter-track">
                    <span :style="{ width: `${memoryUsage}%` }"></span>
                  </div>
                </div>
                <div class="system-mini-grid runtime">
                  <div>
                    <span>运行环境</span>
                    <strong>{{ systemStatus.application?.environment || '-' }}</strong>
                  </div>
                  <div>
                    <span>应用版本</span>
                    <strong>{{ systemStatus.application?.version || '-' }}</strong>
                  </div>
                  <div>
                    <span>启动时间</span>
                    <strong>{{ systemStatus.application?.startTime || '-' }}</strong>
                  </div>
                  <div>
                    <span>运行时长</span>
                    <strong>{{ systemStatus.application?.uptime || '-' }}</strong>
                  </div>
                </div>
                <div class="system-mini-grid">
                  <div>
                    <span>新增用户</span>
                    <strong>{{ todayStats.newUsers }}</strong>
                  </div>
                  <div>
                    <span>活跃用户</span>
                    <strong>{{ todayStats.activeUsers }}</strong>
                  </div>
                  <div>
                    <span>新增文章</span>
                    <strong>{{ todayStats.newArticles }}</strong>
                  </div>
                  <div>
                    <span>新增问答</span>
                    <strong>{{ todayStats.newQuestions }}</strong>
                  </div>
                </div>
              </section>

              <section class="dashboard-card activity-card">
                <div class="card-header compact">
                  <div>
                    <span class="eyebrow">Activity</span>
                    <h2>最近活动</h2>
                  </div>
                  <button class="line-icon-btn" @click="activeTab = 'logs'">
                    <font-awesome-icon icon="list" />
                  </button>
                </div>
                <div class="activity-feed">
                  <div
                    v-for="(activity, index) in recentActivities.activities"
                    :key="`${activity.type}-${activity.time}-${index}`"
                    class="feed-item"
                  >
                    <span class="feed-icon" :class="getActivityTypeClass(activity.type)">
                      <font-awesome-icon :icon="getActivityIcon(activity.type)" />
                    </span>
                    <div>
                      <div class="feed-title">
                        <strong>{{ getActivityTypeText(activity.type) }}</strong>
                        <span>{{ formatDateTime(activity.time) }}</span>
                      </div>
                      <p>
                        {{ activity.user }} {{ activity.description }}
                        <span v-if="activity.title">：{{ activity.title }}</span>
                      </p>
                    </div>
                  </div>
                  <el-empty
                    v-if="recentActivities.activities.length === 0"
                    description="暂无活动"
                    :image-size="72"
                  />
                </div>
              </section>
            </aside>
          </section>
        </div>

        <UserManagement v-else-if="activeTab === 'users'" />
        <ArticleManagement v-else-if="activeTab === 'articles'" />
        <QuestionManagement v-else-if="activeTab === 'questions'" />
        <CommentManagement v-else-if="activeTab === 'comments'" />
        <CategoryManagement v-else-if="activeTab === 'categories'" />
        <TagManagement v-else-if="activeTab === 'tags'" />
        <OperationLog v-else-if="activeTab === 'logs'" />
        <SystemSettings v-else-if="activeTab === 'settings'" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard-container {
  min-height: 100vh;
  color: #1d1d1f;
  background: linear-gradient(135deg, #f6f8fb 0%, #eef1f5 100%);
  background-attachment: fixed;
}

/* 背景主题预设（浅色，可在顶栏自选；深色模式不受影响） */
.admin-dashboard-container:not(.dark).bg-mint {
  background:
    radial-gradient(circle at 12% 10%, rgba(168, 222, 214, 0.30) 0%, transparent 46%),
    radial-gradient(circle at 88% 84%, rgba(190, 227, 222, 0.22) 0%, transparent 50%),
    linear-gradient(135deg, #f5f9f8 0%, #eef3f2 100%);
  background-attachment: fixed;
}

.admin-dashboard-container:not(.dark).bg-ice {
  background:
    radial-gradient(circle at 10% 12%, rgba(150, 190, 255, 0.30) 0%, transparent 44%),
    radial-gradient(circle at 90% 84%, rgba(150, 222, 232, 0.24) 0%, transparent 48%),
    linear-gradient(135deg, #f5f8fd 0%, #eef2f8 100%);
  background-attachment: fixed;
}

.admin-dashboard-container:not(.dark).bg-gold {
  background:
    radial-gradient(circle at 12% 10%, rgba(255, 224, 175, 0.32) 0%, transparent 46%),
    radial-gradient(circle at 88% 86%, rgba(255, 209, 196, 0.22) 0%, transparent 50%),
    linear-gradient(135deg, #fbf8f3 0%, #f4efe7 100%);
  background-attachment: fixed;
}

.admin-dashboard-container:not(.dark).bg-violet {
  background:
    radial-gradient(circle at 12% 10%, rgba(199, 194, 255, 0.30) 0%, transparent 46%),
    radial-gradient(circle at 88% 86%, rgba(226, 200, 245, 0.22) 0%, transparent 50%),
    linear-gradient(135deg, #f7f6fd 0%, #eff0f8 100%);
  background-attachment: fixed;
}

.admin-dashboard-container:not(.dark).bg-gray {
  background:
    radial-gradient(circle at 14% 12%, rgba(205, 212, 224, 0.34) 0%, transparent 48%),
    linear-gradient(135deg, #f6f7f9 0%, #eef0f3 100%);
  background-attachment: fixed;
}

.admin-dashboard-shell {
  display: flex;
  min-height: calc(100vh - 70px);
  margin-top: 70px;
}

.admin-sidebar {
  width: 260px;
  height: calc(100vh - 70px);
  position: fixed;
  top: 70px;
  left: 0;
  z-index: 100;
  overflow: hidden;
  padding: 18px 14px;
  transition: transform 0.28s ease;
}

.sidebar-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-top-color: rgba(255, 255, 255, 0.95);
  border-left-color: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.22);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.01), 0 20px 50px rgba(100, 120, 160, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(60px) saturate(230%);
  -webkit-backdrop-filter: blur(60px) saturate(230%);
}

.sidebar-header {
  padding: 20px 18px 12px;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 15px;
  color: #007aff;
  background: #edf3ff;
  box-shadow: inset 0 0 0 1px rgba(65, 105, 216, 0.12);
}

.logo-title,
.logo-subtitle {
  display: block;
  line-height: 1.2;
}

.logo-title {
  font-size: 1rem;
  font-weight: 750;
  color: #1f2937;
}

.logo-subtitle {
  margin-top: 2px;
  font-size: 0.72rem;
  color: #8190a3;
}

.menu-list {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 6px 10px;
}

.menu-list :deep(.el-menu-item) {
  height: 46px;
  margin: 4px 0;
  border-radius: 14px;
  color: #667085;
  line-height: 46px;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.menu-list :deep(.el-menu-item:hover) {
  color: #007aff;
  background: #f2f6ff;
}

.menu-list :deep(.el-menu-item.is-active) {
  color: #0066d6;
  background: linear-gradient(135deg, #edf4ff 0%, #f7fbff 100%);
  box-shadow: inset 0 0 0 1px rgba(81, 116, 207, 0.14);
}

.menu-icon {
  width: 18px;
  margin-right: 12px;
}

.menu-text {
  font-weight: 650;
}

.menu-badge {
  margin-left: auto;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #d14c5c;
  background: #ffe9ee;
  font-size: 0.72rem;
  font-weight: 750;
}

.sidebar-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px;
  padding: 12px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f7fbff 0%, #ffffff 100%);
  border: 1px solid rgba(127, 149, 176, 0.14);
}

.sidebar-avatar,
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #ffffff;
  font-weight: 750;
}

.sidebar-avatar {
  width: 42px;
  height: 42px;
  border-radius: 15px;
}

.sidebar-profile-meta span,
.sidebar-profile-meta small {
  display: block;
}

.sidebar-profile-meta span {
  font-weight: 750;
  color: #1f2937;
}

.sidebar-profile-meta small {
  color: #8190a3;
  font-size: 0.72rem;
}

.admin-content {
  flex: 1;
  min-width: 0;
  margin-left: 260px;
  padding: 24px;
  transition: margin-left 0.28s ease;
}

.admin-content.full-width {
  margin-left: 0;
}

.dashboard-content {
  width: 100%;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.34fr);
  gap: 22px;
  align-items: start;
}

.dashboard-center,
.dashboard-insights {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dashboard-hero,
.dashboard-card,
.stat-card {
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-top-color: rgba(255, 255, 255, 0.95);
  border-left-color: rgba(255, 255, 255, 0.9);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.22);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.01), 0 20px 50px rgba(100, 120, 160, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(60px) saturate(230%);
  -webkit-backdrop-filter: blur(60px) saturate(230%);
}

.dashboard-hero {
  min-height: 132px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 28px;
  overflow: hidden;
  position: relative;
}

.dashboard-hero::after {
  content: '';
  position: absolute;
  inset: auto -80px -130px auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(132, 169, 246, 0.22), transparent 68%);
  pointer-events: none;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  margin-bottom: 8px;
  color: #77869a;
  font-size: 0.72rem;
  font-weight: 750;
  letter-spacing: 0;
  text-transform: uppercase;
}

.dashboard-hero h1,
.card-header h2 {
  margin: 0;
  color: #1f2937;
  letter-spacing: 0;
}

.dashboard-hero h1 {
  font-size: 2rem;
  font-weight: 800;
}

.dashboard-hero p {
  margin: 8px 0 0;
  color: #667085;
}

.hero-status {
  min-width: 176px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7fbff;
  border: 1px solid rgba(127, 149, 176, 0.12);
}

.hero-status strong,
.hero-status small {
  display: block;
}

.hero-status strong {
  color: #263246;
  font-weight: 750;
  text-transform: capitalize;
}

.hero-status small {
  color: #8190a3;
  font-size: 0.75rem;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #68c3a3;
  box-shadow: 0 0 0 6px rgba(104, 195, 163, 0.16);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  position: relative;
  min-height: 178px;
  padding: 18px;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.58;
  pointer-events: none;
}

.stat-card.blue::before {
  background: linear-gradient(135deg, #e9f1fe 0%, rgba(255, 255, 255, 0) 62%);
}

.stat-card.mint::before {
  background: linear-gradient(135deg, #e1f6ee 0%, rgba(255, 255, 255, 0) 62%);
}

.stat-card.lavender::before {
  background: linear-gradient(135deg, #efeafd 0%, rgba(255, 255, 255, 0) 62%);
}

.stat-card.pink::before {
  background: linear-gradient(135deg, #fde9f1 0%, rgba(255, 255, 255, 0) 62%);
}

.stat-card > * {
  position: relative;
}

.stat-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 15px;
}

.blue .stat-icon {
  color: #007aff;
  background: #e9f1fe;
}

.mint .stat-icon {
  color: #2f9c77;
  background: #e1f6ee;
}

.lavender .stat-icon {
  color: #7c5fd3;
  background: #efeafd;
}

.pink .stat-icon {
  color: #d65d92;
  background: #fde9f1;
}

.stat-chip {
  padding: 4px 9px;
  border-radius: 999px;
  color: #378b70;
  background: #eaf8f3;
  font-size: 0.72rem;
  font-weight: 750;
}

.stat-value {
  color: #172033;
  font-size: 2rem;
  font-weight: 820;
  line-height: 1;
}

.stat-label {
  margin-top: 8px;
  color: #1f2937;
  font-weight: 750;
}

.stat-detail {
  margin-top: 4px;
  color: #8190a3;
  font-size: 0.82rem;
}

.sparkline {
  position: absolute;
  right: 16px;
  bottom: 14px;
  width: 44%;
  height: 42px;
}

.sparkline polyline {
  fill: none;
  stroke: #68c3a3;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.blue .sparkline polyline {
  stroke: #6c8ff5;
}

.mint .sparkline polyline {
  stroke: #5fc7a3;
}

.lavender .sparkline polyline {
  stroke: #a991f7;
}

.pink .sparkline polyline {
  stroke: #ee86b7;
}

.dashboard-card {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.card-header.compact {
  margin-bottom: 14px;
}

.card-header h2 {
  font-size: 1.08rem;
  font-weight: 800;
}

.ghost-command,
.line-action,
.line-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(105, 126, 154, 0.2);
  color: #506176;
  background: #ffffff;
  font: inherit;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.ghost-command {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
}

.ghost-command:hover,
.line-action:hover,
.line-icon-btn:hover {
  color: #007aff;
  border-color: rgba(65, 105, 216, 0.28);
  background: #f7fbff;
  box-shadow: 0 8px 22px rgba(82, 111, 154, 0.1);
}

.ghost-command:disabled {
  cursor: not-allowed;
  opacity: 0.64;
}

.admin-user-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.5fr) 110px 100px minmax(120px, 0.8fr) 132px;
  gap: 14px;
  align-items: center;
}

.table-head {
  padding: 0 14px 8px;
  color: #8a98aa;
  font-size: 0.76rem;
  font-weight: 750;
}

.table-row {
  min-height: 66px;
  padding: 10px 14px;
  border: 1px solid rgba(127, 149, 176, 0.12);
  border-radius: 18px;
  background: #fbfdff;
}

.user-cell {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 15px;
}

.user-cell strong,
.user-cell small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell strong {
  color: #263246;
  font-weight: 800;
}

.user-cell small,
.muted-text {
  color: #8190a3;
  font-size: 0.8rem;
}

.pill-badge {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 750;
  white-space: nowrap;
}

.role-user {
  color: #007aff;
  background: #edf3ff;
}

.role-admin {
  color: #9a5a12;
  background: #fff1d7;
}

.status-active {
  color: #378b70;
  background: #eaf8f3;
}

.status-disabled {
  color: #c34d62;
  background: #ffeaf0;
}

.icon-actions {
  display: flex;
  gap: 8px;
}

.line-icon-btn {
  width: 34px;
  height: 34px;
  padding: 0;
  border-radius: 12px;
}

.tab-header {
  display: flex;
  gap: 8px;
  padding: 5px;
  margin-bottom: 14px;
  overflow-x: auto;
  border-radius: 999px;
  background: #f4f7fb;
}

.tab-item {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 12px;
  border: none;
  border-radius: 999px;
  color: #667085;
  background: transparent;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 750;
  white-space: nowrap;
  cursor: pointer;
}

.tab-item span {
  min-width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  font-size: 0.72rem;
}

.tab-item.active {
  color: #007aff;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(82, 111, 154, 0.1);
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(127, 149, 176, 0.12);
  border-radius: 18px;
  background: #fbfdff;
}

.item-info {
  min-width: 0;
}

.item-title {
  color: #263246;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  color: #8190a3;
  font-size: 0.8rem;
}

.item-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.line-action {
  min-height: 34px;
  padding: 0 11px;
  border-radius: 999px;
}

.line-action.success {
  color: #378b70;
  border-color: rgba(104, 195, 163, 0.32);
  background: #f3fbf8;
}

.line-action.danger {
  color: #c34d62;
  border-color: rgba(238, 138, 154, 0.34);
  background: #fff5f7;
}

.chart-card,
.donut-card,
.system-card,
.activity-card {
  overflow: hidden;
}

.line-chart {
  width: 100%;
  height: 168px;
  display: block;
}

.chart-area {
  fill: rgba(104, 195, 163, 0.14);
}

.chart-line {
  fill: none;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.users-line {
  stroke: #6c8ff5;
}

.articles-line {
  stroke: #68c3a3;
}

.questions-line {
  stroke: #f1a766;
}

.axis-labels,
.chart-legend,
.donut-stats {
  display: flex;
  align-items: center;
}

.axis-labels {
  justify-content: space-between;
  color: #9aa6b6;
  font-size: 0.72rem;
}

.chart-legend,
.donut-stats {
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 14px;
  color: #667085;
  font-size: 0.8rem;
  font-weight: 700;
}

.chart-legend span,
.donut-stats span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.legend-dot {
  width: 9px;
  height: 9px;
  display: inline-block;
  border-radius: 999px;
}

.users-dot {
  background: #6c8ff5;
}

.articles-dot,
.approved-dot {
  background: #68c3a3;
}

.questions-dot,
.pending-dot {
  background: #f1a766;
}

.rejected-dot {
  background: #ee8a9a;
}

.donut-layout {
  display: flex;
  align-items: center;
  gap: 20px;
}

.donut-chart {
  width: 142px;
  aspect-ratio: 1;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(127, 149, 176, 0.08);
}

.donut-hole {
  width: 86px;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(74, 96, 128, 0.12);
}

.donut-hole strong,
.donut-hole small {
  display: block;
}

.donut-hole strong {
  color: #1f2937;
  font-size: 1.4rem;
  font-weight: 850;
}

.donut-hole small {
  color: #8190a3;
  font-size: 0.74rem;
}

.donut-stats {
  flex-direction: column;
  align-items: flex-start;
  margin-top: 0;
}

.system-meter {
  margin-bottom: 16px;
}

.meter-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #667085;
  font-size: 0.84rem;
  font-weight: 750;
}

.meter-label strong {
  color: #1f2937;
  font-weight: 850;
}

.meter-track {
  height: 10px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef3f8;
}

.meter-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6c8ff5 0%, #68c3a3 100%);
}

.system-mini-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.system-mini-grid.runtime {
  margin-bottom: 10px;
}

.system-mini-grid div {
  min-height: 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 12px;
  border-radius: 16px;
  background: #f7fbff;
  border: 1px solid rgba(127, 149, 176, 0.1);
}

.system-mini-grid span {
  color: #8190a3;
  font-size: 0.78rem;
  font-weight: 700;
}

.system-mini-grid strong {
  color: #1f2937;
  font-size: 1.25rem;
  font-weight: 850;
}

.system-mini-grid.runtime strong {
  font-size: 0.92rem;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.activity-feed {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 436px;
  overflow: auto;
  padding-right: 2px;
}

.feed-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  position: relative;
}

.feed-item:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 42px;
  left: 18px;
  bottom: -12px;
  width: 1px;
  background: #e7edf4;
}

.feed-icon {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #007aff;
  background: #edf3ff;
  position: relative;
  z-index: 1;
}

.feed-icon.success {
  color: #378b70;
  background: #eaf8f3;
}

.feed-icon.warning {
  color: #b56d1d;
  background: #fff1d7;
}

.feed-icon.info {
  color: #2b8ba0;
  background: #e7f7fb;
}

.feed-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #1f2937;
}

.feed-title strong {
  font-size: 0.88rem;
  font-weight: 800;
}

.feed-title span {
  flex: 0 0 auto;
  color: #9aa6b6;
  font-size: 0.72rem;
}

.feed-item p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 0.82rem;
  line-height: 1.5;
}

@media (max-width: 1280px) {
  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .dashboard-insights {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .admin-sidebar {
    width: 82px;
    padding-inline: 10px;
  }

  .admin-content {
    margin-left: 82px;
    padding: 18px;
  }

  .logo-title,
  .logo-subtitle,
  .menu-text,
  .sidebar-profile-meta {
    display: none;
  }

  .admin-logo,
  .sidebar-profile {
    justify-content: center;
  }

  .menu-list :deep(.el-menu-item) {
    justify-content: center;
    padding: 0;
  }

  .menu-icon {
    margin-right: 0;
  }

  .menu-badge {
    position: absolute;
    top: 6px;
    right: 6px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    font-size: 0.62rem;
  }

  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: minmax(220px, 1fr) repeat(2, auto);
  }

  .table-row .muted-text {
    display: none;
  }
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: min(320px, calc(100vw - 28px));
    transform: translateX(-108%);
  }

  .admin-sidebar.show {
    transform: translateX(0);
  }

  .admin-content,
  .admin-content.full-width {
    margin-left: 0;
    padding: 14px;
  }

  .logo-title,
  .logo-subtitle,
  .menu-text,
  .sidebar-profile-meta {
    display: block;
  }

  .admin-logo,
  .sidebar-profile {
    justify-content: flex-start;
  }

  .menu-list :deep(.el-menu-item) {
    justify-content: flex-start;
    padding: 0 20px;
  }

  .menu-icon {
    margin-right: 12px;
  }

  .dashboard-hero,
  .pending-item,
  .donut-layout {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-cards,
  .dashboard-insights,
  .system-mini-grid {
    grid-template-columns: 1fr;
  }

  .table-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .pill-badge {
    width: max-content;
  }

  .item-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 520px) {
  .dashboard-hero,
  .dashboard-card,
  .stat-card {
    border-radius: 18px;
  }

  .dashboard-hero {
    padding: 20px;
  }

  .dashboard-hero h1 {
    font-size: 1.55rem;
  }

  .stats-cards {
    gap: 12px;
  }

  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

/* ============================================================
   🌙 夜间模式（仪表盘自身硬编码表面的深色覆盖）
   ============================================================ */
.admin-dashboard-container.dark {
  color: #ffffff;
  background:
    radial-gradient(circle at 20% 20%, rgba(165, 216, 255, 0.18), transparent 40%),
    radial-gradient(circle at 80% 30%, rgba(255, 179, 209, 0.16), transparent 45%),
    radial-gradient(circle at 40% 80%, rgba(181, 245, 236, 0.14), transparent 40%),
    #0b0f1a;
}

/* 漂浮光斑（仅夜间显示，营造 iOS 26 液态玻璃景深） */
.glass-bg-blobs {
  display: none;
}

.admin-dashboard-container.dark .glass-bg-blobs {
  display: block;
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.admin-dashboard-container.dark .glass-bg-blobs span {
  position: absolute;
  width: 460px;
  height: 460px;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  animation: adBlobFloat 12s ease-in-out infinite;
}

.admin-dashboard-container.dark .glass-bg-blobs span:nth-child(1) {
  background: #4dabf7;
  top: -120px;
  left: -80px;
}

.admin-dashboard-container.dark .glass-bg-blobs span:nth-child(2) {
  background: #f783ac;
  bottom: -140px;
  right: -90px;
  animation-delay: -4s;
}

.admin-dashboard-container.dark .glass-bg-blobs span:nth-child(3) {
  background: #63e6be;
  top: 38%;
  left: 55%;
  animation-delay: -8s;
}

@keyframes adBlobFloat {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-30px) scale(1.12); }
}

/* 内容浮于光斑之上 */
.admin-dashboard-container.dark .admin-dashboard-shell {
  position: relative;
  z-index: 1;
}

.dark .dashboard-hero,
.dark .dashboard-card,
.dark .stat-card,
.dark .sidebar-menu {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.18);
  border-top-color: rgba(255, 255, 255, 0.35);
  border-left-color: rgba(255, 255, 255, 0.24);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

/* 主要文字转亮 */
.dark .dashboard-hero h1,
.dark .card-header h2,
.dark .stat-value,
.dark .stat-label,
.dark .item-title,
.dark .feed-title strong,
.dark .donut-hole strong,
.dark .meter-label strong,
.dark .system-mini-grid strong,
.dark .hero-status strong,
.dark .logo-title,
.dark .user-cell strong {
  color: #f5f5f7;
}

/* 次级文字 */
.dark .dashboard-hero p,
.dark .eyebrow,
.dark .stat-detail,
.dark .item-meta,
.dark .feed-item p,
.dark .muted-text,
.dark .user-cell small,
.dark .logo-subtitle,
.dark .sidebar-profile-meta small,
.dark .axis-labels,
.dark .chart-legend,
.dark .donut-stats,
.dark .system-mini-grid span,
.dark .meter-label,
.dark .hero-status small {
  color: #aab2c0;
}

/* 内嵌的浅色小块（hero 状态、迷你格、行卡、药丸 tab）转深色玻璃 */
.dark .hero-status,
.dark .system-mini-grid div,
.dark .table-row,
.dark .pending-item,
.dark .sidebar-profile,
.dark .tab-header {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

.dark .donut-hole {
  background: #1b222c;
}

.dark .tab-item.active {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.dark .meter-track {
  background: rgba(255, 255, 255, 0.1);
}

/* 侧边栏菜单项 */
.dark .menu-list :deep(.el-menu-item) {
  color: #aab2c0;
}

.dark .menu-list :deep(.el-menu-item:hover) {
  color: #3a9bff;
  background: rgba(255, 255, 255, 0.06);
}

.dark .menu-list :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(10, 132, 255, 0.22);
  box-shadow: inset 0 0 0 1px rgba(10, 132, 255, 0.3);
}

/* 线性按钮/图标按钮在深色下 */
.dark .ghost-command,
.dark .line-action,
.dark .line-icon-btn {
  color: #c7ccd6;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
}

.dark .ghost-command:hover,
.dark .line-action:hover,
.dark .line-icon-btn:hover {
  color: #3a9bff;
  background: rgba(255, 255, 255, 0.1);
}

</style>
