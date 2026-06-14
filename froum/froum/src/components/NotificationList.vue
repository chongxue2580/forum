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
    console.error('加载通知失败:', error)
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
    console.error('处理通知失败:', error)
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
    console.error('标记全部已读失败:', error)
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
  <div class="notification-list">
    <div class="notification-toolbar">
      <div class="filter-buttons">
        <button
          v-for="filter in filters"
          :key="filter.type"
          class="filter-btn"
          :class="{ active: filterType === filter.type }"
          @click="filterType = filter.type"
        >
          {{ filter.label }}
        </button>
      </div>

      <button class="action-btn" :disabled="!unreadCount" @click="markAllAsRead">
        全部标记为已读
      </button>
    </div>

    <div v-if="loading" class="state">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>加载中...</span>
    </div>

    <div v-else-if="errorMessage" class="state error">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
      <span>{{ errorMessage }}</span>
      <button class="link-btn" @click="loadNotifications">重试</button>
    </div>

    <div v-else-if="notifications.length === 0" class="state">
      <font-awesome-icon :icon="['fas', 'bell']" />
      <span>暂无通知</span>
    </div>

    <div v-else class="notifications">
      <button
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item"
        :class="{ unread: !notification.isRead }"
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
  max-width: 860px;
  margin: 0 auto;
  padding: 20px;
}

.notification-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
}

.filter-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-btn,
.action-btn,
.page-btn,
.link-btn {
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn {
  padding: 6px 16px;
  border-radius: 16px;
}

.filter-btn.active,
.filter-btn:hover,
.action-btn:hover,
.page-btn:hover:not(:disabled),
.link-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.action-btn,
.page-btn,
.link-btn {
  padding: 8px 14px;
  border-radius: 4px;
}

.action-btn:disabled,
.page-btn:disabled {
  color: #aaa;
  border-color: #ddd;
  cursor: not-allowed;
}

.state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 180px;
  color: #666;
}

.state.error {
  color: #842029;
}

.notifications {
  display: flex;
  flex-direction: column;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.notification-item {
  display: flex;
  gap: 14px;
  width: 100%;
  padding: 16px;
  border: 0;
  border-bottom: 1px solid #eee;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item.unread {
  background: #f6fbff;
}

.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e6f7ff;
  color: #1890ff;
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
  font-weight: 600;
  color: #333;
}

.notification-time {
  color: #999;
  font-size: 13px;
  white-space: nowrap;
}

.notification-text {
  color: #555;
  line-height: 1.5;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.page-info {
  color: #666;
}

@media (max-width: 640px) {
  .notification-toolbar,
  .notification-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
