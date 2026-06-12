<template>
  <div class="question-item" :class="{ 'is-solved': question.solved }">
    <div class="question-stats">
      <div class="stat-box votes">
        <span class="stat-value">{{ formatNumber(question.voteCount || 0) }}</span>
        <span class="stat-label">投票</span>
      </div>
      <div class="stat-box answers" :class="{ 'has-best': question.solved }">
        <span class="stat-value">{{ formatNumber(question.answers || question.answerCount || 0) }}</span>
        <span class="stat-label">回答</span>
      </div>
      <div class="stat-box views">
        <span class="stat-value">{{ formatNumber(question.views || question.viewCount || 0) }}</span>
        <span class="stat-label">浏览</span>
      </div>
    </div>
    
    <div class="question-content">
      <router-link :to="`/question/${question.id}`" class="question-title">
        <span v-if="question.solved" class="solved-badge">
          <font-awesome-icon :icon="['fas', 'check-circle']" />
          <span>已解决</span>
        </span>
        {{ question.title }}
      </router-link>
      
      <div class="question-excerpt">
        {{ truncateText(question.content || question.summary, 150) }}
      </div>
      
      <div class="question-meta">
        <div class="tags">
          <router-link 
            v-for="tag in question.tags" 
            :key="tag" 
            :to="`/tag/${tag}`"
            class="tag"
          >
            <font-awesome-icon :icon="['fas', 'tag']" class="tag-icon" />
            <span>{{ tag }}</span>
          </router-link>
        </div>
        
        <div class="question-info">
          <div class="author">
            <div class="author-avatar" v-if="question.author?.avatarUrl || question.author?.avatar">
              <img :src="question.author.avatarUrl || question.author.avatar" :alt="getAuthorName(question.author)" />
            </div>
            <div class="author-avatar" v-else>
              <span>{{ getAuthorInitials(getAuthorName(question.author)) }}</span>
            </div>
            <router-link :to="`/user/${question.author?.id || ''}`" class="author-name">
              {{ getAuthorName(question.author) || '匿名用户' }}
            </router-link>
          </div>
          
          <span class="time">
            <font-awesome-icon :icon="['fas', 'clock']" />
            <span>{{ formatTime(question.createdAt || question.createTime) }}</span>
          </span>
          
          <div class="question-actions" v-if="isCurrentUserAuthor">
            <router-link :to="`/question/${question.id}/edit`" class="action-btn edit-btn" title="编辑问题">
              <font-awesome-icon :icon="['fas', 'edit']" />
            </router-link>
            <button class="action-btn delete-btn" @click="confirmDelete" title="删除问题">
              <font-awesome-icon :icon="['fas', 'trash-alt']" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, computed } from 'vue'
import { useStore } from 'vuex'

export default defineComponent({
  name: 'QuestionItem',
  props: {
    question: {
      type: Object,
      required: true
    }
  },
  setup(props, { emit }) {
    const store = useStore()
    
    // 检查当前用户是否是问题作者
    const isCurrentUserAuthor = computed(() => {
      const currentUser = store.state.user
      return currentUser && props.question.author && currentUser.id === props.question.author.id
    })
    
    // 格式化数字（如果大于1000，显示为1k）
    const formatNumber = (num) => {
      if (!num) return 0;
      return num > 999 ? `${(num / 1000).toFixed(1)}k` : num
    }
    
    // 截断文本
    const truncateText = (text, maxLength) => {
      if (!text) return ''
      if (text.length <= maxLength) return text
      return text.substring(0, maxLength) + '...'
    }
    
    // 获取作者名称
    const getAuthorName = (author) => {
      if (!author) return '匿名用户';
      // 优先使用 nickname，然后是 username，最后是 name
      return author.nickname || author.username || author.name || '匿名用户';
    };

    // 获取作者首字母
    const getAuthorInitials = (name) => {
      if (!name) return '匿';
      return name.charAt(0).toUpperCase();
    }
    
    // 格式化时间为相对时间（如"3小时前"）
    const formatTime = (time) => {
      if (!time) return '未知时间';
      
      const date = new Date(time);
      
      // 如果日期无效，返回原始值
      if (isNaN(date.getTime())) {
        return time;
      }
      
      // 当前日期
      const now = new Date();
      
      // 计算时间差（毫秒）
      const diff = now - date;
      
      // 计算天数差
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      
      // 今天发布的
      if (days === 0) {
        const hours = Math.floor(diff / (1000 * 60 * 60));
        
        if (hours === 0) {
          const minutes = Math.floor(diff / (1000 * 60));
          if (minutes === 0) {
            return '刚刚';
          }
          return `${minutes}分钟前`;
        }
        
        return `${hours}小时前`;
      }
      
      // 昨天发布的
      if (days === 1) {
        return '昨天';
      }
      
      // 一周内发布的
      if (days < 7) {
        return `${days}天前`;
      }
      
      // 超过一周，显示具体日期
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    };
    
    // 确认删除问题
    const confirmDelete = () => {
      if (confirm('确定要删除这个问题吗？此操作不可撤销。')) {
        emit('delete', props.question)
      }
    }
    
    return {
      isCurrentUserAuthor,
      formatNumber,
      truncateText,
      getAuthorName,
      getAuthorInitials,
      formatTime,
      confirmDelete
    }
  }
})
</script>

<style scoped>
.question-item {
  display: flex;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
  overflow: hidden;
  position: relative;
  border: 1px solid var(--border-color);
  margin-bottom: 1.25rem;
}

.question-item:hover {
  box-shadow: var(--shadow);
  transform: translateY(-2px);
  border-color: var(--primary-light);
}

.question-item.is-solved {
  border-left: 4px solid var(--success-color);
}

.question-stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1.25rem 0.75rem;
  background-color: var(--bg-gray);
  min-width: 80px;
  border-right: 1px solid var(--border-color);
}

.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 0;
  width: 100%;
  text-align: center;
}

.stat-value {
  font-weight: 600;
  font-size: 1.125rem;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--text-light);
}

.votes .stat-value {
  color: var(--text-color);
}

.answers {
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.answers.has-best {
  background-color: rgba(var(--success-rgb), 0.1);
}

.answers.has-best .stat-value {
  color: var(--success-color);
}

.views .stat-value {
  color: var(--text-light);
}

.question-content {
  flex: 1;
  padding: 1.25rem;
  min-width: 0; /* 防止flex子项溢出 */
  display: flex;
  flex-direction: column;
}

.question-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  text-decoration: none;
  margin-bottom: 0.75rem;
  line-height: 1.3;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.question-title:hover {
  color: var(--primary-color);
}

.solved-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background-color: rgba(var(--success-rgb), 0.1);
  color: var(--success-color);
  border-radius: 50px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.question-excerpt {
  color: var(--text-light);
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 1.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: auto;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  background-color: var(--primary-light);
  color: var(--primary-color);
  border-radius: 50px;
  font-size: 0.75rem;
  text-decoration: none;
  transition: var(--transition);
}

.tag-icon {
  font-size: 0.7rem;
}

.tag:hover {
  background-color: var(--primary-color);
  color: white;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.85rem;
  color: var(--text-light);
}

.author {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--primary-light);
  color: var(--primary-color);
  font-weight: 600;
  font-size: 0.8rem;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  color: var(--text-color);
  font-weight: 500;
  text-decoration: none;
  transition: var(--transition);
}

.author-name:hover {
  color: var(--primary-color);
}

.time {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-lighter);
  font-size: 0.8rem;
}

.question-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: none;
  border: none;
  color: var(--text-lighter);
  cursor: pointer;
  transition: var(--transition);
}

.action-btn:hover {
  background-color: var(--bg-gray);
}

.edit-btn:hover {
  color: var(--primary-color);
}

.delete-btn:hover {
  color: var(--error-color);
}

@media (max-width: 768px) {
  .question-item {
    flex-direction: column;
  }
  
  .question-stats {
    flex-direction: row;
    padding: 0.5rem;
    min-width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
    justify-content: space-around;
  }
  
  .stat-box {
    padding: 0.25rem 0.5rem;
  }
  
  .question-meta {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-info {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 576px) {
  .question-content {
    padding: 1rem;
  }
  
  .question-title {
    font-size: 1.1rem;
  }
  
  .question-excerpt {
    font-size: 0.9rem;
    -webkit-line-clamp: 2;
  }
  
  .question-info {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
  
  .time {
    width: 100%;
  }
}
</style> 