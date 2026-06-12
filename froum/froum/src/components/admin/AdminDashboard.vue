<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
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
  getRecentActivities,
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

const activeTab = ref('dashboard')
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
  }
})

const recentActivities = ref({
  activities: []
})

const loading = ref(false)
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

// 加载仪表板数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    // 并行加载所有数据
    const [overviewRes, todayRes, statusRes, activitiesRes] = await Promise.all([
      getDashboardOverview(),
      getTodayStatistics(),
      getSystemStatus(),
      getRecentActivities()
    ])

    const data = unwrapData(overviewRes)
    if (data) {
      // 更新统计数据
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
    }

    todayStats.value = unwrapData(todayRes) || todayStats.value
    systemStatus.value = unwrapData(statusRes) || systemStatus.value
    recentActivities.value = unwrapData(activitiesRes) || recentActivities.value
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
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

const typeMap = {
  user_status: { text: '用户状态', icon: 'user' },
  user_role: { text: '用户角色', icon: 'user-shield' },
  article_audit: { text: '文章审核', icon: 'file-alt' },
  question_audit: { text: '问答审核', icon: 'question-circle' },
  category_audit: { text: '分类管理', icon: 'folder' },
  tag_audit: { text: '标签管理', icon: 'tags' }
}

const actionMap = {
  disable: { text: '禁用', type: 'danger' },
  enable: { text: '启用', type: 'success' },
  set_admin: { text: '设置管理员', type: 'warning' },
  remove_admin: { text: '取消管理员', type: 'info' },
  approve: { text: '通过', type: 'success' },
  reject: { text: '拒绝', type: 'danger' },
  create: { text: '创建', type: 'primary' },
  update: { text: '更新', type: 'warning' },
  delete: { text: '删除', type: 'danger' }
}

// 新的辅助方法
const getActivityIcon = (type) => {
  const iconMap = {
    'USER_REGISTER': 'user-plus',
    'ARTICLE_PUBLISH': 'file-alt',
    'QUESTION_ASK': 'question-circle',
    'COMMENT_POST': 'comment'
  }
  return iconMap[type] || 'info-circle'
}

const getActivityTypeClass = (type) => {
  const classMap = {
    'USER_REGISTER': 'success',
    'ARTICLE_PUBLISH': 'primary',
    'QUESTION_ASK': 'warning',
    'COMMENT_POST': 'info'
  }
  return classMap[type] || 'default'
}

const getActivityTypeText = (type) => {
  const textMap = {
    'USER_REGISTER': '用户注册',
    'ARTICLE_PUBLISH': '文章发布',
    'QUESTION_ASK': '问答提问',
    'COMMENT_POST': '评论发布'
  }
  return textMap[type] || '系统活动'
}

onMounted(() => {
  // 初始化侧边栏状态
  initSidebar();

  // 设置窗口大小改变事件监听器
  setupResizeListener();

  // 加载仪表板数据
  loadDashboardData();
  loadPendingItems();
})

onBeforeUnmount(() => {
  // 移除窗口大小改变事件监听器
  removeResizeListener();
})
</script>

<template>
  <div class="admin-dashboard-container">
    <!-- 顶部导航栏 -->
    <AdminNavBar @toggle-sidebar="toggleSidebar" />
    
    <div class="admin-dashboard">
      <!-- 侧边导航 -->
      <div class="admin-sidebar" :class="{ 'show': sidebarVisible }">
        <div class="sidebar-menu">
          <el-menu
            :default-active="activeTab"
            class="menu-list"
            @select="activeTab = $event"
            background-color="var(--bg-white)"
            text-color="var(--text-color)"
            active-text-color="var(--primary-color)"
          >
            <div class="sidebar-header">
              <div class="admin-logo">
                <font-awesome-icon icon="chart-line" class="logo-icon" />
                <span>FroumX Admin</span>
              </div>
            </div>
            
            <el-menu-item index="dashboard" class="menu-item">
              <font-awesome-icon icon="tachometer-alt" class="menu-icon" />
              <span>仪表盘</span>
            </el-menu-item>
            
            <el-menu-item index="users" class="menu-item">
              <font-awesome-icon icon="users" class="menu-icon" />
              <span>用户管理</span>
            </el-menu-item>
            
            <el-menu-item index="articles" class="menu-item">
              <font-awesome-icon icon="file-alt" class="menu-icon" />
              <span>文章管理</span>
              <span class="menu-badge">{{ stats.articles.pending }}</span>
            </el-menu-item>
            
            <el-menu-item index="questions" class="menu-item">
              <font-awesome-icon icon="question-circle" class="menu-icon" />
              <span>问答管理</span>
              <span class="menu-badge">{{ stats.questions.pending }}</span>
            </el-menu-item>

            <el-menu-item index="comments" class="menu-item">
              <font-awesome-icon icon="comment" class="menu-icon" />
              <span>评论管理</span>
            </el-menu-item>
            
            <el-menu-item index="categories" class="menu-item">
              <font-awesome-icon icon="folder" class="menu-icon" />
              <span>分类管理</span>
              <span class="menu-badge">{{ stats.categories.pending }}</span>
            </el-menu-item>
            
            <el-menu-item index="tags" class="menu-item">
              <font-awesome-icon icon="tags" class="menu-icon" />
              <span>标签管理</span>
              <span class="menu-badge">{{ stats.tags.pending }}</span>
            </el-menu-item>
            
            <el-menu-item index="logs" class="menu-item">
              <font-awesome-icon icon="clipboard-list" class="menu-icon" />
              <span>操作日志</span>
            </el-menu-item>
            
            <el-menu-item index="settings" class="menu-item">
              <font-awesome-icon icon="cog" class="menu-icon" />
              <span>系统设置</span>
            </el-menu-item>
          </el-menu>
        </div>
      </div>
      
      <!-- 主内容区 -->
      <div class="admin-content" :class="{ 'full-width': !sidebarVisible }">
        <!-- 仪表盘 -->
        <div v-if="activeTab === 'dashboard'" class="dashboard-content">
          <div class="dashboard-header">
            <h1>管理员仪表盘</h1>
            <p class="welcome-message">欢迎回来，管理员！今天是 {{ new Date().toLocaleDateString() }}，祝您工作愉快。</p>
          </div>
          
          <!-- 数据概览卡片 -->
          <div class="stats-cards">
            <div class="stat-card users">
              <div class="stat-icon">
                <font-awesome-icon icon="users" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.users.total }}</div>
                <div class="stat-label">用户总数</div>
                <div class="stat-detail">
                  <span class="stat-highlight">+{{ stats.users.newToday }}</span> 今日新增
                </div>
              </div>
            </div>
            
            <div class="stat-card articles">
              <div class="stat-icon">
                <font-awesome-icon icon="file-alt" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.articles.total }}</div>
                <div class="stat-label">文章总数</div>
                <div class="stat-detail">
                  <span class="stat-highlight">{{ stats.articles.pending }}</span> 待审核
                </div>
              </div>
            </div>
            
            <div class="stat-card questions">
              <div class="stat-icon">
                <font-awesome-icon icon="question-circle" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.questions.total }}</div>
                <div class="stat-label">问答总数</div>
                <div class="stat-detail">
                  <span class="stat-highlight">{{ stats.questions.solved }}</span> 已解决
                </div>
              </div>
            </div>
            
            <div class="stat-card logs">
              <div class="stat-icon">
                <font-awesome-icon icon="chart-bar" />
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.logs.today }}</div>
                <div class="stat-label">今日操作</div>
                <div class="stat-detail">
                  <span class="stat-highlight">{{ stats.logs.thisWeek }}</span> 本周总计
                </div>
              </div>
            </div>
          </div>
          
          <!-- 内容区域 -->
          <div class="dashboard-main">
            <!-- 今日数据 -->
            <div class="dashboard-section today-stats">
              <div class="section-header">
                <h2>今日数据</h2>
              </div>
              <div class="today-grid">
                <div class="today-item">
                  <div class="today-icon new-users">
                    <font-awesome-icon icon="user-plus" />
                  </div>
                  <div class="today-info">
                    <div class="today-value">{{ todayStats.newUsers }}</div>
                    <div class="today-label">新增用户</div>
                  </div>
                </div>
                <div class="today-item">
                  <div class="today-icon new-articles">
                    <font-awesome-icon icon="file-alt" />
                  </div>
                  <div class="today-info">
                    <div class="today-value">{{ todayStats.newArticles }}</div>
                    <div class="today-label">新增文章</div>
                  </div>
                </div>
                <div class="today-item">
                  <div class="today-icon new-questions">
                    <font-awesome-icon icon="question-circle" />
                  </div>
                  <div class="today-info">
                    <div class="today-value">{{ todayStats.newQuestions }}</div>
                    <div class="today-label">新增问答</div>
                  </div>
                </div>
                <div class="today-item">
                  <div class="today-icon new-comments">
                    <font-awesome-icon icon="comment" />
                  </div>
                  <div class="today-info">
                    <div class="today-value">{{ todayStats.newComments }}</div>
                    <div class="today-label">新增评论</div>
                  </div>
                </div>
                <div class="today-item">
                  <div class="today-icon active-users">
                    <font-awesome-icon icon="users" />
                  </div>
                  <div class="today-info">
                    <div class="today-value">{{ todayStats.activeUsers }}</div>
                    <div class="today-label">活跃用户</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 待处理事项 -->
            <div class="dashboard-section pending-tasks">
              <div class="section-header">
                <h2>待处理事项</h2>
                <el-button size="small" type="primary" class="refresh-btn" :loading="pendingLoading" @click="loadPendingItems">
                  <font-awesome-icon icon="sync" /> 刷新
                </el-button>
              </div>
              
              <div class="tab-header">
                <button
                  v-for="tab in pendingTabs"
                  :key="tab.key"
                  class="tab-item"
                  :class="{ active: pendingType === tab.key }"
                  @click="pendingType = tab.key"
                >
                  {{ tab.label }} ({{ tab.count }})
                </button>
              </div>
              
              <div class="pending-list" v-loading="pendingLoading">
                <el-empty v-if="!pendingLoading && currentPendingItems.length === 0" description="暂无待处理事项" :image-size="80" />
                <div v-for="item in currentPendingItems" :key="`${item.type}-${item.id}`" class="pending-item">
                  <div class="item-info">
                    <div class="item-title">{{ item.title }}</div>
                    <div class="item-meta">
                      <span><font-awesome-icon icon="user" /> {{ item.author }}</span>
                      <span><font-awesome-icon icon="clock" /> {{ item.time }}</span>
                    </div>
                  </div>
                  <div class="item-actions">
                    <el-button size="small" type="primary" plain @click="openPendingManagement(item.type)">
                      <font-awesome-icon icon="list" /> 管理
                    </el-button>
                    <el-button size="small" type="success" plain @click="approvePendingItem(item)">
                      <font-awesome-icon icon="check" /> 通过
                    </el-button>
                    <el-button size="small" type="danger" plain @click="rejectPendingItem(item)">
                      <font-awesome-icon icon="times" /> 拒绝
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 最近活动 -->
            <div class="dashboard-section recent-activities">
              <div class="section-header">
                <h2>最近活动</h2>
                <el-button size="small" type="primary" class="view-all-btn">
                  <font-awesome-icon icon="list" /> 查看全部
                </el-button>
              </div>
              
              <div class="activity-timeline">
                <div v-for="(activity, index) in recentActivities.activities" :key="`${activity.type}-${activity.time}-${index}`" class="timeline-item">
                  <div class="timeline-icon" :class="getActivityTypeClass(activity.type)">
                    <font-awesome-icon :icon="getActivityIcon(activity.type)" />
                  </div>
                  <div class="timeline-content">
                    <div class="timeline-header">
                      <span class="timeline-type">{{ getActivityTypeText(activity.type) }}</span>
                      <span class="timeline-action">
                        {{ activity.description }}
                      </span>
                    </div>
                    <div class="timeline-body">
                      <strong>{{ activity.user }}</strong> {{ activity.description }}
                      <span v-if="activity.title">：{{ activity.title }}</span>
                    </div>
                    <div class="timeline-footer">
                      <font-awesome-icon icon="clock" /> {{ formatDateTime(activity.time) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 快速访问区域 -->
          <div class="quick-access">
            <div class="section-header">
              <h2>快速访问</h2>
            </div>
            <div class="quick-access-grid">
              <div class="quick-access-card" @click="activeTab = 'users'">
                <div class="card-icon">
                  <font-awesome-icon icon="user-shield" />
                </div>
                <div class="card-title">用户状态</div>
              </div>
              
              <div class="quick-access-card" @click="activeTab = 'articles'">
                <div class="card-icon">
                  <font-awesome-icon icon="file-alt" />
                </div>
                <div class="card-title">文章审核</div>
              </div>
              
              <div class="quick-access-card" @click="activeTab = 'questions'">
                <div class="card-icon">
                  <font-awesome-icon icon="question-circle" />
                </div>
                <div class="card-title">问答审核</div>
              </div>

              <div class="quick-access-card" @click="activeTab = 'comments'">
                <div class="card-icon">
                  <font-awesome-icon icon="comment" />
                </div>
                <div class="card-title">评论管理</div>
              </div>
              
              <div class="quick-access-card" @click="activeTab = 'settings'">
                <div class="card-icon">
                  <font-awesome-icon icon="cog" />
                </div>
                <div class="card-title">系统设置</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 用户管理 -->
        <UserManagement v-else-if="activeTab === 'users'" />
        
        <!-- 文章管理 -->
        <ArticleManagement v-else-if="activeTab === 'articles'" />
        
        <!-- 问答管理 -->
        <QuestionManagement v-else-if="activeTab === 'questions'" />

        <!-- 评论管理 -->
        <CommentManagement v-else-if="activeTab === 'comments'" />
        
        <!-- 分类管理 -->
        <CategoryManagement v-else-if="activeTab === 'categories'" />
        
        <!-- 标签管理 -->
        <TagManagement v-else-if="activeTab === 'tags'" />
        
        <!-- 操作日志 -->
        <OperationLog v-else-if="activeTab === 'logs'" />
        
        <!-- 系统设置 -->
        <SystemSettings v-else-if="activeTab === 'settings'" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg-color);
  color: var(--text-color);
  position: relative;
}

.admin-dashboard-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, var(--bg-color) 0%, rgba(37, 99, 235, 0.02) 100%);
  z-index: -1;
}

.admin-dashboard {
  display: flex;
  flex: 1;
  margin-top: 70px;
  position: relative;
}

.admin-sidebar {
  width: 250px;
  height: calc(100vh - 70px);
  position: fixed;
  left: 0;
  top: 70px;
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--primary-light) 100%);
  border-right: 2px solid rgba(37, 99, 235, 0.1);
  z-index: 100;
  transition: all 0.3s ease;
  overflow-y: auto;
  box-shadow: var(--shadow);
}

.sidebar-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.menu-list {
  border-right: none;
  height: 100%;
  background: transparent;
}

.sidebar-header {
  padding: 1.5rem;
  border-bottom: 2px solid rgba(37, 99, 235, 0.1);
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.25rem;
  font-weight: 700;
  color: white;
}

.logo-icon {
  font-size: 1.5rem;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.menu-item {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 1.5rem;
  position: relative;
  margin: 0.25rem 0.5rem;
  border-radius: var(--radius);
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(37, 99, 235, 0.1);
  transform: translateX(4px);
}

.menu-item.is-active {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
  box-shadow: var(--shadow);
}

.menu-icon {
  margin-right: 0.75rem;
  font-size: 1.125rem;
  transition: transform 0.3s ease;
}

.menu-item:hover .menu-icon {
  transform: scale(1.1);
}

.menu-badge {
  position: absolute;
  right: 1rem;
  background: linear-gradient(135deg, var(--accent-color) 0%, #ff6b9d 100%);
  color: white;
  font-size: 0.75rem;
  height: 22px;
  min-width: 22px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.5rem;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(255, 64, 129, 0.3);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.admin-content {
  flex: 1;
  margin-left: 250px;
  padding: 2rem;
  overflow-y: auto;
  height: calc(100vh - 70px);
  transition: all 0.3s ease;
  background: transparent;
}

.admin-content.full-width {
  margin-left: 0;
}

.dashboard-content {
  width: 100%;
  max-width: none;
  margin: 0;
  position: relative;
}

.dashboard-header {
  margin-bottom: 2rem;
  padding: 2rem;
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--primary-light) 100%);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  border: 1px solid rgba(37, 99, 235, 0.1);
  position: relative;
  overflow: hidden;
}

.dashboard-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color) 0%, var(--accent-color) 100%);
}

.dashboard-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: var(--text-color);
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.welcome-message {
  font-size: 1.125rem;
  color: var(--text-light);
  margin: 0;
}

/* 数据概览卡片 - 与首页风格匹配 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 1.5rem;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  background: linear-gradient(135deg, var(--bg-white) 0%, rgba(37, 99, 235, 0.02) 100%);
  overflow: hidden;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-light);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color) 0%, var(--accent-color) 100%);
}

.stat-card.users::before {
  background: linear-gradient(90deg, var(--primary-color) 0%, #5c6bc0 100%);
}

.stat-card.articles::before {
  background: linear-gradient(90deg, var(--success-color) 0%, #66bb6a 100%);
}

.stat-card.questions::before {
  background: linear-gradient(90deg, var(--warning-color) 0%, #ffb74d 100%);
}

.stat-card.logs::before {
  background: linear-gradient(90deg, #9c27b0 0%, #ba68c8 100%);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  margin-right: 1.25rem;
  color: white;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transition: all 0.3s ease;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
}

.stat-card.articles .stat-icon {
  background: linear-gradient(135deg, var(--success-color) 0%, #388e3c 100%);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.stat-card.questions .stat-icon {
  background: linear-gradient(135deg, var(--warning-color) 0%, #f57c00 100%);
  box-shadow: 0 4px 12px rgba(250, 173, 20, 0.3);
}

.stat-card.logs .stat-icon {
  background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%);
  box-shadow: 0 4px 12px rgba(156, 39, 176, 0.3);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
  color: var(--text-color);
  background: linear-gradient(135deg, var(--text-color) 0%, var(--primary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-light);
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.stat-detail {
  font-size: 0.75rem;
  color: var(--text-light);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.stat-highlight {
  font-weight: 600;
  color: var(--primary-color);
  background: linear-gradient(135deg, var(--primary-light) 0%, rgba(37, 99, 235, 0.1) 100%);
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  margin-right: 0.25rem;
  border: 1px solid rgba(37, 99, 235, 0.2);
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.1);
}

/* 仪表盘主要内容 */
.dashboard-main {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
  width: 100%;
  box-sizing: border-box;
}

.dashboard-section {
  background: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  padding: 20px;
  border: 1px solid var(--border-color);
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.dashboard-section:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--border-color);
}

.section-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
  position: relative;
  padding-left: 12px;
}

.section-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: var(--primary-color);
  border-radius: 2px;
}

/* 待处理项目 */
.tab-header {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 15px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.tab-item {
  padding: 10px 15px;
  cursor: pointer;
  font-weight: 500;
  color: var(--text-light);
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  transition: var(--transition);
  white-space: nowrap;
  font: inherit;
}

.tab-item:hover {
  color: var(--primary-color);
}

.tab-item.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
  font-weight: 600;
}

.pending-list {
  flex: 1;
  overflow-y: auto;
}

.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.pending-item:last-child {
  border-bottom: none;
}

.item-info {
  min-width: 0;
  flex: 1;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.item-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #263238;
  font-size: 15px;
}

.item-meta {
  font-size: 13px;
  color: #78909c;
  display: flex;
  gap: 15px;
}

/* 活动时间线 */
.activity-timeline {
  position: relative;
  flex: 1;
  overflow-y: auto;
}

.timeline-item {
  display: flex;
  margin-bottom: 16px;
  position: relative;
  padding-left: 5px;
}

.timeline-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  margin-right: 16px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.timeline-icon.success {
  background-color: #4CAF50;
}

.timeline-icon.danger {
  background-color: #F44336;
}

.timeline-icon.warning {
  background-color: #FF9800;
}

.timeline-icon.primary {
  background-color: #03A9F4;
}

.timeline-icon.info {
  background-color: #00BCD4;
}

.timeline-content {
  flex: 1;
  background-color: #f5f7fa;
  border-radius: 10px;
  padding: 15px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.timeline-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.timeline-type {
  font-weight: 600;
  color: #263238;
  margin-right: 10px;
  font-size: 14px;
}

.timeline-action {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  color: white;
  font-weight: 500;
}

.timeline-action.success {
  background-color: #4CAF50;
}

.timeline-action.danger {
  background-color: #F44336;
}

.timeline-action.warning {
  background-color: #FF9800;
}

.timeline-action.primary {
  background-color: #03A9F4;
}

.timeline-action.info {
  background-color: #00BCD4;
}

.timeline-body {
  font-size: 14px;
  color: #455a64;
  margin-bottom: 8px;
  line-height: 1.5;
}

.timeline-footer {
  font-size: 12px;
  color: #78909c;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 快速访问区域 */
.quick-access {
  margin-top: 32px;
}

.quick-access-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.quick-access-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.quick-access-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.card-icon {
  font-size: 32px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transition: transform 0.3s ease;
}

.quick-access-card:hover .card-icon {
  transform: scale(1.1);
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #0f172a;
  transition: color 0.3s ease;
}

.quick-access-card:hover .card-title {
  color: #2563eb;
}

/* 今日数据样式 */
.today-stats {
  margin-bottom: 32px;
}

.today-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.today-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.today-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.today-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 20px;
  color: white;
}

.today-icon.new-users {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.today-icon.new-articles {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.today-icon.new-questions {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.today-icon.new-comments {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.today-icon.active-users {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.today-info {
  flex: 1;
}

.today-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.today-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

/* 按钮样式 */
.refresh-btn, .view-all-btn {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%) !important;
  border: none !important;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  border-radius: var(--radius);
  padding: 10px 20px;
  transition: var(--transition);
  font-weight: 500;
  box-shadow: var(--shadow);
}

.refresh-btn:hover, .view-all-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  filter: brightness(1.1);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 992px) {
  .admin-sidebar {
    width: 70px;
    overflow: visible;
  }
  
  .admin-content {
    margin-left: 70px;
  }
  
  .admin-content.full-width {
    margin-left: 0;
  }
  
  .sidebar-header {
    padding: 15px 0;
    display: flex;
    justify-content: center;
  }
  
  .admin-logo span {
    display: none;
  }
  
  .menu-item span {
    display: none;
  }
  
  .menu-icon {
    margin-right: 0;
    font-size: 18px;
  }
  
  .menu-badge {
    top: 5px;
    right: 5px;
    height: 16px;
    min-width: 16px;
    font-size: 10px;
  }
  
  .dashboard-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .admin-content {
    padding: 16px;
  }
  
  .dashboard-header h1 {
    font-size: 24px;
  }
  
  .welcome-message {
    font-size: 14px;
  }
}

@media (max-width: 576px) {
  .admin-sidebar {
    width: 100%;
    height: auto;
    position: fixed;
    left: 0;
    top: 70px;
    transform: translateY(-100%);
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
    z-index: 1000;
  }
  
  .admin-sidebar.show {
    transform: translateY(0);
    opacity: 1;
    visibility: visible;
  }
  
  .admin-content {
    margin-left: 0;
    padding: 12px;
  }
  
  .dashboard-header h1 {
    font-size: 20px;
  }
}

/* 后台仪表盘降噪：更接近管理工具的低装饰界面 */
.admin-dashboard-container::before,
.dashboard-header::before,
.stat-card::before {
  display: none;
}

.admin-dashboard-container {
  background: #f6f7f9;
}

.admin-sidebar {
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  box-shadow: none;
}

.sidebar-header {
  background: #111827;
  border-bottom: 1px solid #1f2937;
}

.menu-item {
  height: 46px;
  margin: 0.125rem 0.5rem;
  border-radius: 6px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.menu-item:hover,
.menu-item:hover .menu-icon {
  transform: none;
}

.menu-item:hover {
  background: #f3f4f6;
}

.menu-item.is-active {
  background: #2563eb;
  box-shadow: none;
}

.menu-badge {
  background: #dc2626;
  border-radius: 999px;
  box-shadow: none;
  animation: none;
}

.admin-content {
  padding: 1.25rem;
}

.dashboard-header,
.stat-card,
.dashboard-section,
.quick-access-card,
.today-item,
.timeline-content {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: none;
}

.dashboard-header {
  padding: 1.25rem;
  margin-bottom: 1rem;
}

.dashboard-header h1 {
  font-size: 1.5rem;
  margin-bottom: 0.35rem;
}

.welcome-message {
  font-size: 0.95rem;
}

.stats-cards {
  gap: 1rem;
  margin-bottom: 1rem;
}

.stat-card {
  padding: 1rem;
  transition: border-color 0.2s ease;
}

.stat-card:hover,
.stat-card:hover .stat-icon,
.dashboard-section:hover,
.quick-access-card:hover,
.quick-access-card:hover .card-icon,
.today-item:hover,
.refresh-btn:hover,
.view-all-btn:hover {
  transform: none;
}

.stat-card:hover,
.dashboard-section:hover,
.quick-access-card:hover,
.today-item:hover {
  box-shadow: none;
  border-color: #d1d5db;
}

.stat-icon,
.stat-card.articles .stat-icon,
.stat-card.questions .stat-icon,
.stat-card.logs .stat-icon,
.today-icon.new-users,
.today-icon.new-articles,
.today-icon.new-questions,
.today-icon.new-comments,
.today-icon.active-users {
  background-image: none;
  box-shadow: none;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  background: #2563eb;
  margin-right: 1rem;
}

.stat-card.articles .stat-icon,
.today-icon.new-articles {
  background: #16a34a;
}

.stat-card.questions .stat-icon,
.today-icon.new-questions {
  background: #d97706;
}

.stat-card.logs .stat-icon,
.today-icon.new-comments {
  background: #7c3aed;
}

.today-icon.new-users {
  background: #2563eb;
}

.today-icon.active-users {
  background: #dc2626;
}

.stat-value {
  background: none;
  -webkit-text-fill-color: currentColor;
  color: #111827;
}

.stat-highlight {
  background: #eff6ff;
  border-color: #bfdbfe;
  border-radius: 4px;
  box-shadow: none;
}

.dashboard-main,
.quick-access,
.today-stats {
  margin-bottom: 1rem;
}

.dashboard-section,
.quick-access-card,
.today-item {
  transition: border-color 0.2s ease;
}

.quick-access-card {
  padding: 1rem;
}

.card-icon {
  background: none;
  -webkit-text-fill-color: #2563eb;
  color: #2563eb;
}

.card-title,
.quick-access-card:hover .card-title {
  color: #111827;
}

.today-item {
  padding: 1rem;
}

.today-icon {
  width: 42px;
  height: 42px;
  border-radius: 6px;
}

.refresh-btn,
.view-all-btn {
  background: #2563eb !important;
  border: 1px solid #2563eb !important;
  border-radius: 4px;
  box-shadow: none;
}

.refresh-btn:hover,
.view-all-btn:hover {
  box-shadow: none;
  filter: none;
}
</style> 
