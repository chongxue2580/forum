<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserFollowers from './UserFollowers.vue'
import UserFollowing from './UserFollowing.vue'
import UserQuestions from './UserQuestions.vue'
import UserAnswers from './UserAnswers.vue'
import { avatarInitial, resolveAvatarUrl } from '../utils/avatar'

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

const createContentErrorState = () => ({
  articles: '',
  questions: '',
  answers: ''
})

const createContentLoadedState = () => ({
  articles: false,
  questions: false,
  answers: false
})

const contentErrors = ref(createContentErrorState())
const contentLoaded = ref(createContentLoadedState())

const contentTabKeys = ['articles', 'questions', 'answers']
const contentLabels = {
  articles: '文章',
  questions: '问答',
  answers: '回答'
}

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

const normalizePageContent = (response) => {
  const data = response?.data || response || {}
  if (Array.isArray(data)) return data
  if (Array.isArray(data.content)) return data.content
  return []
}

const setContentItems = (tab, items) => {
  if (tab === 'articles') {
    userArticles.value = items
  } else if (tab === 'questions') {
    userQuestions.value = items
  } else if (tab === 'answers') {
    userAnswers.value = items
  }
}

// 文章审核状态标签（仅对非"已通过"显示）
const ARTICLE_STATUS_LABELS = {
  PENDING: '待审核',
  REJECTED: '已拒绝',
  DRAFT: '草稿'
}
const articleStatusLabel = (status) => ARTICLE_STATUS_LABELS[String(status || '').toUpperCase()] || ''

const contentLoaders = {
  articles: () => store.dispatch('getUserArticles', { userId: userId.value, page: 0, size: 10 }),
  questions: () => store.dispatch('getUserQuestions', { userId: userId.value, page: 0, size: 10 }),
  answers: () => store.dispatch('getUserAnswers', { userId: userId.value, page: 0, size: 10 })
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
  if (contentTabKeys.includes(newTab) && !contentLoading.value && !contentLoaded.value[newTab]) {
    getUserContent(newTab);
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
    contentErrors.value = createContentErrorState();
    contentLoaded.value = createContentLoadedState();

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
      avatar: resolveAvatarUrl(userData.avatarUrl || userData.avatar || ''),
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
    loadError.value = error.message || '获取用户资料失败';
    user.value = null;
  } finally {
    isLoading.value = false;
  }
}

// 获取用户内容数据
const getUserContent = async (tabsToLoad = contentTabKeys) => {
  if (!userId.value) return;

  const requestedTabs = Array.isArray(tabsToLoad) ? tabsToLoad : [tabsToLoad];
  const targetTabs = requestedTabs.filter(tab => contentTabKeys.includes(tab));
  if (!targetTabs.length) return;

  try {
    contentLoading.value = true;

    await Promise.all(targetTabs.map(async (tab) => {
      contentErrors.value[tab] = '';

      try {
        const response = await contentLoaders[tab]();
        setContentItems(tab, normalizePageContent(response));
        contentLoaded.value[tab] = true;
      } catch (error) {
        setContentItems(tab, []);
        contentErrors.value[tab] = error.message || `${contentLabels[tab]}加载失败`;
        contentLoaded.value[tab] = true;
      }
    }));
  } finally {
    contentLoading.value = false;
  }
}

const retryContent = (tab) => {
  getUserContent(tab);
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
      avatar: resolveAvatarUrl(updatedUser.avatarUrl || updatedUser.avatar || user.value.avatar)
    }
    isEditing.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) {
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
        } catch {
          // 关注状态已更新，统计刷新失败不阻塞主操作。
        }
      }

      ElMessage.success(result.message || '操作成功')
    } else {
      ElMessage.error(result?.message || '关注操作失败');
    }
  } catch (error) {
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
  } catch {
    return '日期格式错误'
  }
}

const sendMessage = () => {
  const targetId = user.value?.id || userId.value
  if (!targetId) {
    router.push('/messages')
    return
  }
  router.push({
    path: '/messages',
    query: { userId: String(targetId), userName: user.value?.name || '' }
  })
}

// 获取作者首字母
const getAuthorInitials = (name) => {
  return avatarInitial(name || '匿');
};

const formatWebsite = (url) => {
  if (!url) return '';
  return url.replace(/^https?:\/\/(www\.)?/, '').replace(/\/$/, '');
};
</script>

<template>
  <div class="user-profile">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin class="spinner-icon" />
      <p>加载用户资料中...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="loadError" class="error-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="error-icon" />
      <h3>加载用户资料失败</h3>
      <p>{{ loadError }}</p>
      <button class="retry-btn kumo-button" @click="getUserData">重试</button>
    </div>
    
    <template v-else-if="user">
      <!-- 是否为当前用户的标记 -->
      <div v-if="isCurrentUser" class="current-user-banner kumo-surface">
        <font-awesome-icon :icon="['fas', 'user-check']" />
        <span>这是您的个人主页</span>
      </div>

      <!-- 个人资料卡片 -->
      <div class="profile-card kumo-surface-strong" :class="{ 'is-current-user': isCurrentUser }">
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
                  <button class="action-btn kumo-button" @click="goToSettings">
                    <font-awesome-icon :icon="['fas', 'cog']" />
                    <span>设置</span>
                  </button>
                  <button class="action-btn kumo-button" @click="startEdit">
                    <font-awesome-icon :icon="['fas', 'edit']" />
                    <span>编辑资料</span>
                  </button>
                </template>
                <template v-else>
                  <button class="follow-btn kumo-button" @click="followUser" :class="{ following: user.isFollowing, loading: isFollowLoading }">
                    <font-awesome-icon :icon="['fas', user.isFollowing ? 'user-check' : 'user-plus']" />
                    <span>{{ user.isFollowing ? '已关注' : '关注' }}</span>
                  </button>
                  <button class="action-btn kumo-button" @click="sendMessage">
                    <font-awesome-icon :icon="['fas', 'envelope']" />
                    <span>私信</span>
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
      <div v-if="isEditing" class="edit-form kumo-surface">
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
          <button class="cancel-btn kumo-button" @click="cancelEdit">取消</button>
          <button class="save-btn kumo-button kumo-button--brand" @click="saveProfile">保存</button>
        </div>
      </div>
      
      <!-- 内容标签页 -->
      <div v-else class="content-tabs kumo-surface">
        <div class="tabs-header kumo-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.key" 
            class="tab-button kumo-tab"
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
            <div v-else-if="contentErrors.articles" class="empty-state content-error-state">
              <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="empty-icon" />
              <h3>文章加载失败</h3>
              <p>{{ contentErrors.articles }}</p>
              <button class="retry-btn kumo-button" @click="retryContent('articles')">重新加载</button>
            </div>
            <div v-else-if="userArticles && userArticles.length" class="article-list">
              <div v-for="(article, index) in userArticles" :key="article.id" class="article-item kumo-surface magnetic-card stagger-item" :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }">
                <div class="article-header">
                  <router-link :to="`/article/${article.id}`" class="article-title">{{ article.title }}</router-link>
                  <span
                    v-if="articleStatusLabel(article.status)"
                    class="article-status"
                    :class="'status-' + String(article.status || '').toLowerCase()"
                  >{{ articleStatusLabel(article.status) }}</span>
                  <span class="article-time">{{ formatDate(article.createTime || article.publishedAt || article.createdAt) }}</span>
                </div>
                <p class="article-content">{{ article.summary }}</p>
                <div class="article-footer">
                  <div class="article-tags" v-if="article.tags && article.tags.length">
                    <span v-for="tag in article.tags" :key="tag.id || tag" class="tag">
                      <font-awesome-icon :icon="['fas', 'tag']" />
                      {{ tag.name || tag }}
                    </span>
                  </div>
                  <div class="article-stats">
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'eye']" />
                      {{ article.views ?? article.viewCount ?? 0 }} 浏览
                    </span>
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'thumbs-up']" />
                      {{ article.likes ?? article.likeCount ?? 0 }} 点赞
                    </span>
                    <span class="stat-item">
                      <font-awesome-icon :icon="['fas', 'comment']" />
                      {{ article.comments ?? article.commentCount ?? 0 }} 评论
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <font-awesome-icon :icon="['fas', 'file-alt']" class="empty-icon" />
              <h3>暂无文章</h3>
              <p>{{ isCurrentUser ? '你还没有发布文章' : '该用户还没有发布文章' }}</p>
            </div>
          </div>
          
          <!-- 问答标签页 -->
          <div v-else-if="activeTab === 'questions'" class="questions-tab">
            <div v-if="contentLoading" class="loading-state">
              <font-awesome-icon :icon="['fas', 'spinner']" spin />
              <span>加载中...</span>
            </div>
            <div v-else-if="contentErrors.questions" class="empty-state content-error-state">
              <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="empty-icon" />
              <h3>问答加载失败</h3>
              <p>{{ contentErrors.questions }}</p>
              <button class="retry-btn kumo-button" @click="retryContent('questions')">重新加载</button>
            </div>
            <div v-else-if="userQuestions && userQuestions.length" class="question-list">
              <div v-for="(question, index) in userQuestions" :key="question.id" class="question-item kumo-surface magnetic-card stagger-item" :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }">
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
              <h3>暂无问答</h3>
              <p>{{ isCurrentUser ? '你还没有发布问题' : '该用户还没有发布问题' }}</p>
            </div>
          </div>

          <!-- 回答标签页 -->
          <div v-else-if="activeTab === 'answers'" class="answers-tab">
            <div v-if="contentLoading" class="loading-state">
              <font-awesome-icon :icon="['fas', 'spinner']" spin />
              <span>加载中...</span>
            </div>
            <div v-else-if="contentErrors.answers" class="empty-state content-error-state">
              <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="empty-icon" />
              <h3>回答加载失败</h3>
              <p>{{ contentErrors.answers }}</p>
              <button class="retry-btn kumo-button" @click="retryContent('answers')">重新加载</button>
            </div>
            <div v-else-if="userAnswers && userAnswers.length" class="answer-list">
              <div v-for="(answer, index) in userAnswers" :key="answer.id" class="answer-item kumo-surface magnetic-card stagger-item" :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }">
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
              <h3>暂无回答</h3>
              <p>{{ isCurrentUser ? '你还没有回答问题' : '该用户还没有回答问题' }}</p>
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
    <div v-else class="empty-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'user-slash']" class="empty-icon" />
      <h3>用户信息不可用</h3>
      <p>无法加载用户资料，请稍后再试</p>
      <button class="retry-btn kumo-button" @click="getUserData">重新加载</button>
    </div>
  </div>
</template>

<style scoped>
.user-profile {
  display: grid;
  gap: 1.25rem;
}

.loading-state,
.error-state,
.empty-state {
  display: grid;
  place-items: center;
  gap: 0.75rem;
  min-height: 16rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.spinner-icon,
.empty-icon,
.error-icon {
  color: var(--kumo-bg-brand);
  font-size: 2.4rem;
}

.error-icon,
.content-error-state .empty-icon {
  color: var(--kumo-status-danger);
}

.loading-state p,
.error-state p,
.empty-state p,
.empty-state h3 {
  margin: 0;
}

.empty-state h3,
.error-state h3 {
  color: var(--kumo-text-default);
  font-size: 1.1rem;
  font-weight: 840;
}

.current-user-banner {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  gap: 0.55rem;
  padding: 0.55rem 0.85rem;
  color: var(--kumo-bg-brand-strong);
  font-weight: 780;
}

.profile-card {
  display: grid;
  gap: 1.5rem;
  padding: clamp(1.25rem, 3vw, 2rem);
}

.profile-card.is-current-user {
  border-color: var(--kumo-bg-brand);
}

.profile-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 1.4rem;
  align-items: start;
}

.profile-avatar {
  width: clamp(5.8rem, 13vw, 8rem);
  height: clamp(5.8rem, 13vw, 8rem);
  overflow: hidden;
  border: 1px solid var(--kumo-hairline-strong);
  border-radius: 50%;
  background: var(--kumo-bg-brand-soft);
  box-shadow: var(--kumo-shadow-sm);
}

.profile-avatar img,
.default-avatar {
  width: 100%;
  height: 100%;
}

.profile-avatar img {
  object-fit: cover;
}

.default-avatar {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-brand-strong));
  color: var(--kumo-text-inverse);
  font-size: 2.8rem;
  font-weight: 900;
}

.profile-info {
  min-width: 0;
}

.info-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.8rem;
}

.user-name {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(1.8rem, 5vw, 3.4rem);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: 0;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.65rem;
}

.follow-btn {
  min-width: 6.5rem;
}

.follow-btn:not(.following) {
  border-color: transparent;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-brand-strong));
  color: var(--kumo-text-inverse);
}

.follow-btn.following {
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.follow-btn.following:hover {
  border-color: var(--kumo-status-danger);
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.follow-btn.loading {
  cursor: not-allowed;
  opacity: 0.68;
}

.user-bio {
  max-width: 48rem;
  margin: 0 0 1rem;
  color: var(--kumo-text-muted);
  line-height: 1.72;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.34rem 0.65rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
  font-size: 0.86rem;
  font-weight: 700;
}

.user-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.75rem;
  padding-top: 1.2rem;
  border-top: 1px solid var(--kumo-hairline);
}

.user-stats .stat-item {
  display: grid;
  place-items: center;
  gap: 0.15rem;
  min-height: 5rem;
  padding: 0.8rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
}

.stat-value {
  color: var(--kumo-bg-brand-strong);
  font-size: clamp(1.45rem, 3vw, 2.1rem);
  font-weight: 900;
  line-height: 1;
}

.stat-label {
  font-size: 0.82rem;
  font-weight: 760;
}

.edit-form {
  display: grid;
  gap: 1.1rem;
  padding: clamp(1.1rem, 2.5vw, 1.6rem);
}

.edit-form h3 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.25rem;
  font-weight: 840;
}

.form-group {
  display: grid;
  gap: 0.45rem;
}

.form-group label {
  color: var(--kumo-text-muted);
  font-weight: 780;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font: inherit;
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.content-tabs {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.tabs-header {
  width: fit-content;
  max-width: 100%;
  overflow-x: auto;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  min-height: 1.5rem;
  padding: 0.15rem 0.45rem;
  border-radius: 999px;
  background: var(--kumo-bg-recessed);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 820;
}

.tab-button.active .tab-count {
  background: var(--kumo-bg-brand);
  color: var(--kumo-text-inverse);
}

.tab-content {
  min-height: 18rem;
}

.article-list,
.question-list,
.answer-list {
  display: grid;
  gap: 1rem;
}

.article-item,
.question-item,
.answer-item {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
}

.article-header,
.question-header,
.answer-header,
.article-footer,
.question-footer,
.answer-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
}

.article-title,
.question-title,
.answer-title {
  color: var(--kumo-text-default);
  font-size: 1.08rem;
  font-weight: 840;
  line-height: 1.35;
  text-decoration: none;
}

.article-title:hover,
.question-title:hover {
  color: var(--kumo-bg-brand-strong);
}

.article-time,
.question-time,
.answer-time {
  flex: 0 0 auto;
  color: var(--kumo-text-subtle);
  font-size: 0.84rem;
  font-weight: 700;
  white-space: nowrap;
}

.article-status {
  flex: 0 0 auto;
  padding: 0.22rem 0.58rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 820;
  white-space: nowrap;
}

.article-status.status-pending,
.article-status.status-draft {
  background: var(--kumo-status-warning-tint);
  color: var(--kumo-status-warning);
}

.article-status.status-rejected {
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.article-content,
.question-content,
.answer-content {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 0.28rem;
  padding: 0.3rem 0.55rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.76rem;
  font-weight: 760;
}

.article-stats,
.question-stats,
.answer-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.article-stats .stat-item,
.question-stats .stat-item,
.answer-stats .stat-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: var(--kumo-text-subtle);
  font-size: 0.84rem;
  font-weight: 700;
}

@media (max-width: 780px) {
  .profile-header,
  .user-stats {
    grid-template-columns: 1fr;
  }

  .profile-header,
  .info-header {
    text-align: center;
  }

  .info-header,
  .article-header,
  .question-header,
  .answer-header,
  .article-footer,
  .question-footer,
  .answer-footer,
  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .profile-avatar {
    justify-self: center;
  }

  .action-buttons,
  .user-meta {
    justify-content: center;
  }

  .tabs-header {
    width: 100%;
  }
}
</style>
