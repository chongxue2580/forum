<template>
  <div class="articles-page">
    <div class="page-header">
      <h1>{{ pageTitle }}</h1>
      <p class="page-description">{{ pageDescription }}</p>
    </div>

    <div class="content-tabs">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'all' }"
        @click="setActiveTab('all')"
      >
        <font-awesome-icon :icon="['fas', 'list']" />
        所有文章
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'hot' }"
        @click="setActiveTab('hot')"
      >
        <font-awesome-icon :icon="['fas', 'fire']" />
        热门文章
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'recommended' }"
        @click="setActiveTab('recommended')"
      >
        <font-awesome-icon :icon="['fas', 'thumbs-up']" />
        推荐文章
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <p>正在加载文章...</p>
    </div>
    
    <!-- 文章列表 -->
    <div v-else-if="activeTab === 'all'" class="articles-container">
      <article-list 
        :articles="articles" 
        :loading="isLoading"
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-items="totalItems"
        :page-size="pageSize"
        @page-change="handlePageChange"
      />
    </div>
    
    <!-- 热门文章列表 -->
    <div v-else-if="activeTab === 'hot'" class="articles-container">
      <article-list 
        :articles="hotArticles" 
        :loading="isLoading"
        title="热门文章"
        :current-page="hotPage"
        :total-pages="hotTotalPages"
        :total-items="hotTotalItems"
        :page-size="pageSize"
        @page-change="handleHotPageChange"
      />
    </div>
    
    <!-- 推荐文章列表 -->
    <div v-else-if="activeTab === 'recommended'" class="articles-container">
      <article-list 
        :articles="recommendedArticles" 
        :loading="isLoading"
        title="推荐文章"
        :current-page="recommendedPage"
        :total-pages="recommendedTotalPages"
        :total-items="recommendedTotalItems"
        :page-size="pageSize"
        @page-change="handleRecommendedPageChange"
      />
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import ArticleList from '../components/ArticleList.vue'

export default defineComponent({
  name: 'ArticlesPage',
  components: {
    ArticleList
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    
    // 状态
    const isLoading = ref(true)
    const activeTab = ref(route.query.tab || 'all')
    const pageSize = ref(10)
    
    // 所有文章相关
    const articles = computed(() => store.state.articles || [])
    const currentPage = ref(Number(route.query.page) || 1)
    const totalPages = ref(1)
    const totalItems = ref(0)
    
    // 热门文章相关
    const hotArticles = ref([])
    const hotPage = ref(Number(route.query.hotPage) || 1)
    const hotTotalPages = ref(1)
    const hotTotalItems = ref(0)
    
    // 推荐文章相关
    const recommendedArticles = ref([])
    const recommendedPage = ref(Number(route.query.recommendedPage) || 1)
    const recommendedTotalPages = ref(1)
    const recommendedTotalItems = ref(0)
    
    // 计算属性
    const pageTitle = computed(() => {
      switch(activeTab.value) {
        case 'hot': return '热门文章';
        case 'recommended': return '推荐文章';
        default: return '所有文章';
      }
    })
    
    const pageDescription = computed(() => {
      switch(activeTab.value) {
        case 'hot': return '浏览量最高的精选文章';
        case 'recommended': return '受用户喜爱的优质内容';
        default: return '探索所有文章，发现精彩内容';
      }
    })
    
    // 方法
    const setActiveTab = (tab) => {
      activeTab.value = tab
      updateQueryParams()
    }
    
    const updateQueryParams = () => {
      const query = { ...route.query, tab: activeTab.value }
      
      // 根据不同的标签页设置页码参数
      if (activeTab.value === 'all') {
        query.page = currentPage.value
      } else if (activeTab.value === 'hot') {
        query.hotPage = hotPage.value
      } else if (activeTab.value === 'recommended') {
        query.recommendedPage = recommendedPage.value
      }
      
      router.push({ query })
    }
    
    // 加载所有文章
    const loadArticles = async () => {
      isLoading.value = true
      try {
        const response = await store.dispatch('fetchArticles', {
          page: currentPage.value,
          pageSize: pageSize.value
        })
        
        if (response) {
          totalPages.value = response.totalPages || 1
          totalItems.value = response.total || 0
        }
      } catch (error) {
        console.error('Error loading articles:', error)
      } finally {
        isLoading.value = false
      }
    }
    
    // 加载热门文章
    const loadHotArticles = async () => {
      isLoading.value = true
      try {
        // 获取热门文章
        const response = await store.dispatch('fetchHotArticles', { 
          limit: 50 // 获取较多文章用于客户端分页
        })
        
        if (response && response.data) {
          const allHotArticles = response.data
          hotTotalItems.value = allHotArticles.length
          hotTotalPages.value = Math.ceil(allHotArticles.length / pageSize.value)
          
          // 客户端分页
          const offset = (hotPage.value - 1) * pageSize.value
          hotArticles.value = allHotArticles.slice(offset, offset + pageSize.value)
        } else {
          console.error('热门文章加载失败: 无数据返回')
          hotArticles.value = []
        }
      } catch (error) {
        console.error('加载热门文章出错:', error)
        hotArticles.value = []
      } finally {
        isLoading.value = false
      }
    }
    
    // 加载推荐文章
    const loadRecommendedArticles = async () => {
      isLoading.value = true
      try {
        // 获取推荐文章
        const response = await store.dispatch('fetchRecommendedArticles', { 
          limit: 50 // 获取较多文章用于客户端分页
        })
        
        if (response && response.data) {
          const allRecommendedArticles = response.data
          recommendedTotalItems.value = allRecommendedArticles.length
          recommendedTotalPages.value = Math.ceil(allRecommendedArticles.length / pageSize.value)
          
          // 客户端分页
          const offset = (recommendedPage.value - 1) * pageSize.value
          recommendedArticles.value = allRecommendedArticles.slice(offset, offset + pageSize.value)
        } else {
          console.error('推荐文章加载失败: 无数据返回')
          recommendedArticles.value = []
        }
      } catch (error) {
        console.error('加载推荐文章出错:', error)
        recommendedArticles.value = []
      } finally {
        isLoading.value = false
      }
    }
    
    // 根据当前标签页加载相应数据
    const loadActiveTabData = () => {
      switch(activeTab.value) {
        case 'hot':
          loadHotArticles()
          break
        case 'recommended':
          loadRecommendedArticles()
          break
        default:
          loadArticles()
      }
    }
    
    // 分页处理
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
    
    // 监听路由变化
    watch(() => route.query, (newQuery) => {
      if (newQuery.tab && newQuery.tab !== activeTab.value) {
        activeTab.value = newQuery.tab
        loadActiveTabData()
      }
      
      // 同步页码
      if (newQuery.page && Number(newQuery.page) !== currentPage.value) {
        currentPage.value = Number(newQuery.page) || 1
        if (activeTab.value === 'all') loadArticles()
      }
      
      if (newQuery.hotPage && Number(newQuery.hotPage) !== hotPage.value) {
        hotPage.value = Number(newQuery.hotPage) || 1
        if (activeTab.value === 'hot') loadHotArticles()
      }
      
      if (newQuery.recommendedPage && Number(newQuery.recommendedPage) !== recommendedPage.value) {
        recommendedPage.value = Number(newQuery.recommendedPage) || 1
        if (activeTab.value === 'recommended') loadRecommendedArticles()
      }
    }, { deep: true })
    
    // 初始化
    onMounted(() => {
      // 根据URL参数设置初始标签
      if (route.query.tab) {
        activeTab.value = route.query.tab
      }
      
      // 确保总是加载数据
      loadActiveTabData()
    })
    
    return {
      isLoading,
      activeTab,
      articles,
      hotArticles,
      recommendedArticles,
      currentPage,
      totalPages,
      totalItems,
      hotPage,
      hotTotalPages,
      hotTotalItems,
      recommendedPage,
      recommendedTotalPages,
      recommendedTotalItems,
      pageSize,
      pageTitle,
      pageDescription,
      setActiveTab,
      handlePageChange,
      handleHotPageChange,
      handleRecommendedPageChange
    }
  }
})
</script>

<style scoped>
.articles-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
  text-align: center;
}

.page-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.page-description {
  color: var(--text-light);
  font-size: 1.1rem;
  max-width: 600px;
  margin: 0 auto;
}

.content-tabs {
  display: flex;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  margin-bottom: 1.5rem;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  cursor: pointer;
  font-weight: 500;
  color: var(--text-light);
  transition: var(--transition);
  border-bottom: 2px solid transparent;
}

.tab-item:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

.tab-item.active {
  color: var(--primary-color);
  background-color: var(--bg-white);
  border-bottom: 2px solid var(--primary-color);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 0;
  color: var(--text-lighter);
}

.loading-spinner {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 1.75rem;
  }
  
  .page-description {
    font-size: 1rem;
  }
  
  .tab-item {
    padding: 0.75rem 0.5rem;
    font-size: 0.9rem;
  }
}

@media (max-width: 576px) {
  .page-header h1 {
    font-size: 1.5rem;
  }
  
  .page-description {
    font-size: 0.9rem;
  }
  
  .tab-item {
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.5rem;
    font-size: 0.8rem;
  }
}
</style> 
