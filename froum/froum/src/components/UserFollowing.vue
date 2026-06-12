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
    console.error('Failed to unfollow user:', error);
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
    avatar: user.avatarUrl || user.avatar || '',
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
    console.error('Failed to load more following:', error);
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
    console.error('Failed to fetch following:', error);
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
  padding: 1.5rem;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
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

.filter-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.5rem;
}

.filter-tab {
  padding: 0.5rem 1rem;
  border: none;
  background: none;
  color: var(--text-light);
  font-size: 0.9rem;
  cursor: pointer;
  border-radius: var(--radius);
  transition: all 0.3s;
}

.filter-tab:hover {
  color: var(--primary-color);
  background-color: var(--bg-light);
}

.filter-tab.active {
  color: var(--primary-color);
  font-weight: 500;
  position: relative;
}

.filter-tab.active::after {
  content: '';
  position: absolute;
  bottom: -0.5rem;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--primary-color);
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

.explore-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding: 0.6rem 1.25rem;
  background-color: var(--primary-color);
  color: white;
  border-radius: var(--radius);
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s;
}

.explore-btn:hover {
  background-color: var(--primary-dark);
}

.following-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.following-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  transition: all 0.3s;
}

.following-item:hover {
  border-color: var(--border-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar img {
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

.user-info {
  flex: 1;
  min-width: 0;
}

.user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--text-color);
  text-decoration: none;
}

.user-name:hover {
  color: var(--primary-color);
}

.verified-icon {
  color: var(--primary-color);
  font-size: 0.9rem;
}

.follows-you {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: var(--text-light);
  background-color: var(--bg-light);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius);
}

.user-bio {
  color: var(--text-light);
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.user-meta {
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

.user-actions {
  display: flex;
  align-items: center;
}

.unfollow-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-color);
  background-color: transparent;
  color: var(--text-color);
}

.unfollow-btn:hover {
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

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: var(--radius);
  padding: 1.5rem;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 1rem;
}

.modal-message {
  color: var(--text-light);
  margin-bottom: 1.5rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.cancel-btn, .confirm-btn {
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-light);
}

.cancel-btn:hover {
  background-color: var(--bg-light);
  color: var(--text-color);
}

.confirm-btn {
  background-color: var(--error-color);
  border: 1px solid var(--error-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
}

.confirm-btn:hover {
  background-color: var(--error-dark);
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
  
  .filter-tabs {
    width: 100%;
    overflow-x: auto;
  }
  
  .following-item {
    flex-direction: column;
  }
  
  .user-avatar {
    margin: 0 auto;
  }
  
  .user-header {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .user-info, .user-actions {
    text-align: center;
  }
  
  .user-meta {
    justify-content: center;
  }
  
  .user-actions {
    margin-top: 1rem;
    justify-content: center;
  }
}
</style> 
