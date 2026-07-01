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
        v-for="(answer, index) in sortedAnswers"
        :key="answer.id" 
        class="answer-item kumo-surface magnetic-card stagger-item"
        :class="{ 'is-best-answer': isAnswerBest(answer.id) }"
        :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
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
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '删除回答失败，请稍后重试'
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
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '设置最佳答案失败，请稍后重试'
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
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '取消最佳答案失败，请稍后重试'
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
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '发布回答失败，请稍后重试'
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
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '添加评论失败，请稍后重试'
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
  display: grid;
  gap: 1.25rem;
}

.answers-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.answers-header h2 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.35rem;
  font-weight: 840;
}

.sort-options {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
}

.sort-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  min-height: 2.2rem;
  padding: 0.45rem 0.75rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--kumo-text-muted);
  font-weight: 760;
  cursor: pointer;
  transition: var(--transition);
}

.sort-btn:hover,
.sort-btn.active {
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.answer-items {
  display: grid;
  gap: 1rem;
}

.answer-item {
  position: relative;
  padding: clamp(1rem, 2.2vw, 1.5rem);
}

.answer-item.is-best-answer {
  border-color: var(--kumo-status-success);
  background: var(--kumo-status-success-tint);
}

.best-answer-badge {
  position: absolute;
  top: -0.85rem;
  left: 1rem;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.34rem 0.68rem;
  border-radius: 999px;
  background: var(--kumo-status-success);
  color: var(--kumo-text-inverse);
  font-size: 0.78rem;
  font-weight: 800;
}

.answer-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 1rem;
}

.vote-controls {
  display: grid;
  place-items: center;
  align-self: start;
  gap: 0.35rem;
  min-width: 4.2rem;
  padding: 0.65rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
}

.vote-btn {
  display: inline-grid;
  place-items: center;
  width: 2.35rem;
  height: 2.35rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 50%;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    background-color var(--kumo-transition),
    color var(--kumo-transition);
}

.vote-btn:hover,
.vote-btn.active {
  transform: translateY(-2px);
  border-color: var(--kumo-bg-brand);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.vote-count {
  min-width: 2rem;
  color: var(--kumo-text-default);
  font-size: 1.25rem;
  font-weight: 900;
  text-align: center;
}

.vote-count.positive {
  color: var(--kumo-status-success);
}

.vote-count.negative {
  color: var(--kumo-status-danger);
}

.answer-content {
  display: grid;
  min-width: 0;
  gap: 1rem;
}

.markdown-content {
  color: var(--kumo-text-default);
  line-height: 1.72;
}

.markdown-content :deep(p:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.answer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.action-btn,
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 2.35rem;
  padding: 0.48rem 0.8rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  font-weight: 760;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition),
    background-color var(--kumo-transition);
}

.action-btn:hover,
.btn:hover {
  transform: translateY(-2px);
  border-color: var(--kumo-hairline-strong);
  color: var(--kumo-bg-brand-strong);
}

.accept-btn:hover {
  border-color: var(--kumo-status-success);
  color: var(--kumo-status-success);
}

.unaccept-btn:hover,
.delete-btn:hover {
  border-color: var(--kumo-status-danger);
  color: var(--kumo-status-danger);
}

.answer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-top: 0.8rem;
  border-top: 1px solid var(--kumo-hairline);
}

.answer-time {
  color: var(--kumo-text-subtle);
  font-size: 0.86rem;
  font-weight: 690;
}

.author-info {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 840;
}

.author-name {
  color: var(--kumo-text-default);
  font-weight: 760;
}

.no-answers {
  display: grid;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 0.6rem;
  min-height: 12rem;
  padding: 2rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
  text-align: center;
}

.empty-icon {
  color: var(--kumo-bg-brand);
  font-size: 2.6rem;
}

.empty-state p {
  margin: 0;
  font-weight: 760;
}

.hint {
  color: var(--kumo-text-subtle);
  font-size: 0.9rem !important;
}

.add-answer-section {
  display: grid;
  gap: 1rem;
  padding: clamp(1rem, 2.2vw, 1.4rem);
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-elevated);
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.add-answer-section.is-focused {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
}

.add-answer-section h3 {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.05rem;
  font-weight: 840;
}

.editor-container {
  display: grid;
  gap: 1rem;
}

.answer-editor {
  width: 100%;
  min-height: 10rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.answer-editor:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.editor-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  color: var(--kumo-text-subtle);
  font-size: 0.88rem;
  font-weight: 690;
}

.markdown-help {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--kumo-bg-brand-strong);
  text-decoration: none;
}

.submit-btn,
.btn-primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-brand-strong));
  color: var(--kumo-text-inverse);
}

.submit-btn:disabled {
  background: var(--kumo-bg-recessed);
  color: var(--kumo-text-subtle);
  cursor: not-allowed;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: var(--kumo-bg-overlay);
  backdrop-filter: var(--kumo-blur);
}

.modal-content {
  display: grid;
  width: min(100%, 28rem);
  overflow: hidden;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-lg);
  backdrop-filter: var(--kumo-blur);
  animation: dialog-in 260ms ease both;
}

.modal-header,
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
}

.modal-header {
  border-bottom: 1px solid var(--kumo-hairline);
}

.modal-header h3 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.1rem;
  font-weight: 840;
}

.close-btn {
  display: inline-grid;
  place-items: center;
  width: 2.2rem;
  height: 2.2rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 50%;
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
  cursor: pointer;
}

.modal-body {
  padding: 1.2rem 1rem;
  color: var(--kumo-text-muted);
}

.modal-footer {
  justify-content: flex-end;
  border-top: 1px solid var(--kumo-hairline);
}

.btn-danger {
  border-color: transparent;
  background: var(--kumo-status-danger);
  color: var(--kumo-text-inverse);
}

@keyframes dialog-in {
  from {
    opacity: 0;
    transform: translateY(1rem) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 760px) {
  .answers-header,
  .answer-meta,
  .answer-actions,
  .editor-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .sort-options {
    width: 100%;
    overflow-x: auto;
  }

  .sort-btn {
    flex: 0 0 auto;
  }

  .answer-main {
    grid-template-columns: 1fr;
  }

  .vote-controls {
    grid-template-columns: auto auto;
    justify-content: center;
  }

  .submit-btn {
    width: 100%;
  }
}
</style>
