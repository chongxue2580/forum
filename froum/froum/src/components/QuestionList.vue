<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { questionService } from '../services/questionService'
import { formatDateTime, getTimeField } from '../utils/dateUtils'
import { avatarInitial, resolveAvatarFrom } from '../utils/avatar'

const props = defineProps({
  questions: {
    type: Array,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentPage: {
    type: Number,
    default: null
  },
  totalPages: {
    type: Number,
    default: null
  },
  totalItems: {
    type: Number,
    default: null
  },
  pageSize: {
    type: Number,
    default: 10
  },
  filter: {
    type: String,
    default: 'all'
  },
  sortBy: {
    type: String,
    default: 'newest'
  },
  tagId: {
    type: Number,
    default: null
  },
  categoryId: {
    type: Number,
    default: null
  },
  userId: {
    type: Number,
    default: null
  },
  searchQuery: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:page', 'update:total', 'page-change'])

const router = useRouter()
const localLoading = ref(true)
const localQuestions = ref([])
const localCurrentPage = ref(1)
const localPageSize = ref(props.pageSize || 10)
const totalQuestions = ref(0)

const isControlled = computed(() => Array.isArray(props.questions))
const activeLoading = computed(() => isControlled.value ? props.loading : localLoading.value)
const activeQuestions = computed(() => isControlled.value ? props.questions : localQuestions.value)
const activeCurrentPage = computed(() => props.currentPage || localCurrentPage.value)
const activePageSize = computed(() => props.pageSize || localPageSize.value)
const activeTotalItems = computed(() => props.totalItems ?? totalQuestions.value)
const activeTotalPages = computed(() => props.totalPages || Math.max(1, Math.ceil(activeTotalItems.value / activePageSize.value)))

const fetchQuestions = async () => {
  if (isControlled.value) return

  localLoading.value = true
  try {
    let response

    if (props.tagId) {
      response = await questionService.getQuestionsByTag(props.tagId, localCurrentPage.value - 1, activePageSize.value)
    } else if (props.searchQuery) {
      response = await questionService.searchQuestions(props.searchQuery, localCurrentPage.value - 1, activePageSize.value)
    } else if (props.userId) {
      response = await questionService.getUserQuestions(props.userId, localCurrentPage.value - 1, activePageSize.value)
    } else if (props.filter === 'solved') {
      response = await questionService.getQuestionsBySolvedStatus(true, localCurrentPage.value - 1, activePageSize.value)
    } else if (props.filter === 'unsolved') {
      response = await questionService.getQuestionsBySolvedStatus(false, localCurrentPage.value - 1, activePageSize.value)
    } else {
      response = await questionService.getPublishedQuestions(localCurrentPage.value - 1, activePageSize.value)
    }

    if (response.success) {
      if (response.data?.content) {
        localQuestions.value = response.data.content
        totalQuestions.value = response.data.totalElements || 0
        emit('update:total', totalQuestions.value)
      } else if (Array.isArray(response.data)) {
        localQuestions.value = response.data
        totalQuestions.value = response.totalElements || response.data.length
        emit('update:total', totalQuestions.value)
      } else {
        localQuestions.value = []
        totalQuestions.value = 0
      }
    }
  } catch (error) {
    console.error('加载问题列表失败：', error)
    localQuestions.value = []
    totalQuestions.value = 0
  } finally {
    localLoading.value = false
  }
}

const goToQuestion = (questionId) => {
  router.push({ name: 'QuestionDetail', params: { id: questionId } })
}

const handlePageChange = (page) => {
  if (page < 1 || page > activeTotalPages.value || page === activeCurrentPage.value) return

  emit('update:page', page)
  emit('page-change', page)

  if (!isControlled.value) {
    localCurrentPage.value = page
    fetchQuestions()
  }
}

watch(
  () => [props.filter, props.sortBy, props.tagId, props.userId, props.searchQuery, props.pageSize],
  () => {
    if (isControlled.value) return
    localCurrentPage.value = 1
    localPageSize.value = props.pageSize || 10
    emit('update:page', 1)
    fetchQuestions()
  }
)

const getQuestionAuthor = (question) => question?.author || question?.user || question?.userInfo || {}

const getAuthorName = (question) => {
  const author = getQuestionAuthor(question)
  return author.nickname || author.username || author.name || question?.authorName || '匿名用户'
}

const getAuthorAvatar = (question) => resolveAvatarFrom(getQuestionAuthor(question))

const formatTime = (question) => {
  const timeValue = getTimeField(question, 'created')
  return formatDateTime(timeValue)
}

const resolveContentImageUrl = (url) => {
  if (!url) return ''
  const normalizedUrl = String(url).trim().replace(/^<|>$/g, '')
  if (/^(https?:|data:image\/|blob:)/i.test(normalizedUrl)) return normalizedUrl
  if (normalizedUrl.startsWith('/')) return normalizedUrl
  if (normalizedUrl.startsWith('uploads/')) return `/${normalizedUrl}`
  if (normalizedUrl.startsWith('images/')) return `/uploads/${normalizedUrl}`
  if (normalizedUrl.startsWith('files/')) return `/uploads/${normalizedUrl}`
  return normalizedUrl
}

const imageUrlPattern = /(?:data:image\/|blob:|https?:\/\/|\/uploads\/|uploads\/)[^\s)'"<>]+?\.(?:png|jpe?g|gif|webp|bmp|svg)(?:\?[^\s)'"<>]*)?|(?:\/uploads\/|uploads\/)images\/[^\s)'"<>]+/i

const parseMarkdownTarget = (target = '') => {
  const trimmed = String(target || '').trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('<')) {
    const end = trimmed.indexOf('>')
    return end > 0 ? trimmed.slice(1, end).trim() : trimmed.replace(/^<|>$/g, '').trim()
  }
  return trimmed.replace(/\s+["'][^"']*["']\s*$/, '').trim()
}

const isImageUrl = (url) => imageUrlPattern.test(resolveContentImageUrl(url))

const extractFirstImageUrl = (content = '') => {
  const text = String(content || '')
  const htmlMatch = text.match(/<img[^>]+src=["']([^"']+)["']/i)
  if (htmlMatch?.[1]) return resolveContentImageUrl(htmlMatch[1])

  const markdownMatch = text.match(/!\[[^\]]*]\(\s*<?([^)\s>]+)(?:\s+["'][^)]*["'])?\s*\)/i)
  if (markdownMatch?.[1]) return resolveContentImageUrl(markdownMatch[1])

  const markdownLinkMatch = text.match(/\[[^\]]*]\(([^)]*)\)/i)
  if (markdownLinkMatch?.[1]) {
    const target = parseMarkdownTarget(markdownLinkMatch[1])
    if (isImageUrl(target)) return resolveContentImageUrl(target)
  }

  const plainMatch = text.match(imageUrlPattern)
  return plainMatch?.[0] ? resolveContentImageUrl(plainMatch[0]) : ''
}

const stripInlineMediaSyntax = (content = '') => String(content || '')
  .replace(/<img\b[^>]*>/gi, ' ')
  .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
  .replace(/\[([^\]]*)]\(([^)]*)\)/g, (match, label, target) => {
    return isImageUrl(parseMarkdownTarget(target)) ? ' ' : label
  })
  .replace(new RegExp(imageUrlPattern.source, 'gi'), ' ')
  .replace(/<[^>]+>/g, ' ')
  .replace(/[#>*_`~\-]+/g, ' ')
  .replace(/\s+/g, ' ')
  .trim()

const normalizeImageCandidate = (candidate) => {
  if (!candidate) return ''
  if (typeof candidate === 'string') return resolveContentImageUrl(candidate)
  return resolveContentImageUrl(candidate.url || candidate.src || candidate.path || candidate.fileUrl || candidate.imageUrl || '')
}

const getQuestionImage = (question) => {
  const directCandidates = [
    question?.coverImage,
    question?.coverUrl,
    question?.image,
    question?.imageUrl,
    question?.thumbnail,
    question?.thumbnailUrl,
    question?.firstImage,
    question?.banner
  ]

  for (const candidate of directCandidates) {
    const imageUrl = normalizeImageCandidate(candidate)
    if (imageUrl) return imageUrl
  }

  const collectionCandidates = [
    question?.images,
    question?.attachments,
    question?.files,
    question?.media
  ]

  for (const collection of collectionCandidates) {
    const list = Array.isArray(collection) ? collection : []
    for (const item of list) {
      const imageUrl = normalizeImageCandidate(item)
      const itemType = String(item?.type || item?.mimeType || item?.contentType || '').toLowerCase()
      if (imageUrl && (isImageUrl(imageUrl) || itemType.startsWith('image/'))) return imageUrl
    }
  }

  return extractFirstImageUrl(question?.content || question?.summary || question?.description || '')
}

const getQuestionSummary = (question) => {
  const summary = stripInlineMediaSyntax(question?.summary || question?.content || question?.description || '')
  if (!summary) return '暂无内容摘要'
  return summary.length > 170 ? `${summary.slice(0, 170)}...` : summary
}

const normalizeTags = (tags) => {
  if (!tags) return []
  if (Array.isArray(tags)) {
    return tags
      .map(tag => typeof tag === 'string' ? tag : tag?.name || tag?.label)
      .filter(Boolean)
  }
  return String(tags).split(',').map(tag => tag.trim()).filter(Boolean)
}

onMounted(() => {
  fetchQuestions()
})
</script>

<template>
  <div class="question-list">
    <div v-if="activeLoading" class="loading-container kumo-surface">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="activeQuestions.length === 0" class="empty-state kumo-surface">
      <div class="empty-icon">
        <font-awesome-icon :icon="['fas', 'question-circle']" />
      </div>
      <h3>暂无问题</h3>
      <p>暂时没有符合条件的问题，你可以尝试其他筛选条件或者创建一个新的问题</p>
      <router-link to="/question/new" class="create-btn kumo-button kumo-button--brand">
        <font-awesome-icon :icon="['fas', 'plus']" />
        创建问题
      </router-link>
    </div>

    <ul v-else class="question-items">
      <li
        v-for="(question, index) in activeQuestions"
        :key="question.id"
        class="question-item magnetic-card stagger-item"
        :style="{ animationDelay: `${Math.min(index, 10) * 38}ms` }"
        @click="goToQuestion(question.id)"
      >
        <div class="question-stats">
          <div class="stat-item">
            <span class="count">{{ question.viewCount || question.views || 0 }}</span>
            <span class="label">浏览</span>
          </div>
          <div class="stat-item">
            <span class="count">{{ question.answerCount || question.commentCount || 0 }}</span>
            <span class="label">回答</span>
          </div>
          <div class="stat-item" :class="{ 'is-solved': question.solved || question.isSolved }">
            <font-awesome-icon
              v-if="question.solved || question.isSolved"
              :icon="['fas', 'check-circle']"
              class="solved-icon"
            />
            <span class="label">{{ (question.solved || question.isSolved) ? '已解决' : '未解决' }}</span>
          </div>
        </div>

        <div class="question-content">
          <div class="question-title-row">
            <span v-if="question.solved || question.isSolved" class="status-pill">
              <font-awesome-icon :icon="['fas', 'check']" />
              Solved
            </span>
            <h3 class="question-title">{{ question.title }}</h3>
          </div>

          <p class="question-summary">{{ getQuestionSummary(question) }}</p>

          <div v-if="getQuestionImage(question)" class="question-image-preview">
            <img
              :src="getQuestionImage(question)"
              :alt="question.title"
              loading="lazy"
              @error="event => event.currentTarget.closest('.question-image-preview')?.classList.add('is-hidden')"
            >
          </div>

          <div class="question-meta">
            <div class="question-tags">
              <span v-for="tag in normalizeTags(question.tags)" :key="tag" class="tag">#{{ tag }}</span>
            </div>

            <div class="question-info">
              <span class="author">
                <span class="avatar">
                  <img v-if="getAuthorAvatar(question)" :src="getAuthorAvatar(question)" :alt="getAuthorName(question)">
                  <span v-else>{{ avatarInitial(getAuthorName(question)) }}</span>
                </span>
                <span class="author-name">{{ getAuthorName(question) }}</span>
              </span>
              <span class="time">{{ formatTime(question) }}</span>
            </div>
          </div>
        </div>
      </li>
    </ul>

    <div class="pagination kumo-surface" v-if="activeTotalItems > activePageSize">
      <button
        :disabled="activeCurrentPage === 1"
        @click="handlePageChange(activeCurrentPage - 1)"
        class="page-btn prev"
        type="button"
      >
        <font-awesome-icon :icon="['fas', 'chevron-left']" />
      </button>

      <span class="page-info">{{ activeCurrentPage }} / {{ activeTotalPages }}</span>

      <button
        :disabled="activeCurrentPage >= activeTotalPages"
        @click="handlePageChange(activeCurrentPage + 1)"
        class="page-btn next"
        type="button"
      >
        <font-awesome-icon :icon="['fas', 'chevron-right']" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.question-list {
  width: 100%;
}

.loading-container {
  display: grid;
  place-items: center;
  gap: 0.75rem;
  min-height: 16rem;
  color: var(--kumo-text-muted);
}

.spinner {
  width: 2.25rem;
  height: 2.25rem;
  border: 3px solid var(--kumo-hairline);
  border-left-color: var(--kumo-bg-accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 0.8rem;
  min-height: 18rem;
  padding: clamp(1.5rem, 5vw, 2.4rem);
  color: var(--kumo-text-muted);
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 4.2rem;
  height: 4.2rem;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-size: 1.75rem;
}

.empty-state h3,
.empty-state p {
  margin: 0;
}

.empty-state h3 {
  color: var(--kumo-text-default);
  font-size: 1.25rem;
  font-weight: 820;
}

.empty-state p {
  max-width: 30rem;
  line-height: 1.65;
}

.question-items {
  display: grid;
  gap: 0.9rem;
  padding: 0;
  margin: 0;
  list-style: none;
}

.question-item {
  display: grid;
  grid-template-columns: 6.75rem minmax(0, 1fr);
  gap: 1rem;
  padding: clamp(0.95rem, 2vw, 1.2rem);
  border: 1px solid color-mix(in srgb, var(--kumo-hairline-strong) 76%, transparent);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), transparent),
    var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-sm);
  cursor: pointer;
}

.question-stats {
  display: grid;
  align-content: start;
  gap: 0.58rem;
}

.stat-item {
  display: grid;
  place-items: center;
  gap: 0.18rem;
  min-height: 4.25rem;
  padding: 0.72rem 0.5rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 14px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  text-align: center;
}

.stat-item .count {
  color: var(--kumo-text-default);
  font-size: 1.35rem;
  font-weight: 850;
  line-height: 1;
}

.stat-item .label {
  font-size: 0.75rem;
  font-weight: 760;
}

.stat-item.is-solved {
  border-color: color-mix(in srgb, var(--kumo-status-success) 35%, var(--kumo-hairline));
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.solved-icon {
  font-size: 1rem;
}

.question-content {
  display: grid;
  gap: 0.78rem;
  min-width: 0;
}

.question-title-row {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.question-title {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(1.08rem, 2vw, 1.35rem);
  font-weight: 840;
  line-height: 1.28;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.32rem;
  min-height: 1.7rem;
  padding: 0.28rem 0.58rem;
  border-radius: 999px;
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
  font-size: 0.74rem;
  font-weight: 780;
}

.question-summary {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  font-size: 0.96rem;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-image-preview {
  width: min(30rem, 100%);
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border: 1px solid var(--kumo-hairline-strong);
  border-radius: 14px;
  background: var(--kumo-bg-subtle);
}

.question-image-preview.is-hidden {
  display: none;
}

.question-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.question-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  min-width: 0;
  padding-top: 0.78rem;
  border-top: 1px solid var(--kumo-hairline);
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  min-width: 0;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 1.75rem;
  padding: 0.28rem 0.56rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 760;
}

.question-info {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.6rem;
  min-width: 0;
  color: var(--kumo-text-subtle);
  font-size: 0.84rem;
  font-weight: 720;
}

.author {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
  color: var(--kumo-text-muted);
}

.author-name {
  max-width: 7rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 1.8rem;
  height: 1.8rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-size: 0.78rem;
  font-weight: 820;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.time {
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  margin: 1rem auto 0;
  gap: 0.65rem;
  padding: 0.55rem;
  border-radius: 999px;
}

.page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.35rem;
  height: 2.35rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  cursor: pointer;
  transition: var(--kumo-transition);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--kumo-hairline-strong);
  background: var(--kumo-bg-accent-soft);
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.page-info {
  min-width: 4.2rem;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  font-weight: 780;
  text-align: center;
}

@media (max-width: 768px) {
  .question-items {
    gap: 0.8rem;
  }

  .question-item {
    grid-template-columns: 1fr;
    gap: 0.85rem;
    border-radius: 16px;
  }

  .question-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    order: 2;
  }

  .stat-item {
    min-height: 3.4rem;
    padding: 0.56rem 0.35rem;
  }

  .stat-item .count {
    font-size: 1.08rem;
  }

  .question-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .question-info {
    justify-content: flex-start;
    width: 100%;
  }

  .question-image-preview {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .question-stats {
    grid-template-columns: 1fr;
  }

  .stat-item {
    display: flex;
    justify-content: center;
    min-height: 2.8rem;
    gap: 0.4rem;
  }

  .question-title-row {
    flex-direction: column;
  }

  .question-summary {
    -webkit-line-clamp: 3;
  }

  .pagination {
    width: 100%;
  }
}
</style>
