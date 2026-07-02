<template>
  <div class="questions-view">
    <ui-page-hero
      title="问答社区"
      description="把问题描述清楚，把答案沉淀下来。这里适合排查问题、收集方案，也适合标记已解决经验。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'question-circle']" />
          Q&A
        </span>
      </template>
      <template #actions>
        <router-link to="/question/new" class="kumo-button kumo-button--brand">
          <font-awesome-icon :icon="['fas', 'plus']" />
          提问题
        </router-link>
      </template>
      <template #aside>
        <div class="qa-summary">
          <div class="qa-summary-header">
            <span class="qa-summary-dot"></span>
            <span>Question console</span>
          </div>
          <strong>{{ totalQuestions }}</strong>
          <span>个问题</span>
          <small>{{ activeFilterLabel }}</small>
          <div class="qa-summary-bars" aria-hidden="true">
            <i></i>
            <i></i>
            <i></i>
          </div>
        </div>
      </template>
    </ui-page-hero>

    <section class="question-toolbar kumo-surface">
      <div class="kumo-tabs" aria-label="问题状态筛选">
        <button
          v-for="filter in filters"
          :key="filter.value"
          class="kumo-tab"
          :class="{ active: activeFilter === filter.value }"
          type="button"
          @click="setFilter(filter.value)"
        >
          <font-awesome-icon :icon="filter.icon" />
          {{ filter.label }}
        </button>
      </div>

      <label class="sort-control">
        <span>排序</span>
        <select v-model="sortOption" class="kumo-input">
          <option value="newest">最新发布</option>
          <option value="oldest">最早发布</option>
          <option value="mostAnswers">回答最多</option>
          <option value="leastAnswers">回答最少</option>
        </select>
      </label>
    </section>

    <section class="questions-list">
      <div v-if="loading" class="state-panel kumo-surface">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
        <span>正在加载问题...</span>
      </div>

      <div v-else-if="filteredQuestions.length === 0" class="state-panel kumo-surface">
        <font-awesome-icon :icon="['fas', 'question-circle']" />
        <h2>{{ emptyStateMessage }}</h2>
        <router-link to="/question/new" class="kumo-button kumo-button--brand">
          <font-awesome-icon :icon="['fas', 'plus']" />
          提问题
        </router-link>
      </div>

      <template v-else>
        <article
          v-for="(question, index) in filteredQuestions"
          :key="question.id"
          class="question-card kumo-surface magnetic-card stagger-item"
          :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
          @click="navigateToQuestion(question.id)"
        >
          <div class="question-stats">
            <span>
              <strong>{{ question.answerCount || 0 }}</strong>
              回答
            </span>
            <span>
              <strong>{{ question.viewCount || 0 }}</strong>
              浏览
            </span>
          </div>

          <div class="question-main">
            <div class="question-title-row">
              <span
                v-if="question.solved"
                class="kumo-status kumo-status--success"
              >
                <font-awesome-icon :icon="['fas', 'check-circle']" />
                已解决
              </span>
              <h2>{{ question.title }}</h2>
            </div>

            <p v-if="getQuestionExcerpt(question)" class="question-excerpt">
              {{ getQuestionExcerpt(question) }}
            </p>

            <div v-if="getQuestionImage(question)" class="question-image-preview">
              <img
                :src="getQuestionImage(question)"
                :alt="question.title"
                loading="lazy"
                @error="event => event.currentTarget.closest('.question-image-preview')?.classList.add('is-hidden')"
              >
            </div>

            <div class="question-meta">
              <div class="tags">
                <span v-for="tag in normalizeTags(question.tags)" :key="tag" class="tag-chip">#{{ tag }}</span>
              </div>
              <div class="user-info">
                <span class="avatar">
                  <img v-if="question.author?.avatar" :src="question.author.avatar" :alt="question.author?.name || '用户'">
                  <span v-else>{{ (question.author?.name || '用').charAt(0) }}</span>
                </span>
                <span>{{ question.author?.name || '匿名' }}</span>
                <small>{{ formatQuestionTime(question.createTime) }}</small>
              </div>
            </div>
          </div>
        </article>

        <div class="pagination kumo-surface">
          <button
            class="kumo-button"
            type="button"
            :disabled="currentPage === 1"
            @click="changePage(currentPage - 1)"
          >
            <font-awesome-icon :icon="['fas', 'chevron-left']" />
            上一页
          </button>
          <span>{{ currentPage }} / {{ totalPages }}</span>
          <button
            class="kumo-button"
            type="button"
            :disabled="currentPage === totalPages"
            @click="changePage(currentPage + 1)"
          >
            下一页
            <font-awesome-icon :icon="['fas', 'chevron-right']" />
          </button>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import UiPageHero from '../components/ui/UiPageHero.vue'
import { questionService } from '../services/questionService'
import { formatDateTime } from '../utils/dateUtils'

const router = useRouter()
const store = useStore()

const loading = ref(true)
const questions = ref([])
const activeFilter = ref('all')
const sortOption = ref('newest')
const currentPage = ref(1)
const pageSize = 10
const totalQuestions = ref(0)

const filters = [
  { value: 'all', label: '全部问题', icon: ['fas', 'list'] },
  { value: 'solved', label: '已解决', icon: ['fas', 'check-circle'] },
  { value: 'unsolved', label: '未解决', icon: ['fas', 'question-circle'] }
]

const activeFilterLabel = computed(() => filters.find(filter => filter.value === activeFilter.value)?.label || '全部问题')

const toTimestamp = (time) => {
  if (!time) return 0
  if (Array.isArray(time)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = time
    return new Date(year, month - 1, day, hour, minute, second).getTime()
  }
  return new Date(time).getTime()
}

const formatQuestionTime = (time) => formatDateTime(time, {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
})

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
const getQuestionExcerpt = (question) => stripInlineMediaSyntax(question?.summary || question?.content || '')

const normalizeTags = (tags) => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.map(tag => typeof tag === 'string' ? tag : tag?.name).filter(Boolean)
  return String(tags).split(',').map(tag => tag.trim()).filter(Boolean)
}

const loadQuestions = async () => {
  loading.value = true

  try {
    const requestParams = {
      page: currentPage.value,
      pageSize
    }
    const response = activeFilter.value === 'solved'
      ? await questionService.getSolvedQuestions(requestParams)
      : activeFilter.value === 'unsolved'
        ? await questionService.getUnsolvedQuestions(requestParams)
        : await questionService.getQuestions(requestParams)

    questions.value = response.data || []
    totalQuestions.value = response.total || response.data?.length || 0
  } catch (error) {
    questions.value = []
    totalQuestions.value = 0
    store.commit('SET_ERROR', error?.message || '问题列表加载失败')
  } finally {
    loading.value = false
  }
}

const filteredQuestions = computed(() => {
  let result = [...questions.value]

  if (activeFilter.value === 'solved') result = result.filter(q => q.solved)
  if (activeFilter.value === 'unsolved') result = result.filter(q => !q.solved)

  if (sortOption.value === 'newest') {
    result.sort((a, b) => toTimestamp(b.createTime) - toTimestamp(a.createTime))
  } else if (sortOption.value === 'oldest') {
    result.sort((a, b) => toTimestamp(a.createTime) - toTimestamp(b.createTime))
  } else if (sortOption.value === 'mostAnswers') {
    result.sort((a, b) => (b.answerCount || 0) - (a.answerCount || 0))
  } else if (sortOption.value === 'leastAnswers') {
    result.sort((a, b) => (a.answerCount || 0) - (b.answerCount || 0))
  }

  return result
})

const totalPages = computed(() => Math.max(1, Math.ceil(totalQuestions.value / pageSize)))

const emptyStateMessage = computed(() => {
  if (activeFilter.value === 'solved') return '暂无已解决问题'
  if (activeFilter.value === 'unsolved') return '暂无未解决问题'
  return '暂无问题'
})

const setFilter = (filter) => {
  activeFilter.value = filter
  currentPage.value = 1
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadQuestions()
}

const navigateToQuestion = (id) => {
  router.push(`/question/${id}`)
}

watch([activeFilter, sortOption], () => {
  currentPage.value = 1
  loadQuestions()
})

onMounted(loadQuestions)
</script>

<style scoped>
.questions-view {
  display: grid;
  gap: clamp(0.9rem, 2vw, 1.25rem);
}

.qa-summary {
  display: grid;
  gap: 0.35rem;
  min-width: min(18rem, 100%);
  padding: 1.15rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 16px;
  background:
    linear-gradient(180deg, var(--kumo-bg-elevated), var(--kumo-bg-subtle));
  box-shadow: var(--kumo-shadow-sm);
}

.qa-summary-header {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 760;
}

.qa-summary-dot {
  width: 0.48rem;
  height: 0.48rem;
  border-radius: 999px;
  background: var(--kumo-bg-accent);
  box-shadow: 0 0 0 5px var(--kumo-bg-accent-soft);
}

.qa-summary strong {
  color: var(--kumo-bg-brand-strong);
  font-size: clamp(2.6rem, 6vw, 3.4rem);
  font-weight: 900;
  line-height: 1;
}

.qa-summary span,
.qa-summary small {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.qa-summary-bars {
  display: grid;
  gap: 0.45rem;
  margin-top: 0.65rem;
}

.qa-summary-bars i {
  display: block;
  height: 0.45rem;
  border-radius: 999px;
  background: var(--kumo-bg-recessed);
}

.qa-summary-bars i:nth-child(1) {
  width: 86%;
  background: var(--kumo-bg-brand-soft);
}

.qa-summary-bars i:nth-child(2) {
  width: 62%;
}

.qa-summary-bars i:nth-child(3) {
  width: 74%;
}

.question-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem;
  border-radius: 18px;
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  color: var(--kumo-text-muted);
  font-weight: 760;
}

.sort-control select {
  min-width: 10rem;
}

.questions-list {
  display: grid;
  gap: 1rem;
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.85rem;
  min-height: 18rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel > svg {
  color: var(--kumo-bg-brand);
  font-size: 2.2rem;
}

.state-panel h2 {
  margin: 0;
  color: var(--kumo-text-default);
  font-weight: 840;
}

.question-card {
  display: grid;
  grid-template-columns: 7rem minmax(0, 1fr);
  gap: 1.1rem;
  padding: 1.1rem;
  border-color: color-mix(in srgb, var(--kumo-hairline-strong) 72%, transparent);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.86), transparent),
    var(--kumo-bg-elevated);
  cursor: pointer;
}

.question-card:hover {
  border-color: var(--kumo-hairline-strong);
}

.question-stats {
  display: grid;
  align-content: start;
  gap: 0.65rem;
}

.question-stats span {
  display: grid;
  gap: 0.15rem;
  padding: 0.8rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 14px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.8rem;
  font-weight: 740;
  text-align: center;
}

.question-stats strong {
  color: var(--kumo-text-default);
  font-size: 1.45rem;
  font-weight: 900;
  line-height: 1;
}

.question-main {
  display: grid;
  gap: 0.85rem;
  min-width: 0;
}

.question-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.question-title-row h2 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(1.1rem, 2vw, 1.45rem);
  font-weight: 840;
  line-height: 1.3;
}

.question-excerpt {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-image-preview {
  width: min(28rem, 100%);
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border: 1px solid var(--kumo-hairline-strong);
  border-radius: 14px;
  background: var(--kumo-bg-subtle);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.44);
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
  gap: 1rem;
  padding-top: 0.8rem;
  border-top: 1px solid var(--kumo-hairline);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag-chip {
  padding: 0.3rem 0.55rem;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 740;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--kumo-text-muted);
  font-size: 0.86rem;
  font-weight: 730;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.9rem;
  height: 1.9rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 840;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info small {
  color: var(--kumo-text-subtle);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 0.85rem;
  border-radius: 18px;
}

.pagination span {
  color: var(--kumo-text-muted);
  font-weight: 780;
}

button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .questions-view {
    gap: 0.85rem;
  }

  .question-toolbar,
  .question-meta {
    align-items: stretch;
    flex-direction: column;
  }

  .question-toolbar {
    padding: 0.75rem;
  }

  .question-toolbar .kumo-tabs {
    width: 100%;
  }

  .question-card {
    grid-template-columns: 1fr;
    gap: 0.85rem;
    padding: 0.9rem;
  }

  .question-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.5rem;
    order: 2;
  }

  .sort-control {
    align-items: stretch;
    flex-direction: column;
  }

  .sort-control select {
    width: 100%;
  }

  .question-image-preview {
    width: 100%;
  }

  .pagination {
    display: grid;
    grid-template-columns: 1fr;
    gap: 0.65rem;
  }

  .pagination .kumo-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .qa-summary {
    padding: 0.95rem;
  }

  .question-title-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .question-title-row h2 {
    font-size: 1.08rem;
  }

  .question-excerpt {
    -webkit-line-clamp: 3;
  }

  .question-stats span {
    padding: 0.7rem;
  }
}
</style>
