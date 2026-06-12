<script>
import ArticleCard from './ArticleCard.vue';
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';

export default {
  name: 'ArticleList',
  components: {
    ArticleCard
  },
  props: {
    articles: {
      type: Array,
      default: () => []
    },
    showTitle: {
      type: Boolean,
      default: true
    },
    title: {
      type: String,
      default: '最新文章'
    },
    loading: {
      type: Boolean,
      default: false
    },
    currentPage: {
      type: Number,
      default: 1
    },
    totalPages: {
      type: Number,
      default: 1
    },
    totalItems: {
      type: Number,
      default: 0
    },
    pageSize: {
      type: Number,
      default: 10
    },
    showPagination: {
      type: Boolean,
      default: true
    }
  },
  emits: ['page-change'],
  setup(props, { emit }) {
    const router = useRouter();
    const activeFilter = ref('all');
    
    const setFilter = (filter) => {
      activeFilter.value = filter;
      
      // 根据filter决定导航到哪个tab
      if (filter === 'all') {
        router.push({ path: '/articles' });
      } else if (filter === 'popular') {
        router.push({ path: '/articles', query: { tab: 'hot' } });
      } else if (filter === 'recommended') {
        router.push({ path: '/articles', query: { tab: 'recommended' } });
      }
    };
    
    // 分页相关方法
    const changePage = (page) => {
      if (page < 1 || page > props.totalPages) return;
      emit('page-change', page);
    };
    
    // 计算页码数组（显示当前页及其前后两页）
    const pageNumbers = computed(() => {
      const total = props.totalPages;
      const current = props.currentPage;
      const pages = [];
      
      // 总是显示第一页
      pages.push(1);
      
      // 计算显示的页码范围
      let startPage = Math.max(2, current - 1);
      let endPage = Math.min(total - 1, current + 1);
      
      // 如果当前页接近起始，增加后面的页码
      if (current <= 3) {
        endPage = Math.min(total - 1, 4);
      }
      
      // 如果当前页接近结束，增加前面的页码
      if (current >= total - 2) {
        startPage = Math.max(2, total - 3);
      }
      
      // 添加前省略号
      if (startPage > 2) {
        pages.push('...');
      }
      
      // 添加中间页码
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
      
      // 添加后省略号
      if (endPage < total - 1) {
        pages.push('...');
      }
      
      // 总是显示最后一页（如果总页数大于1）
      if (total > 1) {
        pages.push(total);
      }
      
      return pages;
    });
    
    // 监听文章数据变化
    watch(() => props.articles, (newArticles) => {
      console.log('ArticleList: articles changed', {
        length: newArticles?.length || 0,
        data: newArticles
      });
    }, { immediate: true, deep: true });
    
    // 组件挂载时记录日志
    onMounted(() => {
      console.log('ArticleList mounted', { 
        articlesLength: props.articles?.length || 0,
        title: props.title,
        showTitle: props.showTitle,
        loading: props.loading,
        pagination: {
          currentPage: props.currentPage,
          totalPages: props.totalPages,
          totalItems: props.totalItems,
          pageSize: props.pageSize
        },
        sampleArticle: props.articles && props.articles.length > 0 
          ? {
              id: props.articles[0].id,
              title: props.articles[0].title,
              category: props.articles[0].category,
              commentsType: Array.isArray(props.articles[0].comments) 
                ? 'array' 
                : typeof props.articles[0].comments
            }
          : 'No articles'
      });
    });
    
    return {
      activeFilter,
      setFilter,
      changePage,
      pageNumbers
    };
  },
  methods: {
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    },
    getAuthorInfo(author) {
      return author || { name: '未知作者', avatar: '' };
    },
    getInitials(name) {
      if (!name) return '?';
      return name.slice(0, 1).toUpperCase();
    },
    getRandomCategory(article) {
      const categories = ['技术', '分享', '问答', '教程'];
      const randomIndex = article.id % categories.length;
      return categories[randomIndex];
    },
    truncateText(text, maxLength = 100) {
      if (!text) return '';
      if (text.length <= maxLength) return text;
      return text.substring(0, maxLength) + '...';
    }
  },
  mounted() {
    console.log('ArticleList mounted, articles:', this.articles?.length || 0);
  }
}
</script>

<template>
  <div class="article-list">
    <div class="article-list-header" v-if="showTitle">
      <h2>{{ title }}</h2>
      <div class="article-filter">
        <button 
          class="filter-btn" 
          :class="{ active: activeFilter === 'all' }"
          @click="setFilter('all')"
        >
          所有文章
        </button>
        <button 
          class="filter-btn" 
          :class="{ active: activeFilter === 'popular' }"
          @click="setFilter('popular')"
        >
          热门文章
        </button>
        <button 
          class="filter-btn" 
          :class="{ active: activeFilter === 'recommended' }"
          @click="setFilter('recommended')"
        >
          推荐文章
        </button>
      </div>
    </div>
    
    <div v-if="loading" class="loading-articles">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <p>加载中...</p>
    </div>
    
    <div v-else-if="!articles || articles.length === 0" class="no-articles">
      <div class="empty-state">
        <font-awesome-icon :icon="['fas', 'file-alt']" class="empty-icon" />
        <p>暂无文章内容</p>
        <p class="empty-hint">欢迎浏览更多内容或发布您的第一篇文章</p>
        <router-link to="/article/new" class="btn btn-primary">
          <font-awesome-icon :icon="['fas', 'edit']" />
          <span>立即发布文章</span>
        </router-link>
      </div>
    </div>
    
    <div v-else>
      <!-- 精选文章（如果有） -->
      <div v-if="articles.length > 0 && articles[0].featured" class="featured-article">
        <article-card 
          :article="articles[0]" 
          class="featured"
        />
      </div>
      
      <!-- 文章列表 -->
      <div class="article-list-container">
        <article-card 
          v-for="(article, index) in articles.filter((_, i) => !(i === 0 && articles[0].featured))" 
          :key="article.id"
          :article="article"
        />
      </div>
      
      <!-- 分页导航 -->
      <div class="pagination" v-if="showPagination && totalPages > 1">
        <div class="pagination-info">
          共 {{ totalItems }} 篇文章，每页显示 {{ pageSize }} 篇，当前位于第 {{ currentPage }}/{{ totalPages }} 页
        </div>
        <div class="pagination-controls">
          <button 
            class="pagination-btn prev" 
            :disabled="currentPage <= 1"
            @click="changePage(currentPage - 1)"
          >
            <font-awesome-icon :icon="['fas', 'chevron-left']" />
            上一页
          </button>
          
          <div class="page-numbers">
            <button 
              v-for="(page, index) in pageNumbers" 
              :key="index"
              :class="[
                'page-number',
                { active: page === currentPage },
                { 'page-ellipsis': page === '...' }
              ]"
              :disabled="page === '...'"
              @click="page !== '...' && changePage(page)"
            >
              {{ page }}
            </button>
          </div>
          
          <button 
            class="pagination-btn next" 
            :disabled="currentPage >= totalPages"
            @click="changePage(currentPage + 1)"
          >
            下一页
            <font-awesome-icon :icon="['fas', 'chevron-right']" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.article-list {
  background-color: transparent;
  margin-bottom: 2rem;
}

.article-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.article-list-header h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);
  position: relative;
  padding-left: 0.75rem;
}

.article-list-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.25rem;
  height: 1.25rem;
  width: 4px;
  background: linear-gradient(to bottom, var(--primary-color), var(--accent-color));
  border-radius: 2px;
}

.article-filter {
  display: flex;
  gap: 0.5rem;
}

.filter-btn {
  background: none;
  border: none;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius);
  font-size: 0.85rem;
  color: var(--text-light);
  cursor: pointer;
  transition: var(--transition);
}

.filter-btn:hover {
  color: var(--primary-color);
  background-color: var(--primary-light);
}

.filter-btn.active {
  color: var(--primary-color);
  background-color: var(--primary-light);
  font-weight: 500;
}

/* 加载状态 */
.loading-articles {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 0;
  color: var(--text-lighter);
}

.loading-spinner {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
}

/* 空状态 */
.no-articles {
  padding: 3rem 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2.5rem;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  border: 1px solid var(--border-color);
}

.empty-icon {
  font-size: 3.5rem;
  color: var(--text-lighter);
  margin-bottom: 1.5rem;
  opacity: 0.7;
}

.empty-state p {
  margin: 0.5rem 0;
  color: var(--text-color);
  font-size: 1.1rem;
}

.empty-hint {
  color: var(--text-light) !important;
  font-size: 0.9rem !important;
  margin-bottom: 1.5rem !important;
}

/* 精选文章 */
.featured-article {
  margin-bottom: 1.5rem;
}

.featured-article :deep(.article-card-inner) {
  border-left: 4px solid var(--primary-color);
  background: linear-gradient(to right, var(--primary-light) 0%, rgba(255, 255, 255, 0) 15%, var(--bg-white) 100%);
}

/* 文章列表 */
.article-list-container {
  margin-bottom: 1.5rem;
}

/* 分页导航 */
.pagination {
  margin-top: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.pagination-info {
  text-align: center;
  color: var(--text-light);
  font-size: 0.9rem;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
}

.pagination-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background-color: var(--bg-white);
  color: var(--text-color);
  font-size: 0.9rem;
  cursor: pointer;
  transition: var(--transition);
}

.pagination-btn:hover:not(:disabled) {
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-light);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 0.25rem;
}

.page-number {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
  background-color: var(--bg-white);
  color: var(--text-color);
  cursor: pointer;
  transition: var(--transition);
}

.page-number:hover:not(:disabled):not(.active) {
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-light);
}

.page-number.active {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.page-ellipsis {
  background-color: transparent !important;
  border-color: transparent !important;
  cursor: default !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .article-list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .article-filter {
    width: 100%;
    justify-content: space-between;
  }
  
  .article-list-container {
    margin-bottom: 1rem;
  }
  
  .pagination-controls {
    flex-wrap: wrap;
  }
  
  .page-numbers {
    order: 3;
    margin-top: 0.5rem;
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 576px) {
  .article-list-header h2 {
    font-size: 1.25rem;
  }
  
  .filter-btn {
    font-size: 0.8rem;
    padding: 0.4rem 0.6rem;
  }
  
  .empty-state {
    padding: 1.5rem;
  }
  
  .empty-icon {
    font-size: 2.5rem;
  }
  
  .empty-state p {
    font-size: 1rem;
  }
  
  .pagination-btn {
    padding: 0.5rem;
  }
  
  .page-number {
    width: 32px;
    height: 32px;
    font-size: 0.8rem;
  }
}
</style> 