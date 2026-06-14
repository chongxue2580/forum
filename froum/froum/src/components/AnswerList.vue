<template>
  <div class="answer-list-container">
    <!-- 回答统计和排序 -->
    <div class="answers-header">
      <h2>{{ answers.length }} 个回答</h2>
      <div class="sort-options">
        <button 
          class="sort-btn" 
          :class="{ active: sortBy === 'votes' }"
          @click="sortBy = 'votes'"
        >
          <font-awesome-icon :icon="['fas', 'sort-amount-down']" />
          按投票排序
        </button>
        <button 
          class="sort-btn" 
          :class="{ active: sortBy === 'newest' }"
          @click="sortBy = 'newest'"
        >
          <font-awesome-icon :icon="['fas', 'clock']" />
          最新回答
        </button>
        <button 
          class="sort-btn" 
          :class="{ active: sortBy === 'oldest' }"
          @click="sortBy = 'oldest'"
        >
          <font-awesome-icon :icon="['fas', 'history']" />
          最早回答
        </button>
      </div>
    </div>
    
    <!-- 回答列表 -->
    <div v-if="sortedAnswers.length > 0" class="answer-items">
      <div 
        v-for="answer in sortedAnswers" 
        :key="answer.id" 
        class="answer-item"
        :class="{ 'is-best-answer': isAnswerBest(answer.id) }"
      >
        <!-- 最佳答案标识 -->
        <div v-if="isAnswerBest(answer.id)" class="best-answer-badge">
          <font-awesome-icon :icon="['fas', 'check-circle']" />
          最佳答案
        </div>
        
        <div class="answer-main">
          <!-- 投票控制 -->
          <div class="vote-controls">
            <button 
              class="vote-btn up" 
              :class="{ active: answer.userVote === 'up' }"
              @click="voteAnswer(answer.id, 'up')"
              title="这个回答有帮助"
            >
              <font-awesome-icon :icon="['fas', 'caret-up']" />
            </button>
            
            <div class="vote-count" :class="{ 
              positive: answer.voteCount > 0, 
              negative: answer.voteCount < 0 
            }">
              {{ answer.voteCount }}
            </div>
            
          </div>
          
          <!-- 回答内容 -->
          <div class="answer-content">
            <!-- 内容主体 -->
            <div class="answer-body">
              <div class="markdown-content" v-html="formatMarkdown(answer.content)"></div>
              
              <!-- 回答操作 -->
              <div class="answer-actions">
                <div class="action-group">
                  <!-- 最佳答案控制 (仅问题作者可见) -->
                  <button 
                    v-if="isQuestionAuthor && !isAnswerBest(answer.id)" 
                    class="action-btn accept-btn"
                    @click="setBestAnswer(answer.id)"
                    title="将此回答标记为最佳答案"
                  >
                    <font-awesome-icon :icon="['far', 'check-circle']" />
                    采纳为最佳答案
                  </button>
                  
                  <button 
                    v-if="isQuestionAuthor && isAnswerBest(answer.id)" 
                    class="action-btn unaccept-btn"
                    @click="unsetBestAnswer"
                    title="取消最佳答案标记"
                  >
                    <font-awesome-icon :icon="['fas', 'times']" />
                    取消最佳答案
                  </button>
                </div>
                
                <div class="action-group" v-if="isAnswerAuthor(answer)">
                  <button 
                    class="action-btn delete-btn"
                    @click="confirmDeleteAnswer(answer.id)"
                    title="删除回答"
                  >
                    <font-awesome-icon :icon="['fas', 'trash-alt']" />
                    删除
                  </button>
                </div>
              </div>
            </div>
            
            <!-- 回答元数据 -->
            <div class="answer-meta">
              <div class="answer-time">
                回答于 {{ formatDate(answer.createTime) }}
              </div>
              
              <div class="author-info">
                <div class="author-avatar" :title="answer.author.name">
                  {{ getAuthorInitials(answer.author.name) }}
                </div>
                <div class="author-name">{{ answer.author.name }}</div>
              </div>
            </div>
            
            <!-- 评论区域 - 使用新组件 -->
            <answer-comments
              :comments="answer.comments || []"
              :answer-id="answer.id"
              @add-comment="handleAddComment"
            />
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="no-answers">
      <div class="empty-state">
        <font-awesome-icon :icon="['fas', 'comment-alt']" class="empty-icon" />
        <p>暂无回答</p>
        <p class="hint">成为第一个回答这个问题的人吧！</p>
      </div>
    </div>
    
    <!-- 添加回答 -->
    <div class="add-answer-section" :class="{ 'is-focused': isEditorFocused }">
      <h3>
        <font-awesome-icon :icon="['fas', 'pen']" />
        撰写回答
      </h3>
      
      <div class="editor-container">
        <textarea
          v-model="newAnswer"
          class="answer-editor"
          placeholder="请输入您的回答内容，支持Markdown格式..."
          @focus="isEditorFocused = true"
          @blur="isEditorFocused = false"
        ></textarea>
        
        <div class="editor-footer">
          <div class="editor-tips">
            <span>支持 Markdown 语法</span>
            <a href="#" target="_blank" class="markdown-help">
              <font-awesome-icon :icon="['fas', 'question-circle']" />
              Markdown 帮助
            </a>
          </div>
          
          <button 
            class="btn btn-primary submit-btn" 
            @click="submitAnswer"
            :disabled="!newAnswer.trim() || isSubmitting"
          >
            <font-awesome-icon :icon="['fas', 'paper-plane']" v-if="!isSubmitting" />
            <font-awesome-icon :icon="['fas', 'spinner']" spin v-else />
            发布回答
          </button>
        </div>
      </div>
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
          <p>确定要删除这个回答吗？此操作无法撤销。</p>
        </div>
        
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showDeleteModal = false">取消</button>
          <button class="btn btn-danger" @click="deleteAnswer(answerToDelete)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import hljs from 'highlight.js'
import AnswerComments from './AnswerComments.vue'
import { questionService } from '../services/questionService'
import { formatDateTime } from '../utils/dateUtils'

// Configure marked with syntax highlighting
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
  name: 'AnswerList',
  components: {
    AnswerComments
  },
  props: {
    questionId: {
      type: [Number, String],
      required: true
    },
    questionAuthorId: {
      type: [Number, String],
      required: true
    },
    initialAnswers: {
      type: Array,
      default: () => []
    },
    initialBestAnswerId: {
      type: [Number, String],
      default: null
    }
  },
  emits: ['answers-updated'],
  setup(props, { emit }) {
    const store = useStore()
    const router = useRouter()
    
    const answers = ref([...props.initialAnswers])
    const bestAnswerId = ref(props.initialBestAnswerId)
    const sortBy = ref('votes') // 'votes', 'newest', 'oldest'
    const newAnswer = ref('')
    const isEditorFocused = ref(false)
    const isSubmitting = ref(false)
    const showDeleteModal = ref(false)
    const answerToDelete = ref(null)
    const commentInputs = ref({})

    const normalizeAnswers = (answerList = []) => answerList.map(answer => ({
      ...answer,
      voteCount: answer.voteCount ?? answer.likeCount ?? answer.likes ?? 0,
      userVote: answer.userVote || (answer.liked || answer.isLiked ? 'up' : null),
      comments: answer.comments || answer.children || answer.replies || []
    }))

    const getFlag = (data, ...keys) => {
      for (const key of keys) {
        if (typeof data?.[key] === 'boolean') {
          return data[key]
        }
      }
      return false
    }

    const unwrapResponseData = (response) => response?.data ?? response ?? null

    const emitAnswersUpdated = (extra = {}) => {
      emit('answers-updated', {
        answers: answers.value,
        bestAnswerId: bestAnswerId.value,
        answerCount: answers.value.length,
        ...extra
      })
    }

    const syncAnswerState = (answerList, shouldEmit = true, initialBestAnswerId = null, latestQuestion = null) => {
      answers.value = normalizeAnswers(answerList)
      bestAnswerId.value = answers.value.find(answer => answer.isBestAnswer)?.id || initialBestAnswerId
      answers.value.forEach(answer => {
        commentInputs.value[answer.id] = commentInputs.value[answer.id] || ''
      })
      if (shouldEmit) {
        emitAnswersUpdated(latestQuestion ? { question: latestQuestion } : {})
      }
    }

    const enrichAnswerLikeStates = async (answerList) => {
      if (!store.state.isAuthenticated || !answerList.length) {
        return normalizeAnswers(answerList)
      }

      const normalizedAnswers = normalizeAnswers(answerList)
      const results = await Promise.allSettled(
        normalizedAnswers.map(answer => questionService.getAnswerLikeInfo(answer.id))
      )

      return normalizedAnswers.map((answer, index) => {
        const result = results[index]
        if (result.status !== 'fulfilled') {
          return answer
        }

        const info = unwrapResponseData(result.value) || {}
        return {
          ...answer,
          voteCount: info.count ?? answer.voteCount ?? 0,
          userVote: getFlag(info, 'liked', 'isLiked') ? 'up' : null
        }
      })
    }

    const loadAnswers = async (latestQuestion = null) => {
      const response = await questionService.getQuestionComments(props.questionId, {
        page: 1,
        pageSize: 100
      })
      const latestAnswers = await enrichAnswerLikeStates(response.data || [])
      syncAnswerState(latestAnswers, true, null, latestQuestion)
    }
    
    // 监听props变化
    watch(() => props.initialAnswers, async (newVal) => {
      const enrichedAnswers = await enrichAnswerLikeStates(newVal || [])
      syncAnswerState(enrichedAnswers, false, props.initialBestAnswerId)
    })
    
    watch(() => props.initialBestAnswerId, (newVal) => {
      bestAnswerId.value = newVal
    })
    
    // 初始化评论输入框
    onMounted(async () => {
      if (answers.value.length > 0) {
        const enrichedAnswers = await enrichAnswerLikeStates(answers.value)
        syncAnswerState(enrichedAnswers, false, props.initialBestAnswerId)
        return
      }

      await loadAnswers()
    })
    
    // 按排序规则排序回答
    const toTimestamp = (time) => {
      if (!time) return 0
      if (Array.isArray(time)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = time
        return new Date(year, month - 1, day, hour, minute, second).getTime()
      }
      return new Date(time).getTime()
    }

    const sortedAnswers = computed(() => {
      const result = [...answers.value]
      const bestId = bestAnswerId.value

      result.sort((a, b) => {
        const aIsBest = bestId && String(a.id) === String(bestId)
        const bIsBest = bestId && String(b.id) === String(bestId)
        if (aIsBest && !bIsBest) return -1
        if (!aIsBest && bIsBest) return 1

        if (sortBy.value === 'votes') {
          return (b.voteCount || 0) - (a.voteCount || 0)
        }
        if (sortBy.value === 'newest') {
          return toTimestamp(b.createTime) - toTimestamp(a.createTime)
        }
        if (sortBy.value === 'oldest') {
          return toTimestamp(a.createTime) - toTimestamp(b.createTime)
        }

        return 0
      })
      
      return result
    })
    
    const isQuestionAuthor = computed(() => {
      return store.state.user && String(store.state.user.id) === String(props.questionAuthorId)
    })
    
    // 格式化Markdown内容
    const formatMarkdown = (content) => {
      return marked.parse(content || '')
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      return formatDateTime(dateString, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    // 获取作者首字母
    const getAuthorInitials = (name) => {
      if (!name) return '?'
      
      return name
        .split(' ')
        .map(n => n[0])
        .join('')
        .toUpperCase()
        .substring(0, 2)
    }
    
    // 确认删除回答
    const confirmDeleteAnswer = (answerId) => {
      answerToDelete.value = answerId
      showDeleteModal.value = true
    }
    
    // 删除回答
    const deleteAnswer = async (answerId) => {
      if (!answerId) return
      
      try {
        await questionService.deleteAnswer(answerId)
        
        // 关闭确认框
        showDeleteModal.value = false
        answerToDelete.value = null
        await loadAnswers()
        
        // 显示成功消息
        store.dispatch('setMessage', {
          type: 'success',
          content: '回答已成功删除'
        })
      } catch (err) {
        console.error('Failed to delete answer:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: '删除回答失败，请稍后重试'
        })
      }
    }
    
    // 设置最佳答案
    const setBestAnswer = async (answerId) => {
      try {
        const response = await questionService.setBestAnswer(props.questionId, answerId)
        await loadAnswers(unwrapResponseData(response))
        
        // 显示成功消息
        store.dispatch('setMessage', {
          type: 'success',
          content: '已设置为最佳答案'
        })
      } catch (err) {
        console.error('Failed to set best answer:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: '设置最佳答案失败，请稍后重试'
        })
      }
    }
    
    // 取消最佳答案
    const unsetBestAnswer = async () => {
      try {
        const response = await questionService.unsetBestAnswer(props.questionId)
        await loadAnswers(unwrapResponseData(response))
        
        // 显示成功消息
        store.dispatch('setMessage', {
          type: 'success',
          content: '已取消最佳答案'
        })
      } catch (err) {
        console.error('Failed to unset best answer:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: '取消最佳答案失败，请稍后重试'
        })
      }
    }
    
    // 提交回答
    const submitAnswer = async () => {
      if (!newAnswer.value.trim()) return
      
      if (!store.state.isAuthenticated) {
        store.dispatch('setMessage', {
          type: 'warning',
          content: '请先登录再回答问题'
        })
        router.push('/login?redirect=' + router.currentRoute.value.fullPath)
        return
      }
      
      isSubmitting.value = true
      
      try {
        await questionService.addAnswer(props.questionId, newAnswer.value)
        
        // 清空输入
        newAnswer.value = ''
        await loadAnswers()
        
        // 显示成功消息
        store.dispatch('setMessage', {
          type: 'success',
          content: '回答已成功发布'
        })
      } catch (err) {
        console.error('Failed to submit answer:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: '发布回答失败，请稍后重试'
        })
      } finally {
        isSubmitting.value = false
      }
    }
    
    // 投票
    const voteAnswer = async (answerId, voteType) => {
      if (!store.state.isAuthenticated) {
        store.dispatch('setMessage', {
          type: 'warning',
          content: '请先登录再投票'
        })
        router.push('/login?redirect=' + router.currentRoute.value.fullPath)
        return
      }
      
      const answer = answers.value.find(a => a.id === answerId)
      if (!answer) return

      if (voteType !== 'up') return
      
      try {
        if (answer.userVote === 'up') {
          await questionService.unlikeAnswer(answerId)
        } else {
          await questionService.likeAnswer(answerId)
        }
        await loadAnswers()
      } catch (err) {
        console.error('Failed to vote answer:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '回答点赞失败'
        })
      }
    }
    
    // 检查答案是否为最佳答案
    const isAnswerBest = (answerId) => {
      return String(answerId) === String(bestAnswerId.value)
    }
    
    // 检查用户是否为答案作者
    const isAnswerAuthor = (answer) => {
      return store.state.user && String(store.state.user.id) === String(answer.author.id)
    }
    
    // 添加评论
    const handleAddComment = async ({ answerId, content }) => {
      if (!store.state.isAuthenticated) {
        store.dispatch('setMessage', {
          type: 'warning',
          content: '请先登录再评论'
        })
        router.push('/login?redirect=' + router.currentRoute.value.fullPath)
        return
      }
      
      try {
        await questionService.addAnswerComment(props.questionId, answerId, content)
        await loadAnswers()
      } catch (err) {
        console.error('Failed to add comment:', err)
        store.dispatch('setMessage', {
          type: 'error',
          content: '添加评论失败，请稍后重试'
        })
      }
    }
    
    return {
      answers,
      sortedAnswers,
      bestAnswerId,
      sortBy,
      newAnswer,
      isEditorFocused,
      isSubmitting,
      showDeleteModal,
      answerToDelete,
      commentInputs,
      isQuestionAuthor,
      isAnswerBest,
      isAnswerAuthor,
      formatMarkdown,
      formatDate,
      getAuthorInitials,
      confirmDeleteAnswer,
      deleteAnswer,
      setBestAnswer,
      unsetBestAnswer,
      submitAnswer,
      voteAnswer,
      handleAddComment
    }
  }
})
</script>

<style scoped>
.answer-list-container {
  margin-top: 40px;
}

.answers-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.answers-header h2 {
  font-size: 1.5em;
  font-weight: 500;
  color: #333;
}

.sort-options {
  display: flex;
  gap: 10px;
}

.sort-btn {
  background: none;
  border: none;
  color: #666;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sort-btn:hover {
  color: #1890ff;
  background: #f0f8ff;
}

.sort-btn.active {
  color: #1890ff;
  font-weight: 500;
}

.answer-items {
  display: flex;
  flex-direction: column;
  gap: 30px;
  margin-bottom: 40px;
}

.answer-item {
  position: relative;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
}

.answer-item.is-best-answer {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.best-answer-badge {
  position: absolute;
  top: -12px;
  left: 20px;
  background: #52c41a;
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  font-size: 14px;
}

.answer-main {
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

.answer-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.markdown-content {
  color: #333;
  line-height: 1.6;
}

.answer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
}

.action-group {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid #eee;
  border-radius: 4px;
  background: white;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.action-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.accept-btn:hover {
  color: #52c41a;
  border-color: #52c41a;
}

.unaccept-btn:hover, .delete-btn:hover {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.answer-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
}

.answer-time {
  color: #999;
  font-size: 14px;
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

.no-answers {
  margin-bottom: 40px;
}

.empty-state {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  margin-bottom: 10px;
  font-size: 18px;
}

.hint {
  font-size: 14px !important;
  color: #666;
}

.add-answer-section {
  background: white;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s;
}

.add-answer-section.is-focused {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.add-answer-section h3 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.answer-editor {
  width: 100%;
  min-height: 150px;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 4px;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-tips {
  color: #999;
  font-size: 14px;
}

.markdown-help {
  margin-left: 15px;
  color: #1890ff;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.submit-btn {
  padding: 8px 20px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.2s;
}

.submit-btn:hover {
  background: #40a9ff;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
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

/* 评论样式 */
.comments-section {
  background: #f9f9f9;
  border-radius: 4px;
  padding: 15px;
  margin-top: 15px;
}

.comments-title {
  font-size: 14px;
  color: #666;
  margin-top: 0;
  margin-bottom: 10px;
  font-weight: 500;
}

.comments-list {
  margin-bottom: 15px;
}

.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  color: #333;
}

.comment-content p {
  margin: 0 0 8px 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #999;
  font-size: 12px;
}

.comment-author {
  font-weight: 500;
  color: #555;
}

.add-comment {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.comment-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.comment-submit {
  padding: 0 12px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}

.comment-submit:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style> 
