<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/components/admin/AdminCommonStyles.css'
import { formatDateTime } from '@/utils/dateUtils'
import {
  approveArticle,
  deleteArticle,
  featureArticle,
  getAllArticles,
  pinArticle,
  rejectArticle,
  unfeatureArticle,
  unpinArticle
} from '@/api/admin'

const router = useRouter()

// 文章状态选项
const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已拒绝' }
]

// 文章类型选项
const typeOptions = [
  { value: 'all', label: '全部类型' },
  { value: 'normal', label: '普通文章' },
  { value: 'official', label: '官方文章' },
  { value: 'featured', label: '精选文章' }
]

const loading = ref(false)
const tableData = ref([])
const viewMode = ref(localStorage.getItem('adminView_articles') || 'grid')
const setView = (v) => { viewMode.value = v; localStorage.setItem('adminView_articles', v) }

// 筛选和搜索
const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const displayedTableData = computed(() => {
  let result = tableData.value
  const keyword = searchQuery.value.trim().toLowerCase()

  if (keyword) {
    result = result.filter(article => {
      return [
        article.title,
        article.author,
        article.category,
        ...(article.tags || [])
      ].some(value => String(value || '').toLowerCase().includes(keyword))
    })
  }

  if (typeFilter.value === 'official') {
    result = result.filter(article => article.isOfficial)
  } else if (typeFilter.value === 'featured') {
    result = result.filter(article => article.isFeatured)
  } else if (typeFilter.value === 'normal') {
    result = result.filter(article => !article.isOfficial && !article.isFeatured)
  }

  return result
})

// KPI（总数取后端 total，其余按当前页统计）
const kpis = computed(() => {
  const list = tableData.value
  const pending = list.filter(a => a.status === 'pending').length
  const approved = list.filter(a => a.status === 'approved').length
  const featured = list.filter(a => a.isFeatured).length
  return [
    { key: 'total', label: '文章总数', value: total.value, icon: 'file-alt', tone: '' },
    { key: 'pending', label: '待审核（本页）', value: pending, icon: 'clock', tone: 'is-warning' },
    { key: 'approved', label: '已通过（本页）', value: approved, icon: 'circle-check', tone: 'is-success' },
    { key: 'featured', label: '精选（本页）', value: featured, icon: 'star', tone: '' }
  ]
})

const normalizeArticle = (article) => ({
  id: article.id,
  title: article.title || '',
  author: article.author?.nickname || article.author?.username || article.author?.name || '未知作者',
  category: typeof article.category === 'string' ? article.category : article.category?.name || '未分类',
  tags: Array.isArray(article.tags) ? article.tags : [],
  status: String(article.status || '').toLowerCase(),
  createTime: formatDateTime(article.createdAt || article.createTime || article.publishedAt),
  updateTime: formatDateTime(article.updatedAt || article.updateTime),
  views: article.viewCount || article.views || 0,
  likes: article.likeCount || article.likes || 0,
  comments: article.commentCount || article.comments || 0,
  isOfficial: Boolean(article.isOfficial),
  isPinned: Boolean(article.isPinned || article.isTop),
  isFeatured: Boolean(article.isFeatured || article.isHighlight)
})

const unwrapPage = (response) => {
  const page = response?.data || response || {}
  return {
    content: Array.isArray(page.content) ? page.content : Array.isArray(page) ? page : [],
    totalElements: page.totalElements || 0
  }
}

const loadArticles = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (statusFilter.value !== 'all') {
      params.status = statusFilter.value.toUpperCase()
    }

    const response = await getAllArticles(params)
    const page = unwrapPage(response)
    tableData.value = page.content.map(normalizeArticle)
    total.value = page.totalElements
  } catch (error) {
    tableData.value = []
    total.value = 0
    ElMessage.error(error.message || '加载文章列表失败')
  } finally {
    loading.value = false
  }
}

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadArticles()
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadArticles()
}

const handleSearch = () => {
  currentPage.value = 1
  loadArticles()
}

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = 'all'
  typeFilter.value = 'all'
  currentPage.value = 1
  loadArticles()
}

// 处理审核操作
const handleApprove = async (row) => {
  try {
    await approveArticle(row.id)
    ElMessage.success('文章已审核通过')
    loadArticles()
  } catch (error) {
    ElMessage.error(error.message || '审核文章失败')
  }
}

const handleReject = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝文章', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    await rejectArticle(row.id, reason)
    ElMessage.success('文章已拒绝')
    loadArticles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '拒绝文章失败')
    }
  }
}

// 处理文章标记
const toggleOfficial = () => {
  ElMessage.warning('当前后端暂未提供官方文章设置接口')
}

const togglePinned = async (row) => {
  try {
    if (row.isPinned) {
      await unpinArticle(row.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinArticle(row.id)
      ElMessage.success('已设为置顶')
    }
    loadArticles()
  } catch (error) {
    ElMessage.error(error.message || '更新置顶状态失败')
  }
}

const toggleFeatured = async (row) => {
  try {
    if (row.isFeatured) {
      await unfeatureArticle(row.id)
      ElMessage.success('已取消精选')
    } else {
      await featureArticle(row.id)
      ElMessage.success('已设为精选')
    }
    loadArticles()
  } catch (error) {
    ElMessage.error(error.message || '更新精选状态失败')
  }
}

// 处理删除操作
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文章「${row.title}」吗？`, '删除文章', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteArticle(row.id)
    ElMessage.success('文章已删除')
    loadArticles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除文章失败')
    }
  }
}

// 处理查看详情
const handleViewDetail = (row) => {
  router.push(`/article/${row.id}`)
}

// 处理编辑操作
const handleEdit = (row) => {
  router.push(`/article/${row.id}/edit`)
}

// 格式化文章状态
const formatStatus = (status) => {
  switch (status) {
    case 'pending':
      return { text: '待审核', type: 'warning' }
    case 'approved':
      return { text: '已通过', type: 'success' }
    case 'rejected':
      return { text: '已拒绝', type: 'danger' }
    default:
      return { text: '未知', type: 'info' }
  }
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div class="admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="file-alt" />
        文章管理
      </h1>

      <div class="header-actions">
        <button class="admin-action-btn primary" @click="router.push('/article/new')">
          <font-awesome-icon icon="plus" />
          新建文章
        </button>
      </div>
    </div>

    <div class="ad-kpi-row">
      <div v-for="kpi in kpis" :key="kpi.key" class="ad-kpi">
        <span class="ad-kpi__icon" :class="kpi.tone">
          <font-awesome-icon :icon="kpi.icon" />
        </span>
        <div>
          <div class="ad-kpi__num">{{ kpi.value }}</div>
          <div class="ad-kpi__label">{{ kpi.label }}</div>
        </div>
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
          placeholder="搜索文章标题、作者或内容"
          clearable
          size="large"
          style="flex: 1 1 240px; min-width: 200px; max-width: 360px;"
        >
          <template #prefix>
            <font-awesome-icon icon="search" />
          </template>
        </el-input>

        <el-select v-model="statusFilter" placeholder="文章状态" size="large" style="flex: 0 1 150px; min-width: 130px; max-width: 170px;">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-select v-model="typeFilter" placeholder="文章类型" size="large" style="flex: 0 1 150px; min-width: 130px; max-width: 170px;">
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <button class="admin-action-btn primary" @click="handleSearch">
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
        <span><font-awesome-icon icon="table" /> 文章列表</span>
        <span class="ad-view-toggle">
          <button :class="{ active: viewMode === 'grid' }" title="网格" @click="setView('grid')"><font-awesome-icon icon="th-large" /></button>
          <button :class="{ active: viewMode === 'list' }" title="列表" @click="setView('list')"><font-awesome-icon icon="list" /></button>
        </span>
      </div>

      <div v-loading="loading">
        <div v-if="displayedTableData.length" class="ad-card-grid" :class="{ 'is-list': viewMode === 'list' }">
          <div v-for="row in displayedTableData" :key="row.id" class="ad-card">
            <div class="ad-card__head">
              <span class="ad-card__id">#{{ row.id }}</span>
              <div style="min-width:0;flex:1;">
                <div class="ad-card__title">{{ row.title }}</div>
                <div class="ad-card__sub">
                  <span><font-awesome-icon icon="user" /> {{ row.author }}</span>
                  <span v-if="row.category"><font-awesome-icon icon="folder" /> {{ row.category }}</span>
                  <span><font-awesome-icon icon="clock" /> {{ row.createTime }}</span>
                </div>
              </div>
            </div>

            <div class="ad-card__pills">
              <span class="ad-pill" :class="'is-' + (formatStatus(row.status).type === 'info' ? 'muted' : formatStatus(row.status).type === 'warning' ? 'warning' : formatStatus(row.status).type === 'danger' ? 'danger' : 'success')">
                {{ formatStatus(row.status).text }}
              </span>
              <span v-if="row.isOfficial" class="ad-pill is-success"><font-awesome-icon icon="shield-alt" /> 官方</span>
              <span v-if="row.isPinned" class="ad-pill is-warning"><font-awesome-icon icon="thumbtack" /> 置顶</span>
              <span v-if="row.isFeatured" class="ad-pill is-accent"><font-awesome-icon icon="star" /> 精选</span>
              <span v-for="(tag, i) in row.tags.slice(0, 3)" :key="i" class="ad-pill is-muted">{{ tag }}</span>
            </div>

            <div class="ad-card__metrics">
              <div class="ad-metric">
                <span class="ad-metric__num">{{ row.views }}</span>
                <span class="ad-metric__label">浏览</span>
              </div>
              <div class="ad-metric">
                <span class="ad-metric__num">{{ row.likes }}</span>
                <span class="ad-metric__label">点赞</span>
              </div>
              <div class="ad-metric">
                <span class="ad-metric__num">{{ row.comments }}</span>
                <span class="ad-metric__label">评论</span>
              </div>
            </div>

            <div class="ad-card__actions">
              <button v-if="row.status === 'pending'" class="ad-btn is-success" @click="handleApprove(row)">
                <font-awesome-icon icon="check" /> 通过
              </button>
              <button v-if="row.status === 'pending'" class="ad-btn is-danger" @click="handleReject(row)">
                <font-awesome-icon icon="times" /> 拒绝
              </button>
              <button class="ad-btn" @click="handleViewDetail(row)">
                <font-awesome-icon icon="eye" /> 查看
              </button>
              <button class="ad-btn is-primary" @click="handleEdit(row)">
                <font-awesome-icon icon="edit" /> 编辑
              </button>
              <button class="ad-btn is-warning" @click="togglePinned(row)">
                <font-awesome-icon icon="thumbtack" /> {{ row.isPinned ? '取消置顶' : '置顶' }}
              </button>
              <button class="ad-btn" @click="toggleFeatured(row)">
                <font-awesome-icon icon="star" /> {{ row.isFeatured ? '取消精选' : '精选' }}
              </button>
              <button class="ad-btn is-danger" @click="handleDelete(row)">
                <font-awesome-icon icon="trash-alt" /> 删除
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="ad-empty">
          <div class="ad-empty__icon"><font-awesome-icon icon="file-alt" /></div>
          <div class="ad-empty__text">没有符合条件的文章</div>
        </div>
      </div>
    </div>

    <div class="admin-pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
@import './AdminCommonStyles.css';

.header-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.article-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.5rem 0;
}

.article-title {
  font-weight: 600;
  color: var(--text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  background: linear-gradient(135deg, var(--text-color) 0%, var(--primary-color) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.article-flags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--ad-text-muted);
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  background: var(--ad-brand-soft);
  border-radius: var(--radius);
  border: 1px solid var(--ad-border);
}

.time-info {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--ad-text-muted);
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  background: var(--ad-brand-soft);
  border-radius: var(--radius);
  border: 1px solid var(--ad-border);
}

.tag-container {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.stats {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--ad-text-muted);
  font-size: 0.875rem;
  padding: 0.375rem 0.75rem;
  background: var(--ad-surface-muted);
  border-radius: var(--radius);
  border: 1px solid var(--ad-border);
  transition: all 0.2s ease;
}

.stat-item:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-sm);
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.button-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 0.75rem;
  }

  .article-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .meta-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .action-buttons {
    width: 100%;
  }

  .button-row {
    flex-direction: column;
  }
}

@media (max-width: 576px) {
  .stats {
    flex-direction: column;
    gap: 0.5rem;
  }

  .tag-container {
    flex-direction: column;
    gap: 0.25rem;
  }
}
</style>
