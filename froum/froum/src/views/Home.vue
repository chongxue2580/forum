<template>
  <div class="home-container">
    <!-- 主要内容区 -->
    <div class="main-content">
      <div class="welcome-banner" v-if="!route.query.search">
        <div class="welcome-content">
          <h1>欢迎来到科技论坛</h1>
          <p>分享技术，交流经验，共同成长</p>
          <div class="banner-actions">
            <router-link to="/article/new" class="btn btn-primary">
              <font-awesome-icon :icon="['fas', 'edit']" />
              发布文章
            </router-link>
            <router-link to="/questions" class="btn btn-secondary">
              <font-awesome-icon :icon="['fas', 'question-circle']" />
              提问
            </router-link>
          </div>
        </div>
        <div class="welcome-image">
          <div class="graphic-element"></div>
        </div>
      </div>
      
      <!-- 搜索结果提示 -->
      <div class="search-results-header" v-if="route.query.search">
        <h2>
          <font-awesome-icon :icon="['fas', 'search']" class="search-icon" />
          <span>{{ route.query.search }}</span> 的搜索结果
        </h2>
        <router-link to="/" class="clear-search">
          <font-awesome-icon :icon="['fas', 'times']" />
          清除搜索
        </router-link>
      </div>
      
      <!-- 内容类型选项卡 -->
      <div class="content-tabs">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'articles' }"
          @click="switchTab('articles')"
        >
          <font-awesome-icon :icon="['fas', 'file-alt']" />
          文章
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'questions' }"
          @click="switchTab('questions')"
        >
          <font-awesome-icon :icon="['fas', 'question-circle']" />
          问答
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-container">
        <div class="loading-spinner">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
        </div>
        <p>正在加载内容...</p>
      </div>
      
      <!-- 内容展示区域 -->
      <div class="content-display" v-else>
        <!-- 文章内容 -->
        <div v-if="activeTab === 'articles'" class="articles-container">
          <!-- 文章头部导航 -->
          <div class="articles-header">
            <h2 class="articles-title">最新文章</h2>
            
            <!-- 文章类型导航链接 -->
            <div class="article-filter-tabs">
              <div 
                class="filter-tab"
                :class="{ active: articleFilterType === 'all' }"
                @click="articleFilterType = 'all'"
              >
                所有文章
              </div>
              <div 
                class="filter-tab"
                :class="{ active: articleFilterType === 'hot' }"
                @click="articleFilterType = 'hot'"
              >
                热门文章
              </div>
              <div 
                class="filter-tab"
                :class="{ active: articleFilterType === 'recommended' }"
                @click="articleFilterType = 'recommended'"
              >
                推荐文章
              </div>
            </div>
          </div>
          
          <!-- 文章列表区域 -->
          <article-list 
            v-if="articleFilterType === 'all'"
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
            v-else-if="articleFilterType === 'hot'"
            :articles="hotArticles" 
            :loading="hotArticlesLoading"
            :show-title="false"
            :show-pagination="false"
          />
          
          <article-list 
            v-else-if="articleFilterType === 'recommended'"
            :articles="recommendedArticles"
            :loading="recommendedArticlesLoading"
            :show-title="false"
            :show-pagination="false"
          />
        </div>
        
        <!-- 问答列表 -->
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
      </div>
    </div>
    
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="stats-card sidebar-section">
        <h3>社区数据</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ articleCount }}</div>
            <div class="stat-label">文章</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ questionCount }}</div>
            <div class="stat-label">问答</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ categoryCount }}</div>
            <div class="stat-label">分类</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ tagCount }}</div>
            <div class="stat-label">标签</div>
          </div>
        </div>
      </div>
      
      <div class="sidebar-section">
        <h3>热门分类</h3>
        <category-list :categories="categories.slice(0, 4)" />
        <router-link to="/categories" class="view-all-link">
          查看全部分类
          <font-awesome-icon :icon="['fas', 'arrow-right']" />
        </router-link>
      </div>
      
      <div class="sidebar-section">
        <h3>热门标签</h3>
        <tag-list :tags="popularTags" />
        <router-link to="/tags" class="view-all-link">
          查看全部标签
          <font-awesome-icon :icon="['fas', 'arrow-right']" />
        </router-link>
      </div>

      <div class="sidebar-section featured-authors">
        <h3>推荐作者</h3>
        <div v-if="featuredAuthorsLoading" class="sidebar-empty">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>加载中...</span>
        </div>
        <div v-else-if="featuredAuthorsError" class="sidebar-empty error">
          {{ featuredAuthorsError }}
        </div>
        <div v-else-if="featuredAuthors.length > 0" class="authors-list">
          <router-link 
            v-for="author in featuredAuthors" 
            :key="author.id" 
            :to="{ name: 'UserProfile', params: { id: String(author.id) }}" 
            class="author-item"
          >
            <div class="author-avatar">
              <img v-if="getAuthorAvatar(author)" :src="getAuthorAvatar(author)" :alt="getAuthorName(author)" />
              <span v-else>{{ getAuthorInitial(author) }}</span>
            </div>
            <div class="author-info">
              <div class="author-name">{{ getAuthorName(author) }}</div>
              <div class="author-articles">{{ author.bio || '最近加入' }}</div>
            </div>
          </router-link>
        </div>
        <div v-else class="sidebar-empty">暂无推荐作者</div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import ArticleList from '../components/ArticleList.vue'
import CategoryList from '../components/CategoryList.vue'
import TagList from '../components/TagList.vue'
import QuestionList from '../components/QuestionList.vue'
import HotArticles from '../components/HotArticles.vue'
import { questionService } from '../services/questionService'
import { userApi } from '../api/userApi'
import { resolveAvatarFrom } from '../utils/avatar'

export default defineComponent({
  name: 'Home',
  components: {
    ArticleList,
    CategoryList,
    TagList,
    QuestionList,
    HotArticles
  },
  setup() {
    const route = useRoute();
    const store = useStore();
    const activeTab = ref(route.query.tab || 'articles'); // 默认显示文章，支持URL参数切换
    const questions = ref([]);
    const isLoading = ref(true);
    const router = useRouter();
    
    // 使用Vuex中的数据
    const articles = computed(() => store.state.articles || []);
    const categories = computed(() => store.state.categories || []);
    const popularTags = computed(() => store.state.popularTags || []);
    
    // 热门文章和推荐文章的数据
    const hotArticles = ref([]);
    const recommendedArticles = ref([]);
    const hotArticlesLoading = ref(false);
    const recommendedArticlesLoading = ref(false);
    const featuredAuthors = ref([]);
    const featuredAuthorsLoading = ref(false);
    const featuredAuthorsError = ref('');
    
    // 计算属性
    const articleCount = computed(() => store.state.articleCount || articles.value.length);
    const categoryCount = computed(() => categories.value.length);
    const tagCount = computed(() => popularTags.value.length);
    const questionCount = computed(() => questions.value.length || 0);

    // 文章分页相关
    const articlePage = ref(1);
    const articlesPerPage = ref(10);
    const articleTotalPages = ref(1);
    const articleTotalItems = ref(0);

    // 问题分页相关
    const questionPage = ref(1);
    const questionsPerPage = ref(10);
    const questionTotalPages = ref(1);
    const questionTotalItems = ref(0);

    // 文章过滤相关
    const articleFilterType = ref('all');
    const searchKeyword = computed(() => String(route.query.search || '').trim());

    // 更新URL参数的方法
    const updateUrlParams = (tab) => {
      router.replace({ 
        query: { 
          ...route.query, 
          tab 
        }
      });
    };
    
    // 切换选项卡的方法
    const switchTab = (tab) => {
      activeTab.value = tab;
      updateUrlParams(tab);
    };

    // 监听activeTab变化，如果是hot或recommended，则加载对应数据
    watch(activeTab, async (newTab) => {
      if (newTab === 'hot') {
        await loadHotArticles();
      } else if (newTab === 'recommended') {
        await loadRecommendedArticles();
      }
    });
    
    // 监听URL参数变化
    watch(() => route.query.tab, (newTab) => {
      if (newTab) {
        activeTab.value = newTab;
      }
    });

    watch(searchKeyword, async () => {
      articlePage.value = 1;
      questionPage.value = 1;
      await loadData();
    });

    // 监听文章过滤类型变化
    watch(articleFilterType, async (newType) => {
      if (newType === 'hot' && (!hotArticles.value || hotArticles.value.length === 0)) {
        await loadHotArticles();
      } else if (newType === 'recommended' && (!recommendedArticles.value || recommendedArticles.value.length === 0)) {
        await loadRecommendedArticles();
      }
    });

    // 加载热门文章
    const loadHotArticles = async () => {
      hotArticlesLoading.value = true;
      try {
        const result = await store.dispatch('fetchHotArticles', { limit: 10 });
        hotArticles.value = result?.data || [];
      } catch (error) {
        console.error('加载热门文章失败:', error);
      } finally {
        hotArticlesLoading.value = false;
      }
    };

    // 加载推荐文章
    const loadRecommendedArticles = async () => {
      recommendedArticlesLoading.value = true;
      try {
        const result = await store.dispatch('fetchRecommendedArticles', { limit: 10 });
        recommendedArticles.value = result?.data || [];
      } catch (error) {
        console.error('加载推荐文章失败:', error);
      } finally {
        recommendedArticlesLoading.value = false;
      }
    };

    const loadFeaturedAuthors = async () => {
      featuredAuthorsLoading.value = true;
      featuredAuthorsError.value = '';
      try {
        const result = await userApi.getRecentUsers(4);
        featuredAuthors.value = result?.data || result || [];
      } catch (error) {
        console.error('加载推荐作者失败:', error);
        featuredAuthors.value = [];
        featuredAuthorsError.value = error.message || '推荐作者加载失败';
      } finally {
        featuredAuthorsLoading.value = false;
      }
    };

    const getAuthorName = (author) => author.nickname || author.username || '用户';

    const getAuthorAvatar = (author) => resolveAvatarFrom(author);

    const getAuthorInitial = (author) => getAuthorName(author).charAt(0).toUpperCase();

    // 生命周期钩子
    onMounted(async () => {
      await loadData();
      
      // 预加载热门文章和推荐文章
      Promise.all([
        loadHotArticles(),
        loadRecommendedArticles(),
        loadFeaturedAuthors()
      ]).catch(error => {
        console.error('Error preloading sidebar data:', error);
      });
    });

    // 方法
    const loadData = async () => {
      isLoading.value = true;
      
      try {
        // 首先加载文章数据
        const articlesResponse = await store.dispatch('fetchArticles', { 
          page: articlePage.value, 
          pageSize: articlesPerPage.value,
          keyword: searchKeyword.value || undefined
        });
        
        // 更新文章分页信息
        if (articlesResponse) {
          articleTotalPages.value = articlesResponse.totalPages || 1;
          articleTotalItems.value = articlesResponse.total || 0;
        }
        
        // 然后加载分类和标签
        const promises = [
          store.dispatch('fetchCategories'),
          store.dispatch('fetchPopularTags')
        ];
        
        await Promise.all(promises);
        
        // 加载问题数据
        await loadQuestions();
      } catch (error) {
        console.error('Error loading data:', error);
        
        // 尝试单独加载每个数据源
        try {
          await store.dispatch('fetchArticles', { 
            page: articlePage.value, 
            pageSize: articlesPerPage.value,
            keyword: searchKeyword.value || undefined
          });
        } catch (articlesError) {
          console.error('Failed to load articles separately:', articlesError);
        }
        
        try {
          await store.dispatch('fetchCategories');
        } catch (categoriesError) {
          console.error('Failed to load categories separately:', categoriesError);
        }
        
        try {
          await store.dispatch('fetchPopularTags');
        } catch (tagsError) {
          console.error('Failed to load tags separately:', tagsError);
        }
        
        try {
          await loadQuestions();
        } catch (questionsError) {
          console.error('Failed to load questions separately:', questionsError);
        }
      } finally {
        isLoading.value = false;
      }
    };
    
    // 加载问题数据
    const loadQuestions = async () => {
      try {
        const questionResponse = await questionService.getQuestions({
          page: questionPage.value,
          pageSize: questionsPerPage.value,
          keyword: searchKeyword.value || undefined
        });

        if (questionResponse.success) {
          // 处理分页数据结构
          if (questionResponse.data && questionResponse.data.content) {
            // Spring Boot分页格式
            questions.value = questionResponse.data.content || [];
            questionTotalItems.value = questionResponse.data.totalElements || 0;
            questionTotalPages.value = questionResponse.data.totalPages || 1;
          } else if (Array.isArray(questionResponse.data)) {
            // 简单数组格式
            questions.value = questionResponse.data || [];
            questionTotalItems.value = questionResponse.totalElements || questionResponse.data.length;
            questionTotalPages.value = questionResponse.totalPages || 1;
          } else {
            questions.value = [];
            questionTotalItems.value = 0;
            questionTotalPages.value = 1;
          }
        } else {
          questions.value = [];
          questionTotalItems.value = 0;
          questionTotalPages.value = 1;
        }
        
        return questionResponse;
      } catch (error) {
        console.error('Error loading questions:', error);
        return null;
      }
    };
    
    // 文章分页变化处理
    const handleArticlePageChange = async (page) => {
      articlePage.value = page;
      isLoading.value = true;
      
      try {
        const articlesResponse = await store.dispatch('fetchArticles', { 
          page: articlePage.value, 
          pageSize: articlesPerPage.value,
          keyword: searchKeyword.value || undefined
        });
        
        if (articlesResponse) {
          articleTotalPages.value = articlesResponse.totalPages || 1;
          articleTotalItems.value = articlesResponse.total || 0;
        }
      } catch (error) {
        console.error('Error changing article page:', error);
      } finally {
        isLoading.value = false;
      }
    };
    
    // 问题分页变化处理
    const handleQuestionPageChange = async (page) => {
      questionPage.value = page;
      isLoading.value = true;
      
      try {
        await loadQuestions();
      } catch (error) {
        console.error('Error changing question page:', error);
      } finally {
        isLoading.value = false;
      }
    };
    
    return {
      route,
      activeTab,
      questions,
      articles,
      categories,
      popularTags,
      articleCount,
      categoryCount,
      tagCount,
      questionCount,
      isLoading,
      articlePage,
      articlesPerPage,
      articleTotalPages,
      articleTotalItems,
      questionPage,
      questionsPerPage,
      questionTotalPages,
      questionTotalItems,
      articleFilterType,
      searchKeyword,
      handleArticlePageChange,
      handleQuestionPageChange,
      hotArticles,
      recommendedArticles,
      hotArticlesLoading,
      recommendedArticlesLoading,
      featuredAuthors,
      featuredAuthorsLoading,
      featuredAuthorsError,
      loadHotArticles,
      loadRecommendedArticles,
      loadFeaturedAuthors,
      getAuthorName,
      getAuthorAvatar,
      getAuthorInitial,
      switchTab
    };
  }
})
</script>

<style scoped>
.home-container {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 1.5rem;
}

.main-content {
  width: 100%;
}

/* 欢迎横幅 */
.welcome-banner {
  display: flex;
  background: linear-gradient(135deg, var(--primary-light) 0%, #e6f7ff 100%);
  border-radius: var(--radius-lg);
  padding: 2rem;
  margin-bottom: 1.5rem;
  overflow: hidden;
  position: relative;
  box-shadow: var(--shadow);
  border: 1px solid rgba(37, 99, 235, 0.1);
}

.welcome-content {
  flex: 1;
  position: relative;
  z-index: 2;
}

.welcome-content h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
  color: var(--text-color);
  line-height: 1.2;
}

.welcome-content p {
  font-size: 1.1rem;
  color: var(--text-light);
  margin-bottom: 1.5rem;
  max-width: 80%;
}

.banner-actions {
  display: flex;
  gap: 1rem;
}

.welcome-image {
  position: relative;
  width: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.graphic-element {
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.1) 0%, rgba(37, 99, 235, 0.05) 50%, transparent 70%);
  border-radius: 50%;
  right: -100px;
  top: -100px;
}

.graphic-element::before {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border: 2px dashed rgba(37, 99, 235, 0.2);
  border-radius: 50%;
  top: 50px;
  left: 50px;
  animation: rotate 20s linear infinite;
}

.graphic-element::after {
  content: '';
  position: absolute;
  width: 100px;
  height: 100px;
  background-color: rgba(37, 99, 235, 0.1);
  border-radius: 50%;
  top: 100px;
  left: 100px;
  animation: pulse 3s ease-in-out infinite;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 0.8;
  }
}

/* 搜索结果提示 */
.search-results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  border-left: 4px solid var(--primary-color);
}

.search-results-header h2 {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-icon {
  color: var(--primary-color);
}

.clear-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-lighter);
  font-size: 0.9rem;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.clear-search:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

/* 内容选项卡 */
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

/* 加载状态 */
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

/* 侧边栏 */
.sidebar {
  width: 100%;
}

.sidebar-section {
  background-color: var(--bg-white);
  border-radius: var(--radius);
  padding: 1.25rem;
  margin-bottom: 1.5rem;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.sidebar-section h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--text-color);
  display: flex;
  align-items: center;
  position: relative;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-section h3::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50px;
  height: 2px;
  background-color: var(--primary-color);
}

/* 社区数据卡片 */
.stats-card {
  background: linear-gradient(135deg, var(--bg-white) 0%, var(--bg-gray) 100%);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.stat-item {
  text-align: center;
  padding: 1rem;
  border-radius: var(--radius);
  background-color: var(--bg-white);
  transition: var(--transition);
  border: 1px solid var(--border-color);
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  border-color: var(--primary-light);
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.9rem;
  color: var(--text-light);
}

/* 查看全部链接 */
.view-all-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding: 0.5rem;
  border-radius: var(--radius);
  color: var(--primary-color);
  font-size: 0.9rem;
  font-weight: 500;
  transition: var(--transition);
  background-color: var(--primary-light);
}

.view-all-link:hover {
  background-color: var(--primary-light);
  transform: translateY(-2px);
}

/* 推荐作者列表 */
.authors-list {
  margin-top: 1rem;
}

.author-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--border-color);
  text-decoration: none;
  color: var(--text-color);
  transition: var(--transition);
}

.author-item:last-child {
  border-bottom: none;
}

.author-item:hover {
  background-color: var(--bg-light);
}

.author-item:hover .author-name {
  color: var(--primary-color);
}

.author-avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
  transition: var(--transition);
  overflow: hidden;
  flex-shrink: 0;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-item:hover .author-avatar {
  transform: scale(1.05);
  box-shadow: 0 0 0 2px var(--primary-color);
}

.author-name {
  font-weight: 500;
  margin-bottom: 0.25rem;
}

.author-articles {
  font-size: 0.8rem;
  color: var(--text-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 210px;
}

.sidebar-empty {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  color: var(--text-light);
  font-size: 0.9rem;
  background: var(--bg-light);
}

.sidebar-empty.error {
  color: var(--error-color);
  border-color: rgba(var(--error-rgb), 0.3);
  background: rgba(var(--error-rgb), 0.06);
}

/* 响应式设计 */
@media (max-width: 992px) {
  .home-container {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    order: 2;
  }
  
  .sidebar-section {
    margin-bottom: 1rem;
  }
}

@media (max-width: 768px) {
  .welcome-banner {
    flex-direction: column;
    padding: 1.5rem;
  }
  
  .welcome-content h1 {
    font-size: 1.5rem;
  }
  
  .welcome-content p {
    font-size: 1rem;
    max-width: 100%;
  }
  
  .banner-actions {
    flex-direction: column;
    gap: 0.75rem;
  }
  
  .welcome-image {
    display: none;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .stat-value {
    font-size: 1.25rem;
  }
  
  .welcome-content h1 {
    font-size: 1.25rem;
  }
}

/* 最新文章部分 */
.section-title {
  display: none;
}

.article-filter-tabs {
  display: flex;
}

.articles-container {
  margin-top: 0;
}

.articles-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.5rem;
}

.articles-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
  position: relative;
  padding-left: 0.75rem;
  border-left: 4px solid var(--primary-color);
}

.article-filter-tabs {
  display: flex;
}

.filter-tab {
  padding: 0.5rem 1rem;
  cursor: pointer;
  position: relative;
  color: var(--text-color);
  font-size: 0.95rem;
  transition: var(--transition);
  margin-left: 1rem;
}

.filter-tab:hover {
  color: var(--primary-color);
}

.filter-tab.active {
  color: var(--primary-color);
  font-weight: 500;
}

.filter-tab.active::after {
  content: '';
  position: absolute;
  bottom: -0.5rem;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--primary-color);
}

@media (max-width: 768px) {
  .articles-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .article-filter-tabs {
    width: 100%;
    justify-content: flex-start;
    border-top: 1px solid var(--border-color);
    padding-top: 0.5rem;
  }
  
  .filter-tab {
    margin-left: 0;
    margin-right: 1rem;
  }
}

@media (max-width: 576px) {
  .articles-title {
    font-size: 1.15rem;
  }
  
  .filter-tab {
    padding: 0.5rem 0.75rem;
    font-size: 0.85rem;
    margin-right: 0.5rem;
  }
}
</style>
