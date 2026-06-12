<template>
  <button 
    @click="handleLike" 
    :disabled="loading" 
    :class="['like-btn', { 'liked': isLiked, 'loading': loading }]"
  >
    <font-awesome-icon 
      :icon="['fas', isLiked ? 'heart' : 'heart']" 
      :class="{ 'liked': isLiked }"
    />
    <span class="like-count">{{ displayCount }}</span>
  </button>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { likeApi } from '@/api/likeApi'

const props = defineProps({
  targetType: {
    type: String,
    required: true,
    validator: value => ['ARTICLE', 'QUESTION', 'COMMENT'].includes(value)
  },
  targetId: {
    type: [Number, String],
    required: true
  },
  initialCount: {
    type: Number,
    default: 0
  },
  initialLiked: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'normal',
    validator: value => ['small', 'normal', 'large'].includes(value)
  }
})

const emit = defineEmits(['like-changed'])

const store = useStore()
const loading = ref(false)
const likeCount = ref(props.initialCount)
const isLiked = ref(props.initialLiked)

const displayCount = computed(() => {
  if (likeCount.value >= 1000) {
    return (likeCount.value / 1000).toFixed(1) + 'k'
  }
  return likeCount.value
})

const handleLike = async () => {
  if (loading.value) return
  
  // 检查用户是否登录
  if (!store.state.isAuthenticated) {
    // 可以触发登录提示
    emit('like-changed', { needLogin: true })
    return
  }
  
  loading.value = true
  
  try {
    const result = await store.dispatch('toggleLike', {
      targetType: props.targetType,
      targetId: props.targetId
    })
    
    if (result.success) {
      isLiked.value = result.isLiked
      likeCount.value = Math.max(0, likeCount.value + (result.isLiked ? 1 : -1))
      
      emit('like-changed', {
        isLiked: result.isLiked,
        count: likeCount.value,
        message: result.message
      })
    } else {
      emit('like-changed', {
        error: result.message || '操作失败'
      })
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
    emit('like-changed', {
      error: error.message || '操作失败，请稍后重试'
    })
  } finally {
    loading.value = false
  }
}

// 初始化时获取最新的点赞信息
onMounted(async () => {
  try {
    const info = await likeApi.getLikeInfo(props.targetType, props.targetId)
    likeCount.value = info.count
    isLiked.value = info.isLiked
  } catch (error) {
    console.error('获取点赞信息失败:', error)
  }
})
</script>

<style scoped>
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #e1e5e9;
  border-radius: 0.5rem;
  background: white;
  color: #6c757d;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.like-btn:hover:not(:disabled) {
  border-color: #007bff;
  color: #007bff;
  background: #f8f9fa;
}

.like-btn.liked {
  border-color: #dc3545;
  color: #dc3545;
  background: #fff5f5;
}

.like-btn.liked .fa-heart {
  color: #dc3545;
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.like-btn.loading {
  opacity: 0.7;
}

.like-count {
  font-weight: 500;
  min-width: 1rem;
  text-align: center;
}

/* 不同尺寸 */
.like-btn.small {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  gap: 0.25rem;
}

.like-btn.large {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  gap: 0.75rem;
}

/* 动画效果 */
.fa-heart {
  transition: all 0.2s ease;
}

.like-btn.liked .fa-heart {
  animation: heartBeat 0.6s ease-in-out;
}

@keyframes heartBeat {
  0% {
    transform: scale(1);
  }
  14% {
    transform: scale(1.3);
  }
  28% {
    transform: scale(1);
  }
  42% {
    transform: scale(1.3);
  }
  70% {
    transform: scale(1);
  }
}
</style>
