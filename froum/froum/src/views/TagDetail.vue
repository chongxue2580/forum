<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ArticleList from '../components/ArticleList.vue'
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
    <div v-if="loading" class="state-panel">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>加载标签内容...</span>
    </div>

    <template v-else>
      <header class="tag-header">
        <div>
          <h1>
            <font-awesome-icon :icon="['fas', 'tag']" />
            {{ tag?.name || `标签 #${tagId}` }}
          </h1>
          <p v-if="tag?.description">{{ tag.description }}</p>
        </div>
        <router-link to="/tags" class="back-link">全部标签</router-link>
      </header>

      <div class="toolbar">
        <div class="tabs">
          <button :class="{ active: activeTab === 'articles' }" @click="switchTab('articles')">
            文章 {{ articleTotal }}
          </button>
          <button :class="{ active: activeTab === 'questions' }" @click="switchTab('questions')">
            问答 {{ questionTotal }}
          </button>
        </div>
        <div class="sorts">
          <button :class="{ active: sortBy === 'newest' }" @click="changeSort('newest')">最新</button>
          <button :class="{ active: sortBy === 'hottest' }" @click="changeSort('hottest')">热门</button>
          <button :class="{ active: sortBy === 'mostComments' }" @click="changeSort('mostComments')">评论</button>
          <button :class="{ active: sortBy === 'mostLiked' }" @click="changeSort('mostLiked')">点赞</button>
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
        <div v-if="!articlesLoading && articles.length === 0" class="state-panel">该标签下暂无文章</div>
      </section>

      <section v-else class="content-section">
        <div v-if="questionsLoading" class="state-panel">
          <font-awesome-icon :icon="['fas', 'spinner']" spin />
          <span>加载问答...</span>
        </div>
        <div v-else-if="questions.length === 0" class="state-panel">该标签下暂无问答</div>
        <div v-else class="question-list">
          <article
            v-for="question in questions"
            :key="question.id"
            class="question-item"
            @click="navigateToQuestion(question.id)"
          >
            <div class="question-stats">
              <strong>{{ question.answerCount }}</strong>
              <span>回答</span>
            </div>
            <div class="question-main">
              <h2>
                <span v-if="question.solved" class="solved-badge">已解决</span>
                {{ question.title }}
              </h2>
              <p>{{ question.summary }}</p>
              <div class="question-meta">
                <span>{{ question.author?.name || '匿名用户' }}</span>
                <span>{{ formatQuestionTime(question.createTime) }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-if="questionTotalPages > 1" class="pager">
          <button :disabled="questionPage === 1" @click="handleQuestionPageChange(questionPage - 1)">上一页</button>
          <span>{{ questionPage }} / {{ questionTotalPages }}</span>
          <button :disabled="questionPage === questionTotalPages" @click="handleQuestionPageChange(questionPage + 1)">下一页</button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.tag-detail-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
}

.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid #e5e7eb;
}

.tag-header h1 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #111827;
  font-size: 28px;
}

.tag-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.back-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 600;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 0;
  flex-wrap: wrap;
}

.tabs,
.sorts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tabs button,
.sorts button,
.pager button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: pointer;
}

.tabs button.active,
.sorts button.active {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.08);
}

.content-section {
  min-height: 300px;
}

.state-panel {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #64748b;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
}

.question-stats {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.question-stats strong {
  color: #111827;
  font-size: 22px;
}

.question-main h2 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 18px;
}

.question-main p {
  margin: 0 0 10px;
  color: #64748b;
  line-height: 1.5;
}

.question-meta {
  display: flex;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.solved-badge {
  display: inline-flex;
  margin-right: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #dcfce7;
  color: #166534;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: center;
  gap: 12px;
  align-items: center;
  padding: 18px 0;
}

.pager button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

@media (max-width: 720px) {
  .tag-header,
  .toolbar {
    flex-direction: column;
  }

  .question-item {
    grid-template-columns: 1fr;
  }

  .question-stats {
    align-items: flex-start;
  }
}
</style>
