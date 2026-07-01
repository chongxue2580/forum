<template>
  <div class="home-view">
    <section v-if="!searchKeyword" class="home-hero kumo-surface-strong reveal-rise">
      <div class="hero-copy">
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'wand-magic-sparkles']" />
          Dev Hub
        </span>
        <h1 class="kumo-heading">科技论坛</h1>
        <p>连接文章、问答与实践经验，让技术讨论更清晰、更高效，也更适合长期沉淀。</p>
        <div class="hero-actions">
          <router-link to="/article/new" class="kumo-button kumo-button--brand">
            <font-awesome-icon :icon="['fas', 'edit']" />
            发布文章
          </router-link>
          <router-link to="/question/new" class="kumo-button">
            <font-awesome-icon :icon="['fas', 'question-circle']" />
            提出问题
          </router-link>
        </div>
      </div>

      <div class="hero-panel">
        <div class="signal-card">
          <span>实时讨论</span>
          <strong>{{ articleCount + questionCount }}</strong>
          <small>条内容正在沉淀</small>
        </div>
        <div class="signal-grid">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </section>

    <section v-else class="search-summary kumo-surface reveal-rise">
      <div>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'search']" />
          搜索结果
        </span>
        <h1 class="kumo-heading">{{ searchKeyword }}</h1>
      </div>
      <router-link to="/" class="kumo-button">
        <font-awesome-icon :icon="['fas', 'times']" />
        清除搜索
      </router-link>
    </section>

    <div class="home-layout">
      <main class="content-column">
        <div class="content-toolbar">
          <div class="kumo-tabs" aria-label="内容类型">
            <button
              class="kumo-tab"
              :class="{ active: activeTab === 'articles' }"
              type="button"
              @click="switchTab('articles')"
            >
              <font-awesome-icon :icon="['fas', 'file-alt']" />
              文章
            </button>
            <button
              class="kumo-tab"
              :class="{ active: activeTab === 'questions' }"
              type="button"
              @click="switchTab('questions')"
            >
              <font-awesome-icon :icon="['fas', 'question-circle']" />
              问答
            </button>
          </div>

          <div v-if="activeTab === 'articles'" class="kumo-tabs compact-tabs" aria-label="文章类型">
            <button
              class="kumo-tab"
              :class="{ active: articleFilterType === 'all' }"
              type="button"
              @click="articleFilterType = 'all'"
            >
              最新
            </button>
            <button
              class="kumo-tab"
              :class="{ active: articleFilterType === 'hot' }"
              type="button"
              @click="articleFilterType = 'hot'"
            >
              热门
            </button>
            <button
              class="kumo-tab"
              :class="{ active: articleFilterType === 'recommended' }"
              type="button"
              @click="articleFilterType = 'recommended'"
            >
              推荐
            </button>
          </div>
        </div>

        <div v-if="isLoading" class="loading-panel kumo-surface">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>正在加载内容...</span>
        </div>

        <template v-else>
          <article-list
            v-if="activeTab === 'articles' && articleFilterType === 'all'"
            :articles="articles"
            :loading="isLoading"
            :show-title="false"
            :current-page="articlePage"
            :total-pages="articleTotalPages"
            :total-items="articleTotalItems"
            :page-size="articlesPerPage"
            @page-change="handleArticlePageChange"
          />

          <article-list
            v-else-if="activeTab === 'articles' && articleFilterType === 'hot'"
            :articles="hotArticles"
            :loading="hotArticlesLoading"
            :show-title="false"
            :show-pagination="false"
          />

          <article-list
            v-else-if="activeTab === 'articles' && articleFilterType === 'recommended'"
            :articles="recommendedArticles"
            :loading="recommendedArticlesLoading"
            :show-title="false"
            :show-pagination="false"
          />

          <question-list
            v-else-if="activeTab === 'questions'"
            :questions="questions"
            :loading="isLoading"
            :current-page="questionPage"
            :total-pages="questionTotalPages"
            :total-items="questionTotalItems"
            :page-size="questionsPerPage"
            @page-change="handleQuestionPageChange"
          />
        </template>
      </main>

      <aside class="home-sidebar">
        <section class="stats-card kumo-surface">
          <div class="section-heading">
            <span class="kumo-eyebrow">社区数据</span>
            <h2 class="kumo-heading">Overview</h2>
          </div>
          <div class="stats-grid">
            <div v-for="stat in stats" :key="stat.label" class="stat-item">
              <span class="stat-icon">
                <font-awesome-icon :icon="['fas', stat.icon]" />
              </span>
              <span class="stat-meta">
                <strong>{{ stat.value }}</strong>
                <span>{{ stat.label }}</span>
                <small>{{ stat.caption }}</small>
              </span>
            </div>
          </div>
        </section>

        <section class="sidebar-section kumo-surface">
          <div class="section-heading inline">
            <h3>热门分类</h3>
            <router-link to="/categories">全部</router-link>
          </div>
          <category-list :categories="categories.slice(0, 4)" />
        </section>

        <section class="sidebar-section kumo-surface">
          <div class="section-heading inline">
            <h3>热门标签</h3>
            <router-link to="/tags">全部</router-link>
          </div>
          <tag-list :tags="popularTags.slice(0, 16)" />
        </section>

        <section class="sidebar-section kumo-surface">
          <div class="section-heading inline">
            <h3>推荐作者</h3>
            <font-awesome-icon v-if="featuredAuthorsLoading" :icon="['fas', 'spinner']" spin />
          </div>
          <div v-if="featuredAuthorsError" class="sidebar-message error">
            {{ featuredAuthorsError }}
          </div>
          <div v-else-if="featuredAuthors.length" class="authors-list">
            <router-link
              v-for="author in featuredAuthors"
              :key="author.id"
              :to="{ name: 'UserProfile', params: { id: String(author.id) } }"
              class="author-item"
            >
              <span class="author-avatar">
                <img v-if="getAuthorAvatar(author)" :src="getAuthorAvatar(author)" :alt="getAuthorName(author)">
                <span v-else>{{ getAuthorInitial(author) }}</span>
              </span>
              <span>
                <strong>{{ getAuthorName(author) }}</strong>
                <small>{{ author.bio || '最近加入' }}</small>
              </span>
            </router-link>
          </div>
          <div v-else class="sidebar-message">暂无推荐作者</div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import ArticleList from '../components/ArticleList.vue'
import CategoryList from '../components/CategoryList.vue'
import TagList from '../components/TagList.vue'
import QuestionList from '../components/QuestionList.vue'
import { questionService } from '../services/questionService'
import { userApi } from '../api/userApi'
import { resolveAvatarFrom } from '../utils/avatar'

const route = useRoute()
const router = useRouter()
const store = useStore()

const activeTab = ref(route.query.tab || 'articles')
const questions = ref([])
const isLoading = ref(true)
const hotArticles = ref([])
const recommendedArticles = ref([])
const hotArticlesLoading = ref(false)
const recommendedArticlesLoading = ref(false)
const featuredAuthors = ref([])
const featuredAuthorsLoading = ref(false)
const featuredAuthorsError = ref('')

const articlePage = ref(1)
const articlesPerPage = ref(10)
const articleTotalPages = ref(1)
const articleTotalItems = ref(0)
const questionPage = ref(1)
const questionsPerPage = ref(10)
const questionTotalPages = ref(1)
const questionTotalItems = ref(0)
const articleFilterType = ref('all')

const articles = computed(() => store.state.articles || [])
const categories = computed(() => store.state.categories || [])
const popularTags = computed(() => store.state.popularTags || [])
const searchKeyword = computed(() => String(route.query.search || '').trim())
const articleCount = computed(() => store.state.articleCount || articles.value.length)
const categoryCount = computed(() => categories.value.length)
const tagCount = computed(() => popularTags.value.length)
const questionCount = computed(() => questionTotalItems.value || questions.value.length || 0)

const stats = computed(() => [
  { label: '文章', value: articleCount.value, caption: '长期沉淀', icon: 'file-alt' },
  { label: '问答', value: questionCount.value, caption: '问题协作', icon: 'question-circle' },
  { label: '分类', value: categoryCount.value, caption: '知识导航', icon: 'folder' },
  { label: '标签', value: tagCount.value, caption: '主题索引', icon: 'tags' }
])

const switchTab = (tab) => {
  activeTab.value = tab
  router.replace({
    query: {
      ...route.query,
      tab
    }
  })
}

const loadHotArticles = async () => {
  hotArticlesLoading.value = true
  try {
    const result = await store.dispatch('fetchHotArticles', { limit: 10 })
    hotArticles.value = result?.data || []
  } catch (error) {
    hotArticles.value = []
  } finally {
    hotArticlesLoading.value = false
  }
}

const loadRecommendedArticles = async () => {
  recommendedArticlesLoading.value = true
  try {
    const result = await store.dispatch('fetchRecommendedArticles', { limit: 10 })
    recommendedArticles.value = result?.data || []
  } catch (error) {
    recommendedArticles.value = []
  } finally {
    recommendedArticlesLoading.value = false
  }
}

const loadFeaturedAuthors = async () => {
  featuredAuthorsLoading.value = true
  featuredAuthorsError.value = ''
  try {
    const result = await userApi.getRecentUsers(4)
    featuredAuthors.value = result?.data || result || []
  } catch (error) {
    featuredAuthors.value = []
    featuredAuthorsError.value = error.message || '推荐作者加载失败'
  } finally {
    featuredAuthorsLoading.value = false
  }
}

const loadQuestions = async () => {
  const questionResponse = await questionService.getQuestions({
    page: questionPage.value,
    pageSize: questionsPerPage.value,
    keyword: searchKeyword.value || undefined
  })

  if (questionResponse.success) {
    if (questionResponse.data?.content) {
      questions.value = questionResponse.data.content || []
      questionTotalItems.value = questionResponse.data.totalElements || 0
      questionTotalPages.value = questionResponse.data.totalPages || 1
    } else if (Array.isArray(questionResponse.data)) {
      questions.value = questionResponse.data || []
      questionTotalItems.value = questionResponse.totalElements || questionResponse.data.length
      questionTotalPages.value = questionResponse.totalPages || 1
    }
  } else {
    questions.value = []
    questionTotalItems.value = 0
    questionTotalPages.value = 1
  }
}

const loadData = async () => {
  isLoading.value = true

  try {
    const articlesResponse = await store.dispatch('fetchArticles', {
      page: articlePage.value,
      pageSize: articlesPerPage.value,
      keyword: searchKeyword.value || undefined
    })

    articleTotalPages.value = articlesResponse?.totalPages || 1
    articleTotalItems.value = articlesResponse?.total || 0

    await Promise.all([
      store.dispatch('fetchCategories'),
      store.dispatch('fetchPopularTags'),
      loadQuestions()
    ])
  } catch (error) {
    store.commit('SET_ERROR', error?.message || '首页内容加载失败')
  } finally {
    isLoading.value = false
  }
}

const handleArticlePageChange = async (page) => {
  articlePage.value = page
  isLoading.value = true

  try {
    const articlesResponse = await store.dispatch('fetchArticles', {
      page: articlePage.value,
      pageSize: articlesPerPage.value,
      keyword: searchKeyword.value || undefined
    })

    articleTotalPages.value = articlesResponse?.totalPages || 1
    articleTotalItems.value = articlesResponse?.total || 0
  } catch (error) {
    store.commit('SET_ERROR', error?.message || '文章分页加载失败')
  } finally {
    isLoading.value = false
  }
}

const handleQuestionPageChange = async (page) => {
  questionPage.value = page
  isLoading.value = true
  try {
    await loadQuestions()
  } catch (error) {
    store.commit('SET_ERROR', error?.message || '问答分页加载失败')
  } finally {
    isLoading.value = false
  }
}

const getAuthorName = (author) => author.nickname || author.username || '用户'
const getAuthorAvatar = (author) => resolveAvatarFrom(author)
const getAuthorInitial = (author) => getAuthorName(author).charAt(0).toUpperCase()

watch(() => route.query.tab, (newTab) => {
  if (newTab) activeTab.value = newTab
})

watch(searchKeyword, async () => {
  articlePage.value = 1
  questionPage.value = 1
  await loadData()
})

watch(articleFilterType, async (newType) => {
  if (newType === 'hot' && hotArticles.value.length === 0) {
    await loadHotArticles()
  } else if (newType === 'recommended' && recommendedArticles.value.length === 0) {
    await loadRecommendedArticles()
  }
})

onMounted(async () => {
  await loadData()
  Promise.all([loadHotArticles(), loadRecommendedArticles(), loadFeaturedAuthors()])
})
</script>

<style scoped>
.home-view {
  display: grid;
  gap: clamp(1rem, 3vw, 1.5rem);
}

.home-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(17rem, 0.4fr);
  gap: 1.5rem;
  min-height: 20rem;
  padding: clamp(1.5rem, 4vw, 3.25rem);
  overflow: hidden;
}

.home-hero::after {
  content: '';
  position: absolute;
  inset: auto -12rem -16rem auto;
  width: 34rem;
  height: 34rem;
  border-radius: 999px;
  background: radial-gradient(circle, var(--kumo-bg-brand-soft), transparent 66%);
  animation: soft-pulse 8s ease-in-out infinite;
}

.hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
}

.hero-copy h1 {
  max-width: 10ch;
  margin: 0;
  font-size: clamp(2.45rem, 6vw, 4.8rem);
  line-height: 0.98;
}

.hero-copy p {
  max-width: 38rem;
  margin: 0;
  color: var(--kumo-text-muted);
  font-size: clamp(0.98rem, 1.5vw, 1.08rem);
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 0.4rem;
}

.hero-panel {
  position: relative;
  z-index: 1;
  display: grid;
  align-content: center;
  gap: 1rem;
}

.signal-card {
  display: grid;
  gap: 0.2rem;
  padding: 1.25rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  box-shadow: var(--kumo-shadow-sm);
}

.signal-card span,
.signal-card small {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.signal-card strong {
  color: var(--kumo-bg-brand-strong);
  font-size: 2.45rem;
  font-weight: 900;
  line-height: 1;
}

.signal-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.signal-grid span {
  aspect-ratio: 1;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background:
    linear-gradient(135deg, var(--kumo-bg-brand-soft), transparent),
    var(--kumo-bg-elevated);
}

.search-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: clamp(1.2rem, 3vw, 2rem);
}

.search-summary h1 {
  margin: 0.5rem 0 0;
  font-size: clamp(2rem, 5vw, 4rem);
}

.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(18rem, 22rem);
  gap: 1.25rem;
  align-items: start;
}

.content-column {
  display: grid;
  gap: 1rem;
}

.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.compact-tabs .kumo-tab {
  min-height: 2.1rem;
  padding-inline: 0.75rem;
}

.loading-panel {
  display: grid;
  place-items: center;
  gap: 0.75rem;
  min-height: 18rem;
  color: var(--kumo-text-muted);
}

.loading-panel svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}

.home-sidebar {
  position: sticky;
  top: 6rem;
  display: grid;
  gap: 1rem;
}

.stats-card,
.sidebar-section {
  display: grid;
  gap: 1rem;
  padding: 1.1rem;
}

.section-heading {
  display: grid;
  gap: 0.5rem;
}

.section-heading.inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-heading h2,
.section-heading h3 {
  margin: 0;
  color: var(--kumo-text-default);
  font-weight: 840;
}

.section-heading h2 {
  font-size: 1.15rem;
}

.section-heading a {
  color: var(--kumo-bg-brand-strong);
  font-weight: 780;
  text-decoration: none;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.55rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-height: 4.3rem;
  padding: 0.75rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-subtle);
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    background-color var(--kumo-transition);
}

.stat-item:hover {
  transform: translateY(-1px);
  border-color: var(--kumo-hairline-strong);
  background: var(--kumo-bg-elevated);
}

.stat-icon {
  width: 2.35rem;
  height: 2.35rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: var(--kumo-radius-sm);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.stat-meta {
  display: grid;
  min-width: 0;
  gap: 0.08rem;
}

.stat-item strong {
  color: var(--kumo-text-default);
  font-size: 1.25rem;
  font-weight: 890;
  line-height: 1;
}

.stat-meta > span {
  color: var(--kumo-text-muted);
  font-size: 0.8rem;
  font-weight: 740;
}

.stat-item small {
  color: var(--kumo-text-subtle);
  font-size: 0.72rem;
  font-weight: 680;
}

.authors-list {
  display: grid;
  gap: 0.7rem;
}

.author-item {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  color: var(--kumo-text-default);
  text-decoration: none;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.55rem;
  height: 2.55rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 850;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-item span:last-child {
  display: grid;
  min-width: 0;
}

.author-item strong {
  overflow: hidden;
  font-weight: 820;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-item small {
  overflow: hidden;
  color: var(--kumo-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-message {
  color: var(--kumo-text-muted);
  font-weight: 720;
}

.sidebar-message.error {
  color: var(--kumo-status-danger);
}

@media (max-width: 1080px) {
  .home-layout,
  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-sidebar {
    position: static;
  }
}

@media (max-width: 680px) {
  .content-toolbar,
  .search-summary {
    align-items: stretch;
    flex-direction: column;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
