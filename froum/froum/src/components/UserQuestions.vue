<template>
  <div class="user-questions">
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>加载问题中...</p>
    </div>
    
    <div v-else-if="error" class="error-state">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
      <p>{{ error }}</p>
      <button class="retry-btn" @click="loadQuestions">重试</button>
    </div>
    
    <div v-else-if="!questions.length" class="empty-state">
      <font-awesome-icon :icon="['fas', 'question-circle']" class="empty-icon" />
      <p>暂无问题</p>
    </div>
    
    <div v-else class="questions-list">
      <div v-for="question in questions" :key="question.id" class="question-item">
        <div class="question-header">
          <router-link :to="`/question/${question.id}`" class="question-title">
            {{ question.title }}
          </router-link>
          <span class="question-time">{{ formatTime(question.createTime) }}</span>
        </div>
        
        <p class="question-content">{{ question.content }}</p>
        
        <div class="question-footer">
          <div class="question-tags">
            <span v-for="tag in question.tags" :key="tag" class="tag">
              <font-awesome-icon :icon="['fas', 'tag']" />
              {{ tag }}
            </span>
          </div>
          
          <div class="question-stats">
            <span class="stat-item">
              <font-awesome-icon :icon="['fas', 'eye']" />
              {{ question.views || 0 }} 浏览
            </span>
            <span class="stat-item">
              <font-awesome-icon :icon="['fas', 'comment']" />
              {{ question.answers?.length || 0 }} 回答
            </span>
            <span class="stat-item">
              <font-awesome-icon :icon="['fas', 'thumbs-up']" />
              {{ question.likes || 0 }} 点赞
            </span>
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
const questions = ref([]);
const isLoading = ref(true);
const error = ref(null);

// 监听userId变化，重新加载问题
watch(() => props.userId, (newId) => {
  if (newId) {
    loadQuestions();
  }
});

onMounted(() => {
  loadQuestions();
});

// 加载用户问题
const loadQuestions = async () => {
  try {
    isLoading.value = true;
    error.value = null;

    // 调用API获取用户问题
    const response = await store.dispatch('getUserQuestions', {
      userId: props.userId,
      page: 0,
      size: 20
    });

    if (response && Array.isArray(response)) {
      questions.value = response;
    } else {
      questions.value = [];
    }
  } catch (err) {
    console.error('Failed to load user questions:', err);
    error.value = '加载问题失败，请稍后再试';
    questions.value = [];
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
.user-questions {
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

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.question-item {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.25rem;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--border-color);
}

.question-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-light);
}

.question-header {
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

.question-time {
  font-size: 0.85rem;
  color: var(--text-light);
  white-space: nowrap;
}

.question-content {
  color: var(--text-light);
  margin-bottom: 1rem;
  line-height: 1.5;
  font-size: 0.95rem;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.question-tags {
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

.question-stats {
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

@media (max-width: 768px) {
  .question-header {
    flex-direction: column;
  }
  
  .question-time {
    margin-top: 0.5rem;
  }
  
  .question-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .question-stats {
    margin-top: 0.5rem;
    width: 100%;
    justify-content: space-between;
  }
}
</style> 