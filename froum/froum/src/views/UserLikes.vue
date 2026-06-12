<template>
  <div class="user-likes-page">
    <div class="container">
      <div class="page-header">
        <h1>我的点赞</h1>
        <p class="text-muted">查看您点赞过的内容</p>
      </div>

      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.key"
          @click="activeTab = tab.key"
          :class="['tab-btn', { active: activeTab === tab.key }]"
        >
          {{ tab.label }}
          <span class="count" v-if="tab.count !== undefined">({{ tab.count }})</span>
        </button>
      </div>

      <div class="tab-content">
        <div v-if="errorMessage" class="error">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
          {{ errorMessage }}
        </div>

        <!-- 文章列表 -->
        <div v-if="activeTab === 'articles'" class="content-list">
          <div v-if="loading" class="loading">
            <font-awesome-icon :icon="['fas', 'spinner']" spin />
            加载中...
          </div>
          <div v-else-if="articles.length === 0" class="empty">
            <font-awesome-icon :icon="['fas', 'heart']" />
            <p>您还没有点赞过任何文章</p>
          </div>
          <div v-else>
            <div 
              v-for="article in articles" 
              :key="article.id" 
              class="content-item"
            >
              <div class="item-header">
                <h3>
                  <router-link :to="`/article/${article.id}`">
                    {{ article.title }}
                  </router-link>
                </h3>
                <span class="date">{{ formatDate(article.likedAt || article.createTime || article.createdAt) }}</span>
              </div>
              <p class="summary">{{ article.summary }}</p>
              <div class="item-meta">
                <span class="author">作者: {{ getAuthorName(article.author) }}</span>
                <span class="stats">
                  <font-awesome-icon :icon="['fas', 'heart']" />
                  {{ article.likes || article.likeCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 问答列表 -->
        <div v-if="activeTab === 'questions'" class="content-list">
          <div v-if="loading" class="loading">
            <font-awesome-icon :icon="['fas', 'spinner']" spin />
            加载中...
          </div>
          <div v-else-if="questions.length === 0" class="empty">
            <font-awesome-icon :icon="['fas', 'heart']" />
            <p>您还没有点赞过任何问答</p>
          </div>
          <div v-else>
            <div 
              v-for="question in questions" 
              :key="question.id" 
              class="content-item"
            >
              <div class="item-header">
                <h3>
                  <router-link :to="`/question/${question.id}`">
                    {{ question.title }}
                  </router-link>
                </h3>
                <span class="date">{{ formatDate(question.likedAt || question.createTime || question.createdAt) }}</span>
              </div>
              <p class="summary">{{ question.content }}</p>
              <div class="item-meta">
                <span class="author">提问者: {{ getAuthorName(question.author) }}</span>
                <span class="stats">
                  <font-awesome-icon :icon="['fas', 'heart']" />
                  {{ question.likes || question.likeCount || question.voteCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-if="activeTab === 'comments'" class="content-list">
          <div v-if="loading" class="loading">
            <font-awesome-icon :icon="['fas', 'spinner']" spin />
            加载中...
          </div>
          <div v-else-if="comments.length === 0" class="empty">
            <font-awesome-icon :icon="['fas', 'heart']" />
            <p>您还没有点赞过任何评论</p>
          </div>
          <div v-else>
            <div 
              v-for="comment in comments" 
              :key="comment.id" 
              class="content-item"
            >
              <div class="item-header">
                <h3>
                  <router-link :to="getCommentTargetLink(comment)">
                    评论
                  </router-link>
                </h3>
                <span class="date">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <p class="summary">{{ comment.content }}</p>
              <div class="item-meta">
                <span class="author">评论者: {{ getAuthorName(comment.user) }}</span>
                <span class="stats">
                  <font-awesome-icon :icon="['fas', 'heart']" />
                  {{ comment.likes || comment.likeCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button 
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage <= 1"
          class="page-btn"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页
        </span>
        <button 
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage >= totalPages"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { likeApi } from '@/api/likeApi'
import { articleService } from '@/services/articleService'
import { questionService } from '@/services/questionService'
import { commentService } from '@/services/commentService'

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
  { 
    key: 'articles', 
    label: '文章', 
    count: pagination.articles.total 
  },
  { 
    key: 'questions', 
    label: '问答', 
    count: pagination.questions.total 
  },
  { 
    key: 'comments', 
    label: '评论', 
    count: pagination.comments.total 
  }
])

const totalPages = computed(() => {
  return pagination[activeTab.value]?.pages || 0
})

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const getAuthorName = (author) => {
  if (!author) return '未知用户'
  if (typeof author === 'string') return author
  return author.nickname || author.username || author.name || '未知用户'
}

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
    switch (activeTab.value) {
      case 'articles':
        await loadArticles()
        break
      case 'questions':
        await loadQuestions()
        break
      case 'comments':
        await loadComments()
        break
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    errorMessage.value = error.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

const loadArticles = async () => {
  try {
    const response = await likeApi.getUserLikedArticles(currentPage.value - 1, pageSize)
    const page = unwrapPage(response)
    articles.value = await loadDetailsById(page.content, id => articleService.getArticleById(id))
    pagination.articles.total = page.totalElements
    pagination.articles.pages = page.totalPages
  } catch (error) {
    console.error('加载点赞文章失败:', error)
    errorMessage.value = error.message || '加载点赞文章失败'
    articles.value = []
  }
}

const loadQuestions = async () => {
  try {
    const response = await likeApi.getUserLikedQuestions(currentPage.value - 1, pageSize)
    const page = unwrapPage(response)
    questions.value = await loadDetailsById(page.content, id => questionService.getQuestionById(id))
    pagination.questions.total = page.totalElements
    pagination.questions.pages = page.totalPages
  } catch (error) {
    console.error('加载点赞问答失败:', error)
    errorMessage.value = error.message || '加载点赞问答失败'
    questions.value = []
  }
}

const loadComments = async () => {
  try {
    const response = await likeApi.getUserLikedComments(currentPage.value - 1, pageSize)
    const page = unwrapPage(response)
    comments.value = await loadDetailsById(page.content, id => commentService.getCommentById(id))
    pagination.comments.total = page.totalElements
    pagination.comments.pages = page.totalPages
  } catch (error) {
    console.error('加载点赞评论失败:', error)
    errorMessage.value = error.message || '加载点赞评论失败'
    comments.value = []
  }
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadData()
  }
}

// 监听标签切换
watch(activeTab, () => {
  currentPage.value = 1
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-likes-page {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  color: #333;
  margin-bottom: 0.5rem;
}

.tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e1e5e9;
}

.tab-btn {
  padding: 1rem 1.5rem;
  border: none;
  background: none;
  color: #6c757d;
  font-size: 1rem;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #007bff;
}

.tab-btn.active {
  color: #007bff;
  border-bottom-color: #007bff;
}

.count {
  font-size: 0.875rem;
  opacity: 0.7;
}

.content-list {
  background: white;
  border-radius: 0.5rem;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  border: 1px solid #dc3545;
  border-radius: 0.5rem;
  color: #842029;
  background: #f8d7da;
}

.loading, .empty {
  text-align: center;
  padding: 3rem;
  color: #6c757d;
}

.empty .fa-heart {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.3;
}

.content-item {
  padding: 1.5rem 0;
  border-bottom: 1px solid #e1e5e9;
}

.content-item:last-child {
  border-bottom: none;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.item-header h3 {
  margin: 0;
  flex: 1;
}

.item-header h3 a {
  color: #333;
  text-decoration: none;
}

.item-header h3 a:hover {
  color: #007bff;
}

.date {
  color: #6c757d;
  font-size: 0.875rem;
  white-space: nowrap;
  margin-left: 1rem;
}

.summary {
  color: #6c757d;
  margin: 0.5rem 0;
  line-height: 1.5;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
  color: #6c757d;
}

.stats {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
}

.page-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #e1e5e9;
  background: white;
  color: #6c757d;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #007bff;
  color: #007bff;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #6c757d;
  font-size: 0.875rem;
}
</style>
