<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/components/admin/AdminCommonStyles.css'
import {
  batchDeleteComments,
  deleteComment,
  getAllComments,
  getCommentStatistics
} from '@/api/admin'

const loading = ref(false)
const statsLoading = ref(false)
const comments = ref([])
const selectedRows = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const filters = reactive({
  targetType: '',
  targetId: '',
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

const selectedIds = computed(() => selectedRows.value.map(row => row.id))

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

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) {
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

    <div class="admin-table-container">
      <el-table
        :data="comments"
        v-loading="loading"
        style="width: 100%"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="评论内容" min-width="320">
          <template #default="{ row }">
            <div class="comment-cell">
              <div class="comment-content">{{ row.content }}</div>
              <div class="comment-meta">
                <span>作者：{{ row.author }}</span>
                <span v-if="row.parentId">回复 #{{ row.parentId }}</span>
                <el-tag v-if="row.isBestAnswer" type="success" size="small">最佳答案</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="目标" min-width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ getTargetTypeText(row.targetType) }}</el-tag>
            <span class="target-id">#{{ row.targetId || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="点赞" width="90" align="center">
          <template #default="{ row }">{{ row.likeCount }}</template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" plain @click="handleDelete(row)">
              <font-awesome-icon icon="trash-alt" /> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
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
  background: #f8fafc;
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
  gap: 12px;
  margin-bottom: 16px;
}

.stat-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-item strong {
  color: #111827;
  font-size: 24px;
}

.filter-panel {
  margin-bottom: 16px;
  padding: 16px;
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 140px;
}

.id-input {
  width: 120px;
}

.date-picker {
  width: 300px;
}

.comment-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-content {
  color: #111827;
  line-height: 1.5;
  white-space: pre-wrap;
}

.comment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: #64748b;
  font-size: 13px;
}

.target-id {
  margin-left: 8px;
  color: #64748b;
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
  }
}
</style>
