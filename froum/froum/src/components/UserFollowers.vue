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
import { resolveAvatarUrl } from '@/utils/avatar';

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
    avatar: resolveAvatarUrl(user.avatarUrl || user.avatar || ''),
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
  display: grid;
  gap: 1rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.section-title {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.2rem;
  font-weight: 840;
}

.search-box {
  position: relative;
  width: min(100%, 18rem);
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  top: 50%;
  color: var(--kumo-text-subtle);
  transform: translateY(-50%);
}

.search-input {
  width: 100%;
  min-height: 2.65rem;
  padding: 0.6rem 1rem 0.6rem 2.35rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.search-input:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.loading-state,
.empty-state {
  display: grid;
  place-items: center;
  gap: 0.65rem;
  min-height: 12rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.spinner,
.empty-icon {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}

.empty-state h3,
.empty-state p {
  margin: 0;
}

.empty-state h3 {
  color: var(--kumo-text-default);
  font-size: 1.1rem;
  font-weight: 840;
}

.followers-list {
  display: grid;
  gap: 0.85rem;
}

.follower-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.follower-item:hover {
  transform: translateY(-3px);
  border-color: var(--kumo-hairline-strong);
  box-shadow: var(--kumo-shadow-sm);
}

.follower-avatar {
  width: 3.6rem;
  height: 3.6rem;
  overflow: hidden;
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
}

.follower-avatar img,
.avatar-placeholder {
  width: 100%;
  height: 100%;
}

.follower-avatar img {
  object-fit: cover;
}

.avatar-placeholder {
  display: grid;
  place-items: center;
  color: var(--kumo-bg-brand-strong);
  font-size: 1.25rem;
  font-weight: 900;
}

.follower-info {
  min-width: 0;
}

.follower-name {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--kumo-text-default);
  font-size: 1rem;
  font-weight: 820;
  text-decoration: none;
}

.follower-name:hover,
.verified-icon {
  color: var(--kumo-bg-brand-strong);
}

.follower-bio {
  display: -webkit-box;
  margin: 0.3rem 0 0.45rem;
  overflow: hidden;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.follower-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.meta-item {
  color: var(--kumo-text-subtle);
  font-size: 0.82rem;
  font-weight: 700;
}

.follower-actions,
.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
}

.load-more {
  margin-top: 0.5rem;
}

.load-more-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 8rem;
  min-height: 2.55rem;
  padding: 0.6rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  font-weight: 760;
  cursor: pointer;
  transition: var(--transition);
}

.load-more-btn:hover {
  border-color: var(--kumo-hairline-strong);
  color: var(--kumo-bg-brand-strong);
}

@media (max-width: 768px) {
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    width: 100%;
  }

  .follower-item {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .follower-avatar {
    justify-self: center;
  }

  .follower-meta {
    justify-content: center;
  }
}
</style>
