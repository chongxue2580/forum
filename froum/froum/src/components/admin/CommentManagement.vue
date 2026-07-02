<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/components/admin/AdminCommonStyles.css'
import {
  batchDeleteComments,
  deleteComment,
  getAllComments,
  getCommentStatistics,
  restoreComment
} from '@/api/admin'

const loading = ref(false)
const statsLoading = ref(false)
const comments = ref([])
const selectedRows = ref([])
const viewMode = ref(localStorage.getItem('adminView_comments') || 'grid')
const setView = (v) => { viewMode.value = v; localStorage.setItem('adminView_comments', v) }
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const filters = reactive({
  targetType: '',
  targetId: '',
  status: 'active',
  dateRange: []
})
const statistics = reactive({
  totalComments: 0,
  articleComments: 0,
  questionComments: 0
})

const targetTypeOptions = [
  { label: '文章评论', value: 'ARTICLE' },
  { label: '问答回答', value: 'QUESTION' }
]

const statusOptions = [
  { label: '正常', value: 'active' },
  { label: '已删除', value: 'deleted' },
  { label: '全部', value: 'all' }
]

const selectedIds = computed(() => selectedRows.value.map(row => row.id))

const isSelected = (row) => selectedRows.value.some(r => r.id === row.id)
const toggleSelect = (row) => {
  selectedRows.value = isSelected(row)
    ? selectedRows.value.filter(r => r.id !== row.id)
    : [...selectedRows.value, row]
}

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || page.total || 0
  }
}

const unwrapData = (response) => response?.data || response || {}

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

const normalizeComment = (comment) => ({
  id: comment.id,
  content: comment.content || '',
  targetType: comment.targetType || (comment.articleId ? 'ARTICLE' : '-'),
  targetId: comment.targetId || comment.articleId,
  author: comment.user?.nickname || comment.user?.username || '未知用户',
  authorId: comment.user?.id,
  parentId: comment.parentId,
  likeCount: comment.likeCount || 0,
  isBestAnswer: Boolean(comment.isBestAnswer),
  isDeleted: Boolean(comment.isDeleted),
  createdAt: formatDateTime(comment.createdAt),
  updatedAt: formatDateTime(comment.updatedAt)
})

const getTargetTypeText = (type) => {
  if (type === 'ARTICLE') return '文章'
  if (type === 'QUESTION') return '问答'
  return type || '-'
}

const loadStatistics = async () => {
  statsLoading.value = true
  try {
    const data = unwrapData(await getCommentStatistics())
    statistics.totalComments = data.totalComments || data.total || 0
    statistics.articleComments = data.articleComments || 0
    statistics.questionComments = data.questionComments || 0
  } catch (error) {
    ElMessage.error(error.message || '加载评论统计失败')
  } finally {
    statsLoading.value = false
  }
}

const loadComments = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchQuery.value.trim()) params.keyword = searchQuery.value.trim()
    if (filters.targetType) params.targetType = filters.targetType
    if (filters.status) params.status = filters.status
    if (filters.targetId) params.targetId = Number(filters.targetId)
    if (filters.dateRange?.length === 2) {
      params.startDate = filters.dateRange[0]
      params.endDate = filters.dateRange[1]
    }

    const page = unwrapPage(await getAllComments(params))
    comments.value = page.content.map(normalizeComment)
    total.value = page.totalElements
    selectedRows.value = []
  } catch (error) {
    comments.value = []
    total.value = 0
    ElMessage.error(error.message || '加载评论列表失败')
  } finally {
    loading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadComments(), loadStatistics()])
}

const handleSearch = () => {
  currentPage.value = 1
  loadComments()
}

const resetFilters = () => {
  searchQuery.value = ''
  filters.targetType = ''
  filters.targetId = ''
  filters.status = 'active'
  filters.dateRange = []
  currentPage.value = 1
  loadComments()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadComments()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadComments()
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除评论 #${row.id} 吗？`, '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(row.id)
    ElMessage.success('评论已删除')
    refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除评论失败')
    }
  }
}

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要恢复评论 #${row.id} 吗？`, '恢复评论', {
      confirmButtonText: '恢复',
      cancelButtonText: '取消',
      type: 'info'
    })
    await restoreComment(row.id)
    ElMessage.success('评论已恢复')
    refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '恢复评论失败')
    }
  }
}

const handleBatchDelete = async () => {  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要删除的评论')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条评论吗？`, '批量删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await batchDeleteComments(selectedIds.value)
    ElMessage.success('已批量删除评论')
    refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除评论失败')
    }
  }
}

onMounted(refreshAll)
</script>

<template>
  <div class="comment-management admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="comment" />
        评论管理
      </h1>
      <div class="header-actions">
        <el-button :loading="loading" @click="refreshAll">
          <font-awesome-icon icon="sync" /> 刷新
        </el-button>
        <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
          <font-awesome-icon icon="trash-alt" /> 批量删除
        </el-button>
      </div>
    </div>

    <div class="stat-grid" v-loading="statsLoading">
      <div class="stat-item">
        <span class="stat-label">评论总数</span>
        <strong>{{ statistics.totalComments }}</strong>
      </div>
      <div class="stat-item">
        <span class="stat-label">文章评论</span>
        <strong>{{ statistics.articleComments }}</strong>
      </div>
      <div class="stat-item">
        <span class="stat-label">问答回答</span>
        <strong>{{ statistics.questionComments }}</strong>
      </div>
    </div>

    <div class="filter-panel admin-content-card">
      <el-input
        v-model="searchQuery"
        placeholder="搜索评论内容或作者"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <font-awesome-icon icon="search" />
        </template>
      </el-input>

      <el-select v-model="filters.targetType" placeholder="目标类型" clearable class="filter-select" @change="handleSearch">
        <el-option v-for="option in targetTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>

      <el-select v-model="filters.status" placeholder="状态" class="filter-select" @change="handleSearch">
        <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>

      <el-input
        v-model="filters.targetId"
        placeholder="目标ID"
        clearable
        class="id-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />

      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="date-picker"
        @change="handleSearch"
      />

      <el-button type="primary" @click="handleSearch">
        <font-awesome-icon icon="search" /> 查询
      </el-button>
      <el-button @click="resetFilters">
        <font-awesome-icon icon="undo" /> 重置
      </el-button>
    </div>

    <div class="ad-list-head" style="margin-bottom:14px;">
      <div class="admin-section-title" style="margin:0;">
        <font-awesome-icon icon="comment" /> 评论列表
      </div>
      <span class="ad-view-toggle">
        <button :class="{ active: viewMode === 'grid' }" title="网格" @click="setView('grid')"><font-awesome-icon icon="th-large" /></button>
        <button :class="{ active: viewMode === 'list' }" title="列表" @click="setView('list')"><font-awesome-icon icon="list" /></button>
      </span>
    </div>

    <div v-loading="loading">
      <div v-if="comments.length" class="ad-card-grid" :class="{ 'is-list': viewMode === 'list' }">
        <div
          v-for="row in comments"
          :key="row.id"
          class="ad-card"
          :class="{ 'is-selected': isSelected(row) }"
        >
          <el-checkbox class="ad-checkbox" :model-value="isSelected(row)" @change="toggleSelect(row)" />

          <div class="ad-card__head">
            <span class="ad-card__id">#{{ row.id }}</span>
            <div style="min-width:0;flex:1;">
              <div class="comment-text">{{ row.content }}</div>
              <div class="ad-card__sub">
                <span><font-awesome-icon icon="user" /> {{ row.author }}</span>
                <span v-if="row.parentId"><font-awesome-icon icon="reply" /> 回复 #{{ row.parentId }}</span>
              </div>
            </div>
          </div>

          <div class="ad-card__pills">
            <span class="ad-pill is-muted">{{ getTargetTypeText(row.targetType) }} #{{ row.targetId || '-' }}</span>
            <span class="ad-pill is-accent"><font-awesome-icon icon="thumbs-up" /> {{ row.likeCount }}</span>
            <span v-if="row.isBestAnswer" class="ad-pill is-success">最佳答案</span>
            <span v-if="row.isDeleted" class="ad-pill is-danger">已删除</span>
          </div>

          <div class="ad-card__sub" style="margin-top:auto;">
            <span><font-awesome-icon icon="clock" /> {{ row.createdAt }}</span>
          </div>

          <div class="ad-card__actions ad-card__actions--compact">
            <button
              v-if="row.isDeleted"
              class="ad-icon-action is-restore"
              title="恢复评论"
              aria-label="恢复评论"
              @click="handleRestore(row)"
            >
              <font-awesome-icon icon="undo" />
            </button>
            <button
              v-else
              class="ad-icon-action is-delete"
              title="删除评论"
              aria-label="删除评论"
              @click="handleDelete(row)"
            >
              <font-awesome-icon icon="trash-alt" />
            </button>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="ad-empty">
        <div class="ad-empty__icon"><font-awesome-icon icon="comment" /></div>
        <div class="ad-empty__text">没有符合条件的评论</div>
      </div>
    </div>

    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.comment-management {
  color: var(--ad-text);
}

.header-actions,
.filter-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.comment-text {
  color: var(--ad-text);
  line-height: 1.5;
  font-size: 0.92rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.stat-item {
  border: 1px solid var(--ad-border);
  border-radius: 22px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--ad-surface);
  box-shadow: var(--ad-shadow);
  backdrop-filter: blur(20px);
}

.stat-label {
  color: var(--ad-muted);
  font-size: 13px;
  font-weight: 700;
}

.stat-item strong {
  color: var(--ad-text);
  font-size: 26px;
  font-weight: 820;
}

.filter-panel {
  margin-bottom: 16px;
  padding: 16px;
}

.search-input {
  flex: 2 1 220px;
  min-width: 180px;
  max-width: 320px;
}

.filter-select {
  flex: 1 1 130px;
  min-width: 120px;
  max-width: 160px;
}

.id-input {
  flex: 1 1 110px;
  min-width: 96px;
  max-width: 140px;
}

.date-picker {
  flex: 1 1 240px;
  min-width: 220px;
  max-width: 280px;
}

.comment-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-content {
  color: var(--ad-text);
  line-height: 1.5;
  white-space: pre-wrap;
}

.comment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--ad-muted);
  font-size: 13px;
}

.target-id {
  margin-left: 8px;
  color: var(--ad-muted);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .search-input,
  .filter-select,
  .id-input,
  .date-picker {
    width: 100%;
    max-width: none;
  }
}

/* Apple 浅色玻璃：输入/按钮打磨 */
.filter-panel :deep(.el-input__wrapper),
.filter-panel :deep(.el-select__wrapper) {
  border-radius: 14px;
  background: var(--ad-surface-muted);
  box-shadow: inset 0 0 0 1px var(--ad-border);
}

.filter-panel :deep(.el-input__wrapper.is-focus),
.filter-panel :deep(.el-select__wrapper.is-focused) {
  background: var(--ad-surface);
  box-shadow: inset 0 0 0 1px var(--ad-ink), 0 0 0 4px rgba(23, 23, 23, 0.06);
}

.comment-management :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

</style>
