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
  display: grid;
  gap: 0.85rem;
  margin-top: 0.4rem;
  padding-top: 1rem;
  border-top: 1px solid var(--kumo-hairline);
}

.comments-title {
  margin: 0;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  font-weight: 800;
}

.comments-list {
  display: grid;
  gap: 0.55rem;
}

.comment-item {
  padding: 0.75rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
}

.comment-content p {
  margin: 0 0 0.4rem;
  color: var(--kumo-text-default);
  font-size: 0.92rem;
  line-height: 1.55;
}

.comment-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.55rem;
  color: var(--kumo-text-subtle);
  font-size: 0.78rem;
  font-weight: 680;
}

.comment-author {
  color: var(--kumo-text-muted);
  font-weight: 800;
}

.add-comment {
  display: flex;
  gap: 0.65rem;
}

.comment-input {
  flex: 1;
  min-width: 0;
  min-height: 2.5rem;
  padding: 0.55rem 0.85rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font-size: 0.92rem;
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.comment-input:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.comment-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.5rem;
  padding: 0.55rem 0.85rem;
  border: 1px solid transparent;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-brand-strong));
  color: var(--kumo-text-inverse);
  font-size: 0.9rem;
  font-weight: 780;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    opacity var(--kumo-transition);
}

.comment-submit:hover:not(:disabled) {
  transform: translateY(-2px);
}

.comment-submit:disabled {
  background: var(--kumo-bg-recessed);
  color: var(--kumo-text-subtle);
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .add-comment {
    flex-direction: column;
  }
}
</style>
