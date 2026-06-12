<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { questionService } from '../services/questionService'
import { formatDateTime, getTimeField } from '../utils/dateUtils'

const props = defineProps({
  filter: {
    type: String,
    default: 'all'
  },
  sortBy: {
    type: String,
    default: 'newest'
  },
  tagId: {
    type: Number,
    default: null
  },
  categoryId: {
    type: Number,
    default: null
  },
  userId: {
    type: Number,
    default: null
  },
  searchQuery: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:page', 'update:total'])

const router = useRouter()
const loading = ref(true)
const questions = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalQuestions = ref(0)

const fetchQuestions = async () => {
  loading.value = true
  try {
    let response

    // 根据不同筛选条件调用不同的API
    if (props.tagId) {
      response = await questionService.getQuestionsByTag(props.tagId, currentPage.value - 1, pageSize.value)
    } else if (props.searchQuery) {
      response = await questionService.searchQuestions(props.searchQuery, currentPage.value - 1, pageSize.value)
    } else if (props.userId) {
      response = await questionService.getUserQuestions(props.userId, currentPage.value - 1, pageSize.value)
    } else if (props.filter === 'solved') {
      response = await questionService.getQuestionsBySolvedStatus(true, currentPage.value - 1, pageSize.value)
    } else if (props.filter === 'unsolved') {
      response = await questionService.getQuestionsBySolvedStatus(false, currentPage.value - 1, pageSize.value)
    } else {
      response = await questionService.getPublishedQuestions(currentPage.value - 1, pageSize.value)
    }

    if (response.success) {
      // 处理分页数据结构
      if (response.data && response.data.content) {
        // Spring Boot分页格式：{data: {content: [...], totalElements: ...}}
        questions.value = response.data.content
        totalQuestions.value = response.data.totalElements || 0
        emit('update:total', response.data.totalElements || 0)
      } else if (Array.isArray(response.data)) {
        // 简单数组格式：{data: [...]}
        questions.value = response.data
        totalQuestions.value = response.totalElements || response.data.length
        emit('update:total', response.totalElements || response.data.length)
      } else {
        console.warn('Unexpected response format:', response)
        questions.value = []
        totalQuestions.value = 0
      }
    }
  } catch (error) {
    console.error('加载问题列表失败：', error)
  } finally {
    loading.value = false
  }
}

const goToQuestion = (questionId) => {
  router.push({ name: 'QuestionDetail', params: { id: questionId } })
}

const handlePageChange = (page) => {
  currentPage.value = page
  emit('update:page', page)
  fetchQuestions()
}

// 当筛选条件变化时重新加载数据
watch(
  () => [props.filter, props.sortBy, props.tagId, props.userId, props.searchQuery],
  () => {
    currentPage.value = 1
    emit('update:page', 1)
    fetchQuestions()
  }
)

// 辅助函数
const getAuthorName = (author) => {
  if (!author) return '匿名用户'
  return author.nickname || author.username || author.name || '匿名用户'
}

const getAuthorAvatar = (author) => {
  if (!author) return '/assets/default-avatar.svg'
  return author.avatarUrl || author.avatar || '/assets/default-avatar.svg'
}

const formatTime = (question) => {
  const timeValue = getTimeField(question, 'created')
  return formatDateTime(timeValue)
}

const getQuestionSummary = (question) => {
  if (!question) return ''

  // 优先使用summary字段
  if (question.summary) {
    return question.summary
  }

  // 如果没有summary，从content中截取
  if (question.content) {
    const plainText = question.content.replace(/<[^>]*>/g, '').trim() // 移除HTML标签
    return plainText.length > 150 ? plainText.substring(0, 150) + '...' : plainText
  }

  return '暂无内容摘要'
}

onMounted(() => {
  fetchQuestions()
})
</script>

<template>
  <div class="question-list">
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
    
    <div v-else-if="questions.length === 0" class="empty-state">
      <div class="empty-icon">
        <font-awesome-icon :icon="['fas', 'question-circle']" size="4x" />
      </div>
      <h3>暂无问题</h3>
      <p>暂时没有符合条件的问题，你可以尝试其他筛选条件或者创建一个新的问题</p>
      <router-link to="/question/new" class="create-btn">
        <font-awesome-icon :icon="['fas', 'plus']" />
        创建问题
      </router-link>
    </div>
    
    <ul v-else class="question-items">
      <li v-for="question in questions" :key="question.id" class="question-item" @click="goToQuestion(question.id)">
        <div class="question-stats">
          <div class="stat-item">
            <span class="count">{{ question.viewCount || question.views || 0 }}</span>
            <span class="label">浏览</span>
          </div>
          <div class="stat-item">
            <span class="count">{{ question.answerCount || question.commentCount || 0 }}</span>
            <span class="label">回答</span>
          </div>
          <div class="stat-item" :class="{ 'is-solved': question.solved || question.isSolved }">
            <font-awesome-icon v-if="question.solved || question.isSolved" :icon="['fas', 'check-circle']" class="solved-icon" />
            <span class="label">{{ (question.solved || question.isSolved) ? '已解决' : '未解决' }}</span>
          </div>
        </div>
        
        <div class="question-content">
          <h3 class="question-title">{{ question.title }}</h3>
          <div class="question-summary" v-if="getQuestionSummary(question)">{{ getQuestionSummary(question) }}</div>
          
          <div class="question-meta">
            <div class="question-tags">
              <span v-for="tag in question.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
            </div>
            
            <div class="question-info">
              <span class="author">
                <img :src="getAuthorAvatar(question.author)" class="avatar" alt="作者头像" />
                {{ getAuthorName(question.author) }}
              </span>
              <span class="time">{{ formatTime(question) }}</span>
            </div>
          </div>
        </div>
      </li>
    </ul>
    
    <div class="pagination" v-if="totalQuestions > pageSize">
      <button 
        :disabled="currentPage === 1" 
        @click="handlePageChange(currentPage - 1)"
        class="page-btn prev"
      >
        <font-awesome-icon :icon="['fas', 'chevron-left']" />
      </button>
      
      <span class="page-info">{{ currentPage }} / {{ Math.ceil(totalQuestions / pageSize) }}</span>
      
      <button 
        :disabled="currentPage >= Math.ceil(totalQuestions / pageSize)" 
        @click="handlePageChange(currentPage + 1)"
        class="page-btn next"
      >
        <font-awesome-icon :icon="['fas', 'chevron-right']" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.question-list {
  width: 100%;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border-left-color: #4b70e2;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin: 20px 0;
}

.empty-icon {
  color: #ccc;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 20px;
  margin-bottom: 10px;
  color: #333;
}

.empty-state p {
  color: #666;
  margin-bottom: 20px;
  max-width: 450px;
  margin-left: auto;
  margin-right: auto;
}

.create-btn {
  display: inline-block;
  background-color: #4b70e2;
  color: white;
  padding: 8px 20px;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.2s;
}

.create-btn:hover {
  background-color: #3a5fc9;
}

.question-items {
  list-style: none;
  padding: 0;
  margin: 0;
}

.question-item {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.2s;
}

.question-item:hover {
  background-color: #f5f8ff;
}

.question-stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 80px;
  margin-right: 16px;
}

.stat-item {
  text-align: center;
  margin-bottom: 8px;
}

.stat-item .count {
  display: block;
  font-size: 18px;
  font-weight: 500;
  color: #555;
}

.stat-item .label {
  font-size: 12px;
  color: #777;
}

.is-solved {
  color: #28a745;
}

.solved-icon {
  margin-right: 4px;
}

.question-content {
  flex: 1;
}

.question-title {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.question-summary {
  color: #555;
  margin-bottom: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
}

.tag {
  background-color: #e1ecf4;
  color: #39739d;
  padding: 4px 8px;
  border-radius: 4px;
  margin-right: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}

.question-info {
  display: flex;
  align-items: center;
  color: #777;
}

.author {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

.avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 6px;
}

.time {
  color: #999;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  gap: 10px;
}

.page-btn {
  background-color: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  background-color: #f0f0f0;
  border-color: #d9d9d9;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.page-info {
  padding: 0 10px;
  color: #666;
}

@media (max-width: 768px) {
  .question-item {
    flex-direction: column;
  }
  
  .question-stats {
    flex-direction: row;
    min-width: auto;
    margin-right: 0;
    margin-bottom: 10px;
    width: 100%;
    justify-content: space-around;
  }
  
  .stat-item {
    margin-bottom: 0;
    margin-right: 10px;
    display: flex;
    align-items: center;
  }
  
  .stat-item .count {
    margin-right: 5px;
    font-size: 16px;
  }
  
  .question-meta {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-tags {
    margin-bottom: 8px;
  }
}
</style> 