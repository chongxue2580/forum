<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { questionService } from '../services/questionService'
import { commentService } from '../services/commentService'
import { followService } from '../services/followService'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import CommentList from './CommentList.vue'
import CommentForm from './CommentForm.vue'

// 配置 marked 使用 highlight.js 进行语法高亮
marked.setOptions({
  highlight: function(code, language) {
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(code, { language }).value
      } catch (err) {
        console.error(err)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true
})

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

const store = useStore()
const router = useRouter()
const route = useRoute()

const questionId = computed(() => Number(props.id))
const question = ref(null)
const loading = ref(true)
const comments = ref([])
const commentsLoading = ref(false)
const isFollowing = ref(false)
const followLoading = ref(false)
const commentContent = ref('')
const submittingComment = ref(false)
const error = ref(null)

// 检查当前用户是否已登录
const isLoggedIn = computed(() => store.state.isAuthenticated && store.state.user)
const currentUser = computed(() => store.state.user)

// 转换问题内容为HTML
const formattedContent = computed(() => {
  if (!question.value || !question.value.content) return ''
  return marked(question.value.content)
})

// 获取问题详情
const fetchQuestion = async () => {
  loading.value = true
  error.value = null
  
  try {
    const response = await questionService.getQuestionById(questionId.value)
    if (response.success) {
      question.value = response.data
      
      // 如果用户已登录，检查是否关注了该问题
      if (isLoggedIn.value) {
        checkFollowingStatus()
      }
      
      // 加载问题的评论
      fetchComments()
    } else {
      error.value = response.message || '加载问题失败'
    }
  } catch (err) {
    console.error('获取问题详情失败：', err)
    error.value = '获取问题详情失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 获取问题的评论
const fetchComments = async () => {
  commentsLoading.value = true
  
  try {
    const response = await commentService.getCommentsByTarget('QUESTION', questionId.value)
    if (response.success) {
      comments.value = response.data
    }
  } catch (err) {
    console.error('获取评论失败：', err)
  } finally {
    commentsLoading.value = false
  }
}

// 检查用户是否关注了该问题
const checkFollowingStatus = async () => {
  if (!isLoggedIn.value) return
  
  try {
    const result = await followService.isFollowing(currentUser.value.id, questionId.value)
    isFollowing.value = result.followed || false
  } catch (err) {
    console.error('检查关注状态失败：', err)
  }
}

// 关注/取消关注问题
const toggleFollow = async () => {
  if (!isLoggedIn.value) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  followLoading.value = true

  try {
    // 使用新的关注API
    const result = await store.dispatch('toggleFollowTarget', {
      targetType: 'QUESTION',
      targetId: questionId.value
    })

    if (result.success) {
      isFollowing.value = result.isFollowed

      // 更新问题的关注数
      if (question.value) {
        question.value.followCount = result.isFollowed
          ? (question.value.followCount || 0) + 1
          : Math.max(0, (question.value.followCount || 0) - 1)
      }
    } else {
      console.error('关注操作失败:', result.message)
    }
  } catch (err) {
    console.error('操作失败：', err)
  } finally {
    followLoading.value = false
  }
}

// 提交评论
const submitComment = async (content) => {
  if (!isLoggedIn.value) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  
  if (!content.trim()) {
    return
  }
  
  submittingComment.value = true
  
  try {
    const response = await commentService.addComment(
      currentUser.value.id,
      content,
      'QUESTION',
      questionId.value,
      null
    )
    
    if (response.success) {
      // 重新加载评论
      fetchComments()
      
      // 更新问题的评论数
      if (question.value) {
        question.value.commentCount = (question.value.commentCount || 0) + 1
      }
      
      // 清空评论框
      commentContent.value = ''
    }
  } catch (err) {
    console.error('提交评论失败：', err)
  } finally {
    submittingComment.value = false
  }
}

// 设置为最佳回答
const setBestAnswer = async (commentId) => {
  if (!isLoggedIn.value || !question.value || question.value.authorId !== currentUser.value.id) {
    return
  }
  
  try {
    await questionService.setBestAnswer(questionId.value, commentId, currentUser.value.id)
    
    // 更新问题状态
    question.value.bestAnswerId = commentId
    question.value.isSolved = true
    
    // 重新加载评论
    fetchComments()
  } catch (err) {
    console.error('设置最佳回答失败：', err)
  }
}

// 删除评论
const deleteComment = async (commentId) => {
  if (!isLoggedIn.value) return
  
  try {
    await commentService.deleteComment(commentId, currentUser.value.id)
    
    // 更新问题的评论数
    if (question.value) {
      question.value.commentCount = Math.max(0, (question.value.commentCount || 0) - 1)
    }
    
    // 重新加载评论
    fetchComments()
  } catch (err) {
    console.error('删除评论失败：', err)
  }
}

// 监听问题ID变化
watch(() => props.id, () => {
  fetchQuestion()
})

onMounted(() => {
  fetchQuestion()
})
</script>

<template>
  <div class="question-detail">
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>
    
    <div v-else-if="error" class="error-container">
      <font-awesome-icon :icon="['fas', 'exclamation-triangle']" class="error-icon" />
      <p>{{ error }}</p>
      <button @click="fetchQuestion" class="retry-btn">重新加载</button>
    </div>
    
    <div v-else-if="question" class="question-container">
      <div class="question-header">
        <h1 class="question-title">{{ question.title }}</h1>
        
        <div class="question-meta">
          <div class="question-stats">
            <div class="stat-item">
              <font-awesome-icon :icon="['fas', 'eye']" />
              <span>{{ question.viewCount || 0 }} 浏览</span>
            </div>
            <div class="stat-item">
              <font-awesome-icon :icon="['fas', 'comment']" />
              <span>{{ question.commentCount || 0 }} 回答</span>
            </div>
            <div class="stat-item">
              <font-awesome-icon :icon="['fas', 'user-plus']" />
              <span>{{ question.followCount || 0 }} 关注</span>
            </div>
          </div>
          
          <div class="question-tags">
            <span v-for="tag in question.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
          </div>
        </div>
        
        <div class="question-author">
          <div class="author-info">
            <img :src="question.author?.avatarUrl || '/assets/default-avatar.svg'" class="author-avatar" alt="作者头像">
            <div>
              <div class="author-name">{{ question.author?.nickname || question.author?.username }}</div>
              <div class="post-time">发布于 {{ question.createdAt }}</div>
            </div>
          </div>
          
          <div class="question-actions">
            <button 
              @click="toggleFollow" 
              :disabled="followLoading" 
              :class="['follow-btn', { 'following': isFollowing }]"
            >
              <font-awesome-icon :icon="['fas', isFollowing ? 'check' : 'plus']" />
              {{ isFollowing ? '已关注' : '关注问题' }}
            </button>
            
            <router-link 
              v-if="isLoggedIn && currentUser.id === question.authorId" 
              :to="{ name: 'EditQuestion', params: { id: questionId } }"
              class="edit-btn"
            >
              <font-awesome-icon :icon="['fas', 'edit']" />
              编辑
            </router-link>
          </div>
        </div>
      </div>
      
      <div class="question-content">
        <div class="markdown-body" v-html="formattedContent"></div>
      </div>
      
      <div class="question-answers">
        <div class="answers-header">
          <h3>
            {{ question.commentCount || 0 }} 个回答
            <span v-if="question.isSolved" class="solved-badge">
              <font-awesome-icon :icon="['fas', 'check-circle']" />
              已解决
            </span>
          </h3>
        </div>
        
        <CommentList 
          v-if="comments.length > 0"
          :comments="comments"
          :loading="commentsLoading"
          :currentUserId="isLoggedIn ? currentUser.id : null"
          :canMarkBestAnswer="isLoggedIn && currentUser.id === question.authorId"
          :bestAnswerId="question.bestAnswerId"
          @delete="deleteComment"
          @setBestAnswer="setBestAnswer"
        />
        
        <div v-else-if="!commentsLoading" class="no-answers">
          <font-awesome-icon :icon="['fas', 'comment-dots']" class="no-answers-icon" />
          <p>还没有回答，来贡献第一个回答吧！</p>
        </div>
        
        <div class="answer-form">
          <h3>发表回答</h3>
          <CommentForm 
            v-if="isLoggedIn"
            :submitting="submittingComment"
            @submit="submitComment"
            placeholder="编写你的回答..."
          />
          <div v-else class="login-required">
            <p>请先登录后再回答问题</p>
            <router-link :to="{ name: 'Login', query: { redirect: route.fullPath } }" class="login-btn">
              登录
            </router-link>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else class="empty-state">
      <font-awesome-icon :icon="['fas', 'question-circle']" class="empty-icon" />
      <p>问题不存在或已被删除</p>
      <router-link :to="{ name: 'Questions' }" class="back-btn">
        返回问题列表
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.question-detail {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 0;
}

.loading-container, .error-container, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  text-align: center;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border-left-color: #4b70e2;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-icon, .empty-icon {
  font-size: 48px;
  color: #dc3545;
  margin-bottom: 16px;
}

.empty-icon {
  color: #6c757d;
}

.error-container p, .empty-state p {
  margin-bottom: 16px;
  color: #555;
}

.retry-btn, .back-btn {
  padding: 8px 16px;
  background-color: #4b70e2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
  text-decoration: none;
  display: inline-block;
}

.retry-btn:hover, .back-btn:hover {
  background-color: #3a5fc9;
}

.question-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.question-header {
  padding: 24px;
  border-bottom: 1px solid #eee;
}

.question-title {
  margin: 0 0 16px 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
  line-height: 1.3;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.question-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  color: #666;
  font-size: 14px;
}

.stat-item svg {
  margin-right: 6px;
  font-size: 16px;
}

.question-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  background-color: #e1ecf4;
  color: #39739d;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 13px;
}

.question-author {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author-info {
  display: flex;
  align-items: center;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
}

.author-name {
  font-weight: 500;
  color: #333;
}

.post-time {
  font-size: 13px;
  color: #777;
  margin-top: 2px;
}

.question-actions {
  display: flex;
  gap: 12px;
}

.follow-btn, .edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}

.follow-btn {
  background-color: #4b70e2;
  color: white;
  border: none;
}

.follow-btn:hover {
  background-color: #3a5fc9;
}

.follow-btn.following {
  background-color: #e9ecef;
  color: #495057;
}

.edit-btn {
  background-color: transparent;
  border: 1px solid #d9d9d9;
  color: #666;
}

.edit-btn:hover {
  background-color: #f5f5f5;
}

.question-content {
  padding: 24px;
  line-height: 1.6;
}

.markdown-body {
  font-size: 16px;
  color: #333;
}

.question-answers {
  padding: 24px;
  border-top: 1px solid #eee;
}

.answers-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.answers-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
}

.solved-badge {
  display: inline-flex;
  align-items: center;
  margin-left: 10px;
  padding: 4px 8px;
  background-color: #28a745;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: normal;
}

.solved-badge svg {
  margin-right: 4px;
}

.no-answers {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

.no-answers-icon {
  font-size: 48px;
  color: #ccc;
  margin-bottom: 16px;
}

.answer-form {
  margin-top: 40px;
}

.answer-form h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.login-required {
  text-align: center;
  padding: 24px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.login-required p {
  margin-bottom: 12px;
  color: #666;
}

.login-btn {
  display: inline-block;
  padding: 8px 20px;
  background-color: #4b70e2;
  color: white;
  border-radius: 4px;
  text-decoration: none;
  transition: background-color 0.2s;
}

.login-btn:hover {
  background-color: #3a5fc9;
}

@media (max-width: 768px) {
  .question-meta {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-stats {
    margin-bottom: 12px;
  }
  
  .question-author {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .author-info {
    margin-bottom: 16px;
  }
  
  .question-actions {
    width: 100%;
  }
  
  .follow-btn, .edit-btn {
    flex: 1;
    justify-content: center;
  }
}
</style> 
