<template>
  <div class="user-following">
    <div class="section-header">
      <h2 class="section-title">关注 ({{ totalItems }})</h2>
      <div class="search-box">
        <font-awesome-icon :icon="['fas', 'search']" class="search-icon" />
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索关注的人..."
          class="search-input"
        />
      </div>
    </div>
    
    <div class="filter-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        class="filter-tab"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </div>
    
    <div v-if="isLoading" class="loading-state">
      <font-awesome-icon :icon="['fas', 'spinner']" spin class="spinner" />
      <p>加载中...</p>
    </div>
    
    <div v-else-if="errorMessage" class="empty-state">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="empty-icon" />
      <h3>关注列表加载失败</h3>
      <p>{{ errorMessage }}</p>
      <button class="load-more-btn" @click="fetchFollowing">重新加载</button>
    </div>

    <div v-else-if="filteredFollowing.length === 0" class="empty-state">
      <font-awesome-icon :icon="['fas', 'user-friends']" class="empty-icon" />
      <h3>暂无关注</h3>
      <p v-if="searchQuery">没有找到匹配 "{{ searchQuery }}" 的用户</p>
      <p v-else>关注感兴趣的用户，获取他们的最新动态</p>
    </div>
    
    <div v-else class="following-list">
      <div 
        v-for="user in filteredFollowing" 
        :key="user.id" 
        class="following-item"
      >
        <div class="user-avatar">
          <img v-if="user.avatar" :src="user.avatar" :alt="user.name">
          <div v-else class="avatar-placeholder">
            {{ getInitials(user.name) }}
          </div>
        </div>
        
        <div class="user-info">
          <div class="user-header">
            <router-link :to="`/user/${user.id}`" class="user-name">
              {{ user.name }}
              <font-awesome-icon 
                v-if="user.isVerified" 
                :icon="['fas', 'check-circle']" 
                class="verified-icon" 
              />
            </router-link>
          </div>
          
          <p class="user-bio">{{ user.bio || '这个人很懒，还没有填写个人简介' }}</p>
          
          <div v-if="user.followedAt" class="user-meta">
            <span class="meta-item">关注于 {{ formatDate(user.followedAt) }}</span>
          </div>
        </div>
        
        <div class="user-actions">
          <button 
            v-if="isCurrentProfile"
            class="unfollow-btn"
            @click="confirmUnfollow(user)"
          >
            <font-awesome-icon :icon="['fas', 'user-minus']" />
            <span>取消关注</span>
          </button>
          <FollowButton
            v-else
            target-type="USER"
            :target-id="user.id"
            size="small"
          />
        </div>
      </div>
    </div>
    
    <div v-if="hasMoreFollowing && !isLoading" class="load-more">
      <button class="load-more-btn" @click="loadMoreFollowing">
        <font-awesome-icon v-if="isLoadingMore" :icon="['fas', 'spinner']" spin />
        <span v-else>加载更多</span>
      </button>
    </div>
    
    <!-- 确认取消关注弹窗 -->
    <div v-if="showUnfollowModal" class="modal-overlay" @click="showUnfollowModal = false">
      <div class="modal-content" @click.stop>
        <h3 class="modal-title">取消关注</h3>
        <p class="modal-message">
          确定要取消关注 <strong>{{ unfollowUser?.name }}</strong> 吗？
        </p>
        <div class="modal-actions">
          <button 
            class="cancel-btn"
            @click="showUnfollowModal = false"
          >
            取消
          </button>
          <button 
            class="confirm-btn"
            @click="unfollowConfirmed"
          >
            <font-awesome-icon v-if="isUnfollowing" :icon="['fas', 'spinner']" spin />
            <span v-else>确认</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
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
const currentUserId = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}').id || null;
  } catch (error) {
    return null;
  }
});
const isCurrentProfile = computed(() => (
  currentUserId.value && profileUserId.value && String(currentUserId.value) === String(profileUserId.value)
));

// 状态
const following = ref([]);
const isLoading = ref(true);
const isLoadingMore = ref(false);
const searchQuery = ref('');
const page = ref(1);
const pageSize = 10;
const totalItems = ref(0);
const totalPages = ref(0);
const errorMessage = ref('');
const hasMoreFollowing = computed(() => page.value < totalPages.value);

// 筛选标签
const tabs = [
  { id: 'all', name: '全部' },
  { id: 'recent', name: '最近关注' }
];
const activeTab = ref('all');

// 取消关注相关
const showUnfollowModal = ref(false);
const unfollowUser = ref(null);
const isUnfollowing = ref(false);

// 获取用户名首字母
const getInitials = (name) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 筛选关注列表
const filteredFollowing = computed(() => {
  let result = following.value;

  // 标签筛选
  if (activeTab.value === 'recent') {
    result = result.filter(user => user.recentlyFollowed);
  }

  // 关键词搜索
  if (searchQuery.value && searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase().trim();
    result = result.filter(user => {
      const nameMatch = user.name && user.name.toLowerCase().includes(query);
      const bioMatch = user.bio && user.bio.toLowerCase().includes(query);
      const usernameMatch = user.username && user.username.toLowerCase().includes(query);

      return nameMatch || bioMatch || usernameMatch;
    });
  }

  return result;
});

// 弹出取消关注确认框
const confirmUnfollow = (user) => {
  unfollowUser.value = user;
  showUnfollowModal.value = true;
};

// 确认取消关注
const unfollowConfirmed = async () => {
  if (!unfollowUser.value || isUnfollowing.value) return;
  
  try {
    isUnfollowing.value = true;
    await followApi.unfollowTarget('USER', unfollowUser.value.id);
    
    // 从列表中移除
    following.value = following.value.filter(user => user.id !== unfollowUser.value.id);
    totalItems.value = Math.max(totalItems.value - 1, 0);
    
    // 关闭弹窗
    showUnfollowModal.value = false;
    unfollowUser.value = null;
    ElMessage.success('已取消关注');
  } catch (error) {
    ElMessage.error(error.message || '取消关注失败');
  } finally {
    isUnfollowing.value = false;
  }
};

const unwrapPage = (response) => {
  const pageData = response?.data || response || {};
  return {
    content: Array.isArray(pageData.content) ? pageData.content : Array.isArray(pageData) ? pageData : [],
    totalElements: pageData.totalElements || 0,
    totalPages: pageData.totalPages || 0
  };
};

const isRecentlyFollowed = (dateString) => {
  if (!dateString) return false;
  const followedAt = new Date(dateString).getTime();
  if (Number.isNaN(followedAt)) return false;
  return Date.now() - followedAt <= 30 * 24 * 60 * 60 * 1000;
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
    recentlyFollowed: isRecentlyFollowed(user.followedAt || user.createdAt),
    isVerified: false
  };
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('zh-CN');
};

// 加载更多关注的人
const loadMoreFollowing = async () => {
  if (isLoadingMore.value || !hasMoreFollowing.value) return;
  
  try {
    isLoadingMore.value = true;
    page.value++;
    const response = await followApi.getUserFollowedUsers(page.value - 1, pageSize, profileUserId.value);
    const pageData = unwrapPage(response);
    following.value = [...following.value, ...pageData.content.map(normalizeUser)];
    totalItems.value = pageData.totalElements;
    totalPages.value = pageData.totalPages;
  } catch (error) {
    errorMessage.value = error.message || '加载更多关注失败';
  } finally {
    isLoadingMore.value = false;
  }
};

// 获取关注列表
const fetchFollowing = async () => {
  try {
    isLoading.value = true;
    errorMessage.value = '';
    page.value = 1;
    const response = await followApi.getUserFollowedUsers(0, pageSize, profileUserId.value);
    const pageData = unwrapPage(response);
    following.value = pageData.content.map(normalizeUser);
    totalItems.value = pageData.totalElements;
    totalPages.value = pageData.totalPages;
  } catch (error) {
    errorMessage.value = error.message || '关注列表加载失败';
    following.value = [];
    totalItems.value = 0;
    totalPages.value = 0;
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchFollowing();
});

watch(profileUserId, () => {
  fetchFollowing();
});
</script>

<style scoped>
.user-following {
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

.filter-tabs {
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  gap: 0.35rem;
  padding: 0.35rem;
  overflow-x: auto;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
}

.filter-tab {
  min-height: 2.25rem;
  padding: 0.45rem 0.85rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--kumo-text-muted);
  font-weight: 760;
  cursor: pointer;
  transition: var(--transition);
}

.filter-tab:hover,
.filter-tab.active {
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
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

.following-list {
  display: grid;
  gap: 0.85rem;
}

.following-item {
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

.following-item:hover {
  transform: translateY(-3px);
  border-color: var(--kumo-hairline-strong);
  box-shadow: var(--kumo-shadow-sm);
}

.user-avatar {
  width: 3.6rem;
  height: 3.6rem;
  overflow: hidden;
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
}

.user-avatar img,
.avatar-placeholder {
  width: 100%;
  height: 100%;
}

.user-avatar img {
  object-fit: cover;
}

.avatar-placeholder {
  display: grid;
  place-items: center;
  color: var(--kumo-bg-brand-strong);
  font-size: 1.25rem;
  font-weight: 900;
}

.user-info {
  min-width: 0;
}

.user-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
}

.user-name {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--kumo-text-default);
  font-size: 1rem;
  font-weight: 820;
  text-decoration: none;
}

.user-name:hover,
.verified-icon {
  color: var(--kumo-bg-brand-strong);
}

.follows-you {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.5rem;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 760;
}

.user-bio {
  display: -webkit-box;
  margin: 0.3rem 0 0.45rem;
  overflow: hidden;
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.meta-item {
  color: var(--kumo-text-subtle);
  font-size: 0.82rem;
  font-weight: 700;
}

.user-actions,
.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
}

.unfollow-btn,
.load-more-btn,
.cancel-btn,
.confirm-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 2.55rem;
  padding: 0.6rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  font-weight: 760;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition),
    background-color var(--kumo-transition);
}

.unfollow-btn:hover {
  transform: translateY(-2px);
  border-color: var(--kumo-status-danger);
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.load-more {
  margin-top: 0.5rem;
}

.load-more-btn {
  min-width: 8rem;
}

.load-more-btn:hover,
.cancel-btn:hover {
  border-color: var(--kumo-hairline-strong);
  color: var(--kumo-bg-brand-strong);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: var(--kumo-bg-overlay);
  backdrop-filter: var(--kumo-blur);
}

.modal-content {
  display: grid;
  gap: 1rem;
  width: min(100%, 28rem);
  padding: 1.25rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-lg);
  backdrop-filter: var(--kumo-blur);
  animation: dialog-in 260ms ease both;
}

.modal-title,
.modal-message {
  margin: 0;
}

.modal-title {
  color: var(--kumo-text-default);
  font-size: 1.2rem;
  font-weight: 840;
}

.modal-message {
  color: var(--kumo-text-muted);
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.confirm-btn {
  border-color: transparent;
  background: var(--kumo-status-danger);
  color: var(--kumo-text-inverse);
  min-width: 5rem;
}

@keyframes dialog-in {
  from {
    opacity: 0;
    transform: translateY(1rem) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 768px) {
  .section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box,
  .filter-tabs {
    width: 100%;
  }

  .following-item {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .user-avatar {
    justify-self: center;
  }

  .user-header,
  .user-meta,
  .modal-actions {
    justify-content: center;
  }
}
</style>
