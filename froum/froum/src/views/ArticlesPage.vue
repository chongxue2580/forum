<template>
  <div class="articles-page">
    <ui-page-hero
      :title="pageTitle"
      :description="pageDescription"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'file-alt']" />
          Articles
        </span>
      </template>
      <template #actions>
        <router-link to="/article/new" class="kumo-button kumo-button--brand">
          <font-awesome-icon :icon="['fas', 'edit']" />
          发布文章
        </router-link>
      </template>
      <template #aside>
        <div class="hero-metric">
          <span>当前频道</span>
          <strong>{{ metricValue }}</strong>
          <small>{{ metricLabel }}</small>
        </div>
      </template>
    </ui-page-hero>

    <section class="articles-shell">
      <div class="kumo-tabs" aria-label="文章频道">
        <button
          class="kumo-tab"
          :class="{ active: activeTab === 'all' }"
          type="button"
          @click="setActiveTab('all')"
        >
          <font-awesome-icon :icon="['fas', 'list']" />
          所有文章
        </button>
        <button
          class="kumo-tab"
          :class="{ active: activeTab === 'hot' }"
          type="button"
          @click="setActiveTab('hot')"
        >
          <font-awesome-icon :icon="['fas', 'fire']" />
          热门
        </button>
        <button
          class="kumo-tab"
          :class="{ active: activeTab === 'recommended' }"
          type="button"
          @click="setActiveTab('recommended')"
        >
          <font-awesome-icon :icon="['fas', 'thumbs-up']" />
          推荐
        </button>
      </div>

      <article-list
        v-if="activeTab === 'all'"
        :articles="articles"
        :loading="isLoading"
        title="所有文章"
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-items="totalItems"
        :page-size="pageSize"
        @page-change="handlePageChange"
      />

      <article-list
        v-else-if="activeTab === 'hot'"
        :articles="hotArticles"
        :loading="isLoading"
        title="热门文章"
        :current-page="hotPage"
        :total-pages="hotTotalPages"
        :total-items="hotTotalItems"
        :page-size="pageSize"
        @page-change="handleHotPageChange"
      />

      <article-list
        v-else
        :articles="recommendedArticles"
        :loading="isLoading"
        title="推荐文章"
        :current-page="recommendedPage"
        :total-pages="recommendedTotalPages"
        :total-items="recommendedTotalItems"
        :page-size="pageSize"
        @page-change="handleRecommendedPageChange"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import ArticleList from '../components/ArticleList.vue'
import UiPageHero from '../components/ui/UiPageHero.vue'

const route = useRoute()
const router = useRouter()
const store = useStore()

const isLoading = ref(true)
const activeTab = ref(route.query.tab || 'all')
const pageSize = ref(10)
const currentPage = ref(Number(route.query.page) || 1)
const totalPages = ref(1)
const totalItems = ref(0)
const hotArticles = ref([])
const hotPage = ref(Number(route.query.hotPage) || 1)
const hotTotalPages = ref(1)
const hotTotalItems = ref(0)
const recommendedArticles = ref([])
const recommendedPage = ref(Number(route.query.recommendedPage) || 1)
const recommendedTotalPages = ref(1)
const recommendedTotalItems = ref(0)

const articles = computed(() => store.state.articles || [])

const pageTitle = computed(() => {
  if (activeTab.value === 'hot') return '热门文章'
  if (activeTab.value === 'recommended') return '推荐文章'
  return '所有文章'
})

const pageDescription = computed(() => {
  if (activeTab.value === 'hot') return '查看社区近期浏览与讨论热度最高的内容。'
  if (activeTab.value === 'recommended') return '聚合更值得收藏和继续阅读的技术文章。'
  return '按时间浏览社区沉淀的技术分享、实践笔记与经验复盘。'
})

const metricValue = computed(() => {
  if (activeTab.value === 'hot') return hotTotalItems.value
  if (activeTab.value === 'recommended') return recommendedTotalItems.value
  return totalItems.value
})

const metricLabel = computed(() => activeTab.value === 'all' ? '篇文章' : '篇精选内容')

const setActiveTab = (tab) => {
  activeTab.value = tab
  updateQueryParams()
}

const updateQueryParams = () => {
  const query = { ...route.query, tab: activeTab.value }

  if (activeTab.value === 'all') query.page = currentPage.value
  if (activeTab.value === 'hot') query.hotPage = hotPage.value
  if (activeTab.value === 'recommended') query.recommendedPage = recommendedPage.value

  router.push({ query })
}

const loadArticles = async () => {
  isLoading.value = true
  try {
    const response = await store.dispatch('fetchArticles', {
      page: currentPage.value,
      pageSize: pageSize.value
    })
    totalPages.value = response?.totalPages || 1
    totalItems.value = response?.total || 0
  } catch (error) {
    store.commit('SET_ERROR', error?.message || '文章加载失败')
  } finally {
    isLoading.value = false
  }
}

const loadHotArticles = async () => {
  isLoading.value = true
  try {
    const response = await store.dispatch('fetchHotArticles', { limit: 50 })
    const allHotArticles = response?.data || []
    hotTotalItems.value = allHotArticles.length
    hotTotalPages.value = Math.max(1, Math.ceil(allHotArticles.length / pageSize.value))
    const offset = (hotPage.value - 1) * pageSize.value
    hotArticles.value = allHotArticles.slice(offset, offset + pageSize.value)
  } catch (error) {
    hotArticles.value = []
    store.commit('SET_ERROR', error?.message || '热门文章加载失败')
  } finally {
    isLoading.value = false
  }
}

const loadRecommendedArticles = async () => {
  isLoading.value = true
  try {
    const response = await store.dispatch('fetchRecommendedArticles', { limit: 50 })
    const allRecommendedArticles = response?.data || []
    recommendedTotalItems.value = allRecommendedArticles.length
    recommendedTotalPages.value = Math.max(1, Math.ceil(allRecommendedArticles.length / pageSize.value))
    const offset = (recommendedPage.value - 1) * pageSize.value
    recommendedArticles.value = allRecommendedArticles.slice(offset, offset + pageSize.value)
  } catch (error) {
    recommendedArticles.value = []
    store.commit('SET_ERROR', error?.message || '推荐文章加载失败')
  } finally {
    isLoading.value = false
  }
}

const loadActiveTabData = () => {
  if (activeTab.value === 'hot') {
    loadHotArticles()
  } else if (activeTab.value === 'recommended') {
    loadRecommendedArticles()
  } else {
    loadArticles()
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  updateQueryParams()
  loadArticles()
}

const handleHotPageChange = (page) => {
  hotPage.value = page
  updateQueryParams()
  loadHotArticles()
}

const handleRecommendedPageChange = (page) => {
  recommendedPage.value = page
  updateQueryParams()
  loadRecommendedArticles()
}

watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== activeTab.value) {
    activeTab.value = newTab
    loadActiveTabData()
  }
})

onMounted(loadActiveTabData)
</script>

<style scoped>
.articles-page {
  display: grid;
  gap: 1.25rem;
}

.hero-metric {
  display: grid;
  gap: 0.2rem;
  padding: 1.2rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.hero-metric span,
.hero-metric small {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.hero-metric strong {
  color: var(--kumo-bg-brand-strong);
  font-size: 3.4rem;
  font-weight: 900;
  line-height: 1;
}

.articles-shell {
  display: grid;
  gap: 1rem;
}
</style>
