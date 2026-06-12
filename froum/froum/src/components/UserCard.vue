<template>
  <div class="user-card" :class="{ 'compact': compact }">
    <div class="user-card-inner">
      <div class="user-header">
        <div class="user-avatar">
          <img v-if="user.avatarUrl || user.avatar" :src="user.avatarUrl || user.avatar" :alt="getUserName(user)">
          <div v-else class="user-initials">{{ getInitials(getUserName(user)) }}</div>
        </div>
        <div class="user-info">
          <h3 class="user-name">
            <router-link v-if="!noLink" :to="`/user/${user.id}`">{{ getUserName(user) }}</router-link>
            <span v-else>{{ getUserName(user) }}</span>
          </h3>
          <p v-if="user.bio" class="user-bio">{{ truncateText(user.bio, 120) }}</p>
        </div>
      </div>
      
      <div v-if="!hideStats && showStats" class="user-stats">
        <div class="stat-item">
          <font-awesome-icon :icon="['fas', 'file-alt']" />
          <span>{{ user.articles || 0 }} 文章</span>
        </div>
        <div class="stat-item">
          <font-awesome-icon :icon="['fas', 'question-circle']" />
          <span>{{ user.questions || 0 }} 问题</span>
        </div>
        <div v-if="!compact" class="stat-item">
          <font-awesome-icon :icon="['fas', 'comment-dots']" />
          <span>{{ user.answers || 0 }} 回答</span>
        </div>
      </div>
      
      <div v-if="user.location || user.joinTime" class="user-meta">
        <div v-if="user.location" class="meta-item">
          <font-awesome-icon :icon="['fas', 'map-marker-alt']" />
          <span>{{ user.location }}</span>
        </div>
        <div v-if="user.joinTime || user.createdAt" class="meta-item">
          <font-awesome-icon :icon="['fas', 'calendar-alt']" />
          <span>{{ formatTime(user.joinTime || user.createdAt) }} 加入</span>
        </div>
      </div>
      
      <div class="user-actions" v-if="showActions">
        <button 
          v-if="user.isFollowing !== undefined" 
          class="action-btn follow-btn" 
          :class="{ 'following': user.isFollowing }"
          @click="handleFollow"
        >
          <font-awesome-icon :icon="user.isFollowing ? ['fas', 'user-minus'] : ['fas', 'user-plus']" />
          <span>{{ user.isFollowing ? '取消关注' : '关注' }}</span>
        </button>
        
        <button 
          v-if="showMessage" 
          class="action-btn message-btn"
          @click="handleMessage"
        >
          <font-awesome-icon :icon="['fas', 'envelope']" />
          <span>发送消息</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'UserCard',
  props: {
    user: {
      type: Object,
      required: true
    },
    compact: {
      type: Boolean,
      default: false
    },
    noLink: {
      type: Boolean,
      default: false
    },
    hideStats: {
      type: Boolean,
      default: false
    },
    showActions: {
      type: Boolean,
      default: true
    },
    showMessage: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    showStats() {
      return this.user.articles !== undefined || 
             this.user.questions !== undefined || 
             this.user.answers !== undefined
    }
  },
  methods: {
    getUserName(user) {
      if (!user) return '匿名用户';
      // 优先使用 nickname，然后是 username，最后是 name
      return user.nickname || user.username || user.name || '匿名用户';
    },

    getInitials(name) {
      if (!name) return '?';
      return name.charAt(0).toUpperCase();
    },
    
    truncateText(text, maxLength) {
      if (!text) return '';
      if (text.length <= maxLength) return text;
      return text.substring(0, maxLength) + '...';
    },
    
    formatTime(time) {
      if (!time) return '未知时间';
      
      const date = new Date(time);
      
      // 如果日期无效，返回原始值
      if (isNaN(date.getTime())) {
        return time;
      }
      
      // 计算年份差异
      const now = new Date();
      const years = now.getFullYear() - date.getFullYear();
      
      if (years > 0) {
        return `${years}年前`;
      } else {
        // 返回具体日期格式
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      }
    },
    
    handleFollow() {
      this.$emit('follow', this.user);
    },
    
    handleMessage() {
      this.$emit('message', this.user);
    }
  }
})
</script>

<style scoped>
.user-card {
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: var(--transition);
}

.user-card:hover {
  box-shadow: var(--shadow);
  transform: translateY(-2px);
  border-color: var(--primary-lighter);
}

.user-card-inner {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.user-card.compact .user-card-inner {
  padding: 1rem;
  gap: 0.75rem;
}

.user-header {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background-color: var(--primary-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-card.compact .user-avatar {
  width: 48px;
  height: 48px;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-initials {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary-color);
}

.user-card.compact .user-initials {
  font-size: 1.25rem;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 0.5rem;
  color: var(--text-color);
  line-height: 1.3;
}

.user-card.compact .user-name {
  font-size: 1rem;
  margin-bottom: 0.25rem;
}

.user-name a {
  color: inherit;
  text-decoration: none;
  transition: var(--transition);
}

.user-name a:hover {
  color: var(--primary-color);
}

.user-bio {
  font-size: 0.9rem;
  color: var(--text-light);
  margin: 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.user-card.compact .user-bio {
  font-size: 0.8rem;
  -webkit-line-clamp: 1;
}

.user-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--text-light);
}

.user-card.compact .user-stats {
  gap: 0.75rem;
  font-size: 0.8rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--text-light);
}

.user-card.compact .user-meta {
  gap: 0.75rem;
  font-size: 0.8rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.user-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border-color);
  background-color: var(--bg-white);
  color: var(--text-color);
}

.user-card.compact .action-btn {
  padding: 0.4rem 0.75rem;
  font-size: 0.8rem;
}

.action-btn:hover {
  border-color: var(--primary-color);
  background-color: rgba(var(--primary-rgb), 0.05);
  color: var(--primary-color);
}

.follow-btn {
  background-color: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}

.follow-btn:hover {
  background-color: var(--primary-dark);
  color: white;
  border-color: var(--primary-dark);
}

.follow-btn.following {
  background-color: var(--bg-white);
  color: var(--text-color);
  border-color: var(--border-color);
}

.follow-btn.following:hover {
  color: var(--error-color);
  border-color: var(--error-color);
  background-color: rgba(var(--error-rgb), 0.05);
}

.message-btn {
  background-color: var(--bg-white);
  color: var(--text-color);
  border-color: var(--border-color);
}

.message-btn:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
  background-color: rgba(var(--primary-rgb), 0.05);
}

@media (max-width: 576px) {
  .user-card-inner {
    padding: 1rem;
  }
  
  .user-avatar {
    width: 48px;
    height: 48px;
  }
  
  .user-card.compact .user-avatar {
    width: 40px;
    height: 40px;
  }
  
  .user-initials {
    font-size: 1.25rem;
  }
  
  .user-card.compact .user-initials {
    font-size: 1rem;
  }
  
  .user-name {
    font-size: 1rem;
  }
  
  .user-card.compact .user-name {
    font-size: 0.9rem;
  }
  
  .user-stats, .user-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .user-actions {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .action-btn {
    width: 100%;
  }
}
</style> 