<template>
  <div class="category-detail-container">
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <p>正在加载内容...</p>
    </div>
    
    <div v-else-if="category" class="category-content">
      <div class="category-header">
        <div class="category-icon">
          <font-awesome-icon :icon="getCategoryIcon(category)" />
        </div>
        <div class="category-info">
          <h1>{{ category.name }}</h1>
          <p class="category-description">{{ category.description }}</p>
          <div class="category-stats">
            <span><font-awesome-icon :icon="['fas', 'file-alt']" /> {{ category.articleCount || 0 }} 篇文章</span>
            <span><font-awesome-icon :icon="['fas', 'user']" /> {{ category.followers || 0 }} 位关注者</span>
          </div>
        </div>
      </div>

      <div class="subcategories-section" v-if="category.subcategories && category.subcategories.length > 0">
        <h2>子分类</h2>
        <div class="subcategory-chips">
          <router-link 
            v-for="subcategory in category.subcategories" 
            :key="subcategory.id" 
            :to="`/category/${category.id}?subcategory=${subcategory.id}`"
            class="subcategory-chip"
            :class="{ active: selectedSubcategory === subcategory.id }"
          >
            {{ subcategory.name }}
            <span class="count">({{ subcategory.articleCount }})</span>
          </router-link>
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-header">
          <h2>文章列表</h2>
          <div class="sort-options">
            <div 
              class="sort-option" 
              :class="{ active: sortBy === 'newest' }"
              @click="changeSortOption('newest')"
            >
              最新发布
            </div>
            <div 
              class="sort-option" 
              :class="{ active: sortBy === 'hottest' }"
              @click="changeSortOption('hottest')"
            >
              热门文章
            </div>
            <div 
              class="sort-option" 
              :class="{ active: sortBy === 'mostComments' }"
              @click="changeSortOption('mostComments')"
            >
              评论最多
            </div>
          </div>
        </div>
      </div>

      <article-list 
        :articles="filteredArticles" 
        :loading="articlesLoading"
        :show-title="false"
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-items="totalItems"
        :page-size="pageSize"
        @page-change="handlePageChange"
      />

      <div v-if="!articlesLoading && filteredArticles.length === 0" class="no-articles">
        <div class="empty-state">
          <font-awesome-icon :icon="['fas', 'file-alt']" class="empty-icon" />
          <p>该分类下暂无文章</p>
          <router-link to="/article/new" class="btn btn-primary">
            发表第一篇文章
          </router-link>
        </div>
      </div>
    </div>

    <div v-else class="error-container">
      <div class="error-message">
        <font-awesome-icon :icon="['fas', 'exclamation-triangle']" class="error-icon" />
        <p>未找到该分类</p>
        <router-link to="/categories" class="btn btn-secondary">
          返回分类列表
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import ArticleList from '../components/ArticleList.vue'

export default defineComponent({
  name: 'CategoryDetailView',
  components: {
    ArticleList
  },
  props: {
    categoryId: {
      type: [String, Number],
      required: true
    }
  },
  setup(props) {
    const store = useStore()
    const route = useRoute()
    const loading = ref(true)
    const articlesLoading = ref(true)
    const category = ref(null)
    const articles = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const totalItems = ref(0)
    const sortBy = ref('newest')
    
    const selectedSubcategory = computed(() => {
      const subcatId = route.query.subcategory
      return subcatId ? Number(subcatId) : null
    })

    const totalPages = computed(() => {
      return Math.ceil(totalItems.value / pageSize.value)
    })

    const filteredArticles = computed(() => {
      if (!selectedSubcategory.value) {
        return articles.value
      }
      return articles.value.filter(article => {
        // 这里假设文章有subcategoryId属性
        // 实际项目中根据数据结构调整此处的逻辑
        return article.subcategoryId === selectedSubcategory.value
      })
    })

    const getCategoryIcon = (category) => {
      // 根据不同分类返回不同图标
      const icons = {
        '前端开发': ['fas', 'code'],
        '后端开发': ['fas', 'server'],
        '移动开发': ['fas', 'mobile-alt'],
        '人工智能': ['fas', 'robot'],
        '开发工具': ['fas', 'tools']
      }
      
      // 默认图标
      return icons[category.name] || ['fas', 'folder'];
    }

    const loadCategoryData = async () => {
      try {
        loading.value = true
        
        // 从store中获取已加载的分类
        let storeCategories = store.state.categories
        
        if (storeCategories.length === 0) {
          // 如果没有加载过分类，则加载分类
          await store.dispatch('fetchCategories')
          storeCategories = store.state.categories
        }
        
        // 根据ID查找当前分类
        const foundCategory = storeCategories.find(c => c.id === Number(props.categoryId))
        category.value = foundCategory || null
        
        if (foundCategory) {
          await loadArticles()
        }
      } catch (error) {
        console.error('加载分类数据失败:', error)
      } finally {
        loading.value = false
      }
    }

    const loadArticles = async () => {
      try {
        articlesLoading.value = true
        
        // 调用获取文章的action，传入分类ID作为过滤条件
        const response = await store.dispatch('fetchArticles', {
          page: currentPage.value,
          pageSize: pageSize.value,
          categoryId: props.categoryId,
          sort: sortBy.value
        })
        
        articles.value = response.data || []
        totalItems.value = response.total || 0
      } catch (error) {
        console.error('加载文章失败:', error)
      } finally {
        articlesLoading.value = false
      }
    }

    const handlePageChange = (page) => {
      currentPage.value = page
      loadArticles()
    }

    const changeSortOption = (option) => {
      sortBy.value = option
      currentPage.value = 1
      loadArticles()
    }

    // 监听分类ID和子分类变化，重新加载数据
    watch(() => [props.categoryId, route.query.subcategory], () => {
      loadCategoryData()
    })

    onMounted(() => {
      loadCategoryData()
    })

    return {
      loading,
      articlesLoading,
      category,
      filteredArticles,
      currentPage,
      pageSize,
      totalItems,
      totalPages,
      selectedSubcategory,
      sortBy,
      getCategoryIcon,
      handlePageChange,
      changeSortOption
    }
  }
})
</script>

<style scoped>
.category-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

.loading-container, .error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.loading-spinner, .error-icon {
  font-size: 2rem;
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.category-header {
  display: flex;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.category-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary-light), var(--primary-color));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  color: white;
  margin-right: 1.5rem;
  flex-shrink: 0;
  box-shadow: var(--shadow);
}

.category-info {
  flex: 1;
}

.category-info h1 {
  margin: 0 0 0.5rem 0;
  color: var(--text-color);
  font-size: 1.75rem;
}

.category-description {
  margin: 0 0 1rem 0;
  color: var(--text-light);
  font-size: 1rem;
}

.category-stats {
  display: flex;
  gap: 1rem;
}

.category-stats span {
  color: var(--text-light);
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.subcategories-section {
  margin-bottom: 2rem;
}

.subcategories-section h2 {
  font-size: 1.25rem;
  margin-bottom: 1rem;
  color: var(--text-color);
}

.subcategory-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.subcategory-chip {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  background-color: #f5f5f5;
  text-decoration: none;
  color: var(--text-color);
  transition: all 0.2s ease;
  font-size: 0.875rem;
}

.subcategory-chip:hover {
  background-color: #e0e0e0;
}

.subcategory-chip.active {
  background-color: var(--primary-color);
  color: white;
}

.subcategory-chip .count {
  margin-left: 0.25rem;
  opacity: 0.7;
  font-size: 0.75rem;
}

.filter-section {
  margin-bottom: 1.5rem;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.filter-header h2 {
  font-size: 1.25rem;
  margin: 0;
  color: var(--text-color);
}

.sort-options {
  display: flex;
  gap: 1rem;
}

.sort-option {
  color: var(--text-light);
  cursor: pointer;
  font-size: 0.875rem;
  transition: color 0.2s ease;
}

.sort-option:hover {
  color: var(--primary-color);
}

.sort-option.active {
  color: var(--primary-color);
  font-weight: 600;
}

.no-articles {
  padding: 3rem 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--text-light);
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin: 0.5rem 0 1.5rem;
  color: var(--text-color);
  font-size: 1rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 20px;
  font-weight: 600;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  text-decoration: none;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  box-shadow: var(--shadow);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.btn-secondary {
  background-color: #f5f5f5;
  color: var(--text-color);
}

.btn-secondary:hover {
  background-color: #e0e0e0;
}

@media (max-width: 768px) {
  .category-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .category-icon {
    margin-bottom: 1rem;
    margin-right: 0;
  }
  
  .filter-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .sort-options {
    margin-top: 1rem;
  }
}
</style> 