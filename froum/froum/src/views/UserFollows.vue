<template>
  <div class="user-follows-page">
    <div class="container">
      <div class="page-header">
        <h1>我的关注</h1>
        <p class="text-muted">管理您关注的用户和问题</p>
      </div>

      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.key"
          @click="activeTab = tab.key"
          :class="['tab-btn', { active: activeTab === tab.key }]"
        >
          {{ tab.label }}
          <span class="count" v-if="tab.count !== undefined">({{ tab.count }})</span>
        </button>
      </div>

      <div class="tab-content">
        <div v-if="errorMessage" class="error">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
          {{ errorMessage }}
        </div>

        <!-- 关注的用户 -->
        <div v-if="activeTab === 'users'" class="content-list">
          <div v-if="loading" class="loading">
            <font-awesome-icon :icon="['fas', 'spinner']" spin />
            加载中...
          </div>
          <div v-else-if="followedUsers.length === 0" class="empty">
            <font-awesome-icon :icon="['fas', 'user-plus']" />
            <p>您还没有关注任何用户</p>
          </div>
          <div v-else class="user-grid">
            <div 
              v-for="user in followedUsers" 
              :key="user.id" 
              class="user-card"
            >
              <div class="user-avatar">
                <img :src="user.avatarUrl || user.avatar || '/assets/default-avatar.svg'" :alt="user.username" />
              </div>
              <div class="user-info">
                <h3>
                  <router-link :to="`/user/${user.id}`">
                    {{ user.nickname || user.username }}
                  </router-link>
                </h3>
                <p class="user-bio">{{ user.bio || '这个用户很懒，什么都没写' }}</p>
                <div class="user-stats">
                  <span>关注于: {{ formatDate(user.followedAt) }}</span>
                </div>
              </div>
              <div class="user-actions">
                <FollowButton 
                  target-type="USER"
                  :target-id="user.id"
                  :initial-followed="true"
                  size="small"
                  @follow-changed="handleFollowChanged"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 关注的问题 -->
        <div v-if="activeTab === 'questions'" class="content-list">
          <div v-if="loading" class="loading">
            <font-awesome-icon :icon="['fas', 'spinner']" spin />
            加载中...
          </div>
          <div v-else-if="followedQuestions.length === 0" class="empty">
            <font-awesome-icon :icon="['fas', 'question-circle']" />
            <p>您还没有关注任何问题</p>
          </div>
          <div v-else>
            <div 
              v-for="question in followedQuestions" 
              :key="question.id" 
              class="question-item"
            >
              <div class="question-header">
                <h3>
                  <router-link :to="`/question/${question.id}`">
                    {{ question.title }}
                  </router-link>
                </h3>
                <span class="date">{{ formatDate(question.followedAt) }}</span>
              </div>
              <p class="question-content">{{ question.content }}</p>
              <div class="question-meta">
                <span class="author">提问者: {{ question.author || question.authorId || '未知' }}</span>
                <div class="question-stats">
                  <span>
                    <font-awesome-icon :icon="['fas', 'eye']" />
                    {{ question.viewCount || 0 }}
                  </span>
                  <span>
                    <font-awesome-icon :icon="['fas', 'comment']" />
                    {{ question.answerCount || question.commentCount || 0 }}
                  </span>
                  <span>
                    <font-awesome-icon :icon="['fas', 'heart']" />
                    {{ question.likeCount || 0 }}
                  </span>
                </div>
              </div>
              <div class="question-actions">
                <FollowButton 
                  target-type="QUESTION"
                  :target-id="question.id"
                  :initial-followed="true"
                  size="small"
                  follow-text="关注问题"
                  following-text="已关注"
                  @follow-changed="handleFollowChanged"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 粉丝列表 -->
        <div v-if="activeTab === 'followers'" class="content-list">
          <div v-if="loading" class="loading">
            <font-awesome-icon :icon="['fas', 'spinner']" spin />
            加载中...
          </div>
          <div v-else-if="followers.length === 0" class="empty">
            <font-awesome-icon :icon="['fas', 'users']" />
            <p>您还没有粉丝</p>
          </div>
          <div v-else class="user-grid">
            <div 
              v-for="follower in followers" 
              :key="follower.id" 
              class="user-card"
            >
              <div class="user-avatar">
                <img :src="follower.avatarUrl || follower.avatar || '/assets/default-avatar.svg'" :alt="follower.username" />
              </div>
              <div class="user-info">
                <h3>
                  <router-link :to="`/user/${follower.id}`">
                    {{ follower.nickname || follower.username }}
                  </router-link>
                </h3>
                <p class="user-bio">{{ follower.bio || '这个用户很懒，什么都没写' }}</p>
                <div class="user-stats">
                  <span>关注于: {{ formatDate(follower.followedAt) }}</span>
                </div>
              </div>
              <div class="user-actions">
                <FollowButton 
                  target-type="USER"
                  :target-id="follower.id"
                  :initial-followed="follower.isFollowing || false"
                  size="small"
                  @follow-changed="handleFollowChanged"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <button 
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage <= 1"
          class="page-btn"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页
        </span>
        <button 
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage >= totalPages"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { followApi } from '@/api/followApi'
import FollowButton from '@/components/FollowButton.vue'

const activeTab = ref('users')
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = 10

const followedUsers = ref([])
const followedQuestions = ref([])
const followers = ref([])

const pagination = reactive({
  users: { total: 0, pages: 0 },
  questions: { total: 0, pages: 0 },
  followers: { total: 0, pages: 0 }
})

const tabs = computed(() => [
  { 
    key: 'users', 
    label: '关注的用户', 
    count: pagination.users.total 
  },
  { 
    key: 'questions', 
    label: '关注的问题', 
    count: pagination.questions.total 
  },
  { 
    key: 'followers', 
    label: '我的粉丝', 
    count: pagination.followers.total 
  }
])

const totalPages = computed(() => {
  return pagination[activeTab.value]?.pages || 0
})

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN')
}

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || 0,
    totalPages: page.totalPages || 0
  }
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''
  
  try {
    switch (activeTab.value) {
      case 'users':
        await loadFollowedUsers()
        break
      case 'questions':
        await loadFollowedQuestions()
        break
      case 'followers':
        await loadFollowers()
        break
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    errorMessage.value = error.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

const loadFollowedUsers = async () => {
  try {
    const response = await followApi.getUserFollowedUsers(currentPage.value - 1, pageSize)
    const page = unwrapPage(response)
    followedUsers.value = page.content
    pagination.users.total = page.totalElements
    pagination.users.pages = page.totalPages
  } catch (error) {
    console.error('加载关注用户失败:', error)
    errorMessage.value = error.message || '加载关注用户失败'
    followedUsers.value = []
  }
}

const loadFollowedQuestions = async () => {
  try {
    const response = await followApi.getUserFollowedQuestions(currentPage.value - 1, pageSize)
    const page = unwrapPage(response)
    followedQuestions.value = page.content
    pagination.questions.total = page.totalElements
    pagination.questions.pages = page.totalPages
  } catch (error) {
    console.error('加载关注问题失败:', error)
    errorMessage.value = error.message || '加载关注问题失败'
    followedQuestions.value = []
  }
}

const loadFollowers = async () => {
  try {
    const response = await followApi.getUserFollowers(currentPage.value - 1, pageSize)
    const page = unwrapPage(response)
    followers.value = page.content
    pagination.followers.total = page.totalElements
    pagination.followers.pages = page.totalPages
  } catch (error) {
    console.error('加载粉丝列表失败:', error)
    errorMessage.value = error.message || '加载粉丝列表失败'
    followers.value = []
  }
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadData()
  }
}

const handleFollowChanged = (event) => {
  if (event.needLogin) {
    // 处理需要登录的情况
    return
  }
  
  if (event.error) {
    console.error('关注操作失败:', event.error)
    return
  }
  
  // 重新加载当前数据
  loadData()
}

// 监听标签切换
watch(activeTab, () => {
  currentPage.value = 1
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-follows-page {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  color: #333;
  margin-bottom: 0.5rem;
}

.tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e1e5e9;
}

.tab-btn {
  padding: 1rem 1.5rem;
  border: none;
  background: none;
  color: #6c757d;
  font-size: 1rem;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #007bff;
}

.tab-btn.active {
  color: #007bff;
  border-bottom-color: #007bff;
}

.count {
  font-size: 0.875rem;
  opacity: 0.7;
}

.content-list {
  background: white;
  border-radius: 0.5rem;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  border: 1px solid #dc3545;
  border-radius: 0.5rem;
  color: #842029;
  background: #f8d7da;
}

.loading, .empty {
  text-align: center;
  padding: 3rem;
  color: #6c757d;
}

.empty .fa-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.3;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #e1e5e9;
  border-radius: 0.5rem;
  transition: all 0.2s ease;
}

.user-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.user-avatar img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
}

.user-info h3 {
  margin: 0 0 0.5rem 0;
}

.user-info h3 a {
  color: #333;
  text-decoration: none;
}

.user-info h3 a:hover {
  color: #007bff;
}

.user-bio {
  color: #6c757d;
  font-size: 0.875rem;
  margin: 0 0 0.5rem 0;
  line-height: 1.4;
}

.user-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.75rem;
  color: #6c757d;
}

.question-item {
  padding: 1.5rem 0;
  border-bottom: 1px solid #e1e5e9;
  position: relative;
}

.question-item:last-child {
  border-bottom: none;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.question-header h3 {
  margin: 0;
  flex: 1;
}

.question-header h3 a {
  color: #333;
  text-decoration: none;
}

.question-header h3 a:hover {
  color: #007bff;
}

.date {
  color: #6c757d;
  font-size: 0.875rem;
  white-space: nowrap;
  margin-left: 1rem;
}

.question-content {
  color: #6c757d;
  margin: 0.5rem 0;
  line-height: 1.5;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.question-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: #6c757d;
}

.question-stats span {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.question-actions {
  position: absolute;
  top: 1rem;
  right: 0;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
}

.page-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #e1e5e9;
  background: white;
  color: #6c757d;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: #007bff;
  color: #007bff;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #6c757d;
  font-size: 0.875rem;
}
</style>
