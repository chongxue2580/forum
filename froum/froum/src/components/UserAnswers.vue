<template>
  <div class="user-answers">
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>加载回答中...</p>
    </div>
    
    <div v-else-if="error" class="error-state">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
      <p>{{ error }}</p>
      <button class="retry-btn" @click="loadAnswers">重试</button>
    </div>
    
    <div v-else-if="!answers.length" class="empty-state">
      <font-awesome-icon :icon="['fas', 'comment-dots']" class="empty-icon" />
      <p>暂无回答</p>
    </div>
    
    <div v-else class="answers-list">
      <div v-for="answer in answers" :key="answer.id" class="answer-item">
        <div class="answer-header">
          <router-link :to="`/question/${answer.questionId}`" class="question-title">
            {{ answer.questionTitle }}
          </router-link>
          <span class="answer-time">{{ formatTime(answer.createTime) }}</span>
        </div>
        
        <div class="answer-content">{{ answer.content }}</div>
        
        <div class="answer-footer">
          <div class="answer-tags" v-if="answer.tags && answer.tags.length">
            <span v-for="tag in answer.tags" :key="tag" class="tag">
              <font-awesome-icon :icon="['fas', 'tag']" />
              {{ tag }}
            </span>
          </div>
          <div class="answer-stats">
            <span class="stat-item">
              <font-awesome-icon :icon="['fas', 'thumbs-up']" />
              {{ answer.likes || 0 }} 点赞
            </span>
            <span class="stat-item">
              <font-awesome-icon :icon="['fas', 'comment']" />
              {{ answer.comments?.length || 0 }} 评论
            </span>
          </div>
          
          <div class="answer-actions">
            <router-link :to="`/question/${answer.questionId}#answer-${answer.id}`" class="view-context">
              查看上下文
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useStore } from 'vuex';

const props = defineProps({
  userId: {
    type: [String, Number],
    required: true
  }
});

const store = useStore();
const answers = ref([]);
const isLoading = ref(true);
const error = ref(null);

// 监听userId变化，重新加载回答
watch(() => props.userId, (newId) => {
  if (newId) {
    loadAnswers();
  }
});

onMounted(() => {
  loadAnswers();
});

// 加载用户回答
const loadAnswers = async () => {
  try {
    isLoading.value = true;
    error.value = null;

    // 调用API获取用户回答
    const response = await store.dispatch('getUserAnswers', {
      userId: props.userId,
      page: 0,
      size: 20
    });

    if (response && Array.isArray(response)) {
      answers.value = response;
    } else {
      answers.value = [];
    }
  } catch (err) {
    console.error('Failed to load user answers:', err);
    error.value = '加载回答失败，请稍后再试';
    answers.value = [];
  } finally {
    isLoading.value = false;
  }
};

// 格式化时间
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
</script>

<style scoped>
.user-answers {
  width: 100%;
}

.loading-state, .error-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top: 4px solid var(--primary-color);
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-icon, .empty-icon {
  font-size: 3rem;
  color: var(--text-light);
  margin-bottom: 1rem;
}

.retry-btn {
  margin-top: 1rem;
  padding: 0.5rem 1.5rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-btn:hover {
  background-color: var(--primary-dark);
}

.answers-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.answer-item {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.25rem;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--border-color);
}

.answer-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-light);
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.question-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-color);
  text-decoration: none;
  flex: 1;
  margin-right: 1rem;
  line-height: 1.4;
}

.question-title:hover {
  color: var(--primary-color);
}

.answer-time {
  font-size: 0.85rem;
  color: var(--text-light);
  white-space: nowrap;
}

.answer-content {
  color: var(--text-light);
  margin-bottom: 1rem;
  line-height: 1.5;
  font-size: 0.95rem;
  white-space: pre-line;
}

.answer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.answer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 0.25rem 0.5rem;
  border-radius: 50px;
  font-size: 0.75rem;
  transition: var(--transition);
}

.tag:hover {
  background-color: var(--primary-color);
  color: white;
}

.answer-stats {
  display: flex;
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--text-lighter);
  font-size: 0.85rem;
}

.answer-actions {
  display: flex;
  align-items: center;
}

.view-context {
  font-size: 0.85rem;
  color: var(--primary-color);
  text-decoration: none;
  transition: color 0.2s;
}

.view-context:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .answer-header {
    flex-direction: column;
  }
  
  .answer-time {
    margin-top: 0.5rem;
  }
  
  .answer-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
}
</style> 