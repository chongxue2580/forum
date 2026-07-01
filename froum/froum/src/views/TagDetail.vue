<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ArticleList from '../components/ArticleList.vue'
import UiPageHero from '../components/ui/UiPageHero.vue'
import { articleService } from '../services/articleService'
import { questionService } from '../services/questionService'
import { tagService } from '../services/tagService'
import { formatDateTime } from '../utils/dateUtils'

const props = defineProps({
  tagId: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const tag = ref(null)
const activeTab = ref('articles')
const sortBy = ref('newest')
const loading = ref(false)
const articlesLoading = ref(false)
const questionsLoading = ref(false)
const articles = ref([])
const questions = ref([])
const articlePage = ref(1)
const questionPage = ref(1)
const pageSize = 10
const articleTotal = ref(0)
const questionTotal = ref(0)

const articleTotalPages = computed(() => Math.max(1, Math.ceil(articleTotal.value / pageSize)))
const questionTotalPages = computed(() => Math.max(1, Math.ceil(questionTotal.value / pageSize)))

const currentTagId = computed(() => Number(props.tagId))

const unwrapData = (response) => response?.data || response || {}

const loadTag = async () => {
  const response = await tagService.getTagById(currentTagId.value)
  tag.value = unwrapData(response)
}

const loadArticles = async () => {
  articlesLoading.value = true
  try {
    const response = await articleService.getArticles({
      tagId: currentTagId.value,
      page: articlePage.value,
      pageSize,
      sort: sortBy.value
    })
    articles.value = response.data || []
    articleTotal.value = response.total || 0
  } finally {
    articlesLoading.value = false
  }
}

const loadQuestions = async () => {
  questionsLoading.value = true
  try {
    const response = await questionService.getQuestions({
      tagId: currentTagId.value,
      page: questionPage.value,
      pageSize,
      sort: sortBy.value
    })
    questions.value = response.data || []
    questionTotal.value = response.total || 0
  } finally {
    questionsLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    await loadTag()
    await Promise.all([loadArticles(), loadQuestions()])
  } finally {
    loading.value = false
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
}

const changeSort = async (sort) => {
  sortBy.value = sort
  articlePage.value = 1
  questionPage.value = 1
  await Promise.all([loadArticles(), loadQuestions()])
}

const handleArticlePageChange = async (page) => {
  articlePage.value = page
  await loadArticles()
}

const handleQuestionPageChange = async (page) => {
  questionPage.value = page
  await loadQuestions()
}

const navigateToQuestion = (id) => {
  router.push(`/question/${id}`)
}

const formatQuestionTime = (time) => formatDateTime(time, {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
})

watch(() => props.tagId, () => {
  articlePage.value = 1
  questionPage.value = 1
  loadData()
})

onMounted(loadData)
</script>

<template>
  <div class="tag-detail-container">
    <div v-if="loading" class="state-panel kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>加载标签内容...</span>
    </div>

    <template v-else>
      <ui-page-hero
        :title="tag?.name || `标签 #${tagId}`"
        :description="tag?.description || '浏览该标签下的文章与问答内容。'"
      >
        <template #eyebrow>
          <span class="kumo-eyebrow">
            <font-awesome-icon :icon="['fas', 'tag']" />
            Tag
          </span>
        </template>
        <template #actions>
          <router-link to="/tags" class="kumo-button">全部标签</router-link>
        </template>
        <template #aside>
          <div class="hero-count">
            <strong>{{ articleTotal + questionTotal }}</strong>
            <span>关联内容</span>
          </div>
        </template>
      </ui-page-hero>

      <div class="toolbar kumo-surface">
        <div class="kumo-tabs" aria-label="内容类型">
          <button class="kumo-tab" :class="{ active: activeTab === 'articles' }" type="button" @click="switchTab('articles')">
            文章 <span>{{ articleTotal }}</span>
          </button>
          <button class="kumo-tab" :class="{ active: activeTab === 'questions' }" type="button" @click="switchTab('questions')">
            问答 <span>{{ questionTotal }}</span>
          </button>
        </div>
        <div class="kumo-tabs" aria-label="排序方式">
          <button class="kumo-tab" :class="{ active: sortBy === 'newest' }" type="button" @click="changeSort('newest')">最新</button>
          <button class="kumo-tab" :class="{ active: sortBy === 'hottest' }" type="button" @click="changeSort('hottest')">热门</button>
          <button class="kumo-tab" :class="{ active: sortBy === 'mostComments' }" type="button" @click="changeSort('mostComments')">评论</button>
          <button class="kumo-tab" :class="{ active: sortBy === 'mostLiked' }" type="button" @click="changeSort('mostLiked')">点赞</button>
        </div>
      </div>

      <section v-if="activeTab === 'articles'" class="content-section">
        <article-list
          :articles="articles"
          :loading="articlesLoading"
          :show-title="false"
          :current-page="articlePage"
          :total-pages="articleTotalPages"
          :total-items="articleTotal"
          :page-size="pageSize"
          @page-change="handleArticlePageChange"
        />
        <div v-if="!articlesLoading && articles.length === 0" class="state-panel kumo-surface">该标签下暂无文章</div>
      </section>

      <section v-else class="content-section">
        <div v-if="questionsLoading" class="state-panel kumo-surface">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>加载问答...</span>
        </div>
        <div v-else-if="questions.length === 0" class="state-panel kumo-surface">该标签下暂无问答</div>
        <div v-else class="question-list">
          <article
            v-for="(question, index) in questions"
            :key="question.id"
            class="question-item kumo-surface magnetic-card stagger-item"
            :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
            @click="navigateToQuestion(question.id)"
          >
            <div class="question-stats">
              <strong>{{ question.answerCount || 0 }}</strong>
              <span>回答</span>
            </div>
            <div class="question-main">
              <h2>
                <span v-if="question.solved" class="kumo-status kumo-status--success">已解决</span>
                {{ question.title }}
              </h2>
              <p>{{ question.summary || question.content }}</p>
              <div class="question-meta">
                <span>{{ question.author?.name || '匿名用户' }}</span>
                <span>{{ formatQuestionTime(question.createTime) }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="questionTotalPages > 1" class="pager kumo-surface">
          <button class="kumo-button" type="button" :disabled="questionPage === 1" @click="handleQuestionPageChange(questionPage - 1)">上一页</button>
          <span>{{ questionPage }} / {{ questionTotalPages }}</span>
          <button class="kumo-button" type="button" :disabled="questionPage === questionTotalPages" @click="handleQuestionPageChange(questionPage + 1)">下一页</button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.tag-detail-container {
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

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
}

.content-section {
  display: grid;
  gap: 1rem;
  min-height: 18rem;
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.8rem;
  min-height: 18rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel > svg {
  color: var(--kumo-bg-brand);
  font-size: 2.2rem;
}

.question-list {
  display: grid;
  gap: 1rem;
}

.question-item {
  display: grid;
  grid-template-columns: 5rem minmax(0, 1fr);
  gap: 1rem;
  padding: 1rem;
  cursor: pointer;
}

.question-stats {
  display: grid;
  place-items: center;
  gap: 0.15rem;
  padding: 0.8rem;
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.question-stats strong {
  color: var(--kumo-text-default);
  font-size: 1.55rem;
  font-weight: 900;
}

.question-main {
  display: grid;
  gap: 0.65rem;
  min-width: 0;
}

.question-main h2 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.18rem;
  font-weight: 840;
}

.question-main p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-meta,
.pager {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: var(--kumo-text-muted);
  font-size: 0.88rem;
  font-weight: 720;
}

.pager {
  justify-content: center;
  padding: 0.85rem;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .toolbar,
  .pager {
    align-items: stretch;
    flex-direction: column;
  }

  .question-item {
    grid-template-columns: 1fr;
  }
}
</style>
