<template>
  <div class="questions-container">
    <div class="questions-header">
      <div class="header-content">
        <h1>问答社区</h1>
        <p class="description">提出问题，获得专业解答</p>
      </div>
      <router-link to="/question/new" class="btn-ask">
        <font-awesome-icon :icon="['fas', 'plus']" />
        提问题
      </router-link>
    </div>

    <div class="filter-container">
      <div class="filter-options">
        <button 
          class="filter-btn" 
          :class="{ active: activeFilter === 'all' }"
          @click="setFilter('all')"
        >
          全部问题
        </button>
        <button 
          class="filter-btn" 
          :class="{ active: activeFilter === 'solved' }"
          @click="setFilter('solved')"
        >
          <font-awesome-icon :icon="['fas', 'check-circle']" />
          已解决问题
        </button>
        <button 
          class="filter-btn" 
          :class="{ active: activeFilter === 'unsolved' }"
          @click="setFilter('unsolved')"
        >
          <font-awesome-icon :icon="['fas', 'question-circle']" />
          未解决问题
        </button>
      </div>

      <div class="filter-sort">
        <select v-model="sortOption" class="sort-select">
          <option value="newest">最新发布</option>
          <option value="oldest">最早发布</option>
          <option value="mostAnswers">回答最多</option>
          <option value="leastAnswers">回答最少</option>
        </select>
      </div>
    </div>

    <div class="questions-content">
      <!-- 问题列表 -->
      <div class="questions-list">
        <div v-if="loading" class="loading-container">
          <font-awesome-icon :icon="['fas', 'spinner']" spin class="loading-icon" />
          <p>加载中...</p>
        </div>
        
        <div v-else-if="filteredQuestions.length === 0" class="empty-state">
          <font-awesome-icon :icon="['fas', 'question-circle']" class="empty-icon" />
          <p>{{ emptyStateMessage }}</p>
          <router-link to="/question/new" class="btn-ask-empty">提问题</router-link>
        </div>
        
        <template v-else>
          <div 
            v-for="question in filteredQuestions" 
            :key="question.id" 
            class="question-item"
            @click="navigateToQuestion(question.id)"
          >
            <div class="question-stats">
              <div class="stat-item">
                <div class="stat-value">{{ question.answerCount }}</div>
                <div class="stat-label">回答</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ question.viewCount }}</div>
                <div class="stat-label">浏览</div>
              </div>
            </div>
            
            <div class="question-main">
              <h3 class="question-title">
                <span v-if="question.solved" class="solved-badge">已解决</span>
                {{ question.title }}
              </h3>
              <p v-if="getQuestionExcerpt(question)" class="question-excerpt">
                {{ getQuestionExcerpt(question) }}
              </p>
              <div v-if="getQuestionImage(question)" class="question-image-preview">
                <img :src="getQuestionImage(question)" :alt="question.title" loading="lazy">
              </div>
              <div class="question-meta">
                <div class="tags">
                  <span v-for="tag in question.tags" :key="tag" class="tag">{{ tag }}</span>
                </div>
                <div class="user-info">
                  <div class="avatar">
                    <img :src="question.author.avatar" :alt="question.author.name">
                  </div>
                  <span class="username">{{ question.author.name }}</span>
                  <span class="time">{{ formatQuestionTime(question.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="pagination">
            <button 
              class="page-btn prev" 
              :disabled="currentPage === 1"
              @click="changePage(currentPage - 1)"
            >
              <font-awesome-icon :icon="['fas', 'chevron-left']" />
              上一页
            </button>
            <div class="page-info">{{ currentPage }} / {{ totalPages }}</div>
            <button 
              class="page-btn next" 
              :disabled="currentPage === totalPages"
              @click="changePage(currentPage + 1)"
            >
              下一页
              <font-awesome-icon :icon="['fas', 'chevron-right']" />
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { questionService } from '../services/questionService'
import { formatDateTime } from '../utils/dateUtils'

export default defineComponent({
  name: 'QuestionsView',
  setup() {
    const router = useRouter()
    const loading = ref(true)
    const questions = ref([])
    const activeFilter = ref('all')
    const sortOption = ref('newest')
    const currentPage = ref(1)
    const pageSize = 10
    const totalQuestions = ref(0)

    const toTimestamp = (time) => {
      if (!time) return 0
      if (Array.isArray(time)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = time
        return new Date(year, month - 1, day, hour, minute, second).getTime()
      }
      return new Date(time).getTime()
    }

    const formatQuestionTime = (time) => {
      return formatDateTime(time, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    const resolveContentImageUrl = (url) => {
      if (!url) return ''
      const normalizedUrl = String(url).trim().replace(/^<|>$/g, '')
      if (/^(https?:|data:image\/|blob:)/i.test(normalizedUrl)) {
        return normalizedUrl
      }
      if (normalizedUrl.startsWith('/')) {
        return normalizedUrl
      }
      if (normalizedUrl.startsWith('uploads/')) {
        return `/${normalizedUrl}`
      }
      return normalizedUrl
    }

    const imageUrlPattern = /(?:data:image\/|blob:|https?:\/\/|\/uploads\/|uploads\/)[^\s)'"<>]+?\.(?:png|jpe?g|gif|webp|bmp|svg)(?:\?[^\s)'"<>]*)?|(?:\/uploads\/|uploads\/)images\/[^\s)'"<>]+/i

    const parseMarkdownTarget = (target = '') => {
      const trimmed = String(target || '').trim()
      if (!trimmed) return ''
      if (trimmed.startsWith('<')) {
        const end = trimmed.indexOf('>')
        return end > 0 ? trimmed.slice(1, end).trim() : trimmed.replace(/^<|>$/g, '').trim()
      }
      return trimmed.replace(/\s+["'][^"']*["']\s*$/, '').trim()
    }

    const isImageUrl = (url) => imageUrlPattern.test(resolveContentImageUrl(url))

    const extractFirstImageUrl = (content = '') => {
      const text = String(content || '')
      const htmlMatch = text.match(/<img[^>]+src=["']([^"']+)["']/i)
      if (htmlMatch?.[1]) {
        return resolveContentImageUrl(htmlMatch[1])
      }

      const markdownMatch = text.match(/!\[[^\]]*]\(\s*<?([^)\s>]+)(?:\s+["'][^)]*["'])?\s*\)/i)
      if (markdownMatch?.[1]) {
        return resolveContentImageUrl(markdownMatch[1])
      }

      const markdownLinkMatch = text.match(/\[[^\]]*]\(([^)]*)\)/i)
      if (markdownLinkMatch?.[1]) {
        const target = parseMarkdownTarget(markdownLinkMatch[1])
        if (isImageUrl(target)) {
          return resolveContentImageUrl(target)
        }
      }

      const plainMatch = text.match(imageUrlPattern)
      return plainMatch?.[0] ? resolveContentImageUrl(plainMatch[0]) : ''
    }

    const stripInlineMediaSyntax = (content = '') => {
      return String(content || '')
        .replace(/<img\b[^>]*>/gi, ' ')
        .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
        .replace(/\[([^\]]*)]\(([^)]*)\)/g, (match, label, target) => {
          return isImageUrl(parseMarkdownTarget(target)) ? ' ' : label
        })
        .replace(new RegExp(imageUrlPattern.source, 'gi'), ' ')
        .replace(/<[^>]+>/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
    }

    const getQuestionImage = (question) => {
      return extractFirstImageUrl(question?.content || question?.summary || '')
    }

    const getQuestionExcerpt = (question) => {
      return stripInlineMediaSyntax(question?.summary || question?.content || '')
    }
    
    const loadQuestions = async () => {
      loading.value = true
      
      try {
        const requestParams = {
          page: currentPage.value,
          pageSize
        }
        const response = activeFilter.value === 'solved'
          ? await questionService.getSolvedQuestions(requestParams)
          : activeFilter.value === 'unsolved'
            ? await questionService.getUnsolvedQuestions(requestParams)
            : await questionService.getQuestions(requestParams)

        questions.value = response.data || []
        totalQuestions.value = response.total || 0
      } catch (error) {
        console.error('加载问题列表失败:', error)
        questions.value = []
        totalQuestions.value = 0
      } finally {
        loading.value = false
      }
    }
    
    // 过滤问题
    const filteredQuestions = computed(() => {
      let result = [...questions.value]
      
      // 按解决状态筛选
      if (activeFilter.value === 'solved') {
        result = result.filter(q => q.solved)
      } else if (activeFilter.value === 'unsolved') {
        result = result.filter(q => !q.solved)
      }
      
      // 排序
      if (sortOption.value === 'newest') {
        result.sort((a, b) => toTimestamp(b.createTime) - toTimestamp(a.createTime))
      } else if (sortOption.value === 'oldest') {
        result.sort((a, b) => toTimestamp(a.createTime) - toTimestamp(b.createTime))
      } else if (sortOption.value === 'mostAnswers') {
        result.sort((a, b) => b.answerCount - a.answerCount)
      } else if (sortOption.value === 'leastAnswers') {
        result.sort((a, b) => a.answerCount - b.answerCount)
      }
      
      return result
    })
    
    // 总页数
    const totalPages = computed(() => {
      return Math.ceil(totalQuestions.value / pageSize)
    })
    
    // 空状态消息
    const emptyStateMessage = computed(() => {
      if (activeFilter.value === 'solved') {
        return '暂无已解决问题'
      } else if (activeFilter.value === 'unsolved') {
        return '暂无未解决问题'
      } else {
        return '暂无问题'
      }
    })
    
    // 设置筛选条件
    const setFilter = (filter) => {
      activeFilter.value = filter
      currentPage.value = 1
    }
    
    // 切换页码
    const changePage = (page) => {
      currentPage.value = page
      loadQuestions()
    }
    
    // 跳转到问题详情
    const navigateToQuestion = (id) => {
      router.push(`/question/${id}`)
    }
    
    // 监听筛选条件和排序方式变化
    watch([activeFilter, sortOption], () => {
      currentPage.value = 1
      loadQuestions()
    })
    
    onMounted(() => {
      loadQuestions()
    })
    
    return {
      loading,
      questions,
      activeFilter,
      sortOption,
      currentPage,
      totalPages,
      filteredQuestions,
      emptyStateMessage,
      setFilter,
      changePage,
      navigateToQuestion,
      formatQuestionTime,
      getQuestionImage,
      getQuestionExcerpt
    }
  }
})
</script>

<style scoped>
.questions-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.questions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-content h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-color);
}

.description {
  color: var(--text-light);
  font-size: 16px;
}

.btn-ask {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background-color: var(--primary-color);
  color: white;
  border-radius: var(--radius);
  text-decoration: none;
  font-weight: 500;
  transition: var(--transition);
}

.btn-ask:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
}

.filter-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background-color: white;
  border-radius: var(--radius);
  padding: 12px 16px;
  box-shadow: var(--shadow-sm);
}

.filter-options {
  display: flex;
  gap: 8px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background-color: white;
  color: var(--text-light);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.filter-btn.active {
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.filter-btn:hover:not(.active) {
  background-color: var(--bg-gray);
}

.sort-select {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background-color: white;
  color: var(--text-color);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: var(--transition);
}

.sort-select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.questions-content {
  display: flex;
  gap: 24px;
}

.questions-list {
  flex: 1;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-lighter);
}

.loading-icon {
  font-size: 32px;
  margin-bottom: 16px;
  color: var(--primary-color);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: white;
  border-radius: var(--radius);
  padding: 60px 20px;
  box-shadow: var(--shadow-sm);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: var(--text-lighter);
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 24px;
  color: var(--text-light);
  font-size: 16px;
}

.btn-ask-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  background-color: var(--primary-color);
  color: white;
  border-radius: var(--radius);
  text-decoration: none;
  font-weight: 500;
  transition: var(--transition);
}

.question-item {
  display: flex;
  background-color: white;
  border-radius: var(--radius);
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
  cursor: pointer;
}

.question-item:hover {
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.question-stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 20px;
  min-width: 80px;
}

.stat-item {
  text-align: center;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.stat-label {
  font-size: 12px;
  color: var(--text-lighter);
}

.question-main {
  flex: 1;
  min-width: 0;
}

.question-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-color);
  display: flex;
  align-items: center;
}

.solved-badge {
  display: inline-block;
  padding: 2px 8px;
  background-color: #52c41a;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: normal;
  margin-right: 8px;
}

.question-excerpt {
  font-size: 14px;
  color: var(--text-light);
  margin-bottom: 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-image-preview {
  width: min(320px, 100%);
  aspect-ratio: 16 / 9;
  margin: 0 0 16px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #f3f4f6;
}

.question-image-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-block;
  padding: 4px 8px;
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--radius-sm);
  font-size: 12px;
  transition: var(--transition);
}

.tag:hover {
  background-color: var(--primary-color);
  color: white;
}

.user-info {
  display: flex;
  align-items: center;
}

.avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 8px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  font-size: 14px;
  color: var(--text-color);
  margin-right: 8px;
}

.time {
  font-size: 12px;
  color: var(--text-lighter);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 32px;
}

.page-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background-color: white;
  color: var(--text-color);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-btn:not(:disabled):hover {
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.page-info {
  margin: 0 16px;
  font-size: 14px;
  color: var(--text-light);
}

@media (max-width: 768px) {
  .questions-content {
    flex-direction: column;
  }
}
</style> 
