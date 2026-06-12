<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import ArticleComments from './ArticleComments.vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { articleService } from '../services/articleService'

const store = useStore()
const router = useRouter()

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

// Configure marked with syntax highlighting
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {}
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const article = ref(null)
const loading = ref(true)
const error = ref('')

// Computed properties
const formattedContent = computed(() => {
  return article.value?.content ? marked.parse(article.value.content) : ''
})

const commentCount = computed(() => {
  return article.value?.comments?.length || 0
})

// 检查用户是否已登录
const isLoggedIn = computed(() => store.state.isAuthenticated && store.state.user)

const fetchArticle = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await articleService.getArticleById(props.id)
    article.value = response.data
    article.value.comments = await articleService.getArticleComments(props.id)
  } catch (err) {
    console.error('获取文章详情失败:', err)
    error.value = err.message || '获取文章详情失败'
  } finally {
    loading.value = false
  }
}

const refreshComments = async () => {
  if (!article.value?.id) return
  article.value.comments = await articleService.getArticleComments(article.value.id)
}

const likeArticle = async () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }

  try {
    console.log('Toggling like for article:', article.value.id)
    // 使用新的点赞API
    const result = await store.dispatch('toggleLike', {
      targetType: 'ARTICLE',
      targetId: article.value.id
    })

    if (result.success) {
      // 更新文章点赞数
      if (result.isLiked) {
        article.value.likes = (article.value.likes || 0) + 1
      } else {
        article.value.likes = Math.max(0, (article.value.likes || 0) - 1)
      }
      showToast(result.message)
    } else {
      showToast(result.message || '操作失败', 'error')
    }
  } catch (error) {
    console.error('Error toggling like:', error)
    showToast(error.message || '操作失败，请稍后重试', 'error')
  }
}

const shareArticle = () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }
  
  // 创建分享URL
  const shareUrl = window.location.href
  
  // 复制链接到剪贴板
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      showToast('链接已复制到剪贴板!')
    })
    .catch(err => {
      console.error('Could not copy text: ', err)
      showToast('复制链接失败', 'error')
    })
}

const reportArticle = () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }
  
  // 这里可以实现举报逻辑，例如打开一个模态框
  showReportModal.value = true
}

const followAuthor = async () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }

  try {
    // 获取作者ID
    const authorId = article.value.author.id

    // 使用新的关注API
    const result = await store.dispatch('toggleFollowTarget', {
      targetType: 'USER',
      targetId: authorId
    })

    if (result && result.success) {
      article.value.author.isFollowing = result.isFollowed
      showToast(result.message)
    } else {
      showToast(result.message || '操作失败', 'error')
    }
  } catch (error) {
    console.error('Error following author:', error)
    showToast(error.message || '操作失败，请稍后重试', 'error')
  }
}

// 简单的消息提示
const toast = ref({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  toast.value = {
    show: true,
    message,
    type
  }
  
  // 3秒后隐藏
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// 举报模态框
const showReportModal = ref(false)
const reportReason = ref('')
const reportReasons = [
  '垃圾内容',
  '违法或违规内容',
  '侵犯版权',
  '不友善或攻击性内容',
  '其他原因'
]

const submitReport = async () => {
  if (!reportReason.value) {
    showToast('请选择举报原因', 'error')
    return
  }

  showToast('当前后端暂未提供举报接口', 'error')
}

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

// Add a handler for the comment events
const handleAddComment = async ({ articleId, content, parentId = null }) => {
  if (article.value.id === articleId) {
    try {
      await articleService.addComment(articleId, { content, parentId });
      await refreshComments();
      showToast('评论已发布');
    } catch (err) {
      console.error('发表评论失败:', err);
      showToast(err.message || '发表评论失败', 'error');
    }
  }
}

const handleLikeComment = async ({ articleId, commentId }) => {
  if (article.value.id === articleId) {
    try {
      await articleService.likeComment(articleId, commentId);
      const comment = article.value.comments.find(c => c.id === commentId);
      if (comment) {
        comment.likes++;
      }
    } catch (err) {
      console.error('点赞评论失败:', err);
      showToast(err.message || '点赞评论失败', 'error');
    }
  }
}

const goToLogin = () => {
  router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
}

// 导航到作者页面
const goToAuthorProfile = () => {
  const author = article.value.author;
  console.log('Attempting to navigate to author profile:', author);
  
  if (!author || author.id === undefined || author.id === null) {
    console.error('Cannot navigate: Author data is invalid', author);
    showToast('无法找到作者信息', 'error');
    return;
  }
  
  // 确保ID是字符串
  const userId = String(author.id);
  console.log('Navigating to author profile with string ID:', userId);
  
  // 使用命名路由
  router.push({
    name: 'UserProfile',
    params: { id: userId }
  }).catch(err => {
    console.error('Navigation error:', err);
    showToast('页面导航错误，请稍后重试', 'error');
  });
}

// 获取作者首字母
const getAuthorInitials = (name) => {
  if (!name) return '匿';
  return name.charAt(0).toUpperCase();
};

// 确保头像路径正确
const getAvatarPath = (avatar) => {
  if (!avatar) return '';
  if (avatar.startsWith('/images/')) return avatar;
  if (avatar.startsWith('/avatar')) {
    return `/images/avatars${avatar}`;
  }
  return avatar;
};

onMounted(() => {
  fetchArticle()
})
</script>

<template>
  <div class="article-detail">
    <div v-if="loading" class="article-state">加载中...</div>
    <div v-else-if="error" class="article-state error">{{ error }}</div>
    <template v-else-if="article">
    <!-- 文章头部 -->
    <div class="article-header">
      <h1 class="article-title">{{ article.title }}</h1>
      
      <div class="article-meta">
        <div class="author-info" @click="goToAuthorProfile">
          <div class="author-avatar">
            <img v-if="article.author.avatar" :src="getAvatarPath(article.author.avatar)" :alt="article.author.name">
            <span v-else>{{ getAuthorInitials(article.author.name) }}</span>
          </div>
          <span class="author-name">{{ article.author.name }}</span>
          <span class="publish-time">
            <font-awesome-icon :icon="['fas', 'calendar-alt']" />
            {{ formatDate(article.createTime) }}
          </span>
        </div>
        
        <div class="article-stats">
          <span class="stat-item">
            <font-awesome-icon :icon="['fas', 'eye']" />
            {{ article.views }} 阅读
          </span>
          <span class="stat-item">
            <font-awesome-icon :icon="['fas', 'comment']" />
            {{ commentCount }} 评论
          </span>
          <span class="stat-item">
            <font-awesome-icon :icon="['fas', 'thumbs-up']" />
            {{ article.likes }} 喜欢
          </span>
        </div>
      </div>
      
      <div class="article-tags">
        <span class="category">{{ article.category }}</span>
        <span v-for="tag in article.tags" :key="tag" class="tag">
          <font-awesome-icon :icon="['fas', 'tag']" class="tag-icon" />
          {{ tag }}
        </span>
      </div>
    </div>

    <!-- 文章内容 -->
    <div class="article-content markdown-body" v-html="formattedContent"></div>

    <!-- 文章操作 -->
    <div class="article-actions">
      <button class="action-btn like-btn" @click="likeArticle">
        <font-awesome-icon :icon="['fas', 'heart']" />
        <span>喜欢文章</span>
      </button>
      <button class="action-btn share-btn" @click="shareArticle">
        <font-awesome-icon :icon="['fas', 'share']" />
        <span>分享文章</span>
      </button>
      <button class="action-btn report-btn" @click="reportArticle">
        <font-awesome-icon :icon="['fas', 'flag']" />
        <span>举报</span>
      </button>
    </div>

    <!-- 作者信息 -->
    <div class="article-author">
      <div class="author-card">
        <div class="author-avatar large" @click="goToAuthorProfile">
          <img v-if="article.author.avatar" :src="getAvatarPath(article.author.avatar)" :alt="article.author.name">
          <span v-else>{{ getAuthorInitials(article.author.name) }}</span>
        </div>
        <div class="author-info">
          <h3 class="author-name" @click="goToAuthorProfile">{{ article.author.name }}</h3>
          <p class="author-bio">资深前端开发工程师</p>
        </div>
        <button class="follow-btn" @click="followAuthor">
          <font-awesome-icon :icon="['fas', 'heart']" />
          <span>关注作者</span>
        </button>
      </div>
    </div>

    <!-- 评论区域 -->
    <article-comments 
      :comments="article.comments" 
      :article-id="article.id"
      @add-comment="handleAddComment"
      @like-comment="handleLikeComment"
    />
    </template>
  </div>
  
  <!-- Toast消息提示 -->
  <div class="toast-container" v-if="toast.show">
    <div class="toast" :class="toast.type">
      <span class="toast-message">{{ toast.message }}</span>
    </div>
  </div>
  
  <!-- 举报模态框 -->
  <div class="modal-overlay" v-if="showReportModal" @click="showReportModal = false">
    <div class="modal-content" @click.stop>
      <h3 class="modal-title">举报文章</h3>
      <p class="modal-subtitle">请选择举报原因</p>
      
      <div class="reason-list">
        <div 
          v-for="(reason, index) in reportReasons" 
          :key="index"
          class="reason-item"
          :class="{ active: reportReason === reason }"
          @click="reportReason = reason"
        >
          <div class="reason-radio">
            <div class="radio-inner" v-if="reportReason === reason"></div>
          </div>
          <span>{{ reason }}</span>
        </div>
      </div>
      
      <div class="modal-actions">
        <button class="cancel-btn" @click="showReportModal = false">取消</button>
        <button class="submit-btn" @click="submitReport">提交</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.article-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem 1rem;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.article-state {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-light);
}

.article-state.error {
  color: var(--error-color);
}

.article-header {
  margin-bottom: 2rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 1.5rem;
}

.article-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 1rem;
  line-height: 1.3;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  color: var(--text-color);
  transition: var(--transition);
}

.author-info:hover .author-name {
  color: var(--primary-color);
}

.author-avatar-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.author-avatar-link:hover .author-avatar {
  transform: scale(1.05);
  box-shadow: 0 0 0 2px var(--primary-color);
}

.author-name-link {
  text-decoration: none;
  color: inherit;
}

.author-name-link:hover .author-name {
  color: var(--primary-color);
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1.1rem;
  overflow: hidden;
  transition: var(--transition);
  cursor: pointer;
}

.author-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 0 0 2px var(--primary-color);
}

.author-avatar.large {
  width: 60px;
  height: 60px;
  font-size: 1.5rem;
}

.author-avatar span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.author-name {
  font-weight: 600;
  color: var(--text-color);
  cursor: pointer;
  transition: var(--transition);
}

.author-name:hover {
  color: var(--primary-color);
}

.publish-time {
  color: var(--text-lighter);
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.article-stats {
  display: flex;
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  color: var(--text-lighter);
  font-size: 0.9rem;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.category {
  background-color: var(--primary-color);
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-size: 0.8rem;
  font-weight: 500;
}

.tag {
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-size: 0.8rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.tag-icon {
  font-size: 0.7rem;
}

.article-content {
  font-size: 1.05rem;
  line-height: 1.7;
  color: var(--text-color);
  margin-bottom: 2rem;
}

/* Markdown styling */
:deep(.markdown-body) {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  color: var(--text-color);
  line-height: 1.7;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4),
:deep(.markdown-body h5),
:deep(.markdown-body h6) {
  margin-top: 1.5em;
  margin-bottom: 0.75em;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-color);
}

:deep(.markdown-body h1) {
  font-size: 2em;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.3em;
}

:deep(.markdown-body h2) {
  font-size: 1.5em;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.3em;
}

:deep(.markdown-body h3) {
  font-size: 1.25em;
}

:deep(.markdown-body p),
:deep(.markdown-body blockquote),
:deep(.markdown-body ul),
:deep(.markdown-body ol),
:deep(.markdown-body dl),
:deep(.markdown-body table),
:deep(.markdown-body pre) {
  margin-bottom: 1em;
}

:deep(.markdown-body code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 0.85em;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-family: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace;
}

:deep(.markdown-body pre) {
  padding: 1em;
  overflow: auto;
  background-color: #f6f8fa;
  border-radius: var(--radius);
  font-family: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace;
}

:deep(.markdown-body pre code) {
  padding: 0;
  background-color: transparent;
  font-size: 0.9em;
}

.article-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  padding: 1.5rem 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 2rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius);
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  background-color: transparent;
}

.like-btn {
  color: var(--error-color);
}

.like-btn:hover {
  background-color: rgba(var(--error-rgb), 0.1);
}

.share-btn {
  color: var(--primary-color);
}

.share-btn:hover {
  background-color: var(--primary-light);
}

.report-btn {
  color: var(--text-lighter);
}

.report-btn:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

.article-author {
  margin-bottom: 2rem;
}

.author-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background-color: var(--bg-gray);
  border-radius: var(--radius);
}

.author-bio {
  color: var(--text-light);
  font-size: 0.95rem;
  margin-top: 0.25rem;
}

.follow-btn {
  margin-left: auto;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  padding: 0.6rem 1.25rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.follow-btn:hover {
  background-color: var(--primary-dark);
}

.article-comments {
  margin-top: 2rem;
}

.comments-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.comment-form {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.form-input {
  flex-grow: 1;
}

.comment-textarea {
  width: 100%;
  min-height: 100px;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  margin-bottom: 0.75rem;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s;
}

.comment-textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2);
}

.submit-btn {
  float: right;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  padding: 0.6rem 1.25rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:disabled {
  background-color: var(--text-lighter);
  cursor: not-allowed;
}

.submit-btn:not(:disabled):hover {
  background-color: var(--primary-dark);
}

.comments-list {
  margin-top: 2rem;
}

.comment-item {
  padding: 1.5rem 0;
  border-top: 1px solid var(--border-color);
}

.comment-author {
  display: flex;
  gap: 1rem;
}

.comment-info {
  flex-grow: 1;
}

.comment-meta {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.comment-author-name {
  font-weight: 600;
  color: var(--text-color);
  margin-right: 0.75rem;
}

.comment-time {
  color: var(--text-lighter);
  font-size: 0.9rem;
}

.comment-content {
  color: var(--text-color);
  line-height: 1.6;
  margin-bottom: 0.75rem;
}

.comment-actions {
  display: flex;
  gap: 1rem;
}

.comment-like,
.comment-reply {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: none;
  color: var(--text-lighter);
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: all 0.3s;
}

.comment-like:hover,
.comment-reply:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

@media (max-width: 768px) {
  .article-title {
    font-size: 1.5rem;
  }
  
  .article-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .article-stats {
    width: 100%;
    justify-content: space-between;
  }
  
  .author-card {
    flex-direction: column;
    text-align: center;
    padding: 1rem;
  }
  
  .follow-btn {
    margin-left: 0;
    margin-top: 0.75rem;
  }
}

/* Toast消息提示样式 */
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.toast {
  padding: 12px 20px;
  border-radius: var(--radius);
  background-color: white;
  box-shadow: var(--shadow);
  display: flex;
  align-items: center;
  animation: fadeIn 0.3s ease;
}

.toast.success {
  background-color: rgba(var(--success-rgb), 0.1);
  border-left: 4px solid var(--success-color);
}

.toast.error {
  background-color: rgba(var(--error-rgb), 0.1);
  border-left: 4px solid var(--error-color);
}

.toast-message {
  font-size: 0.95rem;
  color: var(--text-color);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: var(--radius);
  padding: 1.5rem;
  width: 100%;
  max-width: 450px;
  box-shadow: var(--shadow-lg);
  animation: modalFadeIn 0.3s ease;
}

.modal-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.modal-subtitle {
  color: var(--text-light);
  margin-bottom: 1.25rem;
}

.reason-list {
  margin-bottom: 1.5rem;
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s;
}

.reason-item:hover {
  background-color: var(--bg-gray);
}

.reason-item.active {
  background-color: rgba(var(--primary-rgb), 0.1);
}

.reason-radio {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-color);
  border-radius: 50%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.reason-item.active .reason-radio {
  border-color: var(--primary-color);
}

.radio-inner {
  width: 10px;
  height: 10px;
  background-color: var(--primary-color);
  border-radius: 50%;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.cancel-btn, .submit-btn {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-light);
}

.cancel-btn:hover {
  background-color: var(--bg-gray);
}

.submit-btn {
  background-color: var(--primary-color);
  border: none;
  color: white;
}

.submit-btn:hover {
  background-color: var(--primary-dark);
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style> 
