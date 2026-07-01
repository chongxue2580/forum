<template>
  <div class="messages-page">
    <ui-page-hero
      title="消息中心"
      description="集中处理通知、评论、关注和站内私信，保持讨论节奏清晰。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'bell']" />
          Inbox
        </span>
      </template>
      <template #actions>
        <button class="kumo-button kumo-button--brand" type="button" @click="openComposeDialog()">
          <font-awesome-icon :icon="['fas', 'paper-plane']" />
          写私信
        </button>
      </template>
      <template #aside>
        <div class="hero-count">
          <strong>{{ unreadTotal }}</strong>
          <span>未读消息</span>
        </div>
      </template>
    </ui-page-hero>

    <section class="messages-workspace">
      <aside class="message-sidebar kumo-surface">
        <button
          v-for="type in messageTypes"
          :key="type.key"
          class="type-button"
          :class="{ active: activeType === type.key }"
          type="button"
          @click="activeType = type.key"
        >
          <font-awesome-icon :icon="type.icon" />
          <span>{{ type.label }}</span>
          <strong v-if="getUnreadCount(type.key) > 0">{{ getUnreadCount(type.key) }}</strong>
        </button>

        <button class="type-button mark-read" type="button" @click="markAllAsRead">
          <font-awesome-icon :icon="['fas', 'check-circle']" />
          <span>全部标记为已读</span>
        </button>
      </aside>

      <main class="message-content kumo-surface">
        <div class="content-heading">
          <div>
            <span class="kumo-eyebrow">{{ messageTypeLabel }}</span>
            <h2 class="kumo-heading">消息列表</h2>
          </div>
          <button class="kumo-button" type="button" @click="fetchMessages">
            <font-awesome-icon :icon="['fas', 'sync-alt']" />
            刷新
          </button>
        </div>

        <div v-if="loading" class="state-panel">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>加载中...</span>
        </div>

        <div v-else-if="errorMessage" class="state-panel error">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
          <span>{{ errorMessage }}</span>
          <button class="kumo-button" type="button" @click="fetchMessages">重新加载</button>
        </div>

        <div v-else-if="messages.length === 0" class="state-panel">
          <font-awesome-icon :icon="['fas', 'inbox']" />
          <span>暂无{{ messageTypeLabel }}消息</span>
        </div>

        <div v-else class="message-list">
          <article
            v-for="(message, index) in messages"
            :key="message.id"
            class="message-item magnetic-card stagger-item"
            :class="{ unread: !message.read }"
            :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
          >
            <div class="message-avatar">
              <img v-if="message.sender?.avatar" :src="message.sender.avatar" :alt="message.sender?.name || '用户'">
              <span v-else>{{ getInitials(message.sender?.name || '系统') }}</span>
            </div>

            <div class="message-main">
              <div class="message-header">
                <div>
                  <strong>{{ message.sender?.name || '系统' }}</strong>
                  <span v-if="!message.read" class="kumo-status kumo-status--info">未读</span>
                </div>
                <time>{{ formatTime(message.createdAt) }}</time>
              </div>

              <div class="message-body" v-html="message.content"></div>

              <div v-if="message.replies && message.replies.length > 0" class="message-replies">
                <div class="replies-header">
                  <font-awesome-icon :icon="['fas', 'reply-all']" />
                  <span>回复 {{ message.replies.length }}</span>
                </div>
                <div class="reply-list">
                  <div v-for="reply in message.replies" :key="reply.id" class="reply-item">
                    <span class="reply-avatar">
                      <img v-if="reply.sender?.avatar" :src="reply.sender.avatar" :alt="reply.sender?.name || '用户'">
                      <span v-else>{{ getInitials(reply.sender?.name || '用户') }}</span>
                    </span>
                    <span class="reply-copy">
                      <strong>{{ reply.sender?.name || '当前用户' }}</strong>
                      <small>{{ formatTime(reply.createdAt) }}</small>
                      <em>{{ reply.content }}</em>
                    </span>
                  </div>
                </div>
              </div>

              <div v-if="message.actionType" class="message-actions">
                <button
                  v-if="message.actionType === 'follow'"
                  class="kumo-button"
                  :class="{ active: message.isFollowing }"
                  type="button"
                  @click="handleAction(message)"
                  @mouseenter="handleButtonHover(message.id, 'follow')"
                  @mouseleave="handleButtonLeave"
                >
                  <font-awesome-icon :icon="['fas', message.isFollowing ? 'user-check' : 'user-plus']" />
                  {{ message.isFollowing ? '已关注' : '关注' }}
                </button>
                <button
                  v-if="message.actionType === 'reply'"
                  class="kumo-button"
                  type="button"
                  @click="showReplyDialog(message)"
                  @mouseenter="handleButtonHover(message.id, 'reply')"
                  @mouseleave="handleButtonLeave"
                >
                  <font-awesome-icon :icon="['fas', 'reply']" />
                  回复
                </button>
                <button
                  v-if="message.actionType === 'view'"
                  class="kumo-button"
                  type="button"
                  @click="handleAction(message)"
                  @mouseenter="handleButtonHover(message.id, 'view')"
                  @mouseleave="handleButtonLeave"
                >
                  <font-awesome-icon :icon="['fas', 'eye']" />
                  查看
                </button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="totalPages > 1" class="pagination">
          <button class="kumo-button" type="button" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
            <font-awesome-icon :icon="['fas', 'chevron-left']" />
          </button>
          <span>{{ currentPage }} / {{ totalPages }}</span>
          <button class="kumo-button" type="button" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
            <font-awesome-icon :icon="['fas', 'chevron-right']" />
          </button>
        </div>
      </main>
    </section>

    <div v-if="showCompose" class="dialog-backdrop">
      <div class="dialog-panel kumo-surface">
        <div class="dialog-header">
          <h3>发送站内私信</h3>
          <button type="button" class="kumo-button kumo-button--icon" @click="closeComposeDialog">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
        <div class="dialog-body">
          <div v-if="composeUserName" class="recipient-chip">
            <font-awesome-icon :icon="['fas', 'user']" />
            发送给 <strong>{{ composeUserName }}</strong>
          </div>
          <input v-else v-model="composeUserId" class="kumo-input" type="number" min="1" placeholder="接收用户ID">
          <textarea v-model="composeContent" class="dialog-textarea" placeholder="请输入私信内容..." rows="5" @keydown.ctrl.enter="submitCompose"></textarea>
        </div>
        <div class="dialog-footer">
          <span>Ctrl+Enter 快速发送</span>
          <div>
            <button class="kumo-button" type="button" @click="closeComposeDialog">取消</button>
            <button class="kumo-button kumo-button--brand" type="button" :disabled="!composeUserId || !composeContent.trim()" @click="submitCompose">发送</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showReply" class="dialog-backdrop">
      <div class="dialog-panel kumo-surface">
        <div class="dialog-header">
          <h3>回复 {{ currentMessage?.sender?.name }}</h3>
          <button type="button" class="kumo-button kumo-button--icon" @click="closeReplyDialog">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
        <div class="dialog-body">
          <div class="original-message">
            <strong>原消息</strong>
            <div v-html="currentMessage?.content"></div>
          </div>
          <textarea v-model="replyContent" class="dialog-textarea" placeholder="请输入回复内容..." rows="5" @keydown.ctrl.enter="submitReply"></textarea>
        </div>
        <div class="dialog-footer">
          <span>Ctrl+Enter 快速发送</span>
          <div>
            <button class="kumo-button" type="button" @click="closeReplyDialog">取消</button>
            <button class="kumo-button kumo-button--brand" type="button" :disabled="!replyContent.trim()" @click="submitReply">发送回复</button>
          </div>
        </div>
      </div>
    </div>

    <transition name="toast">
      <div v-if="showSuccessNotification" class="success-toast kumo-surface">
        <font-awesome-icon :icon="['fas', 'check-circle']" />
        <span>回复成功</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UiPageHero from '../components/ui/UiPageHero.vue'
import { notificationService } from '../services/notificationService'

const route = useRoute()
const router = useRouter()
const pageSize = 10

const messageTypes = [
  { key: 'article', label: '文章/问答关注', icon: ['fas', 'file-alt'] },
  { key: 'comment', label: '评论通知', icon: ['fas', 'comment'] },
  { key: 'follow', label: '关注通知', icon: ['fas', 'user'] },
  { key: 'system', label: '站内信', icon: ['fas', 'bell'] }
]

const activeType = ref('article')
const loading = ref(true)
const messages = ref([])
const allMessages = ref([])
const errorMessage = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const hoverButton = ref(null)
const unreadCounts = ref({
  article: 0,
  comment: 0,
  follow: 0,
  system: 0
})

const showReply = ref(false)
const currentMessage = ref(null)
const replyContent = ref('')
const showCompose = ref(false)
const composeUserId = ref('')
const composeUserName = ref('')
const composeContent = ref('')
const showSuccessNotification = ref(false)

const typeLabels = {
  article: '文章/问答被关注',
  comment: '文章/问答被评论',
  follow: '个人被关注',
  system: '站内信'
}

const messageTypeLabel = computed(() => typeLabels[activeType.value] || '')
const unreadTotal = computed(() => Object.values(unreadCounts.value).reduce((sum, value) => sum + value, 0))
const getUnreadCount = (type) => unreadCounts.value[type] || 0

watch(activeType, () => {
  currentPage.value = 1
  applyMessageFilters()
})

const fetchMessages = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await notificationService.getUserNotifications({
      page: 1,
      pageSize: 100
    })
    allMessages.value = response.data.content.map(normalizeNotification)
    syncUnreadCounts()
    applyMessageFilters()
  } catch (error) {
    errorMessage.value = error.message || '消息加载失败'
    messages.value = []
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  applyMessageFilters()
}

const applyMessageFilters = () => {
  const filteredMessages = allMessages.value.filter(message => message.category === activeType.value)
  totalPages.value = Math.max(Math.ceil(filteredMessages.length / pageSize), 1)

  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value
  }

  const start = (currentPage.value - 1) * pageSize
  messages.value = filteredMessages.slice(start, start + pageSize)
}

const syncUnreadCounts = () => {
  unreadCounts.value = allMessages.value.reduce((counts, message) => {
    if (!message.read && counts[message.category] !== undefined) {
      counts[message.category] += 1
    }
    return counts
  }, {
    article: 0,
    comment: 0,
    follow: 0,
    system: 0
  })
  window.dispatchEvent(new Event('notifications-updated'))
}

const toDate = (value) => {
  if (!value) return null
  const date = Array.isArray(value)
    ? new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0)
    : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const normalizeNotification = (notification) => {
  const fromUser = notification.fromUser || {}
  const type = notification.type || 'SYSTEM'
  const category = resolveMessageCategory(type)
  const targetType = resolveTargetType(type)
  const senderName = fromUser.nickname || fromUser.username || fromUser.name || '系统'

  return {
    id: notification.id,
    type,
    category,
    sender: {
      id: fromUser.id,
      name: senderName,
      avatar: fromUser.avatarUrl || fromUser.avatar || null
    },
    title: notification.title,
    content: notification.content || notification.title || '',
    createdAt: toDate(notification.createdAt),
    read: notification.isRead ?? notification.read ?? false,
    actionType: type === 'DIRECT_MESSAGE' ? 'reply' : (targetType ? 'view' : null),
    targetId: type === 'USER_FOLLOW' ? fromUser.id : notification.targetId,
    targetType
  }
}

const resolveMessageCategory = (type) => {
  if (type === 'USER_FOLLOW') return 'follow'
  if (type.includes('COMMENT')) return 'comment'
  if (type.includes('FOLLOW') || type.includes('LIKE')) return 'article'
  return 'system'
}

const resolveTargetType = (type) => {
  if (type === 'DIRECT_MESSAGE') return 'user'
  if (type.startsWith('ARTICLE_')) return 'article'
  if (type.startsWith('QUESTION_')) return 'question'
  if (type === 'USER_FOLLOW') return 'user'
  return null
}

const formatTime = (date) => {
  if (!date) return ''

  const now = new Date()
  const diff = now - date

  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getInitials = (name) => {
  if (!name) return '用'
  return name.charAt(0).toUpperCase()
}

const handleButtonHover = (messageId, actionType) => {
  hoverButton.value = `${messageId}-${actionType}`
}

const handleButtonLeave = () => {
  hoverButton.value = null
}

const markMessageAsRead = async (message) => {
  if (message.read) return

  await notificationService.markNotificationAsRead(message.id)
  message.read = true

  const sourceMessage = allMessages.value.find(item => item.id === message.id)
  if (sourceMessage) sourceMessage.read = true
  syncUnreadCounts()
}

const handleAction = async (message) => {
  try {
    await markMessageAsRead(message)

    if (message.actionType !== 'view' || !message.targetId) return
    if (message.targetType === 'article') router.push(`/article/${message.targetId}`)
    if (message.targetType === 'question') router.push(`/question/${message.targetId}`)
    if (message.targetType === 'user') router.push(`/user/${message.targetId}`)
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const showReplyDialog = (message) => {
  markMessageAsRead(message)
  currentMessage.value = message
  replyContent.value = ''
  showReply.value = true
}

const closeReplyDialog = () => {
  showReply.value = false
  currentMessage.value = null
  replyContent.value = ''
}

const openComposeDialog = (userId = route.query.userId, userName = route.query.userName) => {
  composeUserId.value = userId ? String(userId) : ''
  composeUserName.value = userName ? String(userName) : ''
  composeContent.value = ''
  showCompose.value = true
}

const closeComposeDialog = () => {
  showCompose.value = false
  composeUserId.value = ''
  composeUserName.value = ''
  composeContent.value = ''
}

const submitCompose = async () => {
  if (!composeUserId.value || !composeContent.value.trim()) return

  try {
    await notificationService.sendDirectMessage(Number(composeUserId.value), composeContent.value.trim())
    ElMessage.success('私信已发送')
    closeComposeDialog()
  } catch (error) {
    ElMessage.error(error.message || '发送私信失败')
  }
}

const markAllAsRead = async () => {
  if (!allMessages.value.length) return

  try {
    loading.value = true
    await notificationService.markAllNotificationsAsRead()
    allMessages.value = allMessages.value.map(message => ({ ...message, read: true }))
    syncUnreadCounts()
    applyMessageFilters()
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

const submitReply = async () => {
  if (!replyContent.value.trim() || !currentMessage.value) return

  const receiverId = currentMessage.value.sender?.id
  if (!receiverId) {
    ElMessage.warning('系统通知不能回复')
    return
  }

  try {
    await notificationService.sendDirectMessage(receiverId, replyContent.value.trim())
    closeReplyDialog()
    showSuccessNotification.value = true
    setTimeout(() => {
      showSuccessNotification.value = false
    }, 2000)
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  }
}

onMounted(() => {
  fetchMessages()
  if (route.query.userId) {
    openComposeDialog(route.query.userId, route.query.userName)
  }
})

watch(() => route.query.userId, (userId) => {
  if (userId) {
    openComposeDialog(userId, route.query.userName)
  }
})
</script>

<style scoped>
.messages-page {
  display: grid;
  gap: 1.25rem;
}

.hero-count {
  display: grid;
  gap: 0.2rem;
  padding: 1.2rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.hero-count strong {
  color: var(--kumo-bg-brand-strong);
  font-size: 3.4rem;
  font-weight: 900;
  line-height: 1;
}

.hero-count span {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.messages-workspace {
  display: grid;
  grid-template-columns: 18rem minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.message-sidebar,
.message-content {
  display: grid;
  gap: 0.7rem;
  padding: 1rem;
}

.message-sidebar {
  position: sticky;
  top: 6rem;
}

.type-button {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
  min-height: 3rem;
  padding: 0.7rem 0.8rem;
  border: 1px solid transparent;
  border-radius: var(--kumo-radius-md);
  background: transparent;
  color: var(--kumo-text-muted);
  font-weight: 760;
  text-align: left;
  cursor: pointer;
  transition: var(--transition);
}

.type-button:hover,
.type-button.active {
  border-color: var(--kumo-hairline);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.type-button strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  height: 1.5rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  background: var(--kumo-status-danger);
  color: var(--kumo-text-inverse);
  font-size: 0.76rem;
}

.mark-read {
  margin-top: 0.35rem;
  border-top: 1px solid var(--kumo-hairline);
}

.content-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.5rem;
}

.content-heading h2 {
  margin: 0.45rem 0 0;
  font-size: 1.8rem;
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.8rem;
  min-height: 20rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel > svg {
  color: var(--kumo-bg-brand);
  font-size: 2.25rem;
}

.state-panel.error {
  color: var(--kumo-status-danger);
}

.message-list {
  display: grid;
  gap: 0.8rem;
}

.message-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.message-item.unread {
  border-color: var(--kumo-hairline-strong);
  background:
    linear-gradient(135deg, var(--kumo-bg-brand-soft), transparent 58%),
    var(--kumo-bg-base);
}

.message-avatar,
.reply-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 850;
}

.message-avatar {
  width: 2.75rem;
  height: 2.75rem;
}

.reply-avatar {
  width: 2rem;
  height: 2rem;
}

.message-avatar img,
.reply-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-main {
  display: grid;
  gap: 0.75rem;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.message-header div {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.message-header strong {
  color: var(--kumo-text-default);
  font-weight: 840;
}

.message-header time {
  color: var(--kumo-text-subtle);
  font-size: 0.82rem;
  font-weight: 720;
}

.message-body {
  color: var(--kumo-text-muted);
  line-height: 1.65;
}

.message-body :deep(a) {
  color: var(--kumo-bg-brand-strong);
  font-weight: 760;
  text-decoration: none;
}

.message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.message-actions .kumo-button {
  min-height: 2.25rem;
  padding: 0.5rem 0.8rem;
}

.message-replies {
  display: grid;
  gap: 0.65rem;
  padding: 0.75rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-subtle);
}

.replies-header {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--kumo-text-muted);
  font-weight: 760;
}

.reply-list {
  display: grid;
  gap: 0.65rem;
}

.reply-item {
  display: flex;
  gap: 0.65rem;
}

.reply-copy {
  display: grid;
  min-width: 0;
  gap: 0.15rem;
}

.reply-copy strong {
  color: var(--kumo-text-default);
  font-size: 0.9rem;
}

.reply-copy small {
  color: var(--kumo-text-subtle);
}

.reply-copy em {
  color: var(--kumo-text-muted);
  font-style: normal;
  line-height: 1.5;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding-top: 0.6rem;
}

.pagination span {
  color: var(--kumo-text-muted);
  font-weight: 760;
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: var(--kumo-bg-overlay);
  backdrop-filter: blur(8px);
}

.dialog-panel {
  display: grid;
  width: min(100%, 34rem);
  max-height: min(88vh, 44rem);
  overflow: hidden;
}

.dialog-header,
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid var(--kumo-hairline);
}

.dialog-footer {
  border-top: 1px solid var(--kumo-hairline);
  border-bottom: 0;
}

.dialog-header h3 {
  margin: 0;
  color: var(--kumo-text-default);
  font-weight: 850;
}

.dialog-body {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
  overflow-y: auto;
}

.dialog-textarea {
  width: 100%;
  resize: vertical;
  padding: 0.85rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font: inherit;
}

.dialog-textarea:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.recipient-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  width: fit-content;
  padding: 0.45rem 0.7rem;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 760;
}

.original-message {
  display: grid;
  gap: 0.45rem;
  padding: 0.85rem;
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
}

.dialog-footer > span {
  color: var(--kumo-text-subtle);
  font-size: 0.84rem;
  font-weight: 720;
}

.dialog-footer > div {
  display: flex;
  gap: 0.6rem;
}

.success-toast {
  position: fixed;
  top: 5.5rem;
  right: 1rem;
  z-index: 1100;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.75rem 1rem;
  color: var(--kumo-status-success);
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 920px) {
  .messages-workspace {
    grid-template-columns: 1fr;
  }

  .message-sidebar {
    position: static;
  }
}

@media (max-width: 620px) {
  .content-heading,
  .message-header,
  .dialog-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .message-item {
    grid-template-columns: 1fr;
  }

  .dialog-footer > div {
    flex-direction: column;
  }
}
</style>
