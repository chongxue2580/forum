<template>
  <div class="user-follows-page">
    <ui-page-hero
      title="我的关注"
      description="集中管理关注的用户、问题和粉丝关系，快速回到重要讨论。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'heart']" />
          Following
        </span>
      </template>
      <template #aside>
        <div class="hero-count">
          <strong>{{ activeTotal }}</strong>
          <span>{{ activeTabLabel }}</span>
        </div>
      </template>
    </ui-page-hero>

    <section class="workspace">
      <div class="kumo-tabs" aria-label="关注类型">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="kumo-tab"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <div class="content-panel kumo-surface">
        <div v-if="errorMessage" class="notice notice-error">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
          {{ errorMessage }}
        </div>

        <div v-if="loading" class="state-panel">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>加载中...</span>
        </div>

        <template v-else-if="activeTab === 'users'">
          <div v-if="followedUsers.length === 0" class="state-panel">
            <font-awesome-icon :icon="['fas', 'user-plus']" />
            <span>您还没有关注任何用户</span>
          </div>
          <div v-else class="user-grid">
            <article
              v-for="(item, index) in followedUsers"
              :key="item.id"
              class="user-card kumo-surface magnetic-card stagger-item"
              :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
            >
              <router-link :to="`/user/${item.id}`" class="user-main">
                <span class="avatar">
                  <img :src="resolveUserAvatar(item)" :alt="item.username || '用户'">
                </span>
                <span>
                  <strong>{{ item.nickname || item.username }}</strong>
                  <small>{{ item.bio || '这个用户暂未填写简介' }}</small>
                  <em>关注于 {{ formatDate(item.followedAt) }}</em>
                </span>
              </router-link>
              <FollowButton
                target-type="USER"
                :target-id="item.id"
                :initial-followed="true"
                size="small"
                @follow-changed="handleFollowChanged"
              />
            </article>
          </div>
        </template>

        <template v-else-if="activeTab === 'questions'">
          <div v-if="followedQuestions.length === 0" class="state-panel">
            <font-awesome-icon :icon="['fas', 'question-circle']" />
            <span>您还没有关注任何问题</span>
          </div>
          <div v-else class="question-list">
            <article
              v-for="(question, index) in followedQuestions"
              :key="question.id"
              class="question-card kumo-surface magnetic-card stagger-item"
              :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
            >
              <div class="question-copy">
                <div class="question-header">
                  <router-link :to="`/question/${question.id}`">{{ question.title }}</router-link>
                  <span>{{ formatDate(question.followedAt) }}</span>
                </div>
                <p>{{ question.content }}</p>
                <div class="question-meta">
                  <span>提问者：{{ question.author || question.authorId || '未知' }}</span>
                  <span><font-awesome-icon :icon="['fas', 'eye']" /> {{ question.viewCount || 0 }}</span>
                  <span><font-awesome-icon :icon="['fas', 'comment']" /> {{ question.answerCount || question.commentCount || 0 }}</span>
                  <span><font-awesome-icon :icon="['fas', 'heart']" /> {{ question.likeCount || 0 }}</span>
                </div>
              </div>
              <FollowButton
                target-type="QUESTION"
                :target-id="question.id"
                :initial-followed="true"
                size="small"
                follow-text="关注问题"
                following-text="已关注"
                @follow-changed="handleFollowChanged"
              />
            </article>
          </div>
        </template>

        <template v-else>
          <div v-if="followers.length === 0" class="state-panel">
            <font-awesome-icon :icon="['fas', 'users']" />
            <span>您还没有粉丝</span>
          </div>
          <div v-else class="user-grid">
            <article
              v-for="(follower, index) in followers"
              :key="follower.id"
              class="user-card kumo-surface magnetic-card stagger-item"
              :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
            >
              <router-link :to="`/user/${follower.id}`" class="user-main">
                <span class="avatar">
                  <img :src="resolveUserAvatar(follower)" :alt="follower.username || '用户'">
                </span>
                <span>
                  <strong>{{ follower.nickname || follower.username }}</strong>
                  <small>{{ follower.bio || '这个用户暂未填写简介' }}</small>
                  <em>关注于 {{ formatDate(follower.followedAt) }}</em>
                </span>
              </router-link>
              <FollowButton
                target-type="USER"
                :target-id="follower.id"
                :initial-followed="follower.isFollowing || false"
                size="small"
                @follow-changed="handleFollowChanged"
              />
            </article>
          </div>
        </template>
      </div>

      <div v-if="totalPages > 1" class="pagination kumo-surface">
        <button class="kumo-button" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
          上一页
        </button>
        <span>第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        <button class="kumo-button" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { followApi } from '@/api/followApi'
import FollowButton from '@/components/FollowButton.vue'
import UiPageHero from '@/components/ui/UiPageHero.vue'
import { resolveAvatarFrom } from '@/utils/avatar'
import { formatDate as formatDateValue } from '@/utils/dateUtils'

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
  { key: 'users', label: '关注的用户', count: pagination.users.total },
  { key: 'questions', label: '关注的问题', count: pagination.questions.total },
  { key: 'followers', label: '我的粉丝', count: pagination.followers.total }
])

const totalPages = computed(() => pagination[activeTab.value]?.pages || 0)
const activeTotal = computed(() => pagination[activeTab.value]?.total || 0)
const activeTabLabel = computed(() => tabs.value.find(tab => tab.key === activeTab.value)?.label || '')

const formatDate = (dateString) => dateString ? formatDateValue(dateString) : '未知时间'
const resolveUserAvatar = (user) => resolveAvatarFrom(user) || '/assets/default-avatar.svg'

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
    if (activeTab.value === 'users') await loadFollowedUsers()
    if (activeTab.value === 'questions') await loadFollowedQuestions()
    if (activeTab.value === 'followers') await loadFollowers()
  } catch (error) {
    errorMessage.value = error.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

const loadFollowedUsers = async () => {
  const response = await followApi.getUserFollowedUsers(currentPage.value - 1, pageSize)
  const page = unwrapPage(response)
  followedUsers.value = page.content
  pagination.users.total = page.totalElements
  pagination.users.pages = page.totalPages
}

const loadFollowedQuestions = async () => {
  const response = await followApi.getUserFollowedQuestions(currentPage.value - 1, pageSize)
  const page = unwrapPage(response)
  followedQuestions.value = page.content
  pagination.questions.total = page.totalElements
  pagination.questions.pages = page.totalPages
}

const loadFollowers = async () => {
  const response = await followApi.getUserFollowers(currentPage.value - 1, pageSize)
  const page = unwrapPage(response)
  followers.value = page.content
  pagination.followers.total = page.totalElements
  pagination.followers.pages = page.totalPages
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadData()
  }
}

const handleFollowChanged = (event) => {
  if (event.needLogin) return
  if (event.error) {
    errorMessage.value = event.error
    return
  }
  loadData()
}

watch(activeTab, () => {
  currentPage.value = 1
  loadData()
})

onMounted(loadData)
</script>

<style scoped>
.user-follows-page {
  display: grid;
  gap: 1.25rem;
}

.hero-count {
  display: grid;
  gap: 0.2rem;
  padding: 1.2rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.hero-count strong {
  color: var(--kumo-bg-brand-strong);
  font-size: 3.4rem;
  font-weight: 900;
  line-height: 1;
}

.hero-count span {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.workspace {
  display: grid;
  gap: 1rem;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.45rem;
  height: 1.45rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  font-size: 0.78rem;
}

.content-panel {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.notice {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.75rem 0.9rem;
  border-radius: var(--kumo-radius-md);
  font-weight: 720;
}

.notice-error {
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.75rem;
  min-height: 16rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel svg {
  color: var(--kumo-bg-brand);
  font-size: 2.2rem;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(18rem, 1fr));
  gap: 1rem;
}

.user-card {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.user-main {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  min-width: 0;
  color: var(--kumo-text-default);
  text-decoration: none;
}

.avatar {
  width: 3.4rem;
  height: 3.4rem;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-main span:last-child {
  display: grid;
  min-width: 0;
  gap: 0.15rem;
}

.user-main strong {
  overflow: hidden;
  font-weight: 830;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-main small,
.user-main em {
  overflow: hidden;
  color: var(--kumo-text-muted);
  font-size: 0.86rem;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-list {
  display: grid;
  gap: 1rem;
}

.question-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: start;
  padding: 1rem;
}

.question-copy {
  display: grid;
  gap: 0.65rem;
  min-width: 0;
}

.question-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.question-header a {
  color: var(--kumo-text-default);
  font-size: 1.08rem;
  font-weight: 830;
  text-decoration: none;
}

.question-header span {
  flex: 0 0 auto;
  color: var(--kumo-text-subtle);
  font-size: 0.85rem;
  font-weight: 720;
}

.question-copy p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: var(--kumo-text-muted);
  font-size: 0.86rem;
  font-weight: 720;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 0.85rem;
}

.pagination span {
  color: var(--kumo-text-muted);
  font-weight: 760;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .question-card,
  .question-header,
  .pagination {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
