<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notificationService } from '../services/notificationService'
import { formatFriendlyTime } from '../utils/dateUtils'

const router = useRouter()

const notifications = ref([])
const loading = ref(false)
const errorMessage = ref('')
const filterType = ref('all')
const currentPage = ref(1)
const totalPages = ref(1)
const totalItems = ref(0)
const pageSize = 10

const filters = [
  { type: 'all', label: '全部' },
  { type: 'unread', label: '未读' },
  { type: 'article', label: '文章' },
  { type: 'question', label: '问答' },
  { type: 'follow', label: '关注' }
]

const unreadCount = computed(() => notifications.value.filter(item => !item.isRead).length)

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : [],
    totalElements: page.totalElements || 0,
    totalPages: page.totalPages || 1
  }
}

const normalizeNotification = (notification) => {
  const fromUser = notification.fromUser || {}
  return {
    id: notification.id,
    type: notification.type || 'SYSTEM',
    title: notification.title || '',
    content: notification.content || notification.title || '',
    targetId: notification.targetId,
    isRead: notification.isRead ?? notification.read ?? false,
    createdAt: notification.createdAt,
    from: {
      id: fromUser.id,
      name: fromUser.nickname || fromUser.username || fromUser.name || '系统',
      avatar: fromUser.avatarUrl || fromUser.avatar || ''
    }
  }
}

const loadNotifications = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    let response
    const params = {
      page: currentPage.value,
      pageSize
    }

    if (filterType.value === 'unread') {
      response = await notificationService.getUnreadNotifications(params)
    } else if (filterType.value === 'all') {
      response = await notificationService.getUserNotifications(params)
    } else {
      response = await notificationService.getUserNotifications({
        page: 1,
        pageSize: 100
      })
    }

    const page = unwrapPage(response.data || response)
    const normalized = page.content.map(normalizeNotification)

    if (filterType.value === 'article' || filterType.value === 'question' || filterType.value === 'follow') {
      const filtered = normalized.filter(notification => resolveCategory(notification.type) === filterType.value)
      const start = (currentPage.value - 1) * pageSize
      notifications.value = filtered.slice(start, start + pageSize)
      totalItems.value = filtered.length
      totalPages.value = Math.max(Math.ceil(filtered.length / pageSize), 1)
    } else {
      notifications.value = normalized
      totalItems.value = page.totalElements
      totalPages.value = page.totalPages
    }
  } catch (error) {
    errorMessage.value = error.message || '加载通知失败'
    notifications.value = []
    totalItems.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const resolveCategory = (type) => {
  if (type === 'USER_FOLLOW') return 'follow'
  if (type.startsWith('ARTICLE_')) return 'article'
  if (type.startsWith('QUESTION_')) return 'question'
  if (type.includes('FOLLOW')) return 'follow'
  return 'all'
}

const resolveTargetRoute = (notification) => {
  if (!notification.targetId) return null

  if (notification.type.startsWith('ARTICLE_')) {
    return `/article/${notification.targetId}`
  }
  if (notification.type.startsWith('QUESTION_')) {
    return `/question/${notification.targetId}`
  }
  if (notification.type === 'USER_FOLLOW' && notification.from.id) {
    return `/user/${notification.from.id}`
  }
  return null
}

const markAsRead = async (notification) => {
  if (notification.isRead) return

  await notificationService.markNotificationAsRead(notification.id)
  notification.isRead = true
}

const openNotification = async (notification) => {
  try {
    await markAsRead(notification)
    const targetRoute = resolveTargetRoute(notification)
    if (targetRoute) {
      router.push(targetRoute)
    }
  } catch (error) {
    ElMessage.error(error.message || '处理通知失败')
  }
}

const markAllAsRead = async () => {
  if (!notifications.value.length) return

  try {
    await notificationService.markAllNotificationsAsRead()
    notifications.value = notifications.value.map(item => ({
      ...item,
      isRead: true
    }))
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadNotifications()
}

const formatTime = (dateString) => {
  return dateString ? formatFriendlyTime(dateString) : ''
}

const getNotificationIcon = (type) => {
  if (type.includes('FOLLOW')) return ['fas', 'user-plus']
  if (type.includes('COMMENT') || type.includes('ANSWER')) return ['fas', 'comment']
  if (type.includes('LIKE')) return ['fas', 'heart']
  return ['fas', 'bell']
}

watch(filterType, () => {
  currentPage.value = 1
  loadNotifications()
})

onMounted(() => {
  loadNotifications()
})
</script>

<template>
  <div class="notification-list kumo-page">
    <section class="notification-hero kumo-surface-strong reveal-rise">
      <div>
        <span class="kumo-eyebrow">Notifications</span>
        <h1 class="kumo-heading">通知中心</h1>
        <p class="kumo-muted">集中查看审核、互动、关注与系统提醒。</p>
      </div>
      <button class="kumo-button kumo-button--brand" :disabled="!unreadCount" @click="markAllAsRead">
        <font-awesome-icon :icon="['fas', 'check-double']" />
        全部已读
      </button>
    </section>

    <div class="notification-toolbar kumo-surface">
      <div class="filter-buttons kumo-tabs">
        <button
          v-for="filter in filters"
          :key="filter.type"
          class="kumo-tab"
          :class="{ active: filterType === filter.type }"
          @click="filterType = filter.type"
        >
          {{ filter.label }}
        </button>
      </div>
      <span class="unread-pill">{{ unreadCount }} 未读</span>
    </div>

    <div v-if="loading" class="state kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>加载中...</span>
    </div>

    <div v-else-if="errorMessage" class="state error kumo-surface">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
      <span>{{ errorMessage }}</span>
      <button class="kumo-button" @click="loadNotifications">重试</button>
    </div>

    <div v-else-if="notifications.length === 0" class="state kumo-surface">
      <font-awesome-icon :icon="['fas', 'bell']" />
      <span>暂无通知</span>
    </div>

    <div v-else class="notifications kumo-surface">
      <button
        v-for="(notification, index) in notifications"
        :key="notification.id"
        class="notification-item stagger-item"
        :class="{ unread: !notification.isRead }"
        :style="{ animationDelay: `${index * 42}ms` }"
        @click="openNotification(notification)"
      >
        <span class="notification-icon">
          <font-awesome-icon :icon="getNotificationIcon(notification.type)" />
        </span>

        <span class="notification-content">
          <span class="notification-head">
            <span class="notification-title">{{ notification.title || notification.from.name }}</span>
            <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
          </span>
          <span class="notification-text">{{ notification.content }}</span>
        </span>
      </button>
    </div>

    <div class="pagination" v-if="totalPages > 1">
      <button class="page-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
        上一页
      </button>
      <span class="page-info">第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.notification-list {
  width: min(100% - 2rem, 960px);
  margin: 0 auto;
  padding: clamp(1.25rem, 4vw, 2.5rem) 0;
}

.notification-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  margin-bottom: 1rem;
  padding: clamp(1.5rem, 4vw, 2.5rem);
}

.notification-hero h1 {
  margin: 0.85rem 0 0;
  font-size: clamp(2.15rem, 7vw, 4rem);
}

.notification-hero p {
  margin: 0.75rem 0 0;
}

.notification-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
  padding: 0.65rem;
}

.filter-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.unread-pill {
  display: inline-flex;
  align-items: center;
  min-height: 2.35rem;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  background: var(--kumo-status-info-tint);
  color: var(--kumo-status-info);
  font-size: 0.85rem;
  font-weight: 780;
  white-space: nowrap;
}

.kumo-button:disabled,
.page-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 12rem;
  color: var(--kumo-text-muted);
}

.state.error {
  color: var(--kumo-status-danger);
}

.notifications {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notification-item {
  display: flex;
  gap: 0.9rem;
  width: 100%;
  padding: 1rem;
  border: 0;
  border-bottom: 1px solid var(--kumo-hairline);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background-color var(--kumo-transition),
    transform var(--kumo-transition);
}

.notification-item:hover {
  background: var(--kumo-bg-subtle);
  transform: translateX(4px);
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item.unread {
  background: var(--kumo-status-info-tint);
}

.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  flex-shrink: 0;
}

.notification-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.notification-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.notification-title {
  color: var(--kumo-text-default);
  font-weight: 760;
}

.notification-time {
  color: var(--kumo-text-subtle);
  font-size: 13px;
  white-space: nowrap;
}

.notification-text {
  color: var(--kumo-text-muted);
  line-height: 1.5;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.page-btn {
  min-height: 2.45rem;
  padding: 0.55rem 0.9rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  cursor: pointer;
  transition: var(--kumo-transition);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--kumo-hairline-strong);
  color: var(--kumo-bg-brand-strong);
  box-shadow: var(--kumo-shadow-sm);
}

.page-info {
  color: var(--kumo-text-muted);
}

@media (max-width: 640px) {
  .notification-hero,
  .notification-toolbar,
  .notification-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .notification-hero {
    align-items: stretch;
  }
}
</style>
