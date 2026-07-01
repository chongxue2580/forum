<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import ArticleComments from './ArticleComments.vue'
import UiPageHero from './ui/UiPageHero.vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { articleService } from '../services/articleService'
import { reportService } from '../services/reportService'
import { flattenComments } from '../utils/commentHelper'
import { resolveAvatarUrl } from '../utils/avatar'
import { formatFriendlyTime } from '../utils/dateUtils'

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

const articleCategory = computed(() => {
  const category = article.value?.category
  if (article.value?.categoryName) return article.value.categoryName
  if (typeof category === 'string' && category.trim()) return category.trim()
  if (category?.name) return category.name
  return '未分类'
})

const normalizedTags = computed(() => {
  const tags = article.value?.tags
  if (!tags) return []
  if (Array.isArray(tags)) {
    return tags.map(tag => typeof tag === 'string' ? tag : tag?.name).filter(Boolean)
  }
  return String(tags).split(',').map(tag => tag.trim()).filter(Boolean)
})

const articleSummary = computed(() => {
  const directSummary = article.value?.summary || article.value?.excerpt
  if (directSummary) return directSummary

  const content = article.value?.content || ''
  const plainText = content
    .replace(/<[^>]*>/g, ' ')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/[#*_`[\]()]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

  return plainText.length > 120 ? `${plainText.substring(0, 120)}...` : plainText || '阅读完整内容并参与讨论。'
})

const author = computed(() => article.value?.author || {})

const authorName = computed(() => {
  return author.value.nickname || author.value.username || author.value.name || '匿名作者'
})

const authorAvatar = computed(() => {
  return getAvatarPath(author.value.avatarUrl || author.value.avatar)
})

const articleViews = computed(() => article.value?.viewCount || article.value?.views || 0)
const articleLikes = computed(() => article.value?.likes ?? article.value?.likeCount ?? 0)
const isAuthorFollowing = computed(() => Boolean(author.value.isFollowing))
const followButtonIcon = computed(() => {
  if (isOwnAuthor.value) return 'user'
  return isAuthorFollowing.value ? 'user-check' : 'user-plus'
})
const followButtonLabel = computed(() => {
  if (isOwnAuthor.value) return '作者本人'
  return isAuthorFollowing.value ? '已关注' : '关注作者'
})

// 检查用户是否已登录
const isLoggedIn = computed(() => store.state.isAuthenticated && store.state.user)
const currentUserId = computed(() => store.state.user?.id)
const isOwnAuthor = computed(() => {
  return Boolean(article.value?.author?.id && currentUserId.value && String(article.value.author.id) === String(currentUserId.value))
})

const fetchArticle = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await articleService.getArticleById(props.id)
    article.value = response.data
    article.value.comments = await articleService.getArticleComments(props.id)
    await Promise.all([
      syncAuthorFollowState(),
      syncCommentLikeStates()
    ])
  } catch (err) {
    error.value = err.message || '获取文章详情失败'
  } finally {
    loading.value = false
  }
}

const refreshComments = async () => {
  if (!article.value?.id) return
  article.value.comments = await articleService.getArticleComments(article.value.id)
  await syncCommentLikeStates()
}

const syncAuthorFollowState = async () => {
  if (!isLoggedIn.value || !article.value?.author?.id || isOwnAuthor.value) {
    if (article.value?.author) {
      article.value.author.isFollowing = false
    }
    return
  }

  try {
    const info = await store.dispatch('getFollowInfo', {
      targetType: 'USER',
      targetId: article.value.author.id
    })
    article.value.author.isFollowing = Boolean(info?.isFollowed)
  } catch {
    article.value.author.isFollowing = false
  }
}

const syncCommentLikeStates = async () => {
  if (!isLoggedIn.value || !article.value?.comments?.length) return

  const flatComments = flattenComments(article.value.comments)
  const results = await Promise.allSettled(
    flatComments.map(comment => articleService.getCommentLikeInfo(comment.id))
  )

  results.forEach((result, index) => {
    if (result.status !== 'fulfilled') return

    const comment = flatComments[index]
    const info = result.value || {}
    const count = Number(info.count ?? comment.likes ?? 0)
    comment.likes = count
    comment.likeCount = count
    comment.voteCount = count
    comment.isLiked = Boolean(info.isLiked)
    comment.userVote = info.isLiked ? 'up' : null
  })
}

const likeArticle = async () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }

  try {
    // 使用新的点赞API
    const result = await store.dispatch('toggleLike', {
      targetType: 'ARTICLE',
      targetId: article.value.id
    })

    if (result.success) {
      const nextCount = result.isLiked
        ? Number(articleLikes.value) + 1
        : Math.max(0, Number(articleLikes.value) - 1)
      article.value.likes = nextCount
      article.value.likeCount = nextCount
      showToast(result.message)
    } else {
      showToast(result.message || '操作失败', 'error')
    }
  } catch (error) {
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
    .catch(() => {
      showToast('复制链接失败', 'error')
    })
}

const reportArticle = () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }

  reportTarget.value = {
    type: 'ARTICLE',
    id: article.value?.id || Number(props.id),
    title: article.value?.title ? `文章：${article.value.title}` : ''
  }
  reportReason.value = ''
  showReportModal.value = true
}

const followAuthor = async () => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }

  try {
    // 获取作者ID
    const authorId = author.value.id
    if (authorId === undefined || authorId === null) {
      showToast('无法找到作者信息', 'error')
      return
    }

    if (isOwnAuthor.value) {
      showToast('不能关注自己', 'error')
      return
    }

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
const reportSubmitting = ref(false)
const reportTarget = ref({
  type: 'ARTICLE',
  id: null,
  title: ''
})
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

  if (!reportTarget.value.id) {
    showToast('无法找到举报目标', 'error')
    return
  }

  reportSubmitting.value = true
  try {
    await reportService.createReport({
      targetType: reportTarget.value.type,
      targetId: reportTarget.value.id,
      reason: reportReason.value,
      description: reportTarget.value.title || ''
    })
    showToast('举报已提交，管理员会尽快处理')
    showReportModal.value = false
    reportReason.value = ''
    reportTarget.value = {
      type: 'ARTICLE',
      id: article.value?.id || null,
      title: article.value?.title ? `文章：${article.value.title}` : ''
    }
  } catch (error) {
    showToast(error.message || '提交举报失败，请稍后重试', 'error')
  } finally {
    reportSubmitting.value = false
  }
}

const handleReportComment = (comment) => {
  if (!isLoggedIn.value) {
    goToLogin()
    return
  }

  if (!comment?.id) {
    showToast('无法找到评论信息', 'error')
    return
  }

  reportTarget.value = {
    type: 'COMMENT',
    id: comment.id,
    title: comment.content ? `评论：${String(comment.content).slice(0, 120)}` : ''
  }
  reportReason.value = ''
  showReportModal.value = true
}

const formatDate = (value) => {
  return formatFriendlyTime(value);
}

// Add a handler for the comment events
const handleAddComment = async ({ articleId, content, parentId = null }) => {
  if (article.value.id === articleId) {
    try {
      await articleService.addComment(articleId, { content, parentId });
      await refreshComments();
      showToast('评论已发布');
    } catch (err) {
      showToast(err.message || '发表评论失败', 'error');
    }
  }
}

const handleLikeComment = async ({ articleId, commentId }) => {
  if (article.value.id === articleId) {
    try {
      const comment = flattenComments(article.value.comments).find(c => Number(c.id) === Number(commentId));
      const hasLiked = comment?.userVote === 'up' || comment?.isLiked === true;

      if (hasLiked) {
        await articleService.unlikeComment(articleId, commentId);
      } else {
        await articleService.likeComment(articleId, commentId);
      }

      const info = await articleService.getCommentLikeInfo(commentId);
      if (comment) {
        const count = Number(info.count ?? comment.likes ?? 0);
        comment.likes = count;
        comment.likeCount = count;
        comment.voteCount = count;
        comment.isLiked = Boolean(info.isLiked);
        comment.userVote = info.isLiked ? 'up' : null;
      } else {
        await refreshComments();
      }
    } catch (err) {
      showToast(err.message || '点赞评论失败', 'error');
    }
  }
}

const goToLogin = () => {
  router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
}

// 导航到作者页面
const goToAuthorProfile = () => {
  const articleAuthor = article.value?.author;

  if (!articleAuthor || articleAuthor.id === undefined || articleAuthor.id === null) {
    showToast('无法找到作者信息', 'error');
    return;
  }

  // 确保ID是字符串
  const userId = String(articleAuthor.id);

  // 使用命名路由
  router.push({
    name: 'UserProfile',
    params: { id: userId }
  }).catch(() => {
    showToast('页面导航错误，请稍后重试', 'error');
  });
}

// 获取作者首字母
const getAuthorInitials = (name) => {
  if (!name) return '匿';
  return name.charAt(0).toUpperCase();
};

// 确保头像路径正确（统一解析，兼容 /uploads 与字段差异）
const getAvatarPath = (avatar) => resolveAvatarUrl(avatar);

onMounted(() => {
  fetchArticle()
})
</script>

<template>
  <div class="article-detail-page">
    <div v-if="loading" class="state-panel kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>正在加载文章...</span>
    </div>

    <div v-else-if="error" class="state-panel kumo-surface state-panel--error">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
      <span>{{ error }}</span>
      <button class="kumo-button" type="button" @click="fetchArticle">
        <font-awesome-icon :icon="['fas', 'redo']" />
        重试
      </button>
    </div>

    <template v-else-if="article">
      <ui-page-hero :title="article.title" :description="articleSummary">
        <template #eyebrow>
          <span class="kumo-eyebrow">
            <font-awesome-icon :icon="['fas', 'file-alt']" />
            Article
          </span>
        </template>

        <template #actions>
          <button class="kumo-button kumo-button--brand" type="button" @click="likeArticle">
            <font-awesome-icon :icon="['fas', 'heart']" />
            喜欢 {{ articleLikes }}
          </button>
          <button class="kumo-button" type="button" @click="shareArticle">
            <font-awesome-icon :icon="['fas', 'share']" />
            分享
          </button>
          <button class="kumo-button kumo-button--ghost" type="button" @click="reportArticle">
            <font-awesome-icon :icon="['fas', 'flag']" />
            举报
          </button>
        </template>

        <template #aside>
          <div class="hero-metrics">
            <div class="hero-metric">
              <strong>{{ articleViews }}</strong>
              <span>阅读</span>
            </div>
            <div class="hero-metric">
              <strong>{{ commentCount }}</strong>
              <span>评论</span>
            </div>
            <div class="hero-metric">
              <strong>{{ articleLikes }}</strong>
              <span>喜欢</span>
            </div>
          </div>
        </template>
      </ui-page-hero>

      <section class="article-meta-panel kumo-surface">
        <button class="author-strip" type="button" @click="goToAuthorProfile">
          <span class="author-avatar">
            <img v-if="authorAvatar" :src="authorAvatar" :alt="authorName">
            <span v-else>{{ getAuthorInitials(authorName) }}</span>
          </span>
          <span class="author-copy">
            <strong>{{ authorName }}</strong>
            <small>
              <font-awesome-icon :icon="['fas', 'calendar-alt']" />
              {{ formatDate(article.createTime || article.createdAt) }}
            </small>
          </span>
        </button>

        <div class="article-taxonomy">
          <span class="kumo-status kumo-status--info">
            <font-awesome-icon :icon="['fas', 'folder']" />
            {{ articleCategory }}
          </span>
          <span v-for="tag in normalizedTags" :key="tag" class="tag-chip">
            <font-awesome-icon :icon="['fas', 'tag']" />
            {{ tag }}
          </span>
        </div>
      </section>

      <main class="article-layout">
        <article class="article-content-panel kumo-surface">
          <div class="article-content markdown-body" v-html="formattedContent"></div>
        </article>

        <aside class="article-side">
          <section class="author-card kumo-surface magnetic-card">
            <button class="author-avatar author-avatar--large" type="button" @click="goToAuthorProfile">
              <img v-if="authorAvatar" :src="authorAvatar" :alt="authorName">
              <span v-else>{{ getAuthorInitials(authorName) }}</span>
            </button>
            <div class="author-card-copy">
              <h2 class="kumo-heading">{{ authorName }}</h2>
              <p class="kumo-muted">文章作者</p>
            </div>
            <button
              class="kumo-button follow-button"
              :class="{ 'kumo-button--brand': !isAuthorFollowing && !isOwnAuthor, 'is-following': isAuthorFollowing }"
              :disabled="isOwnAuthor"
              type="button"
              @click="followAuthor"
            >
              <font-awesome-icon :icon="['fas', followButtonIcon]" />
              {{ followButtonLabel }}
            </button>
          </section>

          <section class="side-actions kumo-surface">
            <button class="side-action" type="button" @click="likeArticle">
              <font-awesome-icon :icon="['fas', 'heart']" />
              <span>喜欢文章</span>
            </button>
            <button class="side-action" type="button" @click="shareArticle">
              <font-awesome-icon :icon="['fas', 'share']" />
              <span>复制链接</span>
            </button>
            <button class="side-action" type="button" @click="reportArticle">
              <font-awesome-icon :icon="['fas', 'flag']" />
              <span>举报内容</span>
            </button>
          </section>
        </aside>
      </main>

      <section class="comments-panel kumo-surface">
        <article-comments
          :comments="article.comments"
          :article-id="article.id"
          @add-comment="handleAddComment"
          @like-comment="handleLikeComment"
          @report-comment="handleReportComment"
        />
      </section>
    </template>

    <div v-if="toast.show" class="toast-container">
      <div class="toast kumo-surface" :class="`toast--${toast.type}`">
        <font-awesome-icon :icon="['fas', toast.type === 'error' ? 'exclamation-circle' : 'check-circle']" />
        <span>{{ toast.message }}</span>
      </div>
    </div>

    <div v-if="showReportModal" class="dialog-backdrop" @click="showReportModal = false">
      <section class="report-dialog kumo-surface" role="dialog" aria-modal="true" aria-labelledby="report-title" @click.stop>
        <div class="dialog-heading">
          <span class="kumo-eyebrow">
            <font-awesome-icon :icon="['fas', 'flag']" />
            Report
          </span>
          <h2 id="report-title" class="kumo-heading">举报{{ reportTarget.type === 'COMMENT' ? '评论' : '文章' }}</h2>
          <p class="kumo-muted">请选择一个原因，提交后管理员会在后台处理。</p>
        </div>

        <div class="reason-list">
          <button
            v-for="reason in reportReasons"
            :key="reason"
            class="reason-item"
            :class="{ active: reportReason === reason }"
            type="button"
            @click="reportReason = reason"
          >
            <span class="reason-radio">
              <span v-if="reportReason === reason"></span>
            </span>
            {{ reason }}
          </button>
        </div>

        <div class="dialog-actions">
          <button class="kumo-button" type="button" @click="showReportModal = false">取消</button>
          <button class="kumo-button kumo-button--brand" type="button" :disabled="reportSubmitting" @click="submitReport">
            <font-awesome-icon v-if="reportSubmitting" :icon="['fas', 'spinner']" spin />
            {{ reportSubmitting ? '提交中' : '提交' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.article-detail-page {
  display: grid;
  gap: 1.25rem;
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.8rem;
  min-height: 18rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel > svg {
  color: var(--kumo-bg-brand);
  font-size: 2.1rem;
}

.state-panel--error {
  color: var(--kumo-status-danger);
}

.state-panel--error > svg {
  color: var(--kumo-status-danger);
}

.hero-metrics {
  display: grid;
  gap: 0.7rem;
}

.hero-metric {
  display: grid;
  gap: 0.2rem;
  min-width: 9rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.hero-metric strong {
  color: var(--kumo-bg-brand-strong);
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 900;
  line-height: 1;
}

.hero-metric span {
  color: var(--kumo-text-muted);
  font-size: 0.82rem;
  font-weight: 760;
}

.article-meta-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
}

.author-strip {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--kumo-text-default);
  cursor: pointer;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.8rem;
  height: 2.8rem;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid var(--kumo-hairline);
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-size: 1rem;
  font-weight: 840;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.author-avatar:hover,
.author-strip:hover .author-avatar {
  transform: translateY(-2px) scale(1.03);
  border-color: var(--kumo-hairline-strong);
  box-shadow: var(--kumo-shadow-sm);
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-avatar--large {
  width: 4.4rem;
  height: 4.4rem;
  border: 0;
  font-size: 1.5rem;
  cursor: pointer;
}

.author-copy {
  display: grid;
  min-width: 0;
  text-align: left;
}

.author-copy strong {
  overflow: hidden;
  color: var(--kumo-text-default);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-copy small {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--kumo-text-subtle);
  font-weight: 690;
}

.article-taxonomy {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.32rem 0.62rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 760;
}

.article-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(16rem, 20rem);
  align-items: start;
  gap: 1.25rem;
}

.article-content-panel {
  min-width: 0;
  padding: clamp(1.25rem, 3vw, 2.4rem);
}

.article-content {
  color: var(--kumo-text-default);
  font-size: 1.05rem;
  line-height: 1.78;
}

:deep(.markdown-body) {
  color: var(--kumo-text-default);
}

:deep(.markdown-body > *:first-child) {
  margin-top: 0;
}

:deep(.markdown-body > *:last-child) {
  margin-bottom: 0;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4),
:deep(.markdown-body h5),
:deep(.markdown-body h6) {
  margin: 1.6em 0 0.75em;
  color: var(--kumo-text-default);
  font-weight: 840;
  letter-spacing: 0;
  line-height: 1.22;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2) {
  padding-bottom: 0.45rem;
  border-bottom: 1px solid var(--kumo-hairline);
}

:deep(.markdown-body p),
:deep(.markdown-body blockquote),
:deep(.markdown-body ul),
:deep(.markdown-body ol),
:deep(.markdown-body table),
:deep(.markdown-body pre) {
  margin: 0 0 1.1rem;
}

:deep(.markdown-body a) {
  color: var(--kumo-bg-brand-strong);
  font-weight: 750;
}

:deep(.markdown-body blockquote) {
  padding: 0.7rem 1rem;
  border-left: 4px solid var(--kumo-bg-brand);
  border-radius: var(--kumo-radius-sm);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-text-muted);
}

:deep(.markdown-body code) {
  padding: 0.16rem 0.36rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-sm);
  background: var(--kumo-bg-recessed);
  color: var(--kumo-bg-brand-strong);
  font-family: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace;
  font-size: 0.88em;
}

:deep(.markdown-body pre) {
  overflow: auto;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-recessed);
}

:deep(.markdown-body pre code) {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
}

:deep(.markdown-body img) {
  max-width: 100%;
  border-radius: var(--kumo-radius-md);
  box-shadow: var(--kumo-shadow-sm);
}

:deep(.markdown-body table) {
  width: 100%;
  border-collapse: collapse;
  overflow: hidden;
  border-radius: var(--kumo-radius-sm);
}

:deep(.markdown-body th),
:deep(.markdown-body td) {
  padding: 0.7rem;
  border: 1px solid var(--kumo-hairline);
}

.article-side {
  position: sticky;
  top: 6rem;
  display: grid;
  gap: 1rem;
}

.author-card,
.side-actions {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.author-card {
  justify-items: start;
}

.author-card-copy {
  display: grid;
  gap: 0.25rem;
}

.author-card-copy h2,
.author-card-copy p {
  margin: 0;
}

.author-card-copy h2 {
  font-size: 1.25rem;
}

.follow-button {
  width: 100%;
}

.follow-button.is-following {
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.follow-button:disabled {
  cursor: not-allowed;
  opacity: 0.68;
  transform: none;
}

.side-action {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 0.65rem;
  min-height: 2.8rem;
  padding: 0.55rem 0.7rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
  font-weight: 760;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition),
    background-color var(--kumo-transition);
}

.side-action:hover {
  transform: translateX(3px);
  border-color: var(--kumo-hairline-strong);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.comments-panel {
  padding: clamp(1rem, 2.2vw, 1.5rem);
}

.comments-panel :deep(.article-comments) {
  margin-top: 0;
}

.comments-panel :deep(.comments-title) {
  color: var(--kumo-text-default);
}

.toast-container {
  position: fixed;
  top: 1.2rem;
  right: 1.2rem;
  z-index: 1100;
}

.toast {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  min-width: min(22rem, calc(100vw - 2rem));
  padding: 0.85rem 1rem;
  color: var(--kumo-text-default);
  animation: toast-in 260ms ease both;
}

.toast--success {
  border-color: var(--kumo-status-success);
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.toast--error {
  border-color: var(--kumo-status-danger);
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: var(--kumo-bg-overlay);
  backdrop-filter: var(--kumo-blur);
}

.report-dialog {
  display: grid;
  gap: 1.25rem;
  width: min(100%, 30rem);
  padding: 1.3rem;
  animation: dialog-in 260ms ease both;
}

.dialog-heading {
  display: grid;
  gap: 0.7rem;
}

.dialog-heading h2,
.dialog-heading p {
  margin: 0;
}

.reason-list {
  display: grid;
  gap: 0.55rem;
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  min-height: 3rem;
  padding: 0.7rem 0.85rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
  font-weight: 740;
  text-align: left;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    background-color var(--kumo-transition),
    color var(--kumo-transition);
}

.reason-item:hover,
.reason-item.active {
  transform: translateY(-2px);
  border-color: var(--kumo-bg-brand);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.reason-radio {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.1rem;
  height: 1.1rem;
  flex: 0 0 auto;
  border: 2px solid var(--kumo-hairline-strong);
  border-radius: 50%;
}

.reason-radio span {
  width: 0.52rem;
  height: 0.52rem;
  border-radius: 50%;
  background: var(--kumo-bg-brand);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateY(-0.6rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes dialog-in {
  from {
    opacity: 0;
    transform: translateY(1rem) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 980px) {
  .article-layout {
    grid-template-columns: 1fr;
  }

  .article-side {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .article-meta-panel,
  .dialog-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .article-taxonomy {
    justify-content: flex-start;
  }

  .article-side {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-metric {
    min-width: 0;
  }

  .hero-metric strong {
    font-size: 1.6rem;
  }
}
</style>
