<template>
  <div class="user-followers">
    <div class="section-header">
      <h2 class="section-title">粉丝 ({{ totalItems }})</h2>
      <div class="search-box">
        <font-awesome-icon :icon="['fas', 'search']" class="search-icon" />
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索粉丝..."
          class="search-input"
        />
      </div>
    </div>
    
    <div v-if="isLoading" class="loading-state">
      <font-awesome-icon :icon="['fas', 'spinner']" spin class="spinner" />
      <p>加载中...</p>
    </div>

    <div v-else-if="errorMessage" class="empty-state">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="empty-icon" />
      <h3>粉丝加载失败</h3>
      <p>{{ errorMessage }}</p>
      <button class="load-more-btn" @click="fetchFollowers">重新加载</button>
    </div>
    
    <div v-else-if="filteredFollowers.length === 0" class="empty-state">
      <font-awesome-icon :icon="['fas', 'user-friends']" class="empty-icon" />
      <h3>暂无粉丝</h3>
      <p v-if="searchQuery">没有找到匹配 "{{ searchQuery }}" 的粉丝</p>
      <p v-else>创作优质内容，吸引更多粉丝关注你</p>
    </div>
    
    <div v-else class="followers-list">
      <div 
        v-for="follower in filteredFollowers" 
        :key="follower.id" 
        class="follower-item"
      >
        <div class="follower-avatar">
          <img v-if="follower.avatar" :src="follower.avatar" :alt="follower.name">
          <div v-else class="avatar-placeholder">
            {{ getInitials(follower.name) }}
          </div>
        </div>
        
        <div class="follower-info">
          <router-link :to="`/user/${follower.id}`" class="follower-name">
            {{ follower.name }}
            <font-awesome-icon 
              v-if="follower.isVerified" 
              :icon="['fas', 'check-circle']" 
              class="verified-icon" 
            />
          </router-link>
          <p class="follower-bio">{{ follower.bio || '这个人很懒，还没有填写个人简介' }}</p>
          <div v-if="follower.followedAt" class="follower-meta">
            <span class="meta-item">关注于 {{ formatDate(follower.followedAt) }}</span>
          </div>
        </div>
        
        <div class="follower-actions">
          <FollowButton
            target-type="USER"
            :target-id="follower.id"
            size="small"
          />
        </div>
      </div>
    </div>
    
    <div v-if="hasMoreFollowers && !isLoading" class="load-more">
      <button class="load-more-btn" @click="loadMoreFollowers">
        <font-awesome-icon v-if="isLoadingMore" :icon="['fas', 'spinner']" spin />
        <span v-else>加载更多</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { followApi } from '@/api/followApi';
import FollowButton from '@/components/FollowButton.vue';

const props = defineProps({
  userId: {
    type: [String, Number],
    default: null
  }
});

const route = useRoute();
const profileUserId = computed(() => props.userId || route.params.id);

const followers = ref([]);
const isLoading = ref(true);
const isLoadingMore = ref(false);
const searchQuery = ref('');
const page = ref(1);
const pageSize = 10;
const totalItems = ref(0);
const totalPages = ref(0);
const errorMessage = ref('');

const hasMoreFollowers = computed(() => page.value < totalPages.value);

// 获取用户名首字母
const getInitials = (name) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 过滤粉丝列表
const filteredFollowers = computed(() => {
  if (!searchQuery.value.trim()) return followers.value;

  const query = searchQuery.value.toLowerCase().trim();
  return followers.value.filter(follower => {
    const nameMatch = follower.name && follower.name.toLowerCase().includes(query);
    const bioMatch = follower.bio && follower.bio.toLowerCase().includes(query);
    const usernameMatch = follower.username && follower.username.toLowerCase().includes(query);

    return nameMatch || bioMatch || usernameMatch;
  });
});

const unwrapPage = (response) => {
  const pageData = response?.data || response || {};
  return {
    content: Array.isArray(pageData.content) ? pageData.content : Array.isArray(pageData) ? pageData : [],
    totalElements: pageData.totalElements || 0,
    totalPages: pageData.totalPages || 0
  };
};

const normalizeUser = (user) => {
  const name = user.nickname || user.username || user.name || '用户';
  return {
    id: user.id,
    username: user.username,
    name,
    avatar: user.avatarUrl || user.avatar || '',
    bio: user.bio || '',
    followedAt: user.followedAt || user.createdAt || null,
    isVerified: false
  };
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('zh-CN');
};

// 加载更多粉丝
const loadMoreFollowers = async () => {
  if (isLoadingMore.value || !hasMoreFollowers.value) return;
  
  try {
    isLoadingMore.value = true;
    page.value++;
    const response = await followApi.getUserFollowers(page.value - 1, pageSize, profileUserId.value);
    const pageData = unwrapPage(response);
    followers.value = [...followers.value, ...pageData.content.map(normalizeUser)];
    totalItems.value = pageData.totalElements;
    totalPages.value = pageData.totalPages;
  } catch (error) {
    console.error('Failed to load more followers:', error);
    errorMessage.value = error.message || '加载更多粉丝失败';
  } finally {
    isLoadingMore.value = false;
  }
};

// 获取粉丝列表
const fetchFollowers = async () => {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    page.value = 1;
    const response = await followApi.getUserFollowers(0, pageSize, profileUserId.value);
    const pageData = unwrapPage(response);
    followers.value = pageData.content.map(normalizeUser);
    totalItems.value = pageData.totalElements;
    totalPages.value = pageData.totalPages;
  } catch (error) {
    console.error('Failed to fetch followers:', error);
    errorMessage.value = error.message || '粉丝加载失败';
    followers.value = [];
    totalItems.value = 0;
    totalPages.value = 0;
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchFollowers();
});

watch(profileUserId, () => {
  fetchFollowers();
});
</script>

<style scoped>
.user-followers {
  padding: 1.5rem;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
}

.search-box {
  position: relative;
  width: 250px;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-light);
}

.search-input {
  width: 100%;
  padding: 0.6rem 1rem 0.6rem 2.25rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  font-size: 0.9rem;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 0;
  color: var(--text-light);
  text-align: center;
}

.spinner, .empty-icon {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.followers-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.follower-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  transition: all 0.3s;
}

.follower-item:hover {
  border-color: var(--primary-light);
  box-shadow: 0 2px 8px rgba(var(--primary-rgb), 0.1);
}

.follower-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.follower-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background-color: var(--primary-light);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 600;
}

.follower-info {
  flex: 1;
  min-width: 0;
}

.follower-name {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--text-color);
  margin-bottom: 0.35rem;
  text-decoration: none;
}

.follower-name:hover {
  color: var(--primary-color);
}

.verified-icon {
  color: var(--primary-color);
  font-size: 0.9rem;
}

.follower-bio {
  color: var(--text-light);
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.follower-meta {
  display: flex;
  gap: 1rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--text-light);
  font-size: 0.85rem;
}

.follower-actions {
  display: flex;
  align-items: center;
}

.follow-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--primary-color);
  background-color: var(--primary-color);
  color: white;
}

.follow-btn:hover {
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
}

.follow-btn.following {
  background-color: transparent;
  color: var(--primary-color);
}

.follow-btn.following:hover {
  background-color: rgba(var(--error-rgb), 0.1);
  border-color: var(--error-color);
  color: var(--error-color);
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}

.load-more-btn {
  padding: 0.6rem 1.5rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background-color: transparent;
  color: var(--text-light);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
}

.load-more-btn:hover {
  background-color: var(--bg-light);
  color: var(--text-color);
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .search-box {
    width: 100%;
  }
  
  .follower-item {
    flex-direction: column;
  }
  
  .follower-avatar {
    margin: 0 auto;
  }
  
  .follower-info, .follower-actions {
    text-align: center;
  }
  
  .follower-meta {
    justify-content: center;
  }
  
  .follower-actions {
    margin-top: 1rem;
    justify-content: center;
  }
}
</style> 
