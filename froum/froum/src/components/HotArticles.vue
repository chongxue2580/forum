<script setup>
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import ArticleCard from './ArticleCard.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'hot', // 'hot' 或 'recommended'
    validator: (value) => ['hot', 'recommended'].includes(value)
  },
  limit: {
    type: Number,
    default: 5
  }
})

const emit = defineEmits(['refresh'])
const store = useStore()
const articles = ref([])
const loading = ref(true)
const error = ref('')

// 获取热门文章或推荐文章
const fetchArticles = async () => {
  loading.value = true
  error.value = ''
  
  try {
    let result
    if (props.type === 'hot') {
      result = await store.dispatch('fetchHotArticles', { limit: props.limit })
    } else {
      result = await store.dispatch('fetchRecommendedArticles', { limit: props.limit })
    }
    
    articles.value = result?.data || []
  } catch (err) {
    console.error(`获取${props.type === 'hot' ? '热门' : '推荐'}文章失败:`, err)
    error.value = `获取${props.type === 'hot' ? '热门' : '推荐'}文章失败，请稍后重试`
  } finally {
    loading.value = false
  }
}

// 刷新文章列表
const refresh = () => {
  fetchArticles()
  emit('refresh')
}

onMounted(() => {
  fetchArticles()
})
</script>

<template>
  <div class="article-section">
    <div class="section-header">
      <router-link 
        :to="{ path: '/articles', query: { tab: type } }" 
        class="section-title"
      >
        <h3>{{ type === 'hot' ? '热门文章' : '推荐文章' }}</h3>
      </router-link>
      <button class="refresh-btn" @click="refresh" :disabled="loading">
        <font-awesome-icon :icon="['fas', 'sync-alt']" :spin="loading" />
      </button>
    </div>
    
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <p>正在加载{{ type === 'hot' ? '热门' : '推荐' }}文章...</p>
    </div>
    
    <div v-else-if="error" class="error-state">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
      <p>{{ error }}</p>
      <button class="retry-btn" @click="refresh">
        <font-awesome-icon :icon="['fas', 'redo']" />
        重试
      </button>
    </div>
    
    <div v-else-if="articles.length === 0" class="empty-state">
      <font-awesome-icon :icon="['fas', 'file-alt']" class="empty-icon" />
      <p>暂无{{ type === 'hot' ? '热门' : '推荐' }}文章</p>
    </div>
    
    <div v-else class="articles-list">
      <div v-for="article in articles" :key="article.id" class="article-item">
        <router-link :to="`/article/${article.id}`" class="article-link">
          <div class="article-content">
            <h4 class="article-title">{{ article.title }}</h4>
            <div class="article-meta">
              <span class="author">{{ article.author?.name || '匿名' }}</span>
              <span class="views">
                <font-awesome-icon :icon="['fas', 'eye']" />
                {{ article.views }}
              </span>
              <span class="likes">
                <font-awesome-icon :icon="['fas', 'thumbs-up']" />
                {{ article.likes }}
              </span>
            </div>
          </div>
          <div v-if="article.coverImage" class="article-cover">
            <img :src="article.coverImage" :alt="article.title" />
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.article-section {
  margin-bottom: 1.5rem;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--border-color);
}

.section-title {
  text-decoration: none;
  color: var(--text-color);
  transition: var(--transition);
}

.section-title:hover h3 {
  color: var(--primary-color);
}

.section-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-color);
  position: relative;
  transition: var(--transition);
}

.refresh-btn {
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-light);
  border-radius: 50%;
  cursor: pointer;
  transition: var(--transition);
}

.refresh-btn:hover:not(:disabled) {
  background-color: var(--primary-light);
  color: var(--primary-color);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-state, .error-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  text-align: center;
}

.loading-spinner, .error-icon, .empty-icon {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: var(--text-lighter);
}

.error-icon {
  color: var(--error-color);
}

.retry-btn {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  border: none;
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: var(--transition);
}

.retry-btn:hover {
  background-color: var(--primary-color);
  color: white;
}

.articles-list {
  padding: 0.5rem 0;
}

.article-item {
  border-bottom: 1px solid var(--border-color);
}

.article-item:last-child {
  border-bottom: none;
}

.article-link {
  display: flex;
  padding: 0.75rem 1.25rem;
  color: var(--text-color);
  transition: var(--transition);
  text-decoration: none;
}

.article-link:hover {
  background-color: var(--bg-gray);
}

.article-content {
  flex: 1;
  margin-right: 1rem;
}

.article-title {
  margin: 0 0 0.5rem 0;
  font-size: 0.95rem;
  font-weight: 500;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.8rem;
  color: var(--text-lighter);
}

.views, .likes {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.article-cover {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 576px) {
  .article-cover {
    display: none;
  }
}
</style> 