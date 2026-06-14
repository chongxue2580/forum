<template>
  <div class="question-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin size="2x" />
      </div>
      <p>加载问题内容中...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" size="2x" />
      </div>
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="loadQuestion">重试</button>
    </div>
    
    <!-- 问题内容 -->
    <div v-else-if="question" class="question-content">
      <div class="question-header">
        <h1 class="question-title">{{ question.title }}</h1>
        
        <div class="question-meta">
          <div class="meta-item">
            <font-awesome-icon :icon="['fas', 'clock']" />
            提问于 {{ formattedDate }}
          </div>
          <div class="meta-item">
            <font-awesome-icon :icon="['fas', 'eye']" />
            {{ question.viewCount }} 次查看
          </div>
          <div class="meta-item">
            <font-awesome-icon :icon="['fas', 'comment-alt']" />
            {{ question.answerCount }} 个回答
          </div>
        </div>
        
        <div class="question-actions">
          <button 
            v-if="!isFollowing" 
            class="action-btn follow-btn" 
            @click="followQuestion"
          >
            <font-awesome-icon :icon="['far', 'star']" />
            关注问题
          </button>
          <button 
            v-else 
            class="action-btn following-btn" 
            @click="unfollowQuestion"
          >
            <font-awesome-icon :icon="['fas', 'star']" />
            已关注
          </button>
          
          <div v-if="isAuthor" class="author-actions">
            <button class="action-btn edit-btn" @click="editQuestion">
              <font-awesome-icon :icon="['fas', 'edit']" />
              编辑
            </button>
            <button class="action-btn delete-btn" @click="showDeleteModal = true">
              <font-awesome-icon :icon="['fas', 'trash-alt']" />
              删除
            </button>
          </div>
        </div>
      </div>
      
      <div class="question-body">
        <div class="question-main">
          <!-- 投票控制 -->
          <div class="vote-controls">
            <button 
              class="vote-btn up" 
              :class="{ active: userVote === 'up' }"
              @click="vote('up')"
              title="这个问题有用且清晰"
            >
              <font-awesome-icon :icon="['fas', 'caret-up']" />
            </button>
            
            <div class="vote-count" :class="{ 
              positive: question.voteCount > 0, 
              negative: question.voteCount < 0 
            }">
              {{ question.voteCount }}
            </div>
            
          </div>
          
          <!-- 问题内容 -->
          <div class="question-content-body">
            <div class="markdown-content" v-html="formattedContent"></div>
            
            <div class="content-footer">
              <!-- 标签 -->
              <div class="question-tags">
                <div 
                  v-for="tag in question.tags" 
                  :key="tag.id || tag" 
                  class="tag"
                >
                  {{ tag.name || tag }}
                </div>
              </div>
              
              <!-- 作者信息 -->
              <div class="author-info">
                <div class="author-avatar" :title="question.author.name">
                  {{ authorInitials }}
                </div>
                <div class="author-name">{{ question.author.name }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 问题状态标识 -->
        <div 
          v-if="question.isSolved" 
          class="status-badge solved"
          title="此问题已有满意答案"
        >
          <font-awesome-icon :icon="['fas', 'check-circle']" />
          已解决
        </div>
      </div>
      
      <!-- 回答列表 -->
      <answer-list
        :question-id="questionId"
        :question-author-id="question.author.id"
        :initial-answers="question.answers"
        :initial-best-answer-id="question.bestAnswerId"
        @answers-updated="handleAnswersUpdated"
      />
    </div>
    
    <!-- 删除确认模态框 -->
    <div class="modal-backdrop" v-if="showDeleteModal" @click.self="showDeleteModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="close-btn" @click="showDeleteModal = false">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
        
        <div class="modal-body">
          <p>确定要删除这个问题吗？此操作无法撤销。</p>
        </div>
        
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showDeleteModal = false">取消</button>
          <button class="btn btn-danger" @click="deleteQuestion">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { marked } from 'marked'
import hljs from 'highlight.js'
import AnswerList from '@/components/AnswerList.vue'
import { questionService } from '../services/questionService'
import { formatDateTime, getTimeField } from '../utils/dateUtils'

// 配置marked使用highlight.js
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {}
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true
})

export default defineComponent({
  name: 'QuestionDetail',
  components: {
    AnswerList
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    
    // 状态
    const question = ref(null)
    const loading = ref(true)
    const error = ref(null)
    const showDeleteModal = ref(false)
    const userVote = ref(null) // 'up', 'down', or null
    const isFollowing = ref(false)
    
    // 计算属性
    const questionId = computed(() => route.params.id)
    
    const isAuthor = computed(() => {
      return store.state.user && question.value && 
        String(store.state.user.id) === String(question.value.author.id)
    })
    
    const formattedContent = computed(() => {
      if (!question.value || !question.value.content) return ''
      return marked.parse(question.value.content)
    })
    
    const formattedDate = computed(() => {
      if (!question.value) return ''

      const timeValue = getTimeField(question.value, 'created')
      return formatDateTime(timeValue, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    })
    
    const authorInitials = computed(() => {
      if (!question.value || !question.value.author || !question.value.author.name) return '?'
      
      return question.value.author.name
        .split(' ')
        .map(n => n[0])
        .join('')
        .toUpperCase()
        .substring(0, 2)
    })
    
    // 方法
    const getFlag = (data, ...keys) => {
      for (const key of keys) {
        if (typeof data?.[key] === 'boolean') {
          return data[key]
        }
      }
      return false
    }

    const unwrapResponseData = (response) => response?.data ?? response ?? {}

    const applyQuestionState = (questionState = {}) => {
      if (!question.value || !questionState) return

      if (typeof questionState.commentCount === 'number' || typeof questionState.answerCount === 'number') {
        question.value.answerCount = questionState.commentCount ?? questionState.answerCount
      }
      if (typeof questionState.likeCount === 'number' || typeof questionState.voteCount === 'number') {
        question.value.voteCount = questionState.likeCount ?? questionState.voteCount
      }
      if (typeof questionState.followCount === 'number') {
        question.value.followCount = questionState.followCount
      }
      if (typeof questionState.isSolved === 'boolean' || typeof questionState.solved === 'boolean') {
        const solved = Boolean(questionState.isSolved ?? questionState.solved)
        question.value.isSolved = solved
        question.value.solved = solved
      }
    }

    const refreshQuestionLikeInfo = async () => {
      const response = await questionService.getQuestionLikeInfo(questionId.value)
      const info = unwrapResponseData(response)
      userVote.value = getFlag(info, 'liked', 'isLiked') ? 'up' : null
      if (question.value) {
        question.value.voteCount = info.count ?? question.value.voteCount ?? 0
      }
    }

    const refreshQuestionFollowInfo = async () => {
      const response = await questionService.getQuestionFollowInfo(questionId.value)
      const info = unwrapResponseData(response)
      isFollowing.value = getFlag(info, 'followed', 'isFollowed')
      if (question.value) {
        question.value.followCount = info.count ?? question.value.followCount ?? 0
      }
    }

    const loadQuestion = async () => {
      loading.value = true
      error.value = null

      try {
        const response = await questionService.getQuestionById(questionId.value)

        if (response.success) {
          const questionData = unwrapResponseData(response)
          const commentsResponse = await questionService.getQuestionComments(questionId.value, {
            page: 1,
            pageSize: 100
          })
          const answers = commentsResponse.data || []
          const bestAnswer = answers.find(answer => answer.isBestAnswer)

          question.value = {
            ...questionData,
            answers,
            bestAnswerId: bestAnswer?.id || questionData.bestAnswerId || null,
            answerCount: commentsResponse.total ?? answers.length,
            isSolved: Boolean(questionData.isSolved || questionData.solved || bestAnswer),
            solved: Boolean(questionData.solved || questionData.isSolved || bestAnswer)
          }

          if (store.state.isAuthenticated) {
            await Promise.all([
              refreshQuestionLikeInfo(),
              refreshQuestionFollowInfo()
            ])
          } else {
            userVote.value = null
            isFollowing.value = false
          }
        } else {
          throw new Error(response.message || '获取问题详情失败')
        }
      } catch (err) {
        console.error('Failed to load question:', err)
        error.value = '加载问题失败，请稍后重试'
      } finally {
        loading.value = false
      }
    }
    
    const vote = async (type) => {
      if (!store.state.isAuthenticated) {
        store.dispatch('setMessage', {
          type: 'warning',
          content: '请先登录再进行投票'
        })
        return
      }

      if (type !== 'up') return

      try {
        if (userVote.value === 'up') {
          await questionService.unlikeQuestion(questionId.value)
        } else {
          await questionService.likeQuestion(questionId.value)
        }
        await refreshQuestionLikeInfo()
      } catch (err) {
        console.error('Failed to vote question:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '问题点赞失败'
        })
      }
    }
    
    const followQuestion = async () => {
      if (!store.state.isAuthenticated) {
        store.dispatch('setMessage', {
          type: 'warning',
          content: '请先登录再关注问题'
        })
        return
      }

      try {
        await questionService.followQuestion(questionId.value)
        await refreshQuestionFollowInfo()
      } catch (err) {
        console.error('Failed to follow question:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '关注问题失败'
        })
      }
    }
    
    const unfollowQuestion = async () => {
      if (!store.state.isAuthenticated) {
        store.dispatch('setMessage', {
          type: 'warning',
          content: '请先登录再取消关注问题'
        })
        return
      }

      try {
        await questionService.unfollowQuestion(questionId.value)
        await refreshQuestionFollowInfo()
      } catch (err) {
        console.error('Failed to unfollow question:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '取消关注问题失败'
        })
      }
    }
    
    const editQuestion = () => {
      router.push(`/question/${questionId.value}/edit`)
    }
    
    const deleteQuestion = async () => {
      try {
        await questionService.deleteQuestion(questionId.value)
        
        // 关闭模态框
        showDeleteModal.value = false
        
        // 显示成功消息
        store.dispatch('setMessage', {
          type: 'success',
          content: '问题已成功删除'
        })
        
        // 跳转到问题列表页面
        router.push('/questions')
      } catch (err) {
        console.error('Failed to delete question:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: '删除问题失败，请稍后重试'
        })
      }
    }

    const handleAnswersUpdated = ({ answers, bestAnswerId, answerCount, question: latestQuestion }) => {
      if (!question.value) return

      question.value.answers = answers || []
      question.value.bestAnswerId = bestAnswerId || null
      question.value.answerCount = answerCount ?? question.value.answers.length
      question.value.isSolved = Boolean(bestAnswerId)
      question.value.solved = Boolean(bestAnswerId)
      applyQuestionState(latestQuestion)
    }
    
    // 生命周期钩子
    onMounted(() => {
      loadQuestion()
    })
    
    return {
      question,
      loading,
      error,
      showDeleteModal,
      userVote,
      isFollowing,
      questionId,
      isAuthor,
      formattedContent,
      formattedDate,
      authorInitials,
      loadQuestion,
      vote,
      followQuestion,
      unfollowQuestion,
      editQuestion,
      deleteQuestion,
      handleAnswersUpdated
    }
  }
})
</script>

<style scoped>
.question-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.loading-spinner,
.error-icon {
  margin-bottom: 20px;
  color: #1890ff;
}

.error-icon {
  color: #ff4d4f;
}

.question-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  max-width: 850px;
  margin: 0 auto;
}

.question-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.question-title {
  font-size: 24px;
  color: #333;
  margin-top: 0;
  margin-bottom: 15px;
  font-weight: 600;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
}

.question-actions {
  display: flex;
  justify-content: space-between;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #eee;
  border-radius: 4px;
  background: white;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.follow-btn {
  color: #1890ff;
}

.following-btn {
  background: #e6f7ff;
  color: #1890ff;
  border-color: #91d5ff;
}

.edit-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.delete-btn:hover {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.author-actions {
  display: flex;
  gap: 10px;
}

.question-body {
  padding: 20px;
  position: relative;
}

.question-main {
  display: flex;
  gap: 20px;
}

.vote-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.vote-btn {
  background: none;
  border: none;
  color: #999;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.vote-btn:hover {
  color: #1890ff;
}

.vote-btn.active.up {
  color: #52c41a;
}

.vote-btn.active.down {
  color: #ff4d4f;
}

.vote-count {
  font-size: 18px;
  font-weight: 700;
  color: #666;
  text-align: center;
  min-width: 30px;
}

.vote-count.positive {
  color: #52c41a;
}

.vote-count.negative {
  color: #ff4d4f;
}

.question-content-body {
  flex: 1;
}

.markdown-content {
  color: #333;
  line-height: 1.6;
  margin-bottom: 20px;
  overflow-wrap: break-word;
}

.content-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 30px;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  background: #f5f5f5;
  color: #666;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}

.author-name {
  font-weight: 500;
  color: #333;
}

.status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 4px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.status-badge.solved {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .question-detail-container {
    padding: 1rem;
  }

  .question-content {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .question-detail-container {
    padding: 0.5rem;
  }

  .question-header {
    padding: 16px;
  }

  .question-title {
    font-size: 20px;
  }

  .question-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .question-body {
    padding: 16px;
  }

  .question-main {
    flex-direction: column;
    gap: 16px;
  }

  .vote-controls {
    flex-direction: row;
    justify-content: center;
    gap: 16px;
  }
}

.modal-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 16px;
}

.modal-body {
  padding: 20px 15px;
}

.modal-footer {
  padding: 15px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  background: white;
  color: #666;
  border: 1px solid #eee;
}

.btn-secondary:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-danger:hover {
  background: #ff7875;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover {
  background: #40a9ff;
}
</style> 
