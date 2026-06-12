<template>
  <button 
    @click="handleFollow" 
    :disabled="loading" 
    :class="['follow-btn', { 'following': isFollowed, 'loading': loading }, size]"
  >
    <font-awesome-icon 
      :icon="['fas', isFollowed ? 'check' : 'plus']" 
      v-if="!loading"
    />
    <font-awesome-icon 
      :icon="['fas', 'spinner']" 
      spin 
      v-if="loading"
    />
    <span>{{ buttonText }}</span>
  </button>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { followApi } from '@/api/followApi'

const props = defineProps({
  targetType: {
    type: String,
    required: true,
    validator: value => ['USER', 'QUESTION'].includes(value)
  },
  targetId: {
    type: [Number, String],
    required: true
  },
  initialFollowed: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'normal',
    validator: value => ['small', 'normal', 'large'].includes(value)
  },
  followText: {
    type: String,
    default: null
  },
  followingText: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['follow-changed'])

const store = useStore()
const loading = ref(false)
const isFollowed = ref(props.initialFollowed)

const buttonText = computed(() => {
  if (loading.value) {
    return '处理中...'
  }
  
  if (isFollowed.value) {
    return props.followingText || getDefaultFollowingText()
  } else {
    return props.followText || getDefaultFollowText()
  }
})

const getDefaultFollowText = () => {
  switch (props.targetType) {
    case 'USER':
      return '关注'
    case 'QUESTION':
      return '关注问题'
    default:
      return '关注'
  }
}

const getDefaultFollowingText = () => {
  switch (props.targetType) {
    case 'USER':
      return '已关注'
    case 'QUESTION':
      return '已关注'
    default:
      return '已关注'
  }
}

const handleFollow = async () => {
  if (loading.value) return
  
  // 检查用户是否登录
  if (!store.state.isAuthenticated) {
    // 可以触发登录提示
    emit('follow-changed', { needLogin: true })
    return
  }
  
  loading.value = true
  
  try {
    const result = await store.dispatch('toggleFollowTarget', {
      targetType: props.targetType,
      targetId: props.targetId
    })
    
    if (result.success) {
      isFollowed.value = result.isFollowed
      
      emit('follow-changed', {
        isFollowed: result.isFollowed,
        message: result.message
      })
    } else {
      emit('follow-changed', {
        error: result.message || '操作失败'
      })
    }
  } catch (error) {
    console.error('关注操作失败:', error)
    emit('follow-changed', {
      error: error.message || '操作失败，请稍后重试'
    })
  } finally {
    loading.value = false
  }
}

// 初始化时获取最新的关注信息
onMounted(async () => {
  try {
    const info = await followApi.getFollowInfo(props.targetType, props.targetId)
    isFollowed.value = info.isFollowed
  } catch (error) {
    console.error('获取关注信息失败:', error)
  }
})
</script>

<style scoped>
.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #007bff;
  border-radius: 0.5rem;
  background: white;
  color: #007bff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
  min-width: 80px;
  justify-content: center;
}

.follow-btn:hover:not(:disabled) {
  background: #007bff;
  color: white;
}

.follow-btn.following {
  border-color: #28a745;
  color: #28a745;
}

.follow-btn.following:hover:not(:disabled) {
  border-color: #dc3545;
  color: #dc3545;
  background: #fff5f5;
}

.follow-btn.following:hover:not(:disabled) span {
  content: '取消关注';
}

.follow-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.follow-btn.loading {
  opacity: 0.7;
}

/* 不同尺寸 */
.follow-btn.small {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  gap: 0.25rem;
  min-width: 60px;
}

.follow-btn.large {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  gap: 0.75rem;
  min-width: 120px;
}

/* 动画效果 */
.fa-icon {
  transition: all 0.2s ease;
}

.follow-btn.following .fa-check {
  color: #28a745;
}

/* 悬停时的文本变化效果 */
.follow-btn.following:hover:not(:disabled)::after {
  content: '取消关注';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.follow-btn.following:hover:not(:disabled) span {
  opacity: 0;
}
</style>
