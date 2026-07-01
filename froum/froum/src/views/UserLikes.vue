<template>
  <div class="user-likes-page">
    <ui-page-hero
      title="我的点赞"
      description="回看你认可过的文章、问答和评论，把高价值内容重新找回来。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'heart']" />
          Likes
        </span>
      </template>
      <template #aside>
        <div class="hero-count">
          <strong>{{ activeTotal }}</strong>
          <span>{{ activeTabLabel }}</span>
        </div>
      </template>
    </ui-page-hero>

    <section class="workspace">
      <div class="kumo-tabs" aria-label="点赞内容类型">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="kumo-tab"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <div class="content-panel kumo-surface">
        <div v-if="errorMessage" class="notice notice-error">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
          {{ errorMessage }}
        </div>

        <div v-if="loading" class="state-panel">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>加载中...</span>
        </div>

        <template v-else>
          <div v-if="activeItems.length === 0" class="state-panel">
            <font-awesome-icon :icon="['fas', 'heart']" />
            <span>{{ emptyText }}</span>
          </div>

          <div v-else class="item-list">
            <article
              v-for="(item, index) in activeItems"
              :key="`${activeTab}-${item.id}`"
              class="content-item kumo-surface magnetic-card stagger-item"
              :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
            >
              <div class="item-header">
                <router-link :to="getItemLink(item)">
                  {{ getItemTitle(item) }}
                </router-link>
                <span>{{ formatDate(item.likedAt || item.createTime || item.createdAt) }}</span>
              </div>
              <p>{{ getItemSummary(item) }}</p>
              <div class="item-meta">
                <span>{{ getAuthorLabel(item) }}：{{ getAuthorName(item.author || item.user) }}</span>
                <span>
                  <font-awesome-icon :icon="['fas', 'heart']" />
                  {{ item.likes || item.likeCount || item.voteCount || 0 }}
                </span>
              </div>
            </article>
          </div>
        </template>
      </div>

      <div v-if="totalPages > 1" class="pagination kumo-surface">
        <button class="kumo-button" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
          上一页
        </button>
        <span>第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        <button class="kumo-button" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { likeApi } from '@/api/likeApi'
import { articleService } from '@/services/articleService'
import { questionService } from '@/services/questionService'
import { commentService } from '@/services/commentService'
import UiPageHero from '@/components/ui/UiPageHero.vue'
import { formatDate as formatDateValue } from '@/utils/dateUtils'

const activeTab = ref('articles')
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = 10

const articles = ref([])
const questions = ref([])
const comments = ref([])

const pagination = reactive({
  articles: { total: 0, pages: 0 },
  questions: { total: 0, pages: 0 },
  comments: { total: 0, pages: 0 }
})

const tabs = computed(() => [
  { key: 'articles', label: '文章', count: pagination.articles.total },
  { key: 'questions', label: '问答', count: pagination.questions.total },
  { key: 'comments', label: '评论', count: pagination.comments.total }
])

const totalPages = computed(() => pagination[activeTab.value]?.pages || 0)
const activeTotal = computed(() => pagination[activeTab.value]?.total || 0)
const activeTabLabel = computed(() => tabs.value.find(tab => tab.key === activeTab.value)?.label || '')
const activeItems = computed(() => {
  if (activeTab.value === 'articles') return articles.value
  if (activeTab.value === 'questions') return questions.value
  return comments.value
})

const emptyText = computed(() => {
  if (activeTab.value === 'articles') return '您还没有点赞过任何文章'
  if (activeTab.value === 'questions') return '您还没有点赞过任何问答'
  return '您还没有点赞过任何评论'
})

const formatDate = (dateString) => dateString ? formatDateValue(dateString) : '未知时间'

const getAuthorName = (author) => {
  if (!author) return '未知用户'
  if (typeof author === 'string') return author
  return author.nickname || author.username || author.name || '未知用户'
}

const getAuthorLabel = () => {
  if (activeTab.value === 'questions') return '提问者'
  if (activeTab.value === 'comments') return '评论者'
  return '作者'
}

const getItemLink = (item) => {
  if (activeTab.value === 'articles') return `/article/${item.id}`
  if (activeTab.value === 'questions') return `/question/${item.id}`
  return getCommentTargetLink(item)
}

const getItemTitle = (item) => {
  if (activeTab.value === 'comments') return '评论'
  return item.title || '未命名内容'
}

const getItemSummary = (item) => item.summary || item.content || '暂无摘要'

const getCommentTargetLink = (comment) => {
  if (comment.targetType === 'ARTICLE') return `/article/${comment.targetId}`
  if (comment.targetType === 'QUESTION') return `/question/${comment.targetId}`
  return '/user/likes'
}

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || 0,
    totalPages: page.totalPages || 0
  }
}

const loadDetailsById = async (ids, loader) => {
  const results = await Promise.allSettled(ids.map(id => loader(id)))
  return results
    .filter(result => result.status === 'fulfilled')
    .map(result => result.value?.data || result.value)
    .filter(Boolean)
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    if (activeTab.value === 'articles') await loadArticles()
    if (activeTab.value === 'questions') await loadQuestions()
    if (activeTab.value === 'comments') await loadComments()
  } catch (error) {
    errorMessage.value = error.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

const loadArticles = async () => {
  const response = await likeApi.getUserLikedArticles(currentPage.value - 1, pageSize)
  const page = unwrapPage(response)
  articles.value = await loadDetailsById(page.content, id => articleService.getArticleById(id))
  pagination.articles.total = page.totalElements
  pagination.articles.pages = page.totalPages
}

const loadQuestions = async () => {
  const response = await likeApi.getUserLikedQuestions(currentPage.value - 1, pageSize)
  const page = unwrapPage(response)
  questions.value = await loadDetailsById(page.content, id => questionService.getQuestionById(id))
  pagination.questions.total = page.totalElements
  pagination.questions.pages = page.totalPages
}

const loadComments = async () => {
  const response = await likeApi.getUserLikedComments(currentPage.value - 1, pageSize)
  const page = unwrapPage(response)
  comments.value = await loadDetailsById(page.content, id => commentService.getCommentById(id))
  pagination.comments.total = page.totalElements
  pagination.comments.pages = page.totalPages
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadData()
  }
}

watch(activeTab, () => {
  currentPage.value = 1
  loadData()
})

onMounted(loadData)
</script>

<style scoped>
.user-likes-page {
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

.workspace {
  display: grid;
  gap: 1rem;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.45rem;
  height: 1.45rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  font-size: 0.78rem;
}

.content-panel {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.notice {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--kumo-radius-md);
  font-weight: 720;
}

.notice-error {
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.75rem;
  min-height: 16rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel svg {
  color: var(--kumo-bg-brand);
  font-size: 2.2rem;
}

.item-list {
  display: grid;
  gap: 1rem;
}

.content-item {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
}

.item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.item-header a {
  color: var(--kumo-text-default);
  font-size: 1.08rem;
  font-weight: 830;
  text-decoration: none;
}

.item-header span {
  flex: 0 0 auto;
  color: var(--kumo-text-subtle);
  font-size: 0.85rem;
  font-weight: 720;
}

.content-item p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: var(--kumo-text-muted);
  font-size: 0.88rem;
  font-weight: 720;
}

.item-meta span:last-child {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: var(--kumo-status-danger);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 0.85rem;
}

.pagination span {
  color: var(--kumo-text-muted);
  font-weight: 760;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .item-header,
  .item-meta,
  .pagination {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
