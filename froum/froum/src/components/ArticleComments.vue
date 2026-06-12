<template>
  <div class="article-comments">
    <h2 class="comments-title">
      <font-awesome-icon :icon="['fas', 'comment']" />
      评论区 ({{ parsedComments.length }})
    </h2>
    
    <!-- 添加评论 - 已登录用户 -->
    <div class="comment-form" v-if="isLoggedIn">
      <div class="user-avatar">
        <img v-if="userAvatar" :src="getAvatarPath(userAvatar)" :alt="userName" />
        <span v-else>{{ userInitial }}</span>
      </div>
      <div class="form-input">
        <textarea 
          v-model="newComment" 
          placeholder="写下你的评论..."
          class="comment-textarea"
        ></textarea>
        <button 
          class="submit-btn" 
          @click="submitComment"
          :disabled="!newComment.trim()"
        >
          提交评论
        </button>
      </div>
    </div>
    
    <!-- 未登录提示 -->
    <div class="login-prompt" v-else>
      <div class="login-message">
        <font-awesome-icon :icon="['fas', 'lock']" class="lock-icon" />
        <p>您需要登录后才能发表评论</p>
      </div>
      <button class="login-btn" @click="goToLogin">
        <font-awesome-icon :icon="['fas', 'sign-in-alt']" />
        登录
      </button>
    </div>
    
    <!-- 评论列表 -->
    <div class="comments-list">
      <div 
        v-for="comment in parsedComments" 
        :key="comment.id" 
        class="comment-item"
      >
        <div class="comment-author">
          <div class="author-avatar">
            <img v-if="comment.author.avatar" :src="getAvatarPath(comment.author.avatar)" :alt="comment.author.name" />
            <span v-else>{{ getInitials(comment.author.name) }}</span>
          </div>
          <div class="comment-info">
            <div class="comment-meta">
              <span class="comment-author-name">{{ comment.author.name }}</span>
              <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <button 
                class="comment-like" 
                @click="likeComment(comment.id)"
              >
                <font-awesome-icon :icon="['fas', 'thumbs-up']" />
                <span>{{ comment.likes }}</span>
              </button>
              <button class="comment-reply">
                <font-awesome-icon :icon="['fas', 'comment']" />
                <span>回复</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import { parseComments, formatDate, getInitials, getAvatarPath } from '../utils/commentHelper';

const store = useStore();
const router = useRouter();

const props = defineProps({
  comments: {
    type: Array,
    required: true,
    default: () => []
  },
  articleId: {
    type: [Number, String],
    required: true
  }
});

// 用户登录状态
const isLoggedIn = computed(() => store.state.isAuthenticated && store.state.user);
const userInitial = computed(() => {
  if (store.state.user && store.state.user.username) {
    return store.state.user.username.substring(0, 1).toUpperCase();
  }
  return '用';
});
const userName = computed(() => store.state.user?.username || '');
const userAvatar = computed(() => store.state.user?.avatar || '');

// 解析评论数据
const parsedComments = computed(() => {
  return parseComments(props.comments);
});

const newComment = ref('');

// Methods
const submitComment = () => {
  if (!isLoggedIn.value) {
    goToLogin();
    return;
  }
  
  if (!newComment.value.trim()) return;

  // Emit event to parent component
  emit('add-comment', { 
    articleId: props.articleId, 
    content: newComment.value.trim()
  });
  
  // Clear input
  newComment.value = '';
};

const goToLogin = () => {
  router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } });
};

const likeComment = (commentId) => {
  if (!isLoggedIn.value) {
    goToLogin();
    return;
  }
  
  emit('like-comment', { 
    articleId: props.articleId, 
    commentId 
  });
};

const emit = defineEmits(['add-comment', 'like-comment']);
</script>

<style scoped>
.article-comments {
  margin-top: 2rem;
}

.comments-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: var(--text-color);
}

.comment-form {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-input {
  flex-grow: 1;
}

.comment-textarea {
  width: 100%;
  min-height: 100px;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  margin-bottom: 0.75rem;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s;
}

.comment-textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2);
}

.submit-btn {
  float: right;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  padding: 0.6rem 1.25rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:disabled {
  background-color: var(--text-lighter);
  cursor: not-allowed;
}

.submit-btn:not(:disabled):hover {
  background-color: var(--primary-dark);
}

/* 登录提示样式 */
.login-prompt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  background-color: var(--bg-light);
  border: 1px dashed var(--border-color);
  border-radius: var(--radius);
  margin-bottom: 2rem;
}

.login-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-light);
}

.lock-icon {
  font-size: 1.25rem;
  color: var(--text-light);
}

.login-btn {
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--radius);
  padding: 0.5rem 1.25rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.login-btn:hover {
  background-color: var(--primary-dark);
}

.comments-list {
  margin-top: 2rem;
}

.comment-item {
  padding: 1.5rem 0;
  border-top: 1px solid var(--border-color);
}

.comment-author {
  display: flex;
  gap: 1rem;
}

.comment-info {
  flex-grow: 1;
}

.comment-meta {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.comment-author-name {
  font-weight: 600;
  color: var(--text-color);
  margin-right: 0.75rem;
}

.comment-time {
  color: var(--text-lighter);
  font-size: 0.9rem;
}

.comment-content {
  color: var(--text-color);
  line-height: 1.6;
  margin-bottom: 0.75rem;
}

.comment-actions {
  display: flex;
  gap: 1rem;
}

.comment-like,
.comment-reply {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: none;
  color: var(--text-lighter);
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: all 0.3s;
}

.comment-like:hover,
.comment-reply:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1.1rem;
  overflow: hidden;
}
</style> 
