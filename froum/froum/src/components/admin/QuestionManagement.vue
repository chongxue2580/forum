<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveQuestion,
  deleteQuestion as deleteQuestionApi,
  getAllQuestions,
  rejectQuestion,
  solveQuestion,
  unsolveQuestion
} from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const questions = ref([])
const viewMode = ref(localStorage.getItem('adminView_questions') || 'grid')
const setView = (v) => { viewMode.value = v; localStorage.setItem('adminView_questions', v) }
const searchQuery = ref('')
const filterOptions = ref({
  status: '',
  solved: ''
})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap = {
  approved: { text: '已通过', class: 'status-approved', type: 'success' },
  rejected: { text: '已拒绝', class: 'status-rejected', type: 'danger' },
  pending: { text: '审核中', class: 'status-pending', type: 'warning' }
}

const normalizePage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || page.total || 0
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = Array.isArray(value)
    ? new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0)
    : new Date(value)

  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const normalizeQuestion = (question) => {
  const authorName = question.author?.nickname || question.author?.username || question.author?.name || '未知作者'
  const tags = Array.isArray(question.tags)
    ? question.tags.map(tag => typeof tag === 'string' ? tag : tag.name).filter(Boolean)
    : []

  return {
    id: question.id,
    title: question.title || '',
    content: question.content || '',
    author: {
      id: question.author?.id,
      name: authorName
    },
    createTime: formatDateTime(question.createdAt || question.createTime),
    views: question.viewCount || question.views || 0,
    answers: question.commentCount || question.answerCount || question.answers || 0,
    solved: Boolean(question.isSolved ?? question.solved),
    status: String(question.status || 'PENDING').toLowerCase(),
    tags
  }
}

const filteredQuestions = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  let result = [...questions.value]

  if (keyword) {
    result = result.filter(question =>
      question.title.toLowerCase().includes(keyword) ||
      question.content.toLowerCase().includes(keyword) ||
      question.author.name.toLowerCase().includes(keyword) ||
      question.tags.some(tag => tag.toLowerCase().includes(keyword))
    )
  }

  if (filterOptions.value.solved !== '') {
    const isSolved = filterOptions.value.solved === 'true'
    result = result.filter(question => question.solved === isSolved)
  }

  return result
})

const getAvatarColor = (name) => {
  const colors = [
    '#596f8f',
    '#4f6f5a',
    '#8b6f3e',
    '#8a8175',
    '#9d4a3f'
  ]
  const safeName = name || '用户'
  const index = safeName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
  return colors[index]
}

const getAvatarText = (name) => {
  return (name || '用').charAt(0)
}

const loadQuestions = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterOptions.value.status) {
      params.status = filterOptions.value.status.toUpperCase()
    }

    const response = await getAllQuestions(params)
    const page = normalizePage(response)
    questions.value = page.content.map(normalizeQuestion)
    total.value = page.totalElements
  } catch (error) {
    questions.value = []
    total.value = 0
    ElMessage.error(error.message || '加载问答列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadQuestions()
}

const resetFilters = () => {
  searchQuery.value = ''
  filterOptions.value = {
    status: '',
    solved: ''
  }
  currentPage.value = 1
  loadQuestions()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadQuestions()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadQuestions()
}

const updateQuestionStatus = async (question, status) => {
  const statusText = {
    approved: '通过',
    rejected: '拒绝'
  }

  try {
    if (status === 'rejected') {
      const { value: reason } = await ElMessageBox.prompt(`请输入拒绝问题「${question.title}」的原因`, '拒绝问题', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea'
      })
      await rejectQuestion(question.id, reason)
    } else {
      await ElMessageBox.confirm(`确定要通过问题「${question.title}」的审核吗？`, '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      })
      await approveQuestion(question.id)
    }

    ElMessage.success(`已${statusText[status]}问题审核`)
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新问题状态失败')
    }
  }
}

const toggleQuestionSolved = async (question) => {
  try {
    await ElMessageBox.confirm(
      `确定要${question.solved ? '取消解决' : '标记解决'}问题「${question.title}」吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    if (question.solved) {
      await unsolveQuestion(question.id)
    } else {
      await solveQuestion(question.id)
    }

    ElMessage.success(`已${question.solved ? '取消解决' : '标记解决'}问题`)
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新解决状态失败')
    }
  }
}

const viewQuestion = (question) => {
  router.push(`/question/${question.id}`)
}

const deleteQuestion = async (question) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除问题「${question.title}」吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteQuestionApi(question.id)
    ElMessage.success('问题已删除')
    loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除问题失败')
    }
  }
}

onMounted(loadQuestions)
</script>

<template>
  <div class="admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="question-circle" />
        问答管理
      </h1>

      <div class="header-actions">
        <button class="admin-action-btn primary" @click="router.push('/question/new')">
          <font-awesome-icon icon="plus" />
          新建问答
        </button>
      </div>
    </div>

    <div class="admin-content-card">
      <div class="admin-section-title">
        <font-awesome-icon icon="filter" />
        筛选条件
      </div>
      <div class="admin-search-filter">
        <el-input
          v-model="searchQuery"
          placeholder="搜索问题标题、内容或作者..."
          clearable
          size="large"
          style="flex: 1 1 240px; min-width: 200px; max-width: 360px;"
        >
          <template #prefix>
            <font-awesome-icon icon="search" />
          </template>
        </el-input>

        <el-select v-model="filterOptions.status" placeholder="审核状态" clearable size="large" style="flex: 0 1 150px; min-width: 130px; max-width: 170px;">
          <el-option label="全部状态" value="" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="审核中" value="pending" />
        </el-select>

        <el-select v-model="filterOptions.solved" placeholder="解决状态" clearable size="large" style="flex: 0 1 150px; min-width: 130px; max-width: 170px;">
          <el-option label="全部问题" value="" />
          <el-option label="已解决" value="true" />
          <el-option label="未解决" value="false" />
        </el-select>

        <button class="admin-action-btn primary" @click="handleFilter">
          <font-awesome-icon icon="filter" />
          筛选
        </button>

        <button class="admin-action-btn" @click="resetFilters">
          <font-awesome-icon icon="sync" />
          重置
        </button>
      </div>
    </div>

    <div class="admin-content-card">
      <div class="admin-section-title ad-list-head">
        <span><font-awesome-icon icon="table" /> 问答列表</span>
        <span class="ad-view-toggle">
          <button :class="{ active: viewMode === 'grid' }" title="网格" @click="setView('grid')"><font-awesome-icon icon="th-large" /></button>
          <button :class="{ active: viewMode === 'list' }" title="列表" @click="setView('list')"><font-awesome-icon icon="list" /></button>
        </span>
      </div>
      <div v-loading="loading">
        <div v-if="filteredQuestions.length" class="ad-card-grid" :class="{ 'is-list': viewMode === 'list' }">
          <div v-for="row in filteredQuestions" :key="row.id" class="ad-card">
            <div class="ad-card__head">
              <span class="ad-card__avatar" :style="{ backgroundColor: getAvatarColor(row.author.name) }">{{ getAvatarText(row.author.name) }}</span>
              <div style="min-width:0;flex:1;">
                <div class="ad-card__title">{{ row.title }}</div>
                <div class="ad-card__sub">
                  <span>{{ row.author.name }}</span>
                  <span><font-awesome-icon icon="clock" /> {{ row.createTime }}</span>
                </div>
              </div>
            </div>

            <p class="question-desc">{{ row.content }}</p>

            <div class="ad-card__pills">
              <span class="ad-pill" :class="statusMap[row.status].type === 'success' ? 'is-success' : statusMap[row.status].type === 'danger' ? 'is-danger' : 'is-warning'">
                {{ statusMap[row.status].text }}
              </span>
              <span v-if="row.solved" class="ad-pill is-success"><font-awesome-icon icon="check-circle" /> 已解决</span>
              <span class="ad-pill is-muted"><font-awesome-icon icon="eye" /> {{ row.views }}</span>
              <span class="ad-pill is-muted"><font-awesome-icon icon="comment-dots" /> {{ row.answers }}</span>
              <span v-for="tag in row.tags.slice(0, 2)" :key="tag" class="ad-pill is-accent">{{ tag }}</span>
            </div>

            <div class="ad-card__actions">
              <button v-if="row.status === 'pending'" class="ad-btn is-success" @click="updateQuestionStatus(row, 'approved')">
                <font-awesome-icon icon="check" /> 通过
              </button>
              <button v-if="row.status === 'pending'" class="ad-btn is-danger" @click="updateQuestionStatus(row, 'rejected')">
                <font-awesome-icon icon="times" /> 拒绝
              </button>
              <button class="ad-btn" :class="{ 'is-success': !row.solved }" @click="toggleQuestionSolved(row)">
                <font-awesome-icon icon="check-circle" /> {{ row.solved ? '取消解决' : '标记解决' }}
              </button>
              <button class="ad-btn is-primary" @click="viewQuestion(row)">
                <font-awesome-icon icon="eye" /> 查看
              </button>
              <button class="ad-btn is-danger" @click="deleteQuestion(row)">
                <font-awesome-icon icon="trash-alt" /> 删除
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="ad-empty">
          <div class="ad-empty__icon"><font-awesome-icon icon="question-circle" /></div>
          <div class="ad-empty__text">没有符合条件的问答</div>
        </div>
      </div>
    </div>

    <div class="admin-pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        :current-page="currentPage"
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
/* 使用统一的admin样式 */
@import './AdminCommonStyles.css';

/* 问答管理特定样式 */
.question-desc {
  margin: 0;
  color: var(--ad-muted);
  font-size: 0.86rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.admin-table-cell {
  padding: 12px 0;
}

.admin-cell-main {
  margin-bottom: 8px;
}

.admin-cell-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ad-text);
  line-height: 1.4;
}

.admin-cell-desc {
  margin: 0;
  font-size: 12px;
  color: var(--ad-muted);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.admin-cell-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.admin-meta-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-author-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ad-muted);
}

.admin-time-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ad-soft);
}

.admin-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.admin-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ad-muted);
}

.admin-table-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.admin-action-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 响应式优化 */
@media (max-width: 1400px) {
  .admin-search-filter {
    flex-wrap: wrap;
    gap: 12px;
  }

  .admin-search-filter .el-input {
    min-width: 250px;
  }

  .admin-search-filter .el-select {
    min-width: 120px;
  }
}

@media (max-width: 768px) {
  .admin-page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .admin-search-filter {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-search-filter .el-input,
  .admin-search-filter .el-select {
    width: 100%;
    min-width: auto;
  }

  .admin-cell-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .admin-meta-left {
    width: 100%;
    justify-content: space-between;
  }

  .admin-tags {
    width: 100%;
  }
}
</style>
