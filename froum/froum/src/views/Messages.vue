<template>
  <div class="messages-page">
    <div class="page-header">
      <h1>消息中心</h1>
      <button class="compose-btn" @click="openComposeDialog()">
        <font-awesome-icon :icon="['fas', 'paper-plane']" />
        <span>写私信</span>
      </button>
    </div>

    <div class="messages-container">
      <div class="messages-sidebar">
        <div class="message-types">
          <div 
            class="message-type" 
            :class="{ active: activeType === 'article' }"
            @click="activeType = 'article'"
          >
            <font-awesome-icon :icon="['fas', 'file-alt']" />
            <span>文章/问答被关注通知</span>
            <span v-if="getUnreadCount('article') > 0" class="unread-badge">{{ getUnreadCount('article') }}</span>
          </div>
          <div 
            class="message-type" 
            :class="{ active: activeType === 'comment' }"
            @click="activeType = 'comment'"
          >
            <font-awesome-icon :icon="['fas', 'comment']" />
            <span>文章/问答被评论通知</span>
            <span v-if="getUnreadCount('comment') > 0" class="unread-badge">{{ getUnreadCount('comment') }}</span>
          </div>
          <div 
            class="message-type" 
            :class="{ active: activeType === 'follow' }"
            @click="activeType = 'follow'"
          >
            <font-awesome-icon :icon="['fas', 'user']" />
            <span>个人被关注通知</span>
            <span v-if="getUnreadCount('follow') > 0" class="unread-badge">{{ getUnreadCount('follow') }}</span>
          </div>
          <div 
            class="message-type" 
            :class="{ active: activeType === 'system' }"
            @click="activeType = 'system'"
          >
            <font-awesome-icon :icon="['fas', 'bell']" />
            <span>站内信通知</span>
            <span v-if="getUnreadCount('system') > 0" class="unread-badge">{{ getUnreadCount('system') }}</span>
          </div>
          <div 
            class="message-type" 
            @click="markAllAsRead"
          >
            <font-awesome-icon :icon="['fas', 'cog']" />
            <span>全部标记为已读</span>
          </div>
        </div>
      </div>

      <div class="messages-content">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="errorMessage" class="error-state">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="empty-icon" />
          <p>{{ errorMessage }}</p>
          <button class="retry-btn" @click="fetchMessages">重新加载</button>
        </div>
        
        <div v-else-if="messages.length === 0" class="empty-state">
          <font-awesome-icon :icon="['fas', 'inbox']" class="empty-icon" />
          <p>暂无{{ messageTypeLabel }}消息</p>
        </div>
        
        <div v-else class="message-list">
          <div 
            v-for="message in messages" 
            :key="message.id" 
            class="message-item"
            :class="{ unread: !message.read }"
          >
            <div class="message-avatar">
              <img v-if="message.sender?.avatar" :src="message.sender.avatar" :alt="message.sender?.name || '用户'" />
              <div v-else class="default-avatar">
                <span>{{ getInitials(message.sender?.name || '用户') }}</span>
              </div>
            </div>
            
            <div class="message-content">
              <div class="message-header">
                <span class="sender-name">{{ message.sender?.name || '系统' }}</span>
                <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              </div>
              
              <div class="message-body" v-html="message.content"></div>
              
              <!-- 显示回复列表 -->
              <div v-if="message.replies && message.replies.length > 0" class="message-replies">
                <div class="replies-header">
                  <font-awesome-icon :icon="['fas', 'reply-all']" />
                  <span>回复 ({{ message.replies.length }})</span>
                </div>
                
                <div class="reply-list">
                  <div v-for="reply in message.replies" :key="reply.id" class="reply-item">
                    <div class="reply-avatar">
                      <img v-if="reply.sender?.avatar" :src="reply.sender.avatar" :alt="reply.sender?.name || '用户'" />
                      <div v-else class="default-avatar small">
                        <span>{{ getInitials(reply.sender?.name || '用户') }}</span>
                      </div>
                    </div>
                    
                    <div class="reply-content">
                      <div class="reply-header">
                        <span class="reply-sender">{{ reply.sender?.name || '当前用户' }}</span>
                        <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                      </div>
                      
                      <div class="reply-text">{{ reply.content }}</div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="message-actions" v-if="message.actionType">
                <button 
                  v-if="message.actionType === 'follow'" 
                  class="action-button follow"
                  :class="{ active: message.isFollowing }"
                  @click="handleAction(message)"
                  @mouseenter="handleButtonHover(message.id, 'follow')"
                  @mouseleave="handleButtonLeave"
                >
                  <font-awesome-icon :icon="['fas', message.isFollowing ? 'user-check' : 'user-plus']" />
                  <span>{{ message.isFollowing ? '已关注' : '关注' }}</span>
                </button>
                
                <button 
                  v-if="message.actionType === 'reply'" 
                  class="action-button reply"
                  :class="{ hover: hoverButton === `${message.id}-reply` }"
                  @click="showReplyDialog(message)"
                  @mouseenter="handleButtonHover(message.id, 'reply')"
                  @mouseleave="handleButtonLeave"
                >
                  <font-awesome-icon :icon="['fas', 'reply']" />
                  <span>回复</span>
                </button>
                
                <button 
                  v-if="message.actionType === 'view'" 
                  class="action-button view"
                  :class="{ hover: hoverButton === `${message.id}-view` }"
                  @click="handleAction(message)"
                  @mouseenter="handleButtonHover(message.id, 'view')"
                  @mouseleave="handleButtonLeave"
                >
                  <font-awesome-icon :icon="['fas', 'eye']" />
                  <span>查看</span>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div class="pagination" v-if="totalPages > 1">
          <button 
            class="pagination-btn prev" 
            :disabled="currentPage === 1"
            @click="changePage(currentPage - 1)"
          >
            <font-awesome-icon :icon="['fas', 'chevron-left']" />
          </button>
          
          <div class="pagination-info">
            {{ currentPage }} / {{ totalPages }}
          </div>
          
          <button 
            class="pagination-btn next" 
            :disabled="currentPage === totalPages"
            @click="changePage(currentPage + 1)"
          >
            <font-awesome-icon :icon="['fas', 'chevron-right']" />
          </button>
        </div>
      </div>
    </div>

    <!-- 私信发送对话框 -->
    <div class="reply-dialog" v-if="showCompose">
      <div class="reply-dialog-content">
        <div class="reply-dialog-header">
          <h3>发送站内私信</h3>
          <button class="close-btn" @click="closeComposeDialog">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
        
        <div class="reply-dialog-body">
          <div class="reply-input">
            <div v-if="composeUserName" class="recipient-chip">
              <font-awesome-icon :icon="['fas', 'user']" />
              <span>发送给 <strong>{{ composeUserName }}</strong></span>
            </div>
            <input
              v-else
              v-model="composeUserId"
              class="recipient-input"
              type="number"
              min="1"
              placeholder="接收用户ID"
            />
            <textarea
              v-model="composeContent"
              placeholder="请输入私信内容..."
              rows="4"
              @keydown.ctrl.enter="submitCompose"
            ></textarea>
          </div>
        </div>
        
        <div class="reply-dialog-footer">
          <div class="reply-tips">
            提示: 按 Ctrl+Enter 快速发送
          </div>
          <div class="reply-actions">
            <button class="cancel-btn" @click="closeComposeDialog">取消</button>
            <button 
              class="submit-btn" 
              :disabled="!composeUserId || !composeContent.trim()" 
              @click="submitCompose"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 回复对话框 -->
    <div class="reply-dialog" v-if="showReply">
      <div class="reply-dialog-content">
        <div class="reply-dialog-header">
          <h3>回复 {{ currentMessage?.sender?.name }}</h3>
          <button class="close-btn" @click="closeReplyDialog">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
        
        <div class="reply-dialog-body">
          <div class="original-message">
            <p><strong>原消息:</strong></p>
            <div v-html="currentMessage?.content"></div>
          </div>
          
          <div class="reply-input">
            <textarea 
              v-model="replyContent" 
              placeholder="请输入回复内容..." 
              rows="4"
              @keydown.ctrl.enter="submitReply"
            ></textarea>
          </div>
        </div>
        
        <div class="reply-dialog-footer">
          <div class="reply-tips">
            提示: 按 Ctrl+Enter 快速发送
          </div>
          <div class="reply-actions">
            <button class="cancel-btn" @click="closeReplyDialog">取消</button>
            <button 
              class="submit-btn" 
              :disabled="!replyContent.trim()" 
              @click="submitReply"
            >
              发送回复
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 成功提示 -->
    <div class="success-notification" v-if="showSuccessNotification">
      <div class="notification-content">
        <font-awesome-icon :icon="['fas', 'check-circle']" />
        <span>回复成功！</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { notificationService } from '../services/notificationService';

// 添加路由器
const route = useRoute();
const router = useRouter();
const pageSize = 10;

// 状态
const activeType = ref('article');
const loading = ref(true);
const messages = ref([]);
const allMessages = ref([]);
const errorMessage = ref('');
const currentPage = ref(1);
const totalPages = ref(1);
const hoverButton = ref(null); // 跟踪当前鼠标悬停的按钮
const unreadCounts = ref({
  article: 0,
  comment: 0,
  follow: 0,
  system: 0
});

// 回复对话框状态
const showReply = ref(false);
const currentMessage = ref(null);
const replyContent = ref('');
const showCompose = ref(false);
const composeUserId = ref('');
const composeUserName = ref('');
const composeContent = ref('');

// 成功提示状态
const showSuccessNotification = ref(false);

// 消息类型标签映射
const typeLabels = {
  article: '文章/问答被关注',
  comment: '文章/问答被评论',
  follow: '个人被关注',
  system: '站内信'
};

// 计算属性
const messageTypeLabel = computed(() => typeLabels[activeType.value] || '');

// 获取指定类型的未读消息数量
const getUnreadCount = (type) => {
  return unreadCounts.value[type] || 0;
};

// 监听消息类型变化
watch(activeType, () => {
  currentPage.value = 1;
  applyMessageFilters();
});

// 获取消息
const fetchMessages = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await notificationService.getUserNotifications({
      page: 1,
      pageSize: 100
    });
    allMessages.value = response.data.content.map(normalizeNotification);
    syncUnreadCounts();
    applyMessageFilters();
  } catch (error) {
    console.error('获取消息失败:', error);
    errorMessage.value = error.message || '消息加载失败';
    messages.value = [];
    totalPages.value = 1;
  } finally {
    loading.value = false;
  }
};

// 切换页码
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
  applyMessageFilters();
};

const applyMessageFilters = () => {
  const filteredMessages = allMessages.value.filter(message => message.category === activeType.value);
  totalPages.value = Math.max(Math.ceil(filteredMessages.length / pageSize), 1);

  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }

  const start = (currentPage.value - 1) * pageSize;
  messages.value = filteredMessages.slice(start, start + pageSize);
};

const syncUnreadCounts = () => {
  unreadCounts.value = allMessages.value.reduce((counts, message) => {
    if (!message.read && counts[message.category] !== undefined) {
      counts[message.category] += 1;
    }
    return counts;
  }, {
    article: 0,
    comment: 0,
    follow: 0,
    system: 0
  });
  // 通知全局（顶栏未读红点）刷新
  window.dispatchEvent(new Event('notifications-updated'));
};

// 兼容后端 LocalDateTime（数组 [年,月,日,时,分,秒]）/字符串/时间戳
const toDate = (value) => {
  if (!value) return null;
  const date = Array.isArray(value)
    ? new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0)
    : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const normalizeNotification = (notification) => {  const fromUser = notification.fromUser || {};
  const type = notification.type || 'SYSTEM';
  const category = resolveMessageCategory(type);
  const targetType = resolveTargetType(type);
  const senderName = fromUser.nickname || fromUser.username || fromUser.name || '系统';

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
  };
};

const resolveMessageCategory = (type) => {
  if (type === 'USER_FOLLOW') return 'follow';
  if (type.includes('COMMENT')) return 'comment';
  if (type.includes('FOLLOW') || type.includes('LIKE')) return 'article';
  return 'system';
};

const resolveTargetType = (type) => {
  if (type === 'DIRECT_MESSAGE') return 'user';
  if (type.startsWith('ARTICLE_')) return 'article';
  if (type.startsWith('QUESTION_')) return 'question';
  if (type === 'USER_FOLLOW') return 'user';
  return null;
};

// 格式化时间
const formatTime = (date) => {
  if (!date) return '';
  
  const now = new Date();
  const diff = now - date;
  
  // 小于1小时
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000);
    return `${minutes}分钟前`;
  }
  
  // 小于24小时
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000);
    return `${hours}小时前`;
  }
  
  // 小于30天
  if (diff < 2592000000) {
    const days = Math.floor(diff / 86400000);
    return `${days}天前`;
  }
  
  // 大于30天，显示具体日期
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
};

// 获取名字首字母
const getInitials = (name) => {
  if (!name) return '用';
  return name.charAt(0).toUpperCase();
};

// 处理按钮鼠标悬停
const handleButtonHover = (messageId, actionType) => {
  hoverButton.value = `${messageId}-${actionType}`;
};

// 处理按钮鼠标离开
const handleButtonLeave = () => {
  hoverButton.value = null;
};

// 处理按钮点击事件
const markMessageAsRead = async (message) => {
  if (message.read) return;

  await notificationService.markNotificationAsRead(message.id);
  message.read = true;

  const sourceMessage = allMessages.value.find(item => item.id === message.id);
  if (sourceMessage) {
    sourceMessage.read = true;
  }
  syncUnreadCounts();
};

const handleAction = async (message) => {
  try {
    await markMessageAsRead(message);

    if (message.actionType !== 'view' || !message.targetId) {
      return;
    }

    if (message.targetType === 'article') {
      router.push(`/article/${message.targetId}`);
    } else if (message.targetType === 'question') {
      router.push(`/question/${message.targetId}`);
    } else if (message.targetType === 'user') {
      router.push(`/user/${message.targetId}`);
    }
  } catch (error) {
    console.error('处理消息操作失败:', error);
    ElMessage.error(error.message || '操作失败');
  }
};

// 显示回复对话框
const showReplyDialog = (message) => {
  // 标记为已读
  markMessageAsRead(message);
  
  // 设置当前消息
  currentMessage.value = message;
  replyContent.value = '';
  showReply.value = true;
};

// 关闭回复对话框
const closeReplyDialog = () => {
  showReply.value = false;
  currentMessage.value = null;
  replyContent.value = '';
};

const openComposeDialog = (userId = route.query.userId, userName = route.query.userName) => {
  composeUserId.value = userId ? String(userId) : '';
  composeUserName.value = userName ? String(userName) : '';
  composeContent.value = '';
  showCompose.value = true;
};

const closeComposeDialog = () => {
  showCompose.value = false;
  composeUserId.value = '';
  composeUserName.value = '';
  composeContent.value = '';
};

const submitCompose = async () => {
  if (!composeUserId.value || !composeContent.value.trim()) return;

  try {
    await notificationService.sendDirectMessage(Number(composeUserId.value), composeContent.value.trim());
    ElMessage.success('私信已发送');
    closeComposeDialog();
  } catch (error) {
    console.error('发送私信失败:', error);
    ElMessage.error(error.message || '发送私信失败');
  }
};

const markAllAsRead = async () => {
  if (!allMessages.value.length) return;

  try {
    loading.value = true;
    await notificationService.markAllNotificationsAsRead();
    allMessages.value = allMessages.value.map(message => ({
      ...message,
      read: true
    }));
    syncUnreadCounts();
    applyMessageFilters();
    ElMessage.success('已全部标记为已读');
  } catch (error) {
    console.error('全部标记为已读失败:', error);
    ElMessage.error(error.message || '操作失败');
  } finally {
    loading.value = false;
  }
};

// 提交回复
const submitReply = async () => {
  if (!replyContent.value.trim() || !currentMessage.value) return;

  const receiverId = currentMessage.value.sender?.id;
  if (!receiverId) {
    ElMessage.warning('系统通知不能回复');
    return;
  }

  try {
    await notificationService.sendDirectMessage(receiverId, replyContent.value.trim());
    closeReplyDialog();
    showSuccessNotification.value = true;
    setTimeout(() => {
      showSuccessNotification.value = false;
    }, 2000);
  } catch (error) {
    console.error('回复私信失败:', error);
    ElMessage.error(error.message || '回复失败');
  }
};

// 组件挂载时加载消息
onMounted(() => {
  fetchMessages();
  if (route.query.userId) {
    openComposeDialog(route.query.userId, route.query.userName);
  }
});

watch(() => route.query.userId, (userId) => {
  if (userId) {
    openComposeDialog(userId, route.query.userName);
  }
});
</script>

<style scoped>
.messages-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
}

.page-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.compose-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: 4px;
  background: #1890ff;
  color: #fff;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 500;
}

.compose-btn:hover {
  background: #40a9ff;
}

.messages-container {
  display: flex;
  gap: 20px;
}

.messages-sidebar {
  flex: 0 0 250px;
  border-right: 1px solid #eee;
}

.message-types {
  display: flex;
  flex-direction: column;
}

.message-type {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 5px;
  position: relative;
}

.message-type:hover {
  background-color: #f5f5f5;
}

.message-type.active {
  background-color: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

.message-type svg {
  margin-right: 10px;
  width: 16px;
}

.unread-badge {
  position: absolute;
  right: 15px;
  background-color: #ff4d4f;
  color: white;
  font-size: 12px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.messages-content {
  flex: 1;
  min-height: 500px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #999;
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #cf1322;
}

.retry-btn {
  margin-top: 12px;
  padding: 6px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
  color: #333;
  cursor: pointer;
}

.retry-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
  color: #ccc;
}

.message-list {
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #eee;
  position: relative;
}

.message-item.unread {
  background-color: #f0f9ff;
}

.message-avatar {
  flex: 0 0 40px;
  margin-right: 15px;
}

.message-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.default-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.message-content {
  flex: 1;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.sender-name {
  font-weight: 500;
  color: #333;
}

.message-time {
  color: #999;
  font-size: 12px;
}

.message-body {
  margin-bottom: 10px;
  line-height: 1.5;
}

.message-body a {
  color: #1890ff;
  text-decoration: none;
}

.message-body a:hover {
  text-decoration: underline;
}

.message-actions {
  display: flex;
  gap: 10px;
}

.action-button {
  padding: 5px 15px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.action-button svg {
  margin-right: 5px;
  width: 12px;
}

.action-button.view {
  color: #333;
  border-color: #d9d9d9;
}

.action-button.view.hover {
  color: #1890ff;
  border-color: #1890ff;
  background-color: #e6f7ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 5px rgba(24, 144, 255, 0.15);
}

.action-button.reply {
  color: #333;
  border-color: #d9d9d9;
}

.action-button.reply.hover {
  color: #52c41a;
  border-color: #52c41a;
  background-color: #f6ffed;
  transform: translateY(-2px);
  box-shadow: 0 2px 5px rgba(82, 196, 26, 0.15);
}

.action-button.follow {
  color: #333;
  border-color: #d9d9d9;
}

.action-button.follow:hover {
  color: #722ed1;
  border-color: #722ed1;
  background-color: #f9f0ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 5px rgba(114, 46, 209, 0.15);
}

.action-button.follow.active {
  color: #722ed1;
  border-color: #722ed1;
  background-color: #f9f0ff;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}

.pagination-btn {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.pagination-btn:disabled {
  color: #d9d9d9;
  cursor: not-allowed;
}

.pagination-btn:not(:disabled):hover {
  border-color: #1890ff;
  color: #1890ff;
}

.pagination-info {
  margin: 0 10px;
  color: #666;
}

/* 回复对话框样式 */
.reply-dialog {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.reply-dialog-content {
  width: 500px;
  max-width: 90%;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

.reply-dialog-header {
  padding: 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reply-dialog-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.reply-dialog-body {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

.original-message {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f9f9f9;
  border-radius: 4px;
  border-left: 3px solid #1890ff;
}

.original-message p {
  margin-top: 0;
  margin-bottom: 8px;
}

.recipient-input,
.reply-input textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-family: inherit;
  font-size: 14px;
  transition: all 0.2s;
}

.recipient-input {
  margin-bottom: 12px;
}

.recipient-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 14px;
  border-radius: 999px;
  background: var(--primary-light, #eef3ff);
  color: var(--primary-color, #4169d8);
  font-size: 0.9rem;
}

.recipient-chip strong {
  font-weight: 700;
}

.reply-input textarea {
  resize: vertical;
}

.recipient-input:focus,
.reply-input textarea:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.reply-dialog-footer {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reply-tips {
  color: #999;
  font-size: 12px;
}

.reply-actions {
  display: flex;
  gap: 8px;
}

.cancel-btn {
  padding: 6px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: white;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.submit-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  background-color: #1890ff;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover {
  background-color: #40a9ff;
}

.submit-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

/* 回复列表样式 */
.message-replies {
  margin-top: 10px;
  margin-bottom: 15px;
  background-color: #f9f9f9;
  border-radius: 6px;
  padding: 10px;
  border-left: 3px solid #1890ff;
}

.replies-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.replies-header svg {
  margin-right: 6px;
  font-size: 12px;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  display: flex;
  padding: 8px;
  border-bottom: 1px dashed #eee;
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-avatar {
  flex: 0 0 30px;
  margin-right: 10px;
}

.reply-avatar img {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
}

.default-avatar.small {
  width: 30px;
  height: 30px;
  font-size: 12px;
}

.reply-content {
  flex: 1;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.reply-sender {
  font-weight: 500;
  font-size: 14px;
  color: #333;
}

.reply-time {
  color: #999;
  font-size: 12px;
}

.reply-text {
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  word-break: break-word;
}

@media (max-width: 768px) {
  .messages-container {
    flex-direction: column;
  }
  
  .messages-sidebar {
    flex: none;
    border-right: none;
    border-bottom: 1px solid #eee;
    padding-bottom: 10px;
    margin-bottom: 10px;
  }
  
  .message-types {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .message-type {
    padding: 8px 12px;
    font-size: 14px;
  }
  
  .reply-dialog-content {
    width: 95%;
  }
}

/* 成功提示样式 */
.success-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1100;
  animation: slideIn 0.3s forwards, fadeOut 0.3s 2.7s forwards;
}

.notification-content {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.notification-content svg {
  color: #52c41a;
  font-size: 16px;
  margin-right: 8px;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}
</style> 
