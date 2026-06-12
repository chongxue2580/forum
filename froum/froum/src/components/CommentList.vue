<template>
  <div class="comment-list">
    <h2 class="section-title">最新评论</h2>
    
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <div>加载评论中...</div>
    </div>
    
    <div v-else-if="error" class="error-container">
      <div class="error-icon">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
      </div>
      <div>{{ error }}</div>
    </div>
    
    <div v-else-if="comments.length === 0" class="empty-container">
      <div class="empty-icon">
        <font-awesome-icon :icon="['fas', 'comment-slash']" />
      </div>
      <div>暂无评论</div>
    </div>
    
    <div v-else class="comments-container">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <div class="user-info">
            <div class="avatar" v-if="comment.author && comment.author.avatar">
              <img :src="comment.author.avatar" :alt="comment.author.name" />
            </div>
            <div class="avatar-placeholder" v-else>
              {{ getAuthorInitials(comment.author ? comment.author.name : '未知用户') }}
            </div>
            <div class="user-name">{{ comment.author ? comment.author.name : '未知用户' }}</div>
          </div>
          <div class="comment-date">{{ formatDate(comment.createTime) }}</div>
        </div>
        
        <div class="comment-content" v-html="formatMarkdown(comment.content)"></div>
        
        <div class="comment-footer">
          <div class="comment-actions">
            <button class="action-button like-button" :class="{ active: comment.userLiked }">
              <font-awesome-icon :icon="['fas', 'thumbs-up']" />
              <span>{{ comment.likes || 0 }}</span>
            </button>
            
            <button class="action-button">
              <font-awesome-icon :icon="['fas', 'reply']" />
              <span>回复</span>
            </button>
          </div>
          
          <div class="source-link" v-if="comment.source">
            <router-link :to="comment.source.url">
              <font-awesome-icon :icon="['fas', 'link']" />
              <span>查看源{{ comment.source.type === 'article' ? '文章' : '问题' }}</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { marked } from 'marked'
import hljs from 'highlight.js'

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
  name: 'CommentList',
  props: {
    comments: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    const store = useStore()
    const loading = ref(false)
    const error = ref(null)
    
    // 格式化Markdown内容
    const formatMarkdown = (content) => {
      return marked.parse(content || '')
    }
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return ''
      
      try {
        const date = new Date(dateString)
        return new Intl.DateTimeFormat('zh-CN', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        }).format(date)
      } catch (err) {
        console.error('Error formatting date:', err)
        return dateString
      }
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
    
    return {
      loading,
      error,
      formatMarkdown,
      formatDate,
      getAuthorInitials
    }
  }
})
</script>

<style scoped>
.comment-list {
  width: 100%;
}

.section-title {
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
  color: var(--text-dark);
  font-weight: 600;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 2rem;
  text-align: center;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.loading-spinner,
.error-icon,
.empty-icon {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
}

.error-icon {
  color: var(--error-color);
}

.empty-icon {
  color: var(--text-light);
}

.comments-container {
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.comment-item {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar,
.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  background-color: var(--primary-light);
  color: var(--primary-color);
  font-weight: 600;
  font-size: 0.875rem;
}

.user-name {
  font-weight: 600;
  color: var(--text-dark);
}

.comment-date {
  font-size: 0.875rem;
  color: var(--text-light);
}

.comment-content {
  margin-bottom: 1rem;
  line-height: 1.6;
  color: var(--text-dark);
}

.comment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-actions {
  display: flex;
  gap: 1rem;
}

.action-button {
  background: none;
  border: none;
  color: var(--text-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  padding: 0.5rem;
  border-radius: var(--radius);
  transition: var(--transition);
}

.action-button:hover {
  background-color: var(--bg-gray);
  color: var(--primary-color);
}

.action-button.active {
  color: var(--primary-color);
}

.source-link a {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--primary-color);
  text-decoration: none;
  transition: var(--transition);
}

.source-link a:hover {
  text-decoration: underline;
}
</style> 