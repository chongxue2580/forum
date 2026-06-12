<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserFollowers from './UserFollowers.vue'
import UserFollowing from './UserFollowing.vue'
import UserQuestions from './UserQuestions.vue'
import UserAnswers from './UserAnswers.vue'

const store = useStore()
const route = useRoute()
const router = useRouter()

// 从路由参数获取用户ID
const userId = computed(() => {
  return route.params.id ? String(route.params.id) : ''
})

// 判断是否为当前用户的个人主页
const isCurrentUser = computed(() => {
  const loggedInUserId = store.state.user?.id;
  const profileUserId = userId.value;

  return loggedInUserId && profileUserId && String(loggedInUserId) === String(profileUserId);
})

// 判断是否登录
const isLoggedIn = computed(() => store.state.isAuthenticated);

// 初始化用户数据，基于路由参数
const user = ref(null)

const isLoading = ref(true)
const loadError = ref('')
const isEditing = ref(false)
const editForm = ref({
  name: '',
  bio: '',
  email: ''
})

const tabs = ref([
  { key: 'articles', name: '文章', count: 0 },
  { key: 'questions', name: '问答', count: 0 },
  { key: 'answers', name: '回答', count: 0 },
  { key: 'followers', name: '关注者', count: 0 },
  { key: 'following', name: '关注', count: 0 }
])

const activeTab = ref('articles')

// 用户内容数据
const userArticles = ref([])
const userQuestions = ref([])
const userAnswers = ref([])
const contentLoading = ref(false)

const createEmptyStats = () => ({
  articles: 0,
  questions: 0,
  answers: 0,
  followers: 0,
  following: 0
})

const updateTabCounts = () => {
  const stats = user.value?.stats || createEmptyStats()
  tabs.value = [
    { key: 'articles', name: '文章', count: stats.articles || 0 },
    { key: 'questions', name: '问答', count: stats.questions || 0 },
    { key: 'answers', name: '回答', count: stats.answers || 0 },
    { key: 'followers', name: '关注者', count: stats.followers || 0 },
    { key: 'following', name: '关注', count: stats.following || 0 }
  ];
}

const normalizeStats = (userStats, followStats) => {
  const statsData = userStats?.data || userStats || {}
  const normalized = {
    articles: statsData.articles || statsData.articleCount || 0,
    questions: statsData.questions || statsData.questionCount || 0,
    answers: statsData.answers || statsData.answerCount || 0,
    followers: statsData.followers || statsData.followerCount || 0,
    following: statsData.following || statsData.followingCount || 0
  }

  if (followStats) {
    const followData = followStats.data || followStats
    normalized.followers = followData.followers || normalized.followers
    normalized.following = followData.followingUsers || followData.following || normalized.following
  }

  return normalized
}

const normalizeFollowStatus = (value) => {
  const data = value?.data || value
  if (typeof data === 'boolean') return data
  return Boolean(data?.isFollowing ?? data?.isFollowed)
}

// 组件挂载时获取用户数据
onMounted(() => {
  getUserData();
});

// 监听路由参数变化，重新获取用户数据
watch(() => route.params.id, (newId, oldId) => {
  if (newId !== oldId) {
    getUserData();
  }
});

// 监听标签切换，按需加载内容
watch(activeTab, (newTab) => {
  // 如果切换到内容标签页且还没有加载过内容，则加载
  if (['articles', 'questions', 'answers'].includes(newTab) && !contentLoading.value) {
    if ((newTab === 'articles' && userArticles.value.length === 0) ||
        (newTab === 'questions' && userQuestions.value.length === 0) ||
        (newTab === 'answers' && userAnswers.value.length === 0)) {
      getUserContent();
    }
  }
});

// 获取用户数据
const getUserData = async () => {
  try {
    isLoading.value = true;
    loadError.value = ''; // 清除之前的错误
    user.value = null;
    userArticles.value = [];
    userQuestions.value = [];
    userAnswers.value = [];

    if (!userId.value) {
      throw new Error('缺少用户ID参数');
    }

    const params = { userId: userId.value };
    const requests = [
      store.dispatch('fetchUserProfile', params),
      store.dispatch('fetchUserStats', params),
      isLoggedIn.value && !isCurrentUser.value
        ? store.dispatch('checkFollowStatus', { targetUserId: userId.value })
        : Promise.resolve(false)
    ];

    if (isCurrentUser.value && isLoggedIn.value) {
      requests.push(store.dispatch('fetchFollowStats'));
    }

    const [userData, userStats, followStatus, followStats] = await Promise.all(requests);

    if (!userData) {
      throw new Error('用户资料不存在');
    }

    user.value = {
      ...userData,
      id: userData.id || userId.value,
      name: userData.nickname || userData.username || userData.name || '用户',
      email: userData.email || '',
      avatar: userData.avatarUrl || userData.avatar || '',
      bio: userData.bio || '这个人很懒，什么都没写',
      joinTime: userData.createdAt || userData.joinTime || null,
      articles: userData.articles || [],
      stats: normalizeStats(userStats, followStats),
      isFollowing: normalizeFollowStatus(followStatus)
    };

    updateTabCounts();

    // 获取用户内容数据
    await getUserContent();

  } catch (error) {
    console.error('Failed to fetch user data:', error);
    loadError.value = error.message || '获取用户资料失败';
    user.value = null;
  } finally {
    isLoading.value = false;
  }
}

// 获取用户内容数据
const getUserContent = async () => {
  if (!userId.value) return;

  try {
    contentLoading.value = true;

    // 并行获取用户的文章、问答和回答
    const [articles, questions, answers] = await Promise.all([
      store.dispatch('getUserArticles', { userId: userId.value, page: 0, size: 10 }),
      store.dispatch('getUserQuestions', { userId: userId.value, page: 0, size: 10 }),
      store.dispatch('getUserAnswers', { userId: userId.value, page: 0, size: 10 })
    ]);

    // 处理文章数据
    if (articles && articles.content) {
      userArticles.value = articles.content;
    } else if (Array.isArray(articles)) {
      userArticles.value = articles;
    } else {
      userArticles.value = [];
    }

    // 处理问答数据
    if (questions && questions.content) {
      userQuestions.value = questions.content;
    } else if (Array.isArray(questions)) {
      userQuestions.value = questions;
    } else {
      userQuestions.value = [];
    }

    // 处理回答数据
    if (answers && answers.content) {
      userAnswers.value = answers.content;
    } else if (Array.isArray(answers)) {
      userAnswers.value = answers;
    } else {
      userAnswers.value = [];
    }

  } catch (error) {
    console.error('Failed to fetch user content:', error);
  } finally {
    contentLoading.value = false;
  }
}

const startEdit = () => {
  editForm.value = {
    name: user.value.name,
    bio: user.value.bio,
    email: user.value.email
  }
  isEditing.value = true
}

const saveProfile = async () => {
  try {
    const payload = {
      nickname: editForm.value.name,
      bio: editForm.value.bio,
      email: editForm.value.email,
      avatarUrl: user.value.avatar
    }

    const updatedUser = await store.dispatch('updateUserProfile', payload)
    user.value = {
      ...user.value,
      ...updatedUser,
      name: updatedUser.nickname || updatedUser.name || editForm.value.name,
      bio: updatedUser.bio ?? editForm.value.bio,
      email: updatedUser.email ?? editForm.value.email,
      avatar: updatedUser.avatarUrl || updatedUser.avatar || user.value.avatar
    }
    isEditing.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) {
    console.error('Failed to save profile:', error)
    ElMessage.error(error.message || '保存个人资料失败')
  }
}

const cancelEdit = () => {
  isEditing.value = false
}

// 关注用户
const isFollowLoading = ref(false)

const followUser = async () => {
  // 如果未登录，跳转到登录页面
  if (!isLoggedIn.value) {
    router.push({
      path: '/login',
      query: { redirect: `/user/${userId.value}` }
    });
    return;
  }

  try {
    isFollowLoading.value = true;

    // 使用新的关注API
    const result = await store.dispatch('toggleFollowTarget', {
      targetType: 'USER',
      targetId: user.value.id
    });

    if (result && result.success) {
      user.value.isFollowing = result.isFollowed;
      user.value.stats.followers += result.isFollowed ? 1 : -1;

      // 如果是当前用户的资料页面，刷新关注统计
      if (isCurrentUser.value) {
        try {
          const followStats = await store.dispatch('fetchFollowStats');
          if (followStats) {
            user.value.stats.following = followStats.followingUsers || 0;
            user.value.stats.followers = followStats.followers || 0;

            // 更新标签计数
            tabs.value = [
              { key: 'articles', name: '文章', count: user.value.stats?.articles || 0 },
              { key: 'questions', name: '问答', count: user.value.stats?.questions || 0 },
              { key: 'answers', name: '回答', count: user.value.stats?.answers || 0 },
              { key: 'followers', name: '关注者', count: user.value.stats?.followers || 0 },
              { key: 'following', name: '关注', count: user.value.stats?.following || 0 }
            ];
          }
        } catch (error) {
          console.error('刷新关注统计失败:', error);
        }
      }

      ElMessage.success(result.message || '操作成功')
    } else {
      ElMessage.error(result?.message || '关注操作失败');
    }
  } catch (error) {
    console.error('关注操作失败:', error);
    ElMessage.error(error.message || '关注操作失败')
  } finally {
    isFollowLoading.value = false;
  }
}

const goToSettings = () => {
  router.push('/settings')
}

const formatDate = (dateValue) => {
  if (!dateValue) return ''

  try {
    let date

    // 处理数组格式的日期（Spring Boot LocalDateTime）
    if (Array.isArray(dateValue)) {
      if (dateValue.length >= 3) {
        const [year, month, day] = dateValue
        date = new Date(year, month - 1, day) // JavaScript月份从0开始
      } else {
        return '日期格式无效'
      }
    } else if (typeof dateValue === 'string') {
      // 处理字符串格式的日期
      if (dateValue.includes('T')) {
        date = new Date(dateValue)
      } else if (dateValue.includes('-')) {
        date = new Date(dateValue.replace(' ', 'T'))
      } else {
        date = new Date(dateValue)
      }
    } else {
      date = new Date(dateValue)
    }

    if (isNaN(date.getTime())) {
      return '日期无效'
    }

    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
  } catch (error) {
    console.error('Date formatting error:', error, 'for date:', dateValue)
    return '日期格式错误'
  }
}

const sendMessage = () => {
  router.push('/messages');
}

// 获取作者首字母
const getAuthorInitials = (name) => {
  if (!name) return '匿';
  return name.charAt(0);
};

const formatWebsite = (url) => {
  if (!url) return '';
  return url.replace(/^https?:\/\/(www\.)?/, '').replace(/\/$/, '');
};
</script>

<template>
  <div class="user-profile">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>加载用户资料中...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="loadError" class="error-state">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
      <h3>加载用户资料失败</h3>
      <p>{{ loadError }}</p>
      <button class="retry-btn" @click="getUserData">重试</button>
    </div>
    
    <template v-else-if="user">
      <!-- 是否为当前用户的标记 -->
      <div v-if="isCurrentUser" class="current-user-banner">
        <font-awesome-icon :icon="['fas', 'user-check']" />
        <span>这是您的个人主页</span>
      </div>

      <!-- 个人资料卡片 -->
      <div class="profile-card" :class="{ 'is-current-user': isCurrentUser }">
        <div class="profile-header">
          <div class="profile-avatar">
            <img v-if="user?.avatar" :src="user.avatar" :alt="user?.name">
            <div v-else class="default-avatar">{{ getAuthorInitials(user?.name) }}</div>
          </div>
          <div class="profile-info">
            <div class="info-header">
              <h1 class="user-name">{{ user?.name || '用户' }}</h1>
              <div class="action-buttons">
                <template v-if="isCurrentUser">
                  <button class="action-btn" @click="goToSettings">
                    <font-awesome-icon :icon="['fas', 'cog']" />
                    <span>设置</span>
                  </button>
                  <button class="action-btn" @click="startEdit">
                    <font-awesome-icon :icon="['fas', 'edit']" />
                    <span>编辑资料</span>
                  </button>
                </template>
                <template v-else>
                  <button class="follow-btn" @click="followUser" :class="{ following: user.isFollowing, loading: isFollowLoading }">
                    <font-awesome-icon :icon="['fas', user.isFollowing ? 'user-check' : 'user-plus']" />
                    <span>{{ user.isFollowing ? '已关注' : '关注' }}</span>
                  </button>
                  <button class="action-btn" @click="sendMessage">
                    <font-awesome-icon :icon="['fas', 'envelope']" />
                    <span>消息中心</span>
                  </button>
                </template>
              </div>
            </div>
            
            <p class="user-bio">{{ user?.bio || '这个人很懒，什么都没写' }}</p>
            
            <div class="user-meta">
              <div class="meta-item" v-if="user?.location">
                <font-awesome-icon :icon="['fas', 'map-marker-alt']" />
                <span>{{ user.location }}</span>
              </div>
              <div class="meta-item" v-if="user?.email">
                <font-awesome-icon :icon="['fas', 'envelope']" />
                <span>{{ user.email }}</span>
              </div>
              <div class="meta-item" v-if="user?.joinTime">
                <font-awesome-icon :icon="['fas', 'calendar-alt']" />
                <span>加入于 {{ formatDate(user.joinTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="user-stats">
          <div class="stat-item">
            <div class="stat-value">{{ user.stats?.articles || 0 }}</div>
            <div class="stat-label">文章</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ user.stats?.questions || 0 }}</div>
            <div class="stat-label">问题</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ user.stats?.answers || 0 }}</div>
            <div class="stat-label">回答</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ user.stats?.followers || 0 }}</div>
            <div class="stat-label">关注者</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ user.stats?.following || 0 }}</div>
            <div class="stat-label">关注</div>
          </div>
        </div>
      </div>
      
      <!-- 编辑表单 -->
      <div v-if="isEditing" class="edit-form">
        <h3>编辑个人资料</h3>
        <div class="form-group">
          <label for="name">昵称</label>
          <input type="text" id="name" v-model="editForm.name">
        </div>
        <div class="form-group">
          <label for="bio">个人简介</label>
          <textarea id="bio" v-model="editForm.bio" rows="3"></textarea>
        </div>
        <div class="form-group">
          <label for="email">电子邮箱</label>
          <input type="email" id="email" v-model="editForm.email">
        </div>
        <div class="form-actions">
          <button class="cancel-btn" @click="cancelEdit">取消</button>
          <button class="save-btn" @click="saveProfile">保存</button>
        </div>
      </div>
      
      <!-- 内容标签页 -->
      <div v-else class="content-tabs">
        <div class="tabs-header">
          <button 
            v-for="tab in tabs" 
            :key="tab.key" 
            class="tab-button" 
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.name }} <span class="tab-count">{{ tab.count }}</span>
          </button>
        </div>
        
        <div class="tab-content">
          <!-- 文章标签页 -->
          <div v-if="activeTab === 'articles'" class="articles-tab">
            <div v-if="contentLoading" class="loading-state">
              <font-awesome-icon :icon="['fas', 'spinner']" spin />
              <span>加载中...</span>
            </div>
            <div v-else-if="userArticles && userArticles.length" class="article-list">
              <div v-for="article in userArticles" :key="article.id" class="article-item">
                <div class="article-header">
                  <router-link :to="`/article/${article.id}`" class="article-title">{{ article.title }}</router-link>
                  <span class="article-time">{{ formatDate(article.createTime) }}</span>
                </div>
                <p class="article-content">{{ article.summary }}</p>
                <div class="article-footer">
                  <div class="article-tags" v-if="article.tags && article.tags.length">
                    <span v-for="tag in article.tags" :key="tag" class="tag">
                      <font-awesome-icon :icon="['fas', 'tag']" />
                      {{ tag }}
                    </span>
                  </div>
                  <div class="article-stats">
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'eye']" />
                      {{ article.views || 0 }} 浏览
                    </span>
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'thumbs-up']" />
                      {{ article.likes || 0 }} 点赞
                    </span>
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'comment']" />
                      {{ article.comments || 0 }} 评论
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <font-awesome-icon :icon="['fas', 'file-alt']" class="empty-icon" />
              <p>暂无文章</p>
            </div>
          </div>
          
          <!-- 问答标签页 -->
          <div v-else-if="activeTab === 'questions'" class="questions-tab">
            <div v-if="contentLoading" class="loading-state">
              <font-awesome-icon :icon="['fas', 'spinner']" spin />
              <span>加载中...</span>
            </div>
            <div v-else-if="userQuestions && userQuestions.length" class="question-list">
              <div v-for="question in userQuestions" :key="question.id" class="question-item">
                <div class="question-header">
                  <router-link :to="`/question/${question.id}`" class="question-title">{{ question.title }}</router-link>
                  <span class="question-time">{{ formatDate(question.createdAt) }}</span>
                </div>
                <p class="question-content">{{ question.content || question.summary }}</p>
                <div class="question-footer">
                  <div class="question-stats">
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'eye']" />
                      {{ question.viewCount || 0 }} 浏览
                    </span>
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'thumbs-up']" />
                      {{ question.likeCount || 0 }} 点赞
                    </span>
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'comment']" />
                      {{ question.commentCount || 0 }} 回答
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <font-awesome-icon :icon="['fas', 'question-circle']" class="empty-icon" />
              <p>暂无问答</p>
            </div>
          </div>

          <!-- 回答标签页 -->
          <div v-else-if="activeTab === 'answers'" class="answers-tab">
            <div v-if="contentLoading" class="loading-state">
              <font-awesome-icon :icon="['fas', 'spinner']" spin />
              <span>加载中...</span>
            </div>
            <div v-else-if="userAnswers && userAnswers.length" class="answer-list">
              <div v-for="answer in userAnswers" :key="answer.id" class="answer-item">
                <div class="answer-header">
                  <span class="answer-title">回答了问题</span>
                  <span class="answer-time">{{ formatDate(answer.createdAt) }}</span>
                </div>
                <p class="answer-content">{{ answer.content }}</p>
                <div class="answer-footer">
                  <div class="answer-stats">
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'thumbs-up']" />
                      {{ answer.likeCount || 0 }} 点赞
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <font-awesome-icon :icon="['fas', 'comment']" class="empty-icon" />
              <p>暂无回答</p>
            </div>
          </div>
          
          <!-- 关注者标签页 -->
          <div v-else-if="activeTab === 'followers'" class="followers-tab">
            <user-followers :user-id="userId" />
          </div>
          
          <!-- 关注标签页 -->
          <div v-else-if="activeTab === 'following'" class="following-tab">
            <user-following :user-id="userId" />
          </div>
        </div>
      </div>
    </template>

    <!-- 用户数据为空时的默认状态 -->
    <div v-else class="empty-state">
      <font-awesome-icon :icon="['fas', 'user-slash']" class="empty-icon" />
      <h3>用户信息不可用</h3>
      <p>无法加载用户资料，请稍后再试</p>
      <button class="retry-btn" @click="getUserData">重新加载</button>
    </div>
  </div>
</template>

<style scoped>
.user-profile {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top: 4px solid var(--primary-color);
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-icon {
  font-size: 3rem;
  color: #ff5252;
  margin-bottom: 1rem;
}

.retry-btn {
  margin-top: 1rem;
  padding: 0.5rem 1.5rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-btn:hover {
  background-color: var(--primary-dark);
}

.current-user-banner {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.profile-card {
  background-color: white;
  border-radius: 0.8rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 2rem;
  margin-bottom: 2rem;
}

.profile-card.is-current-user {
  border-left: 4px solid var(--primary-color);
}

.profile-header {
  display: flex;
  gap: 2rem;
  margin-bottom: 1.5rem;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--primary-color);
  color: white;
  font-size: 3rem;
  font-weight: 600;
}

.profile-info {
  flex: 1;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.user-name {
  font-size: 1.8rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 0.75rem;
}

.action-btn, .follow-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: none;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn {
  background-color: #f0f2f5;
  color: #666;
}

.action-btn:hover {
  background-color: #e0e0e0;
}

.follow-btn {
  background-color: var(--primary-color);
  color: white;
  font-weight: 500;
}

.follow-btn:hover {
  background-color: var(--primary-dark);
}

.follow-btn.following {
  background-color: #f0f2f5;
  color: #666;
}

.follow-btn.following:hover {
  background-color: #ff5252;
  color: white;
}

.follow-btn.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.user-bio {
  color: #666;
  margin: 0 0 1rem;
  line-height: 1.6;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  font-size: 0.9rem;
}

.meta-item a {
  color: var(--primary-color);
  text-decoration: none;
}

.meta-item a:hover {
  text-decoration: underline;
}

.user-stats {
  display: flex;
  border-top: 1px solid #eee;
  padding-top: 1.5rem;
}

.user-stats .stat-item {
  flex: 1;
  text-align: center;
  padding: 0 0.5rem;
  border-right: 1px solid #eee;
}

.user-stats .stat-item:last-child {
  border-right: none;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 0.25rem;
}

.stat-label {
  color: #999;
  font-size: 0.9rem;
}

.edit-form {
  background-color: white;
  border-radius: 0.8rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 2rem;
  margin-bottom: 2rem;
}

.edit-form h3 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #333;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #666;
  font-weight: 500;
}

.form-group input, .form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-group input:focus, .form-group textarea:focus {
  border-color: var(--primary-color);
  outline: none;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}

.cancel-btn, .save-btn {
  padding: 0.5rem 1.5rem;
  border-radius: 0.5rem;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background-color: #f0f2f5;
  color: #666;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
}

.save-btn {
  background-color: var(--primary-color);
  color: white;
}

.save-btn:hover {
  background-color: var(--primary-dark);
}

.content-tabs {
  background-color: white;
  border-radius: 0.8rem;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.tabs-header {
  display: flex;
  border-bottom: 1px solid #eee;
  overflow-x: auto;
}

.tab-button {
  padding: 1rem 1.5rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: #666;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-button:hover {
  color: var(--primary-color);
}

.tab-button.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

.tab-count {
  display: inline-block;
  background-color: #f0f2f5;
  color: #666;
  border-radius: 50%;
  padding: 0.1rem 0.5rem;
  font-size: 0.8rem;
  margin-left: 0.5rem;
}

.tab-button.active .tab-count {
  background-color: var(--primary-color);
  color: white;
}

.tab-content {
  padding: 2rem;
  min-height: 300px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: #999;
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.article-item {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.25rem;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--border-color);
}

.article-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-light);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.article-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-color);
  text-decoration: none;
  flex: 1;
  margin-right: 1rem;
  line-height: 1.4;
}

.article-title:hover {
  color: var(--primary-color);
}

.article-time {
  font-size: 0.85rem;
  color: var(--text-light);
  white-space: nowrap;
}

.article-content {
  color: var(--text-light);
  margin-bottom: 1rem;
  line-height: 1.5;
  font-size: 0.95rem;
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 0.25rem 0.5rem;
  border-radius: 50px;
  font-size: 0.75rem;
  transition: var(--transition);
}

.tag:hover {
  background-color: var(--primary-color);
  color: white;
}

.article-stats {
  display: flex;
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--text-lighter);
  font-size: 0.85rem;
}

/* 问答列表样式 - 和文章保持一致 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.question-item {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.25rem;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--border-color);
}

.question-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-light);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
  gap: 1rem;
}

.question-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  text-decoration: none;
  line-height: 1.4;
  flex: 1;
}

.question-title:hover {
  color: var(--primary);
}

.question-time {
  color: var(--text-lighter);
  font-size: 0.85rem;
  white-space: nowrap;
}

.question-content {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-stats {
  display: flex;
  gap: 1rem;
}

/* 回答列表样式 - 和文章、问答保持一致 */
.answer-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.answer-item {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.25rem;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--border-color);
}

.answer-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-light);
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
  gap: 1rem;
}

.answer-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  flex: 1;
}

.answer-time {
  color: var(--text-lighter);
  font-size: 0.85rem;
  white-space: nowrap;
}

.answer-content {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.answer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.answer-stats {
  display: flex;
  gap: 1rem;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 1rem;
  }
  
  .info-header {
    flex-direction: column;
    align-items: center;
  }
  
  .user-meta {
    justify-content: center;
  }
  
  .user-stats {
    flex-wrap: wrap;
  }
  
  .user-stats .stat-item {
    flex-basis: 33.333%;
    padding: 0.75rem 0;
    border-right: none;
    border-bottom: 1px solid #eee;
  }
  
  .tabs-header {
    justify-content: flex-start;
  }
  
  .article-header {
    flex-direction: column;
  }
  
  .article-time {
    margin-top: 0.5rem;
  }
  
  .article-footer {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .article-stats {
    margin-top: 0.5rem;
    width: 100%;
    justify-content: space-between;
  }

  .question-header {
    flex-direction: column;
  }

  .question-time {
    margin-top: 0.5rem;
  }

  .question-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .question-stats {
    margin-top: 0.5rem;
    width: 100%;
    justify-content: space-between;
  }

  .answer-header {
    flex-direction: column;
  }

  .answer-time {
    margin-top: 0.5rem;
  }

  .answer-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .answer-stats {
    margin-top: 0.5rem;
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 576px) {
  .action-buttons {
    width: 100%;
    justify-content: center;
  }
  
  .user-stats .stat-item {
    flex-basis: 50%;
  }
  
  .tab-button {
    padding: 0.75rem 1rem;
  }
}
</style> 
