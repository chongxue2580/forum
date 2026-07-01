<template>
  <div class="category-detail-container">
    <div v-if="loading" class="state-panel kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>正在加载内容...</span>
    </div>

    <template v-else-if="category">
      <ui-page-hero
        :title="category.name"
        :description="category.description || '浏览该分类下的文章和讨论内容。'"
      >
        <template #eyebrow>
          <span class="kumo-eyebrow">
            <font-awesome-icon :icon="getCategoryIcon(category)" />
            Category
          </span>
        </template>
        <template #aside>
          <div class="hero-count">
            <strong>{{ category.articleCount || totalItems || 0 }}</strong>
            <span>篇文章</span>
            <small>{{ category.followers || 0 }} 位关注者</small>
          </div>
        </template>
      </ui-page-hero>

      <section v-if="category.subcategories && category.subcategories.length > 0" class="subcategory-panel kumo-surface">
        <div class="section-heading">
          <h2 class="kumo-heading">子分类</h2>
        </div>
        <div class="subcategory-chips">
          <router-link
            v-for="subcategory in category.subcategories"
            :key="subcategory.id"
            :to="`/category/${category.id}?subcategory=${subcategory.id}`"
            class="kumo-button"
            :class="{ 'kumo-button--brand': selectedSubcategory === subcategory.id }"
          >
            {{ subcategory.name }}
            <span>{{ subcategory.articleCount }}</span>
          </router-link>
        </div>
      </section>

      <section class="article-panel">
        <div class="toolbar kumo-surface">
          <h2 class="kumo-heading">文章列表</h2>
          <div class="kumo-tabs" aria-label="排序方式">
            <button class="kumo-tab" :class="{ active: sortBy === 'newest' }" type="button" @click="changeSortOption('newest')">最新</button>
            <button class="kumo-tab" :class="{ active: sortBy === 'hottest' }" type="button" @click="changeSortOption('hottest')">热门</button>
            <button class="kumo-tab" :class="{ active: sortBy === 'mostComments' }" type="button" @click="changeSortOption('mostComments')">评论最多</button>
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

        <div v-if="!articlesLoading && filteredArticles.length === 0" class="state-panel kumo-surface">
          <font-awesome-icon :icon="['fas', 'file-alt']" />
          <span>该分类下暂无文章</span>
          <router-link to="/article/new" class="kumo-button kumo-button--brand">发表第一篇文章</router-link>
        </div>
      </section>
    </template>

    <div v-else class="state-panel kumo-surface error">
      <font-awesome-icon :icon="['fas', 'exclamation-triangle']" />
      <span>未找到该分类</span>
      <router-link to="/categories" class="kumo-button">返回分类列表</router-link>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import ArticleList from '../components/ArticleList.vue'
import UiPageHero from '../components/ui/UiPageHero.vue'

export default defineComponent({
  name: 'CategoryDetailView',
  components: {
    ArticleList,
    UiPageHero
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
        store.commit('SET_ERROR', error?.message || '加载分类数据失败')
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

        // fetchArticles 出错时可能返回 undefined，做兜底避免详情页静默崩溃
        articles.value = response?.data || []
        totalItems.value = response?.total || 0
      } catch (error) {
        store.commit('SET_ERROR', error?.message || '加载文章失败')
        articles.value = []
        totalItems.value = 0
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

.hero-count span,
.hero-count small {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.subcategory-panel,
.toolbar {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.section-heading h2,
.toolbar h2 {
  margin: 0;
  font-size: 1.45rem;
}

.subcategory-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.article-panel {
  display: grid;
  gap: 1rem;
}

.toolbar {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
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
  font-size: 2.2rem;
}

.state-panel.error {
  color: var(--kumo-status-danger);
}

@media (max-width: 760px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>