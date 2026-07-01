<template>
  <div class="question-detail-container">
    <div v-if="loading" class="state-panel kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>加载问题内容中...</span>
    </div>

    <div v-else-if="error" class="state-panel kumo-surface state-panel--error">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
      <span>{{ error }}</span>
      <button class="kumo-button" type="button" @click="loadQuestion">
        <font-awesome-icon :icon="['fas', 'redo']" />
        重试
      </button>
    </div>

    <template v-else-if="question">
      <ui-page-hero :title="question.title" :description="questionSummary">
        <template #eyebrow>
          <span class="kumo-eyebrow">
            <font-awesome-icon :icon="['fas', 'question-circle']" />
            Question
          </span>
          <span v-if="questionSolved" class="kumo-status kumo-status--success">
            <font-awesome-icon :icon="['fas', 'check-circle']" />
            已解决
          </span>
        </template>

        <template #actions>
          <button
            class="kumo-button"
            :class="{ 'kumo-button--brand': !isFollowing, 'is-following': isFollowing }"
            type="button"
            @click="isFollowing ? unfollowQuestion() : followQuestion()"
          >
            <font-awesome-icon :icon="[isFollowing ? 'fas' : 'far', 'star']" />
            {{ isFollowing ? '已关注' : '关注问题' }}
          </button>
          <button v-if="isAuthor" class="kumo-button" type="button" @click="editQuestion">
            <font-awesome-icon :icon="['fas', 'edit']" />
            编辑
          </button>
          <button v-if="isAuthor" class="kumo-button danger-action" type="button" @click="showDeleteModal = true">
            <font-awesome-icon :icon="['fas', 'trash-alt']" />
            删除
          </button>
        </template>

        <template #aside>
          <div class="hero-metrics">
            <div class="hero-metric">
              <strong>{{ viewCount }}</strong>
              <span>查看</span>
            </div>
            <div class="hero-metric">
              <strong>{{ answerCount }}</strong>
              <span>回答</span>
            </div>
            <div class="hero-metric">
              <strong>{{ followCount }}</strong>
              <span>关注</span>
            </div>
          </div>
        </template>
      </ui-page-hero>

      <section class="question-meta-panel kumo-surface">
        <div class="author-strip">
          <span class="author-avatar">
            <img v-if="authorAvatar" :src="authorAvatar" :alt="authorName">
            <span v-else>{{ authorInitials }}</span>
          </span>
          <span class="author-copy">
            <strong>{{ authorName }}</strong>
            <small>
              <font-awesome-icon :icon="['fas', 'clock']" />
              提问于 {{ formattedDate }}
            </small>
          </span>
        </div>

        <div class="question-tags">
          <span v-for="tag in questionTags" :key="tag" class="tag-chip">
            <font-awesome-icon :icon="['fas', 'tag']" />
            {{ tag }}
          </span>
        </div>
      </section>

      <main class="question-layout">
        <article class="question-panel kumo-surface">
          <aside class="vote-card">
            <button
              class="vote-button"
              :class="{ active: userVote === 'up' }"
              type="button"
              title="这个问题有用且清晰"
              @click="vote('up')"
            >
              <font-awesome-icon :icon="['fas', 'caret-up']" />
            </button>
            <strong :class="{ positive: voteCount > 0, negative: voteCount < 0 }">{{ voteCount }}</strong>
            <span>赞同</span>
          </aside>

          <div class="question-main">
            <div class="markdown-content" v-html="formattedContent"></div>
          </div>
        </article>

        <section class="answers-panel kumo-surface">
          <answer-list
            :question-id="questionId"
            :question-author-id="questionAuthor.id"
            :initial-answers="question.answers"
            :initial-best-answer-id="question.bestAnswerId"
            @answers-updated="handleAnswersUpdated"
          />
        </section>
      </main>

      <div v-if="showDeleteModal" class="dialog-backdrop" @click.self="showDeleteModal = false">
        <section class="delete-dialog kumo-surface" role="dialog" aria-modal="true" aria-labelledby="delete-question-title">
          <div class="dialog-heading">
            <span class="kumo-eyebrow">
              <font-awesome-icon :icon="['fas', 'trash-alt']" />
              Delete
            </span>
            <h2 id="delete-question-title" class="kumo-heading">确认删除</h2>
            <p class="kumo-muted">确定要删除这个问题吗？此操作无法撤销。</p>
          </div>

          <div class="dialog-actions">
            <button class="kumo-button" type="button" @click="showDeleteModal = false">取消</button>
            <button class="kumo-button delete-button" type="button" @click="deleteQuestion">删除</button>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { marked } from 'marked'
import hljs from 'highlight.js'
import AnswerList from '@/components/AnswerList.vue'
import UiPageHero from '@/components/ui/UiPageHero.vue'
import { questionService } from '../services/questionService'
import { formatDateTime, getTimeField } from '../utils/dateUtils'
import { avatarInitial, resolveAvatarFrom } from '../utils/avatar'

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
    AnswerList,
    UiPageHero
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
      return Boolean(store.state.user && question.value?.author?.id &&
        String(store.state.user.id) === String(question.value.author.id))
    })
    
    const formattedContent = computed(() => {
      if (!question.value || !question.value.content) return ''
      return marked.parse(question.value.content)
    })

    const questionSummary = computed(() => {
      const content = question.value?.content || ''
      const plainText = content
        .replace(/<[^>]*>/g, ' ')
        .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
        .replace(/[#*_`[\]()]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()

      return plainText.length > 120 ? `${plainText.substring(0, 120)}...` : plainText || '查看问题详情并参与回答。'
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
    
    const questionAuthor = computed(() => question.value?.author || question.value?.user || {})
    const authorName = computed(() => {
      const author = questionAuthor.value
      return author.nickname || author.username || author.name || '匿名用户'
    })
    const authorAvatar = computed(() => resolveAvatarFrom(questionAuthor.value))
    const authorInitials = computed(() => avatarInitial(authorName.value))
    const questionTags = computed(() => {
      const tags = question.value?.tags
      if (!tags) return []
      if (Array.isArray(tags)) {
        return tags.map(tag => typeof tag === 'string' ? tag : tag?.name).filter(Boolean)
      }
      return String(tags).split(',').map(tag => tag.trim()).filter(Boolean)
    })
    const viewCount = computed(() => question.value?.viewCount || question.value?.views || 0)
    const answerCount = computed(() => question.value?.answerCount || question.value?.commentCount || 0)
    const voteCount = computed(() => question.value?.voteCount || question.value?.likeCount || 0)
    const followCount = computed(() => question.value?.followCount || 0)
    const questionSolved = computed(() => Boolean(question.value?.isSolved || question.value?.solved))
    
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
        error.value = err.message || '加载问题失败，请稍后重试'
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
        store.dispatch('setMessage', {
          type: 'error',
          content: err.message || '删除问题失败，请稍后重试'
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
      questionSummary,
      formattedDate,
      questionAuthor,
      authorName,
      authorAvatar,
      authorInitials,
      questionTags,
      viewCount,
      answerCount,
      voteCount,
      followCount,
      questionSolved,
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
  display: grid;
  gap: 1.25rem;
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
  font-size: 2.1rem;
}

.state-panel--error,
.danger-action {
  color: var(--kumo-status-danger);
}

.hero-metrics {
  display: grid;
  gap: 0.7rem;
}

.hero-metric {
  display: grid;
  gap: 0.2rem;
  min-width: 9rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.hero-metric strong {
  color: var(--kumo-bg-brand-strong);
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 900;
  line-height: 1;
}

.hero-metric span {
  color: var(--kumo-text-muted);
  font-size: 0.82rem;
  font-weight: 760;
}

.is-following {
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.question-meta-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
}

.author-strip {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 0.75rem;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.8rem;
  height: 2.8rem;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid var(--kumo-hairline);
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 840;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-copy {
  display: grid;
  min-width: 0;
}

.author-copy strong {
  overflow: hidden;
  color: var(--kumo-text-default);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-copy small {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--kumo-text-subtle);
  font-weight: 690;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.32rem 0.62rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 760;
}

.question-layout {
  display: grid;
  gap: 1.25rem;
}

.question-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 1.25rem;
  padding: clamp(1.25rem, 3vw, 2.4rem);
}

.vote-card {
  position: sticky;
  top: 6rem;
  display: grid;
  place-items: center;
  gap: 0.35rem;
  align-self: start;
  min-width: 4.8rem;
  padding: 0.8rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
}

.vote-button {
  display: inline-grid;
  place-items: center;
  width: 2.6rem;
  height: 2.6rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 50%;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition),
    background-color var(--kumo-transition);
}

.vote-button:hover,
.vote-button.active {
  transform: translateY(-2px);
  border-color: var(--kumo-bg-brand);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.vote-card strong {
  color: var(--kumo-text-default);
  font-size: 1.55rem;
  font-weight: 900;
}

.vote-card strong.positive {
  color: var(--kumo-status-success);
}

.vote-card strong.negative {
  color: var(--kumo-status-danger);
}

.vote-card span {
  font-size: 0.78rem;
  font-weight: 760;
}

.question-main {
  min-width: 0;
}

.markdown-content {
  color: var(--kumo-text-default);
  font-size: 1.05rem;
  line-height: 1.78;
  overflow-wrap: break-word;
}

:deep(.markdown-content > *:first-child) {
  margin-top: 0;
}

:deep(.markdown-content > *:last-child) {
  margin-bottom: 0;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3),
:deep(.markdown-content h4),
:deep(.markdown-content h5),
:deep(.markdown-content h6) {
  margin: 1.6em 0 0.75em;
  color: var(--kumo-text-default);
  font-weight: 840;
  letter-spacing: 0;
  line-height: 1.22;
}

:deep(.markdown-content p),
:deep(.markdown-content blockquote),
:deep(.markdown-content ul),
:deep(.markdown-content ol),
:deep(.markdown-content table),
:deep(.markdown-content pre) {
  margin: 0 0 1.1rem;
}

:deep(.markdown-content blockquote) {
  padding: 0.7rem 1rem;
  border-left: 4px solid var(--kumo-bg-brand);
  border-radius: var(--kumo-radius-sm);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-text-muted);
}

:deep(.markdown-content code) {
  padding: 0.16rem 0.36rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-sm);
  background: var(--kumo-bg-recessed);
  color: var(--kumo-bg-brand-strong);
  font-family: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace;
  font-size: 0.88em;
}

:deep(.markdown-content pre) {
  overflow: auto;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-recessed);
}

:deep(.markdown-content pre code) {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
}

.answers-panel {
  padding: clamp(1rem, 2.2vw, 1.5rem);
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: var(--kumo-bg-overlay);
  backdrop-filter: var(--kumo-blur);
}

.delete-dialog {
  display: grid;
  gap: 1.25rem;
  width: min(100%, 28rem);
  padding: 1.3rem;
  animation: dialog-in 260ms ease both;
}

.dialog-heading {
  display: grid;
  gap: 0.7rem;
}

.dialog-heading h2,
.dialog-heading p {
  margin: 0;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.delete-button {
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
  .hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-metric {
    min-width: 0;
  }

  .hero-metric strong {
    font-size: 1.6rem;
  }

  .question-meta-panel,
  .dialog-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .question-tags {
    justify-content: flex-start;
  }

  .question-panel {
    grid-template-columns: 1fr;
  }

  .vote-card {
    position: static;
    grid-template-columns: auto auto auto;
    justify-content: center;
  }
}
</style>
