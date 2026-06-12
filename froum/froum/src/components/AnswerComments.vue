<template>
  <div class="comments-section">
    <h4 class="comments-title" v-if="comments && comments.length > 0">
      评论 ({{ comments.length }})
    </h4>
    
    <div v-if="comments && comments.length > 0" class="comments-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-content">
          <p>{{ comment.content }}</p>
          <div class="comment-meta">
            <span class="comment-author">{{ comment.author.name }}</span>
            <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 添加评论 -->
    <div class="add-comment">
      <input 
        v-model="commentInput" 
        type="text" 
        placeholder="添加评论..." 
        class="comment-input"
        @keyup.enter="submitComment"
      >
      <button 
        class="comment-submit" 
        @click="submitComment"
        :disabled="!commentInput || !commentInput.trim()"
      >
        添加评论
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { formatDateTime } from '../utils/dateUtils';

const props = defineProps({
  comments: {
    type: Array,
    required: true,
    default: () => []
  },
  answerId: {
    type: [Number, String],
    required: true
  }
});

const emit = defineEmits(['add-comment']);
const commentInput = ref('');

// Methods
const submitComment = () => {
  if (!commentInput.value.trim()) return;
  
  emit('add-comment', {
    answerId: props.answerId,
    content: commentInput.value.trim()
  });
  
  // Clear input
  commentInput.value = '';
};

const formatDate = (dateString) => {
  return formatDateTime(dateString, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};
</script>

<style scoped>
.comments-section {
  margin-top: 20px;
  border-top: 1px solid #eee;
  padding-top: 15px;
}

.comments-title {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-bottom: 10px;
}

.comments-list {
  margin-bottom: 15px;
}

.comment-item {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content p {
  margin: 0 0 5px;
  font-size: 14px;
  color: #333;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #999;
}

.comment-author {
  font-weight: 500;
  color: #666;
}

.add-comment {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.comment-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #eee;
  border-radius: 4px;
  font-size: 14px;
}

.comment-input:focus {
  border-color: #1890ff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.comment-submit {
  padding: 0 12px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.comment-submit:hover:not(:disabled) {
  background: #40a9ff;
}

.comment-submit:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}
</style> 
